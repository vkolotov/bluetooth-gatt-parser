package org.openhab.bluetooth.gattparser.spec;

/*-
 * Copyright (C) 2017 Sputnik Dev
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.junit.Test;
import org.openhab.bluetooth.gattparser.spec.FieldFormat;
import org.openhab.bluetooth.gattparser.spec.FieldType;

import static org.junit.Assert.assertEquals;

public class FieldFormatTest {

    @Test
    public void testValueOf() throws Exception {
        assertFieldType("boolean", FieldType.BOOLEAN, 1, FieldFormat.valueOf("bOoLean"));
        assertFieldType("nibble", FieldType.UINT, 4, FieldFormat.valueOf("nIbblE"));
        assertFieldType("float32", FieldType.FLOAT_IEE754, 32, FieldFormat.valueOf("fLoat32"));
        assertFieldType("float64", FieldType.FLOAT_IEE754, 64, FieldFormat.valueOf("fLoAt64"));
        assertFieldType("SFLOAT", FieldType.FLOAT_IEE11073, 16, FieldFormat.valueOf("SFLOAT"));
        assertFieldType("FLOAT", FieldType.FLOAT_IEE11073, 32, FieldFormat.valueOf("FLOAT"));
        // GSS aliases for the IEEE-11073 floats.
        assertFieldType("medfloat16", FieldType.FLOAT_IEE11073, 16, FieldFormat.valueOf("meDFloat16"));
        assertFieldType("medfloat32", FieldType.FLOAT_IEE11073, 32, FieldFormat.valueOf("meDFloat32"));
        //assertFieldType("duint16", FieldType.UINT, 16, FieldFormat.valueOf("duint16"));
        assertFieldType("utf8s", FieldType.UTF8S, FieldFormat.FULL_SIZE, FieldFormat.valueOf("uTf8s"));
        assertFieldType("utf16s", FieldType.UTF16S, FieldFormat.FULL_SIZE, FieldFormat.valueOf("Utf16s"));

        assertFieldType("7bit", FieldType.UINT, 7, FieldFormat.valueOf("7Bit"));
        assertFieldType("700bit", FieldType.UINT, 700, FieldFormat.valueOf("700biT"));
        assertFieldType("uint10", FieldType.UINT, 10, FieldFormat.valueOf("uiNT10"));
        assertFieldType("uint3", FieldType.UINT, 3, FieldFormat.valueOf("uiNT3"));

        assertFieldType("sint3", FieldType.SINT, 3, FieldFormat.valueOf("siNT3"));
        assertFieldType("sint65", FieldType.SINT, 65, FieldFormat.valueOf("siNT65"));
    }

    @Test(expected = IllegalStateException.class)
    public void testValueOfInvalidFormat() {
        FieldFormat.valueOf("siNTunknown");
    }

    @Test
    public void testIsReal() {
        assertIsReal(false, "sfloat", "float", "float32", "float64", "boolean", "utf8s", "utf16s", "struct");
        assertIsReal(true, "nibble", "uint2", "uint64", "sint2");
    }

    @Test
    public void testIsDecimal() {
        assertIsDecimal(false, "nibble", "uint2", "uint64", "sint2", "boolean", "utf8s", "utf16s", "struct");
        assertIsDecimal(true, "sfloat", "float", "float32", "float64");
    }

    @Test
    public void testIsBoolean() {
        assertIsBoolean(false, "nibble", "uint2", "uint64", "sint2", "utf8s", "utf16s", "struct", "sfloat",
                "float", "float32", "float64");
        assertIsBoolean(true, "boolean");
    }

    @Test
    public void testIsNumber() {
        assertIsNumber(false, "boolean", "utf8s", "utf16s", "struct");
        assertIsNumber(true, "nibble", "uint2", "uint64", "sint2", "sfloat", "float", "float32", "float64");
    }

    @Test
    public void testIsString() {
        assertIsString(false, "nibble", "uint2", "uint64", "sint2", "sfloat", "float", "float32", "float64",
                "boolean", "struct");
        assertIsString(true, "utf8s", "utf16s");
    }

    @Test
    public void testIsStruct() {
        assertIsStruct(false, "nibble", "uint2", "uint64", "sint2", "sfloat", "float", "float32", "float64",
                "boolean", "utf8s", "utf16s");
        assertIsStruct(true, "struct");
    }

    private void assertFieldType(String name, FieldType fieldType, int size, FieldFormat actual) {
        assertEquals(name, actual.getName());
        assertEquals(fieldType, actual.getType());
        assertEquals(size, actual.getSize());
    }

    private void assertIsReal(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.valueOf(format).isReal());
        }
    }

    private void assertIsDecimal(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.valueOf(format).isDecimal());
        }
    }

    private void assertIsBoolean(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.valueOf(format).isBoolean());
        }
    }

    private void assertIsNumber(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.valueOf(format).isNumber());
        }
    }

    private void assertIsString(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.valueOf(format).isString());
        }
    }

    private void assertIsStruct(boolean expected, String... formats) {
        for (String format : formats) {
            assertEquals(expected, FieldFormat.valueOf(format).isStruct());
        }
    }

}
