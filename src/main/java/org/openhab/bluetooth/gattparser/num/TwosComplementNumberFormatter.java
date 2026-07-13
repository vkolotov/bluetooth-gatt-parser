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

import java.math.BigInteger;
import java.util.BitSet;

/**
 * Two's complement &amp; little-endian number formatter.
 * Stateless and threadsafe.
 */
public class TwosComplementNumberFormatter implements RealNumberFormatter {

    private final static int BIG_INTEGER_MAX_SIZE = 20 * 8;

    @Override
    public Integer deserializeInteger(BitSet bits, int size, boolean signed) {
        if (size > 32) {
            throw new IllegalArgumentException("size must be less or equal 32");
        }
        return (int) deserialize(bits, size, signed);
    }

    @Override
    public Long deserializeLong(BitSet bits, int size, boolean signed) {
        if (size > 64) {
            throw new IllegalArgumentException("size must be less or equal than 64");
        }
        return deserialize(bits, size, signed);
    }

    /**
     * Reads up to 64 bits, little-endian, into a long and sign-extends when required. The BitSet
     * already holds the bits little-endian (bit 0 = LSB), so a straight accumulation of the low
     * {@code size} bits yields the unsigned magnitude; a set top bit under two's-complement is then
     * extended into the unused high bits.
     */
    private long deserialize(BitSet bits, int size, boolean signed) {
        if (size == 1) {
            signed = false;
        }
        long value = 0L;
        int limit = Math.min(size, bits.length());
        for (int i = 0; i < limit; i++) {
            if (bits.get(i)) {
                value |= 1L << i;
            }
        }
        if (signed && size > 1 && size < 64 && (value & (1L << (size - 1))) != 0) {
            value |= -(1L << size); // sign-extend the two's-complement top bit
        }
        return value;
    }

    @Override
    public BigInteger deserializeBigInteger(BitSet bits, int size, boolean signed) {
        if (size == 1) {
            signed = false;
        }

        boolean isNegative = signed && size > 1 && bits.get(size - 1);
        BigInteger value = isNegative ? BigInteger.ONE.negate() : BigInteger.ZERO;
        for (int i = 0; i < bits.length() && i < size; i++) {
            if (isNegative && !bits.get(i)) {
                value = value.clearBit(i);
            } else if (!isNegative && bits.get(i)) {
                value = value.setBit(i);
            }
        }
        return value;
    }

    @Override
    public BitSet serialize(Integer number, int size, boolean signed) {
        if (size == 1) {
            signed = false;
        }
        int length = Math.min(size, Integer.SIZE);
        BitSet bitSet = BitSet.valueOf(new long[] { number }).get(0, length);
        if (signed && number < 0) {
            bitSet.set(length - 1);
        }
        return bitSet;
    }

    @Override
    public BitSet serialize(Long number, int size, boolean signed) {
        if (size == 1) {
            signed = false;
        }
        int length = Math.min(size, Long.SIZE);
        BitSet bitSet = BitSet.valueOf(new long[] { number }).get(0, length);
        if (signed && number < 0) {
            bitSet.set(length - 1);
        }
        return bitSet;
    }

    @Override
    public BitSet serialize(BigInteger number, int size, boolean signed) {
        if (size == 1) {
            signed = false;
        }
        BitSet bitSet = new BitSet(size);

        int length = Math.min(size, BIG_INTEGER_MAX_SIZE);
        for (int i = 0; i < length - (signed ? 1 : 0); i++) {
            if (number.testBit(i)) {
                bitSet.set(i);
            }
        }
        if (signed && number.signum() == -1) {
            bitSet.set(length - 1);
        }
        return bitSet;
    }
}
