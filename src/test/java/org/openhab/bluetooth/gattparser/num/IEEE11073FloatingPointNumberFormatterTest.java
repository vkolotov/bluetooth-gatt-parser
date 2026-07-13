package org.openhab.bluetooth.gattparser.num;

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

import java.util.BitSet;

import org.junit.Test;
import org.openhab.bluetooth.gattparser.num.IEEE11073FloatingPointNumberFormatter;

import static org.junit.Assert.assertEquals;

public class IEEE11073FloatingPointNumberFormatterTest {

    private IEEE11073FloatingPointNumberFormatter formatter = new IEEE11073FloatingPointNumberFormatter();

    @Test
    public void testDeserializeSFloat() throws Exception {
        assertEquals(36.4,
                formatter.deserializeSFloat(BitSet.valueOf(new long[]{0b1111000101101100})), 0.00001);
        assertEquals(3.64,
                formatter.deserializeSFloat(BitSet.valueOf(new long[]{0b1110000101101100})), 0.00001);

        assertEquals(364,
                formatter.deserializeSFloat(BitSet.valueOf(new long[]{0b0000000101101100})), 0.00001);
        assertEquals(3640,
                formatter.deserializeSFloat(BitSet.valueOf(new long[]{0b0001000101101100})), 0.00001);

        assertEquals(-36.4,
                formatter.deserializeSFloat(BitSet.valueOf(new long[]{0b1111111010010100})), 0.00001);
        assertEquals(-364,
                formatter.deserializeSFloat(BitSet.valueOf(new long[]{0b0000111010010100})), 0.00001);
        assertEquals(-3640,
                formatter.deserializeSFloat(BitSet.valueOf(new long[]{0b0001111010010100})), 0.00001);

        assertEquals(Float.NaN,
                formatter.deserializeSFloat(BitSet.valueOf(new long[]{IEEE11073FloatingPointNumberFormatter.SFLOAT_NaN})), 0.0);
        assertEquals(Float.NEGATIVE_INFINITY,
                formatter.deserializeSFloat(BitSet.valueOf(new long[]{IEEE11073FloatingPointNumberFormatter.SFLOAT_NEGATIVE_INFINITY})), 0.0);
        assertEquals(Float.POSITIVE_INFINITY,
                formatter.deserializeSFloat(BitSet.valueOf(new long[]{IEEE11073FloatingPointNumberFormatter.SFLOAT_POSITIVE_INFINITY})), 0.0);
    }

    @Test
    public void testDeserializeFloat() throws Exception {
        assertEquals(36.4,
                formatter.deserializeFloat(BitSet.valueOf(new long[]{0b11111111000000000000000101101100})), 0.00001);
        assertEquals(3.64,
                formatter.deserializeFloat(BitSet.valueOf(new long[]{0b11111110000000000000000101101100})), 0.00001);

        assertEquals(364,
                formatter.deserializeFloat(BitSet.valueOf(new long[]{0b00000000000000000000000101101100})), 0.00001);
        assertEquals(3640,
                formatter.deserializeFloat(BitSet.valueOf(new long[]{0b00000001000000000000000101101100})), 0.00001);

        assertEquals(-36.4,
                formatter.deserializeFloat(BitSet.valueOf(new long[]{0b11111111111111111111111010010100})), 0.00001);
        assertEquals(-364,
                formatter.deserializeFloat(BitSet.valueOf(new long[]{0b00000000111111111111111010010100})), 0.00001);
        assertEquals(-3640,
                formatter.deserializeFloat(BitSet.valueOf(new long[]{0b00000001111111111111111010010100})), 0.00001);


        assertEquals(Float.NaN,
                formatter.deserializeFloat(BitSet.valueOf(new long[]{IEEE11073FloatingPointNumberFormatter.FLOAT_NaN})), 0.0);
        assertEquals(Float.NEGATIVE_INFINITY,
                formatter.deserializeFloat(BitSet.valueOf(new long[]{IEEE11073FloatingPointNumberFormatter.FLOAT_NEGATIVE_INFINITY})), 0.0);
        assertEquals(Float.POSITIVE_INFINITY,
                formatter.deserializeFloat(BitSet.valueOf(new long[]{IEEE11073FloatingPointNumberFormatter.FLOAT_POSITIVE_INFINITY})), 0.0);
    }

    @Test
    public void testDeserializeDouble() throws Exception {
        // No 64-bit IEEE-11073 form: deserializeDouble decodes the 32-bit FLOAT as a Double.
        assertEquals(36.4,
                formatter.deserializeDouble(BitSet.valueOf(new long[]{0b11111111000000000000000101101100})), 0.00001);
    }

    @Test
    public void testSerializeSFloat() {
        for (float v : new float[] {0.0F, 36.4F, -36.4F, 364F, -364F, 3.64F, 0.001F, -3640F}) {
            float back = formatter.deserializeSFloat(formatter.serializeSFloat(v));
            assertEquals("SFLOAT round-trip " + v, v, back, Math.max(0.001, Math.abs(v) * 0.001));
        }
    }

    @Test
    public void testSerializeSFloatSpecialValues() {
        assertEquals(Float.NaN, formatter.deserializeSFloat(formatter.serializeSFloat(Float.NaN)), 0.0);
        assertEquals(Float.POSITIVE_INFINITY,
                formatter.deserializeSFloat(formatter.serializeSFloat(Float.POSITIVE_INFINITY)), 0.0);
        assertEquals(Float.NEGATIVE_INFINITY,
                formatter.deserializeSFloat(formatter.serializeSFloat(Float.NEGATIVE_INFINITY)), 0.0);
    }

    @Test
    public void testSerializeFloat() {
        for (float v : new float[] {0.0F, 36.4F, -36.4F, 364F, -364F, 3.64F, 12345.678F, -3640F}) {
            float back = formatter.deserializeFloat(formatter.serializeFloat(v));
            assertEquals("FLOAT round-trip " + v, v, back, Math.max(0.001, Math.abs(v) * 0.001));
        }
    }

    @Test
    public void testSerializeDouble() {
        double back = formatter.deserializeDouble(formatter.serializeDouble(36.4));
        assertEquals(36.4, back, 0.001);
    }


}
