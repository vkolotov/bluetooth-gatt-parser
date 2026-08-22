package org.openhab.bluetooth.gattparser;

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

import org.apache.commons.beanutils.converters.AbstractConverter;
import org.apache.commons.beanutils.converters.ArrayConverter;
import org.apache.commons.beanutils.converters.BigDecimalConverter;
import org.apache.commons.beanutils.converters.BigIntegerConverter;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.beanutils.converters.ByteConverter;
import org.apache.commons.beanutils.converters.DoubleConverter;
import org.apache.commons.beanutils.converters.FloatConverter;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.beanutils.converters.LongConverter;
import org.apache.commons.beanutils.converters.StringConverter;
import org.openhab.bluetooth.gattparser.num.TwosComplementNumberFormatter;
import org.openhab.bluetooth.gattparser.spec.Enumeration;
import org.openhab.bluetooth.gattparser.spec.Field;
import org.openhab.bluetooth.gattparser.spec.FieldFormat;
import org.openhab.bluetooth.gattparser.spec.FieldType;
import org.openhab.bluetooth.gattparser.spec.FlagUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.BitSet;

/**
 * Bluetooth GATT field holder. Field holder encapsulates notion about field type and field value as well as some
 * helper methods to access field values in a user-friendly manner.
 *
 * @author Vlad Kolotov
 */
public class FieldHolder {

    private final Field field;
    private Object value;

    /**
     * Creates a new field holder for a given GATT field and its raw value.
     * @param field GATT field specification
     * @param value field value
     */
    protected FieldHolder(Field field, Object value) {
        this.field = field;
        this.value = value;
    }

    /**
     * Create a new field holder for a given GATT field.
     * @param field GATT field specification
     */
    protected FieldHolder(Field field) {
        this.field = field;
    }

    /**
     * Returns the GATT field specification.
     * @return GATT field specification
     */
    public Field getField() {
        return field;
    }

    /**
     * Checks whether the field is a number.
     * @return true if a given field is a number, false otherwise
     */
    public boolean isNumber() {
        return field.getFormat().isNumber();
    }

    /**
     * Checks whether the field is of boolean type.
     * @return true if a given field is of type boolean, false otherwise
     */
    public boolean isBoolean() {
        return field.getFormat().isBoolean();
    }

    /**
     * Checks whether the field is of string type.
     * @return true if a given field is of type string, false otherwise
     */
    public boolean isString() {
        return field.getFormat().isString();
    }

    /**
     * Checks whether the field is of struct type.
     * @return true if a given field is of type struct, false otherwise
     */
    public boolean isStruct() {
        return field.getFormat().isStruct();
    }

    /**
     * Returns an Integer representation of the field or a default value in case if the field cannot
     * be converted to an Integer.
     * @param def the default value to be returned if an error occurs converting the field
     * @return an Integer representation of the field
     */
    public Integer getInteger(Integer def) {
        Integer result = new IntegerConverter(null).convert(Integer.class, prepareValue());
        if (result != null) {
            double multiplier = getMultiplier();
            double offset = getOffset();
            if (multiplier != 1.0 || offset != 0.0) {
                return (int) Math.round(result * multiplier + offset);
            } else {
                return result;
            }
        } else {
            return def;
        }
    }

    /**
     * Returns a Long representation of the field or a default value in case if the field cannot
     * be converted to a Long.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a Long representation of the field
     */
    public Long getLong(Long def) {
        Long result = new LongConverter(null).convert(Long.class, prepareValue());
        if (result != null) {
            double multiplier = getMultiplier();
            double offset = getOffset();
            if (multiplier != 1.0 || offset != 0.0) {
                return Math.round(result * multiplier + offset);
            } else {
                return result;
            }
        } else {
            return def;
        }
    }

    /**
     * Returns a BigInteger representation of the field or a default value in case if the field cannot
     * be converted to a BigInteger.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a BigInteger representation of the field
     */
    public BigInteger getBigInteger(BigInteger def) {
        BigDecimal result = new BigDecimalConverter(null).convert(BigDecimal.class, prepareValue());
        return result != null
                ? result.multiply(getMultiplierExact())
                        .add(BigDecimal.valueOf(getOffset())).setScale(0, RoundingMode.HALF_UP).toBigInteger()
                : def;
    }

    /**
     * Returns a BigDecimal representation of the field or a default value in case if the field cannot
     * be converted to a BigDecimal.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a BigDecimal representation of the field
     */
    public BigDecimal getBigDecimal(BigDecimal def) {
        BigDecimal result = new BigDecimalConverter(null).convert(BigDecimal.class, prepareValue());
        return result != null
                ? result.multiply(getMultiplierExact())
                : def;
    }

    /**
     * Returns a Float representation of the field or a default value in case if the field cannot
     * be converted to a Float.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a Float representation of the field
     */
    public Float getFloat(Float def) {
        Float result = new FloatConverter(null).convert(Float.class, prepareValue());
        if (result != null) {
            return (float) (result * getMultiplier() + getOffset());
        } else {
            return def;
        }
    }

    /**
     * Returns a Double representation of the field or a default value in case if the field cannot
     * be converted to a Double.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a Double representation of the field
     */
    public Double getDouble(Double def) {
        Double result = new FloatConverter(null).convert(Double.class, prepareValue());
        if (result != null) {
            return result * getMultiplier() + getOffset();
        } else {
            return def;
        }
    }

    /**
     * Returns a Boolean representation of the field or a default value in case if the field cannot
     * be converted to a Boolean.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a Boolean representation of the field
     */
    public Boolean getBoolean(Boolean def) {
        return new BooleanConverter(def).convert(Boolean.class, prepareValue());
    }

    /**
     * Returns a String representation of the field or a default value in case if the field cannot
     * be converted to a String.
     * @param def the default value to be returned if an error occurs converting the field
     * @return a String representation of the field
     */
    public String getString(String def) {
        return new StringConverter(def).convert(String.class, prepareValue());
    }

    /**
     * Returns an array representation of the field or a default value in case if the field cannot
     * be converted to array.
     * @param def the default value to be returned if an error occurs converting the field
     * @return an array representation of the field
     */
    public byte[] getBytes(byte[] def) {
        return new ArrayConverter(byte[].class, new ByteConverter()).convert(byte[].class, value);
    }

    /**
     * Returns an Integer representation of the field or null in case if the field cannot
     * be converted to an Integer.
     * @return an Integer representation of the field
     */
    public Integer getInteger() {
        return getInteger(null);
    }

    /**
     * Returns a Long representation of the field or null in case if the field cannot
     * be converted to a Long.
     * @return a Long representation of the field
     */
    public Long getLong() {
        return getLong(null);
    }

    /**
     * Returns a BigInteger representation of the field or null in case if the field cannot
     * be converted to a BigInteger.
     * @return a BigInteger representation of the field
     */
    public BigInteger getBigInteger() {
        return getBigInteger(null);
    }

    /**
     * Returns a BigDecimal representation of the field or null in case if the field cannot
     * be converted to a BigDecimal.
     * @return a BigDecimal representation of the field
     */
    public BigDecimal getBigDecimal() {
        return getBigDecimal(null);
    }

    /**
     * Returns a Float representation of the field or null in case if the field cannot
     * be converted to a Float.
     * @return a Float representation of the field
     */
    public Float getFloat() {
        return getFloat(null);
    }

    /**
     * Returns a Double representation of the field or null in case if the field cannot
     * be converted to a Double.
     * @return a Double representation of the field
     */
    public Double getDouble() {
        return getDouble(null);
    }

    /**
     * Returns a Boolean representation of the field or null in case if the field cannot
     * be converted to a Boolean.
     * @return a Boolean representation of the field
     */
    public Boolean getBoolean() {
        return getBoolean(null);
    }

    /**
     * Returns a String representation of the field or null in case if the field cannot
     * be converted to a String.
     * @return a String representation of the field
     */
    public String getString() {
        return getString(null);
    }

    /**
     * Returns an array representation of the field or null in case if the field cannot
     * be converted to an array.
     * @return a String representation of the field
     */
    public byte[] getBytes() {
        return getBytes(null);
    }

    /**
     * Returns field raw value.
     * @return field raw value
     */
    public Object getRawValue() {
        return value;
    }

    /**
     * Returns field enumeration according to the field value.
     * @return fields enumeration according to the field value
     */
    public Enumeration getEnumeration() {
        BigInteger key;
        if (field.getFormat().isStruct() && value instanceof byte[]) {
            byte[] data = (byte[]) value;
            key = new TwosComplementNumberFormatter().deserializeBigInteger(BitSet.valueOf(data),
                    data.length * 8, false);
        } else if (field.getFormat().isString() && value instanceof String) {
            String encoding = field.getFormat().getType() == FieldType.UTF8S ? "UTF-8" : "UTF-16";
            try {
                byte[] data = ((String) value).getBytes(encoding);
                key = new TwosComplementNumberFormatter().deserializeBigInteger(BitSet.valueOf(data),
                        data.length * 8, false);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        } else {
            key = getBigInteger();
        }
        return FlagUtils.getEnumeration(field, key).orElse(null);
    }

    /**
     * Returns field enumeration value according to the field value.
     * @return fields enumeration value according to the field value
     */
    public String getEnumerationValue() {
        Enumeration enumeration = getEnumeration();
        return enumeration != null ? enumeration.getValue() : null;
    }

    /**
     * Returns field enumeration "requires" according to the field value.
     * @return fields enumeration "requires" (or a its flag) according to the field value
     */
    public String getEnumerationRequires() {
        Enumeration enumeration = getEnumeration();
        return enumeration != null ? enumeration.getRequires() : null;
    }

    /**
     * Sets the field value into a new boolean value.
     * @param value a new field value
     */
    public void setBoolean(Boolean value) {
        this.value = value;
    }

    /**
     * Sets the field value into a new Integer value.
     * @param value a new field value
     */
    public void setInteger(Integer value) {
        if (value == null) {
            this.value = null;
        } else {
            Double maximum = field.getMaximum();
            if (maximum != null && maximum < value) {
                throw new IllegalArgumentException("Value [" + value + "] is greater than maximum: " + maximum);
            }
            Double minimum = field.getMinimum();
            if (minimum != null && minimum > value) {
                throw new IllegalArgumentException("Value [" + value + "] is less than minimum: " + minimum);
            }
            double multiplier = getMultiplier();
            double offset = getOffset();
            if (multiplier != 1.0 || offset != 0.0) {
                this.value = getConverter().convert(null, Math.round((value - offset) / multiplier));
            } else {
                this.value = getConverter().convert(null, value);
            }
        }
    }

    /**
     * Sets the field value into a new Long value.
     * @param value a new field value
     */
    public void setLong(Long value) {
        if (value == null) {
            this.value = null;
        } else {
            Double maximum = field.getMaximum();
            if (maximum != null && maximum < value) {
                throw new IllegalArgumentException("Value [" + value + "] is greater than maximum: " + maximum);
            }
            Double minimum = field.getMinimum();
            if (minimum != null && minimum > value) {
                throw new IllegalArgumentException("Value [" + value + "] is less than minimum: " + minimum);
            }
            double multiplier = getMultiplier();
            double offset = getOffset();
            if (multiplier != 1.0 || offset != 0.0) {
                this.value = getConverter().convert(null, Math.round((value - offset) / multiplier));
            } else {
                this.value = getConverter().convert(null, value);
            }
        }
    }

    /**
     * Sets the field value into a new BigInteger value.
     * @param value a new field value
     */
    public void setBigInteger(BigInteger value) {
        if (value == null) {
            this.value = null;
        } else {
            BigDecimal vl = new BigDecimal(value);
            Double maximum = field.getMaximum();
            if (maximum != null && vl.compareTo(new BigDecimal(maximum)) > 0) {
                throw new IllegalArgumentException("Value [" + value + "] is greater than maximum: " + maximum);
            }
            Double minimum = field.getMinimum();
            if (minimum != null && vl.compareTo(new BigDecimal(minimum)) < 0) {
                throw new IllegalArgumentException("Value [" + value + "] is less than minimum: " + minimum);
            }
            double multiplier = getMultiplier();
            double offset = getOffset();
            BigInteger adjusted;
            if (multiplier != 1.0 || offset != 0.0) {
                adjusted = vl.subtract(BigDecimal.valueOf(offset)).setScale(0, RoundingMode.HALF_UP)
                                .divide(BigDecimal.valueOf(multiplier)).toBigInteger();
            } else {
                adjusted = value;
            }

            if (field.getFormat().isStruct()) {
                this.value = new TwosComplementNumberFormatter().serialize(adjusted,
                        adjusted.bitLength(), false).toByteArray();
            } else {
                this.value = getConverter().convert(null, adjusted);
            }
        }
    }

    /**
     * Sets the field value into a new Float value.
     * @param value a new field value
     */
    public void setFloat(Float value) {
        if (value == null) {
            this.value = null;
        } else {
            Double maximum = field.getMaximum();
            if (maximum != null && maximum < value) {
                throw new IllegalArgumentException("Value [" + value + "] is greater than maximum: " + maximum);
            }
            Double minimum = field.getMinimum();
            if (minimum != null && minimum > value) {
                throw new IllegalArgumentException("Value [" + value + "] is less than minimum: " + minimum);
            }
            this.value = getConverter().convert(null, (value - getOffset()) / getMultiplier());
        }
    }

    /**
     * Sets the field value into a new Double value.
     * @param value a new field value
     */
    public void setDouble(Double value) {
        if (value == null) {
            this.value = null;
        } else {
            Double maximum = field.getMaximum();
            if (maximum != null && maximum < value) {
                throw new IllegalArgumentException("Value [" + value + "] is greater than maximum: " + maximum);
            }
            Double minimum = field.getMinimum();
            if (minimum != null && minimum > value) {
                throw new IllegalArgumentException("Value [" + value + "] is less than minimum: " + minimum);
            }
            this.value = getConverter().convert(null, (value - getOffset()) / getMultiplier());
        }
    }

    /**
     * Sets the field value into a new String value.
     * @param value a new field value
     */
    public void setString(String value) {
        this.value = value;
    }

    /**
     * Sets the field value to a "struct" (array) value from an array.
     * @param struct a new field value
     */
    public void setStruct(byte[] struct) {
        value = struct;
    }

    /**
     * Sets the field value from the given enumeration (enumeration key).
     * @param value a new field value
     */
    public void setEnumeration(Enumeration value) {
        if (value == null) {
            this.value = null;
        } else {

            BigInteger key = value.getKey();

            if (field.getFormat().isStruct()) {
                this.value = new TwosComplementNumberFormatter().serialize(key, key.bitLength(), false).toByteArray();
            } else if (field.getFormat().isString()) {
                String encoding = field.getFormat().getType() == FieldType.UTF8S ? "UTF-8" : "UTF-16";
                try {
                    this.value = new String(new TwosComplementNumberFormatter().serialize(key, key.bitLength(), false)
                            .toByteArray(), encoding);
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                setBigInteger(key);
            }
        }
    }

    /**
     * Sets the field value to a raw value.
     * @param value a new field value
     */
    public void setRawValue(Object value) {
        this.value = value;
    }

    /**
     * Checks whether field value is set.
     * @return true if field value is set, false otherwise
     */
    public boolean isValueSet() {
        return value != null;
    }

    @Override
    public String toString() {
        return getString();
    }

    private double getMultiplier() {
        double multiplier = 1;
        if (field.getDecimalExponent() != null) {
            multiplier = Math.pow(10, field.getDecimalExponent());
        }
        if (field.getBinaryExponent() != null) {
            multiplier *= Math.pow(2, field.getBinaryExponent());
        }
        if (field.getMultiplier() != null && field.getMultiplier() != 0) {
            multiplier *= (double) field.getMultiplier();
        }
        return multiplier;
    }

    /**
     * Exact BigDecimal counterpart of {@link #getMultiplier()}. The decimal exponent is applied via
     * {@link BigDecimal#scaleByPowerOfTen(int)} and the binary exponent via exact powers of two, so
     * the BigDecimal paths do not inherit the rounding error of {@code Math.pow(10, e)}.
     */
    private BigDecimal getMultiplierExact() {
        BigDecimal multiplier = BigDecimal.ONE;
        if (field.getDecimalExponent() != null) {
            multiplier = multiplier.scaleByPowerOfTen(field.getDecimalExponent());
        }
        if (field.getBinaryExponent() != null) {
            int b = field.getBinaryExponent();
            BigDecimal powerOfTwo = new BigDecimal(BigInteger.TWO.pow(Math.abs(b)));
            multiplier = b >= 0
                    ? multiplier.multiply(powerOfTwo)
                    : multiplier.divide(powerOfTwo); // negative power of two is always exact (terminates in binary)
        }
        if (field.getMultiplier() != null && field.getMultiplier() != 0) {
            multiplier = multiplier.multiply(BigDecimal.valueOf(field.getMultiplier()));
        }
        return multiplier;
    }

    /**
     * Reads offset-to-be-added to field value received from request.
     * This is an extension to official GATT characteristic field specification, 
     * allowing to implement subset of proprietary devices that almost follow standard
     * GATT specifications.
     * @return offset as double if set, 0 if not present
     */
    private double getOffset() {
        return (field.getOffset() != null) ? field.getOffset() : 0;
    }

    private AbstractConverter getConverter() {
        FieldFormat fieldFormat = field.getFormat();
        int size = fieldFormat.getSize();
        switch (fieldFormat.getType()) {
            case BOOLEAN: return new BooleanConverter();
            case UINT:
                if (size < 32) {
                    return new IntegerConverter();
                } else if (size < 64) {
                    return new LongConverter();
                } else {
                    return new BigIntegerConverter();
                }
            case SINT:
                if (size <= 32) {
                    return new IntegerConverter();
                } else if (size <= 64) {
                    return new LongConverter();
                } else {
                    return new BigIntegerConverter();
                }
            case FLOAT_IEE754:
            case FLOAT_IEE11073: return size <= 32 ? new FloatConverter() : new DoubleConverter();
            case UTF8S:
            case UTF16S: return new StringConverter();
            default:
                throw new IllegalStateException("Unsupported field format: " + fieldFormat.getType());
        }
    }

    private Object prepareValue() {
        if (field.getFormat().isStruct() && value instanceof byte[]) {
            byte[] data = (byte[]) value;
            return new TwosComplementNumberFormatter().deserializeBigInteger(BitSet.valueOf(data),
                    data.length * 8, false);
        } else {
            return value;
        }
    }

}
