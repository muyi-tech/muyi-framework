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
package io.github.muyitech.common.core.beans;

import java.io.Serializable;
import java.util.Objects;

/**
 * 通用键值对容器。
 *
 * <p>
 * 以 K/V 两个泛型字段存储一组键值数据，支持序列化，可用于方法返回值、批量参数传递等场景。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author keep simple
 * @since 2025/4/7
 */
public class KeyValue<K, V> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 键
     */
    private K key;

    /**
     * 值
     */
    private V value;

    /**
     * 空构造方法。
     */
    public KeyValue() {}

    /**
     * 全参构造方法。
     * @param key 键
     * @param value 值
     */
    public KeyValue(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * 获取键.
     * @return 键
     */
    public K getKey() {
        return key;
    }

    /**
     * 设置键.
     * @param key 键
     */
    public void setKey(K key) {
        this.key = key;
    }

    /**
     * 获取值.
     * @return 值
     */
    public V getValue() {
        return value;
    }

    /**
     * 设置值.
     * @param value 值
     */
    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyValue)) {
            return false;
        }
        KeyValue<?, ?> that = (KeyValue<?, ?>) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "KeyValue(key=" + key + ", value=" + value + ")";
    }
}
