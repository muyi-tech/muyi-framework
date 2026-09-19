/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.muyitech.systemtest;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;

/**
 * 部署系统测试支撑工具：以真实 JVM 进程（{@code java -jar}）启动示例应用的可执行 jar，
 * 提供空闲端口分配、HTTP 探活、请求执行与进程优雅停止。
 *
 * <p>
 * 可执行 jar 由 {@code maven-dependency-plugin} 从本地仓库复制到
 * {@code target/system-test-jars/}（可用 {@code -Dsystem-test.jars.dir} 覆盖）。
 *
 * @author keep simple
 * @since 2026/9/14
 */
final class JarDeploymentSupport {

    /** 可执行 jar 所在目录 */
    static final Path JARS_DIR =
            Path.of(System.getProperty("system-test.jars.dir", "target/system-test-jars"));

    /** 应用进程日志目录 */
    static final Path LOGS_DIR = Path.of("target/system-test-logs");

    private static final Duration READY_TIMEOUT = Duration.ofSeconds(60);

    private static final long READY_POLL_MILLIS = 500L;

    private JarDeploymentSupport() {}

    /**
     * 分配一个当前空闲的 TCP 端口（先释放后使用，存在极小的竞争窗口，可接受）。
     * @return 空闲端口号
     * @throws IOException 分配失败
     */
    static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * 以 {@code java -jar} 启动可执行 jar，通过 {@code --server.port} 注入指定端口，
     * 进程日志重定向到 {@code target/system-test-logs/<appName>.log}。
     * @param jarNamePrefix 可执行 jar 文件名前缀（不含版本号）
     * @param port 监听端口
     * @param appName 应用名（用于日志文件命名）
     * @return 应用进程
     * @throws IOException 进程启动失败
     */
    static Process startJar(String jarNamePrefix, int port, String appName) throws IOException {
        Files.createDirectories(LOGS_DIR);
        Path jar = locateJar(jarNamePrefix);
        Path logFile = LOGS_DIR.resolve(appName + ".log");
        return new ProcessBuilder(
                        "java", "-jar", jar.toAbsolutePath().toString(), "--server.port=" + port)
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile())
                .start();
    }

    /**
     * 轮询探活，直到探活端点返回 HTTP 200 或超时；超时失败时输出应用日志尾部辅助定位。
     * @param client HTTP 客户端
     * @param probeUrl 探活 URL
     * @param logFile 应用日志文件
     * @throws InterruptedException 轮询等待被中断
     */
    static void awaitReady(HttpClient client, String probeUrl, Path logFile)
            throws InterruptedException {
        long deadline = System.nanoTime() + READY_TIMEOUT.toNanos();
        StringBuilder lastError = new StringBuilder();
        while (System.nanoTime() < deadline) {
            try {
                HttpResponse<String> response = get(client, probeUrl);
                if (response.statusCode() == 200) {
                    return;
                }
                lastError.append("HTTP ").append(response.statusCode()).append("; ");
            } catch (RuntimeException ex) {
                lastError.append(ex.getClass().getSimpleName()).append("; ");
            }
            Thread.sleep(READY_POLL_MILLIS);
        }
        Assertions.fail(
                "应用 "
                        + READY_TIMEOUT.getSeconds()
                        + "s 内未就绪: "
                        + probeUrl
                        + " — "
                        + lastError
                        + "\n--- 应用日志尾部 ---\n"
                        + logTail(logFile));
    }

    /**
     * 执行 HTTP GET 请求。
     * @param client HTTP 客户端
     * @param url 目标 URL
     * @return 响应
     */
    static HttpResponse<String> get(HttpClient client, String url) {
        return get(client, url, Map.of());
    }

    /**
     * 执行带请求头的 HTTP GET 请求。
     * @param client HTTP 客户端
     * @param url 目标 URL
     * @param headers 请求头（名 → 值，可为空 Map）
     * @return 响应
     */
    static HttpResponse<String> get(HttpClient client, String url, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
        headers.forEach(builder::header);
        try {
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new IllegalStateException("HTTP GET 失败: " + url, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP GET 被中断: " + url, ex);
        }
    }

    /**
     * 优雅停止进程：先 destroy 等待 10s，未退出则 destroyForcibly。
     * @param process 应用进程
     */
    static void stop(Process process) {
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    private static Path locateJar(String jarNamePrefix) {
        try (Stream<Path> files = Files.list(JARS_DIR)) {
            return files.filter(
                            path -> {
                                String name = path.getFileName().toString();
                                return name.startsWith(jarNamePrefix) && name.endsWith(".jar");
                            })
                    .findFirst()
                    .orElseThrow(
                            () ->
                                    new IllegalStateException(
                                            "未找到前缀为 "
                                                    + jarNamePrefix
                                                    + " 的可执行 jar（"
                                                    + JARS_DIR.toAbsolutePath()
                                                    + "），请先构建示例应用：mvn clean install -f"
                                                    + " smoke-test/<smoke-test>/pom.xml"));
        } catch (IOException ex) {
            throw new IllegalStateException("读取目录失败: " + JARS_DIR.toAbsolutePath(), ex);
        }
    }

    private static String logTail(Path logFile) {
        if (!Files.isRegularFile(logFile)) {
            return "（日志文件不存在: " + logFile.toAbsolutePath() + "）";
        }
        try {
            var lines = Files.readAllLines(logFile);
            int from = Math.max(0, lines.size() - 40);
            return String.join("\n", lines.subList(from, lines.size()));
        } catch (IOException ex) {
            return "（读取日志失败: " + ex.getMessage() + "）";
        }
    }
}
