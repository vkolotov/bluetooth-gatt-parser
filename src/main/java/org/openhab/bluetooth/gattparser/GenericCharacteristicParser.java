package org.openhab.bluetooth.gattparser;

import org.openhab.bluetooth.gattparser.num.FloatingPointNumberFormatter;
import org.openhab.bluetooth.gattparser.num.RealNumberFormatter;
import org.openhab.bluetooth.gattparser.spec.BluetoothGattSpecificationReader;
import org.openhab.bluetooth.gattparser.spec.Characteristic;
import org.openhab.bluetooth.gattparser.spec.Field;
import org.openhab.bluetooth.gattparser.spec.FieldFormat;
import org.openhab.bluetooth.gattparser.spec.FieldType;
import org.openhab.bluetooth.gattparser.spec.FlagUtils;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * A generic implementation of a GATT characteristic parser capable of reading and writing standard/approved
 * Bluetooth GATT characteristics as well as user defined GATT characteristics. Quite often some parts of the Bluetooth
 * GATT specification is misleading and also incomplete, furthermore some "approved" GATT XML fields do not
 * follow the specification, therefore the implementation of this parser is based not only on Bluetooth GATT
 * specification (Core v5) but also based on some heuristic methods, e.g. by studying/following GATT XML files for
 * some services and characteristics.
 *
 * @author Vlad Kolotov
 */
public class GenericCharacteristicParser implements CharacteristicParser {

    private final Logger logger = LoggerFactory.getLogger(GenericCharacteristicParser.class);
    private final BluetoothGattSpecificationReader reader;

    GenericCharacteristicParser(BluetoothGattSpecificationReader reader) {
        this.reader = reader;
    }

    @Override
    public LinkedHashMap<String, FieldHolder> parse(Characteristic characteristic, byte[] raw)
            throws CharacteristicFormatException {
        LinkedHashMap<String, FieldHolder> result = new LinkedHashMap<>();

        validate(characteristic);

        int offset = 0;
        List<Field> fields = characteristic.getValue().getFields();
        Set<String> requires = FlagUtils.getReadFlags(fields, raw);
        requires.add("Mandatory");
        for (Field field : fields) {
            List<String> requirements = field.getRequirements();
            if (requirements != null && !requirements.isEmpty() && !requires.containsAll(requirements)) {
                // skipping field as per requirement in the Flags field
                continue;
            }
            if (field.getReference() != null) {
                LinkedHashMap<String, FieldHolder> subCharacteristic =
                        parse(reader.getCharacteristicByType(field.getReference().trim()), getRemainder(raw, offset));
                result.putAll(subCharacteristic);
                int size = getSize(subCharacteristic.values());
                if (size == FieldFormat.FULL_SIZE) {
                    break;
                }
                offset += size;
            } else {
                if (FlagUtils.isFlagsField(field)) {
                    // skipping flags field
                    offset += field.getFormat().getSize();
                    continue;
                }
                FieldFormat fieldFormat = field.getFormat();
                if (field.isRepeated()) {
                    // Array field (GSS uint16[n] etc.): repeat the element either a fixed number
                    // of times or, when repeats == 0, until the data is exhausted. Elements are
                    // exposed as indexed holders "Name[0]", "Name[1]", ...
                    int elementSize = fieldFormat.getSize();
                    if (elementSize == FieldFormat.FULL_SIZE || elementSize <= 0) {
                        throw new CharacteristicFormatException(
                                "Repeated field \"" + field.getName() + "\" must have a fixed-size element format.");
                    }
                    int repeats = field.getRepeats();
                    int index = 0;
                    while (repeats == 0 ? offset + elementSize <= raw.length * 8 : index < repeats) {
                        result.put(field.getName() + "[" + index + "]", parseField(field, raw, offset));
                        offset += elementSize;
                        index++;
                    }
                    continue;
                }
                result.put(field.getName(), parseField(field, raw, offset));
                if (fieldFormat.getSize() == FieldFormat.FULL_SIZE) {
                    // full size field, e.g. a string
                    break;
                }
                offset += field.getFormat().getSize();
            }
        }
        return result;
    }

    @Override
    public byte[] serialize(Collection<FieldHolder> fieldHolders) throws CharacteristicFormatException {
        BitSet bitSet = new BitSet();
        int offset = 0;

        for (FieldHolder holder : fieldHolders) {
            if (holder.isValueSet()) {
                int size = holder.getField().getFormat().getSize();
                BitSet serialized = serialize(holder);
                if (size == FieldFormat.FULL_SIZE) {
                    size = serialized.length();
                }
                concat(bitSet, serialized, offset, size);
                offset += size;
            }
        }
        // BitSet does not keep 0, fields could be set all to 0, resulting bitSet to be of 0 length,
        // however data array must not be empty, hence forcing to return an array with first byte of 0 value
        byte[] data = bitSet.isEmpty() ? new byte[] {0} : bitSet.toByteArray();
        return data.length > 20 ? Arrays.copyOf(bitSet.toByteArray(), 20) : data;
    }

    Object parse(Field field, byte[] raw, int offset) {
        FieldFormat fieldFormat = field.getFormat();
        int size = fieldFormat.getSize();
        switch (fieldFormat.getType()) {
            case BOOLEAN: return parseBoolean(raw, offset);
            case UINT: return deserializeReal(raw, offset, size, false);
            case SINT: return deserializeReal(raw, offset, size, true);
            case FLOAT_IEE754: return deserializeFloat(
                    BluetoothGattParserFactory.getIEEE754FloatingPointNumberFormatter(), raw, offset, size);
            case FLOAT_IEE11073: return deserializeFloat(
                    BluetoothGattParserFactory.getIEEE11073FloatingPointNumberFormatter(), raw, offset, size);
            case UTF8S: return deserializeString(raw, offset, "UTF-8");
            case UTF16S: return deserializeString(raw, offset, "UTF-16");
            case STRUCT: return BitSet.valueOf(raw).get(offset, offset + raw.length * 8).toByteArray();
            default:
                throw new IllegalStateException("Unsupported field format: " + fieldFormat.getType());
        }
    }

    BitSet serialize(boolean value) {
        BitSet bitSet = new BitSet();
        if (value) {
            bitSet.set(0);
        }
        return bitSet;
    }

    void concat(BitSet target, BitSet source, int offset, int size) {
        for (int i = 0; i < size; i++) {
            if (source.get(i)) {
                target.set(offset + i);
            }
        }
    }

    private BitSet serialize(FieldHolder holder) {
        FieldFormat fieldFormat = holder.getField().getFormat();
        switch (fieldFormat.getType()) {
            case BOOLEAN: return serialize(holder.getBoolean(null));
            case UINT:
            case SINT: return serializeReal(holder);
            case FLOAT_IEE754: return serializeFloat(
                    BluetoothGattParserFactory.getIEEE754FloatingPointNumberFormatter(), holder);
            case FLOAT_IEE11073: return serializeFloat(
                    BluetoothGattParserFactory.getIEEE11073FloatingPointNumberFormatter(), holder);
            case UTF8S: return serializeString(holder, "UTF-8");
            case UTF16S: return serializeString(holder, "UTF-16");
            case STRUCT: return BitSet.valueOf((byte[]) holder.getRawValue());
            default:
                throw new IllegalStateException("Unsupported field format: " + fieldFormat.getType());
        }
    }

    private Boolean parseBoolean(byte[] raw, int offset) {
        return BitSet.valueOf(raw).get(offset);
    }

    private FieldHolder parseField(Field field, byte[] raw, int offset) {
        FieldFormat fieldFormat = field.getFormat();
        if (fieldFormat.getSize() != FieldFormat.FULL_SIZE && offset + fieldFormat.getSize() > raw.length * 8) {
            throw new CharacteristicFormatException(
                    "Not enough bits to parse field \"" + field.getName() + "\". "
                            + "Data length: " + raw.length + " bytes. "
                            + "Looks like your device does not conform SIG specification.");
        }
        Object value = parse(field, raw, offset);
        return new FieldHolder(field, value);

    }

    private void validate(Characteristic characteristic) {
        if (!characteristic.isValidForRead()) {
            logger.error("Characteristic cannot be parsed: \"{}\".", characteristic.getName());
            throw new CharacteristicFormatException("Characteristic cannot be parsed: \"" +
                    characteristic.getName() + "\".");
        }
    }

    private int getSize(Collection<FieldHolder> holders) {
        int size = 0;
        for (FieldHolder holder : holders) {
            Field field = holder.getField();
            if (field.getFormat().getSize() == FieldFormat.FULL_SIZE) {
                return FieldFormat.FULL_SIZE;
            }
            size += field.getFormat().getSize();
        }
        return size;
    }

    private BitSet serializeReal(FieldHolder holder) {
        RealNumberFormatter realNumberFormatter = BluetoothGattParserFactory.getTwosComplementNumberFormatter();
        int size = holder.getField().getFormat().getSize();
        boolean signed = holder.getField().getFormat().getType() == FieldType.SINT;
        if ((signed && size <= 32) || (!signed && size < 32)) {
            return realNumberFormatter.serialize((Integer) holder.getRawValue(), size, signed);
        } else if ((signed && size <= 64) || (!signed && size < 64)) {
            return realNumberFormatter.serialize((Long) holder.getRawValue(), size, signed);
        } else {
            return realNumberFormatter.serialize((BigInteger) holder.getRawValue(), size, signed);
        }
    }

    private Object deserializeReal(byte[] raw, int offset, int size, boolean signed) {
        RealNumberFormatter realNumberFormatter = BluetoothGattParserFactory.getTwosComplementNumberFormatter();
        int toIndex = offset + size;
        if ((signed && size <= 32) || (!signed && size < 32)) {
            return realNumberFormatter.deserializeInteger(BitSet.valueOf(raw).get(offset, toIndex), size, signed);
        } else if ((signed && size <= 64) || (!signed && size < 64)) {
            return realNumberFormatter.deserializeLong(BitSet.valueOf(raw).get(offset, toIndex), size, signed);
        } else {
            return realNumberFormatter.deserializeBigInteger(BitSet.valueOf(raw).get(offset, toIndex), size, signed);
        }
    }

    private Object deserializeFloat(FloatingPointNumberFormatter formatter, byte[] raw, int offset, int size) {
        int toIndex = offset + size;
        if (size == 16) {
            return formatter.deserializeSFloat(BitSet.valueOf(raw).get(offset, toIndex));
        } else if (size == 32) {
            return formatter.deserializeFloat(BitSet.valueOf(raw).get(offset, toIndex));
        } else if (size == 64) {
            return formatter.deserializeDouble(BitSet.valueOf(raw).get(offset, toIndex));
        } else {
            throw new IllegalStateException("Unknown bit size for float numbers: " + size);
        }
    }

    private BitSet serializeFloat(FloatingPointNumberFormatter formatter, FieldHolder holder) {
        int size = holder.getField().getFormat().getSize();
        if (size == 16) {
            return formatter.serializeSFloat(holder.getFloat(null));
        } else if (size == 32) {
            return formatter.serializeFloat(holder.getFloat(null));
        } else if (size == 64) {
            return formatter.serializeDouble(holder.getDouble(null));
        } else {
            throw new IllegalStateException("Invalid bit size for float numbers: " + size);
        }
    }

    private String deserializeString(byte[] raw, int offset, String encoding) {
        try {
            return new String(BitSet.valueOf(raw).get(offset, offset + raw.length * 8).toByteArray(), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private BitSet serializeString(FieldHolder holder, String encoding) {
        try {
            return BitSet.valueOf(holder.getString(null).getBytes(encoding));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] getRemainder(byte[] raw, int offset) {
        byte[] remained = BitSet.valueOf(raw).get(offset, raw.length * 8).toByteArray();
        byte[] remainedWithTrailingZeros = new byte[(raw.length - (int) Math.ceil(offset / 8.0))];
        System.arraycopy(remained, 0, remainedWithTrailingZeros, 0, remained.length);
        return remainedWithTrailingZeros;
    }

}
