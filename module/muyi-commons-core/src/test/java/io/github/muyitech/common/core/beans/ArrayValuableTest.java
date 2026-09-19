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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.github.muyitech.common.core.enums.DeletedEnum;
import io.github.muyitech.common.core.enums.StatusEnum;
import org.junit.jupiter.api.Test;

/**
 * {@link ArrayValuable} 接口行为测试。
 */
class ArrayValuableTest {

    @Test
    void array_returnsAllEnumValues() {
        assertArrayEquals(new Integer[] {1, 0}, DeletedEnum.YES.array());
        assertArrayEquals(new Integer[] {0, 1}, StatusEnum.ENABLE.array());
    }
}
