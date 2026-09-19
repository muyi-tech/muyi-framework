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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.junit.jupiter.api.Test;

/**
 * {@link KeyValue} 单元测试。
 */
class KeyValueTest {

    @Test
    void noArgsConstructor_createsNullFields() {
        KeyValue<String, Integer> kv = new KeyValue<>();
        assertNull(kv.getKey());
        assertNull(kv.getValue());
    }

    @Test
    void allArgsConstructor_setsFields() {
        KeyValue<String, Integer> kv = new KeyValue<>("age", 18);
        assertEquals("age", kv.getKey());
        assertEquals(18, kv.getValue());
    }

    @Test
    void setters_updateFields() {
        KeyValue<String, Integer> kv = new KeyValue<>();
        kv.setKey("name");
        kv.setValue(1);
        assertEquals("name", kv.getKey());
        assertEquals(1, kv.getValue());
    }

    @Test
    void equalsAndHashCode_consistent() {
        KeyValue<String, Integer> a = new KeyValue<>("k", 1);
        KeyValue<String, Integer> b = new KeyValue<>("k", 1);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void serializable_roundTrip() throws Exception {
        KeyValue<String, Integer> original = new KeyValue<>("age", 18);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        try (ObjectInputStream in =
                new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            @SuppressWarnings("unchecked")
            KeyValue<String, Integer> restored = (KeyValue<String, Integer>) in.readObject();
            assertEquals(original, restored);
        }
    }

    @Test
    void implementsSerializable() {
        assertTrue(Serializable.class.isAssignableFrom(KeyValue.class));
    }

    @Test
    void equals_sameInstanceNullAndDifferentType() {
        KeyValue<String, Integer> kv = new KeyValue<>("k", 1);
        assertEquals(kv, kv);
        assertNotEquals(kv, null);
        assertNotEquals(kv, new Object());
    }

    @Test
    void equals_differentKeyOrValue_false() {
        KeyValue<String, Integer> base = new KeyValue<>("k", 1);
        assertNotEquals(base, new KeyValue<>("other", 1));
        assertNotEquals(base, new KeyValue<>("k", 2));
    }

    @Test
    void equals_nullFields_symmetric() {
        KeyValue<String, Integer> a = new KeyValue<>();
        KeyValue<String, Integer> b = new KeyValue<>();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void toString_containsKeyAndValue() {
        KeyValue<String, Integer> kv = new KeyValue<>("age", 18);
        String text = kv.toString();
        assertTrue(text.contains("age"));
        assertTrue(text.contains("18"));
    }
}
