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

/**
 * IEEE11073 floating point number formatter.
 * Stateless and threadsafe.
 *
 * @author Vlad Kolotov
 */
public class IEEE11073FloatingPointNumberFormatter implements FloatingPointNumberFormatter {

    public static final int SFLOAT_NaN = 0x07FF;
    public static final int SFLOAT_NRes = 0x0800;
    public static final int SFLOAT_POSITIVE_INFINITY = 0x07FE;
    public static final int SFLOAT_NEGATIVE_INFINITY = 0x0802;
    public static final int SFLOAT_RESERVED = 0x0801;

    public static final int FLOAT_NaN = 0x007FFFFF;
    public static final int FLOAT_NRes = 0x00800000;
    public static final int FLOAT_POSITIVE_INFINITY = 0x007FFFFE;
    public static final int FLOAT_NEGATIVE_INFINITY = 0x00800002;
    public static final int FLOAT_RESERVED = 0x00800001;

    private static final int SFLOAT_NEGATIVE_INFINITY_SIGNED = 0xFFFFF802;
    private static final int FLOAT_NEGATIVE_INFINITY_SIGNED = 0xFF800002;

    private TwosComplementNumberFormatter twosComplementNumberFormatter = new TwosComplementNumberFormatter();

    @Override
    public Float deserializeSFloat(BitSet bits) {
        BitSet exponentBits = bits.get(12, 16);
        BitSet mantissaBits = bits.get(0, 12);
        int exponent = twosComplementNumberFormatter.deserializeInteger(exponentBits, 4, true);
        int mantissa = twosComplementNumberFormatter.deserializeInteger(mantissaBits, 12, true);
        if (exponent == 0) {
            if (mantissa == SFLOAT_NaN) {
                return Float.NaN;
            } else if (mantissa == SFLOAT_POSITIVE_INFINITY) {
                return Float.POSITIVE_INFINITY;
            } else if (mantissa == SFLOAT_NEGATIVE_INFINITY_SIGNED) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return (float) ((double) mantissa * Math.pow(10, exponent));
    }

    @Override
    public Float deserializeFloat(BitSet bits) {
        BitSet exponentBits = bits.get(24, 32);
        BitSet mantissaBits = bits.get(0, 24);
        int exponent = twosComplementNumberFormatter.deserializeInteger(exponentBits, 8, true);
        int mantissa = twosComplementNumberFormatter.deserializeInteger(mantissaBits, 24, true);
        if (exponent == 0) {
            if (mantissa == FLOAT_NaN) {
                return Float.NaN;
            } else if (mantissa == FLOAT_POSITIVE_INFINITY) {
                return Float.POSITIVE_INFINITY;
            } else if (mantissa == FLOAT_NEGATIVE_INFINITY_SIGNED) {
                return Float.NEGATIVE_INFINITY;
            }
        }
        return (float) ((double) mantissa * Math.pow(10, exponent));
    }

    @Override
    public Double deserializeDouble(BitSet bits) {
        // IEEE-11073 defines no 64-bit form; the 32-bit FLOAT is the widest, decoded as a Double.
        Float value = deserializeFloat(bits);
        return value != null ? value.doubleValue() : null;
    }


    @Override
    public BitSet serializeSFloat(Float number) {
        return serialize(number, 12, 4, SFLOAT_NaN, SFLOAT_POSITIVE_INFINITY, SFLOAT_NEGATIVE_INFINITY);
    }

    @Override
    public BitSet serializeFloat(Float number) {
        return serialize(number, 24, 8, FLOAT_NaN, FLOAT_POSITIVE_INFINITY, FLOAT_NEGATIVE_INFINITY);
    }

    @Override
    public BitSet serializeDouble(Double number) {
        return serializeFloat(number != null ? number.floatValue() : null);
    }

    /**
     * Encodes a decimal value as an IEEE-11073 SFLOAT/FLOAT: a two's-complement mantissa in the low
     * {@code mantissaSize} bits and a base-10 exponent in the top {@code exponentSize} bits, laid out
     * little-endian to match the deserialize path. The exponent is chosen so the mantissa fits its
     * signed range while preserving as much precision as the format allows.
     */
    private BitSet serialize(Float number, int mantissaSize, int exponentSize,
                             int nanMantissa, int posInfMantissa, int negInfMantissa) {
        int mantissa;
        int exponent = 0;
        if (number == null || Float.isNaN(number)) {
            mantissa = nanMantissa;
        } else if (number == Float.POSITIVE_INFINITY) {
            mantissa = posInfMantissa;
        } else if (number == Float.NEGATIVE_INFINITY) {
            mantissa = negInfMantissa;
        } else {
            long mantissaMax = (1L << (mantissaSize - 1)) - 1; // largest positive signed mantissa
            long mantissaMin = -(1L << (mantissaSize - 1));
            int expMax = (1 << (exponentSize - 1)) - 1;
            int expMin = -(1 << (exponentSize - 1));
            double value = number;
            // Raise the exponent until the mantissa fits the signed range.
            double scaled = value;
            while ((Math.round(scaled) > mantissaMax || Math.round(scaled) < mantissaMin) && exponent < expMax) {
                scaled /= 10.0;
                exponent++;
            }
            if (Math.round(scaled) > mantissaMax || Math.round(scaled) < mantissaMin) {
                // The value is too large for this format even at the maximum exponent. Encoding the
                // truncated mantissa would silently wrap (and can land on the NaN sentinel), so
                // saturate to the corresponding infinity instead.
                mantissa = scaled > 0 ? posInfMantissa : negInfMantissa;
                exponent = 0;
            } else {
                // Lower the exponent to keep fractional precision while the mantissa still fits.
                while (exponent > expMin) {
                    double finer = scaled * 10.0;
                    if (Math.round(finer) > mantissaMax || Math.round(finer) < mantissaMin) {
                        break;
                    }
                    scaled = finer;
                    exponent--;
                }
                mantissa = (int) Math.round(scaled);
            }
        }

        BitSet result = new BitSet(mantissaSize + exponentSize);
        BitSet mantissaBits = twosComplementNumberFormatter.serialize(mantissa, mantissaSize, true);
        BitSet exponentBits = twosComplementNumberFormatter.serialize(exponent, exponentSize, true);
        for (int i = 0; i < mantissaSize; i++) {
            if (mantissaBits.get(i)) {
                result.set(i);
            }
        }
        for (int i = 0; i < exponentSize; i++) {
            if (exponentBits.get(i)) {
                result.set(mantissaSize + i);
            }
        }
        return result;
    }

}
