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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.openhab.bluetooth.gattparser.BluetoothGattParserFactory;
import org.openhab.bluetooth.gattparser.CharacteristicFormatException;
import org.openhab.bluetooth.gattparser.FieldHolder;
import org.openhab.bluetooth.gattparser.GattRequest;
import org.openhab.bluetooth.gattparser.GenericCharacteristicParser;
import org.openhab.bluetooth.gattparser.num.FloatingPointNumberFormatter;
import org.openhab.bluetooth.gattparser.num.RealNumberFormatter;
import org.openhab.bluetooth.gattparser.spec.Bit;
import org.openhab.bluetooth.gattparser.spec.BitField;
import org.openhab.bluetooth.gattparser.spec.BluetoothGattSpecificationReader;
import org.openhab.bluetooth.gattparser.spec.Characteristic;
import org.openhab.bluetooth.gattparser.spec.Field;
import org.openhab.bluetooth.gattparser.spec.FlagUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyByte;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("jdk.internal.reflect.*")
@PrepareForTest({ BluetoothGattParserFactory.class, FlagUtils.class })
public class GenericCharacteristicParserTest {

    private static final String CHARACTERISTIC_UUID = "2A19";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Field flagField;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Characteristic characteristic;
    @Mock
    private RealNumberFormatter twosComplementNumberFormatter;
    @Mock
    private FloatingPointNumberFormatter ieee754NumberFormatter;
    @Mock
    private FloatingPointNumberFormatter ieee11073NumberFormatter;

    private BluetoothGattSpecificationReader reader = mock(BluetoothGattSpecificationReader.class, RETURNS_DEEP_STUBS);
    @Spy
    private GenericCharacteristicParser parser = new GenericCharacteristicParser(reader);

    @Before
    public void setUp() {
        PowerMockito.mockStatic(BluetoothGattParserFactory.class, FlagUtils.class);
        when(characteristic.getUuid()).thenReturn(CHARACTERISTIC_UUID);
        when(BluetoothGattParserFactory.getTwosComplementNumberFormatter()).thenReturn(twosComplementNumberFormatter);
        when(BluetoothGattParserFactory.getIEEE754FloatingPointNumberFormatter()).thenReturn(ieee754NumberFormatter);
        when(BluetoothGattParserFactory.getIEEE11073FloatingPointNumberFormatter()).thenReturn(ieee11073NumberFormatter);
        when(flagField.getName()).thenReturn("fLags");
    }


    @Test(expected = CharacteristicFormatException.class)
    public void testParseNotValidForRead() throws CharacteristicFormatException {
        when(characteristic.isValidForRead()).thenReturn(Boolean.FALSE);
        parser.parse(characteristic, new byte[]{ });
        verify(characteristic.isValidForRead(), times(1));
    }

    @Test
    public void testParseFormats() throws CharacteristicFormatException, UnsupportedEncodingException {
        when(ieee754NumberFormatter.deserializeSFloat(Matchers.<BitSet>any())).thenReturn(0.0F);
        when(ieee754NumberFormatter.deserializeFloat(Matchers.<BitSet>any())).thenReturn(0.0F);
        when(ieee754NumberFormatter.deserializeDouble(Matchers.<BitSet>any())).thenReturn(0.0D);

        when(ieee11073NumberFormatter.deserializeSFloat(Matchers.<BitSet>any())).thenReturn(0.0F);
        when(ieee11073NumberFormatter.deserializeFloat(Matchers.<BitSet>any())).thenReturn(0.0F);

        when(twosComplementNumberFormatter.deserializeInteger(Matchers.<BitSet>any(), anyByte(), anyBoolean())).thenReturn(0);
        when(twosComplementNumberFormatter.deserializeLong(Matchers.<BitSet>any(), anyByte(), anyBoolean())).thenReturn(0L);
        when(twosComplementNumberFormatter.deserializeBigInteger(Matchers.<BitSet>any(), anyByte(), anyBoolean())).thenReturn(BigInteger.ZERO);


        byte[] data = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
        BitSet bits = BitSet.valueOf(data);
        assertParseFormat(false, "boolean", new byte[] {(byte) 0b010});
        assertParseFormat(true, "boolean", new byte[] {(byte) 0b101});

        assertParseFormat(0, "2bit", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 2, false);
        assertParseFormat(0, "8bit", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 8, false);
        assertParseFormat(0, "uint8", data);
        verify(twosComplementNumberFormatter, times(2)).deserializeInteger(bits, 8, false);
        assertParseFormat(0, "sint8", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 8, true);

        assertParseFormat(0, "sint31", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 31, true);
        assertParseFormat(0, "uint31", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 31, false);
        assertParseFormat(0, "sint32", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(bits, 32, true);
        assertParseFormat(0L, "uint32", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeLong(bits, 32, false);
        assertParseFormat(0L, "sint33", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeLong(bits, 33, true);

        assertParseFormat(0L, "sint63", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeLong(bits, 63, true);
        assertParseFormat(0L, "uint63", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeLong(bits, 63, false);
        assertParseFormat(0L, "sint64", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeLong(bits, 64, true);
        assertParseFormat(BigInteger.ZERO, "uint64", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeBigInteger(bits, 64, false);
        assertParseFormat(BigInteger.ZERO, "sint65", data);
        verify(twosComplementNumberFormatter, times(1)).deserializeBigInteger(bits, 65, true);

        assertParseFormat(0.0F, "float32", data);
        verify(ieee754NumberFormatter, times(1)).deserializeFloat(bits);
        assertParseFormat(0.0D, "float64", data);
        verify(ieee754NumberFormatter, times(1)).deserializeDouble(bits);
        assertParseFormat(0.0F, "sfloat", data);
        verify(ieee11073NumberFormatter, times(1)).deserializeSFloat(bits);
        assertParseFormat(0.0F, "float", data);
        verify(ieee11073NumberFormatter, times(1)).deserializeFloat(bits);

        assertParseFormat("test8", "utf8s", "test8".getBytes());
        assertParseFormat("test16", "utf16s", "test16".getBytes("UTF-16"));
    }

    @Test
    public void testParse() throws CharacteristicFormatException, UnsupportedEncodingException {
        Object value = new Object();
        doReturn(value).when(parser).parse(any(Field.class), any(byte[].class), anyInt());

        List<Field> fields = new ArrayList<>();
        Field flagsField = MockUtils.mockFieldFormat("flags", "uint8");
        fields.add(flagsField); // should be ignored
        fields.add(MockUtils.mockFieldFormat("Field1", "uint8", "C1"));
        fields.add(MockUtils.mockFieldFormat("Field2", "uint8", "C1", "C2"));
        fields.add(MockUtils.mockFieldFormat("Field3", "uint8", "C2"));
        fields.add(MockUtils.mockFieldFormat("Field4", "uint8", new String[]{}));
        fields.add(MockUtils.mockFieldFormat("Field5", "uint8", "C5"));
        fields.add(MockUtils.mockFieldFormat("Field6", "uint8", "Mandatory"));
        when(reader.getFields(characteristic)).thenReturn(fields);
        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        when(FlagUtils.isFlagsField(flagsField)).thenReturn(true);
        when(FlagUtils.getReadFlags(anyList(), any(byte[].class))).thenReturn(
                new HashSet<>(Arrays.asList("C1", "C3", "C4")));
        assertFieldsExist(value, "Field1", "Field4", "Field6");

        when(FlagUtils.getReadFlags(anyList(), any(byte[].class))).thenReturn(
                new HashSet<>(Arrays.asList("C2")));
        assertFieldsExist(value, "Field3", "Field4", "Field6");

        when(FlagUtils.getReadFlags(anyList(), any(byte[].class))).thenReturn(
                new HashSet<>(Arrays.asList("C1", "C2")));
        assertFieldsExist(value, "Field1", "Field2", "Field3", "Field4", "Field6");
    }

    @Test(expected = CharacteristicFormatException.class)
    public void testParseNotEnoughData() throws CharacteristicFormatException, UnsupportedEncodingException {
        byte[] data = new byte[] {0};
        assertParseFormat(0, "8bit", data);

        assertParseFormat(0, "9bit", data);
    }

    @Test
    public void testParseComplex() {
        byte[] data = new byte[] {85, -86, 85, -86, 85, -86, 85, -86};

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "uint40", new String[]{}));
        fields.add(MockUtils.mockFieldFormat("Field2", "uint24", new String[]{}));
        when(reader.getFields(characteristic)).thenReturn(fields);
        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        parser.parse(characteristic, data);

        verify(twosComplementNumberFormatter, times(1)).deserializeLong(BitSet.valueOf(data).get(0, 40), 40, false);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(BitSet.valueOf(data).get(40, 64), 24, false);

    }

    @Test
    public void testParseRepeatedFieldToEnd() {
        when(twosComplementNumberFormatter.deserializeInteger(Matchers.<BitSet>any(), anyInt(), anyBoolean())).thenReturn(7);
        // 4 bytes = two uint16 elements; repeats == 0 means consume to the end of the data.
        byte[] data = new byte[] {1, 0, 2, 0};

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockRepeatedFieldFormat("RR-Interval", "uint16", 0));
        when(reader.getFields(characteristic)).thenReturn(fields);
        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        LinkedHashMap<String, FieldHolder> result = parser.parse(characteristic, data);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("RR-Interval[0]"));
        assertTrue(result.containsKey("RR-Interval[1]"));
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(BitSet.valueOf(data).get(0, 16), 16, false);
        verify(twosComplementNumberFormatter, times(1)).deserializeInteger(BitSet.valueOf(data).get(16, 32), 16, false);
    }

    @Test
    public void testParseRepeatedFieldFixedCount() {
        when(twosComplementNumberFormatter.deserializeInteger(Matchers.<BitSet>any(), anyInt(), anyBoolean())).thenReturn(3);
        // 3 bytes present, but the field is fixed at exactly 2 uint8 occurrences.
        byte[] data = new byte[] {10, 20, 30};

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockRepeatedFieldFormat("Sample", "uint8", 2));
        when(reader.getFields(characteristic)).thenReturn(fields);
        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        LinkedHashMap<String, FieldHolder> result = parser.parse(characteristic, data);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("Sample[0]"));
        assertTrue(result.containsKey("Sample[1]"));
    }

    @Test
    public void testParseComplexWithReferences() {
        when(twosComplementNumberFormatter.deserializeInteger(Matchers.<BitSet>any(), eq(8), eq(false))).thenReturn(10);
        // Flags for inner fields: C1, C2
        Set<String> flags = new HashSet<String>() {{
            add("C1");
            add("C2");
        }};
        when(twosComplementNumberFormatter.deserializeInteger(Matchers.<BitSet>any(), eq(8), eq(true))).thenReturn(-12);
        when(twosComplementNumberFormatter.deserializeInteger(Matchers.<BitSet>any(), eq(16), eq(false))).thenReturn(13);
        when(twosComplementNumberFormatter.deserializeInteger(Matchers.<BitSet>any(), eq(16), eq(true))).thenReturn(14);
        byte[] data = new byte[] {10, 0b11, -12, 13, 0, 14, 0};

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "uint8", new String[] {}));
        Field field = mock(Field.class);
        when(field.getReference()).thenReturn("org.bluetooth.characteristic_id");
        fields.add(field);
        fields.add(MockUtils.mockFieldFormat("Field2", "sint16", new String[] {}));

        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        List<Field> innerFields = new ArrayList<>();
        Field innerFlags = MockUtils.mockFieldFormat("flags", "8bit", new String[] {});
        BitField bitField = mock(BitField.class);
        when(innerFlags.getBitField()).thenReturn(bitField);
        List<Bit> bits = new ArrayList<Bit>() {{
            add(MockUtils.mockBit(0, "C1"));
            add(MockUtils.mockBit(1, "C2"));
            add(MockUtils.mockBit(2, "C3"));
        }};
        when(bitField.getBits()).thenReturn(bits);
        innerFields.add(innerFlags);
        innerFields.add(MockUtils.mockFieldFormat("InnerField1", "sint8", "C1"));
        innerFields.add(MockUtils.mockFieldFormat("InnerField0", "uint16", "C3"));
        innerFields.add(MockUtils.mockFieldFormat("InnerField2", "uint16", "C2"));
        Characteristic referenced = mock(Characteristic.class, RETURNS_DEEP_STUBS);
        when(referenced.getValue().getFields()).thenReturn(innerFields);
        when(referenced.isValidForRead()).thenReturn(true);
        when(FlagUtils.getReadFlags(anyList(), any(byte[].class))).thenReturn(flags);
        when(FlagUtils.isFlagsField(innerFlags)).thenReturn(true);
        when(reader.getCharacteristicByType("org.bluetooth.characteristic_id")).thenReturn(referenced);

        LinkedHashMap<String, FieldHolder> result = parser.parse(characteristic, data);
        assertEquals(4, result.size());
        assertEquals(10, (int) result.get("Field1").getInteger(null));

        assertEquals(-12, (int) result.get("InnerField1").getInteger(null));

        assertEquals(13, (int) result.get("InnerField2").getInteger(null));

        assertEquals(14, (int) result.get("Field2").getInteger(null));
    }

    @Test
    public void serialize() {

        int sint4 = 13;
        String str = "awesome value";
        int strLength = BitSet.valueOf(str.getBytes()).length();

        when(twosComplementNumberFormatter.serialize(eq(sint4), eq(4), eq(true))).thenReturn(BitSet.valueOf(new long[] {sint4}));

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "uint8"));
        fields.add(MockUtils.mockFieldFormat("Field2", "sint4"));
        fields.add(MockUtils.mockFieldFormat("Field3", "utf8s"));
        fields.add(MockUtils.mockFieldFormat("Field4", "sint4"));
        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);

        request.setField("Field2", sint4);
        request.setField("Field3", str);
        request.setField("Field4", sint4);

        byte[] data = parser.serialize(request.getAllFieldHolders());
        assertNotNull(data);
        assertEquals(sint4, data[0] & 0x0000000F);
        BitSet textBytes = BitSet.valueOf(data).get(4, 4 + strLength);
        assertEquals(str, new String(textBytes.toByteArray()));

        BitSet field4 = BitSet.valueOf(data).get(4 + strLength, 4 + strLength + 4);
        assertEquals(sint4, field4.toByteArray()[0]);
    }

    @Test
    public void testSerializeBoolean() {
        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "boolean"));
        fields.add(MockUtils.mockFieldFormat("Field2", "boolean"));
        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);
        request.setField("Field1", true);
        request.setField("Field2", false);

        byte[] data = parser.serialize(request.getAllFieldHolders());
        assertNotNull(data);

        verify(parser, times(1)).serialize(true);
        assertTrue((data[0] & 1) == 1);
        assertTrue(((data[0] >>> 1) & 1) == 0);
    }

    @Test
    public void testSerializeReal() throws UnsupportedEncodingException {
        int sint32 = -123;
        int uint32 = 123;
        int sint64 = -124;
        int uint64 = 124;
        BigInteger sint128 = new BigInteger("-125");

        when(twosComplementNumberFormatter.serialize(anyInt(), anyInt(), eq(true))).thenReturn(BitSet.valueOf(new long[] { sint32 }));
        when(twosComplementNumberFormatter.serialize(anyLong(), anyInt(), eq(false))).thenReturn(BitSet.valueOf(new long[] { uint32 }));
        when(twosComplementNumberFormatter.serialize(anyLong(), anyInt(), eq(true))).thenReturn(BitSet.valueOf(new long[] { sint64 }));
        when(twosComplementNumberFormatter.serialize(any(BigInteger.class), anyInt(), eq(false))).thenReturn(BitSet.valueOf(new long[] { uint64 }));
        when(twosComplementNumberFormatter.serialize(any(BigInteger.class), anyInt(), eq(true))).thenReturn(BitSet.valueOf(sint128.toByteArray()));

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "sint32"));
        fields.add(MockUtils.mockFieldFormat("Field2", "uint32"));
        fields.add(MockUtils.mockFieldFormat("Field3", "sint64"));
        fields.add(MockUtils.mockFieldFormat("Field4", "uint64"));
        fields.add(MockUtils.mockFieldFormat("Field5", "sint128"));

        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);
        request.setField("Field1", sint32);
        request.setField("Field2", uint32);
        request.setField("Field3", sint64);
        request.setField("Field4", uint64);
        request.setField("Field5", sint128);

        byte[] data = parser.serialize(request.getAllFieldHolders());
        assertNotNull(data);
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        verify(twosComplementNumberFormatter, times(1)).serialize(sint32, 32, true);
        assertEquals(sint32, data[0]);
        verify(twosComplementNumberFormatter, times(1)).serialize((long) uint32, 32, false);
        assertEquals(uint32, data[4]);

        verify(twosComplementNumberFormatter, times(1)).serialize((long) sint64, 64, true);
        assertEquals(sint64, data[8]);

        verify(twosComplementNumberFormatter, times(1)).serialize(new BigInteger(String.valueOf(uint64)), 64, false);
        assertEquals(uint64, data[16]);

        verify(twosComplementNumberFormatter, times(1)).serialize(sint128, 128, true);
        // resulting array gets truncated to 20 bytes (length of GATT payload)
        assertEquals(20, data.length);
        //assertEquals(sint128.intValue(), data[24]);
    }

    @Test
    public void testSerializeZero() throws UnsupportedEncodingException {
        int sint32 = 0;

        when(twosComplementNumberFormatter.serialize(anyInt(), anyInt(), eq(true))).thenReturn(BitSet.valueOf(new long[] { 0 }));

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "sint32"));

        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);
        request.setField("Field1", sint32);

        byte[] data = parser.serialize(request.getAllFieldHolders());
        assertArrayEquals(new byte[] {0}, data);
    }

    @Test
    public void testSerializeString() throws UnsupportedEncodingException {
        String utf8 = "test8";
        String utf16 = new String("test16".getBytes("UTF-8"), "UTF-16");

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "utf8s"));
        fields.add(MockUtils.mockFieldFormat("Field2", "utf16s"));

        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);
        request.setField("Field1", utf8);
        request.setField("Field2", utf16);

        byte[] data = parser.serialize(request.getAllFieldHolders());
        BitSet bitSet = BitSet.valueOf(data);
        int utf8Length = BitSet.valueOf(utf8.getBytes()).length();
        assertEquals(utf8, new String(bitSet.get(0, utf8Length).toByteArray()));
        assertEquals(utf16, new String(bitSet.get(utf8Length, 8 * 20).toByteArray(), "UTF-16"));
    }

    @Test
    public void testSerializeDecimal() throws UnsupportedEncodingException {

        float float32 = 123.4F;
        double float64 = 456.7D;
        float sfloat = 89.1F;
        float _float = 234.5F;

        when(ieee754NumberFormatter.serializeFloat(anyFloat())).thenReturn(BitSet.valueOf(new long[] {
                Float.floatToRawIntBits(float32) }));
        when(ieee754NumberFormatter.serializeDouble(anyDouble())).thenReturn(BitSet.valueOf(new long[] {
                Double.doubleToLongBits(float64) }));

        when(ieee11073NumberFormatter.serializeSFloat(anyFloat())).thenReturn(BitSet.valueOf(new long[] {
                Float.floatToRawIntBits(sfloat) }));
        when(ieee11073NumberFormatter.serializeFloat(anyFloat())).thenReturn(BitSet.valueOf(new long[] {
                Float.floatToRawIntBits(_float) }));

        List<Field> fields = new ArrayList<>();
        fields.add(MockUtils.mockFieldFormat("Field1", "float32"));
        fields.add(MockUtils.mockFieldFormat("Field2", "float64"));
        fields.add(MockUtils.mockFieldFormat("Field3", "SFLOAT"));
        fields.add(MockUtils.mockFieldFormat("Field4", "FLOAT"));

        GattRequest request = new GattRequest(CHARACTERISTIC_UUID, fields);
        request.setField("Field1", float32);
        request.setField("Field2", float64);
        request.setField("Field3", sfloat);
        request.setField("Field4", _float);

        byte[] data = parser.serialize(request.getAllFieldHolders());
        assertNotNull(data);

        verify(ieee754NumberFormatter, times(1)).serializeFloat(float32);
        verify(ieee754NumberFormatter, times(1)).serializeDouble(float64);

        verify(ieee11073NumberFormatter, times(1)).serializeSFloat(sfloat);
        verify(ieee11073NumberFormatter, times(1)).serializeFloat(_float);
    }

    @Test
    public void testParseAndSerializeStringFields() throws Exception {
        assertParseAndSerializeStringFields("utf8s", "UTF-8");
        assertParseAndSerializeStringFields("utf16s", "UTF-16");
    }

    @Test
    public void testParseAndSerializeStructFields() throws Exception {
        // Testing that structure fields that go after another field can be parsed correctly

        // mocking number formatter to parse/serialize integers returning 1 set bit
        when(twosComplementNumberFormatter.deserializeInteger(Matchers.<BitSet>any(), anyByte(), anyBoolean())).thenReturn(1);
        BitSet oneBit = new BitSet();
        oneBit.set(0);
        when(twosComplementNumberFormatter.serialize(any(Integer.class), anyInt(), anyBoolean())).thenReturn(oneBit);

        // mocking fields, first field is a dummy field with a length of 1 bit, next one is our target field
        List<Field> fields = new ArrayList<>();
        Field field1 = MockUtils.mockFieldFormat("Field1", "uint1");
        fields.add(field1);
        Field field2 = MockUtils.mockFieldFormat("Field2", "struct");
        fields.add(field2);
        when(reader.getFields(characteristic)).thenReturn(fields);
        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        // data that we are testing
        byte[] field2Data = {12, 24, 56};
        BitSet data = new BitSet();
        data.set(0);
        parser.concat(data, BitSet.valueOf(field2Data), 1, field2Data.length * 8);

        // performing the test to check if we can parse data
        LinkedHashMap<String, FieldHolder> response = parser.parse(characteristic, data.toByteArray());
        assertEquals(2, response.size());
        assertEquals(1, (int) response.get("Field1").getInteger());
        assertArrayEquals(field2Data, response.get("Field2").getBytes());

        // Now, do it in reverse

        // mocking field holders
        FieldHolder holder1 = new FieldHolder(field1);
        holder1.setInteger(1);
        FieldHolder holder2 = new FieldHolder(field2);
        holder2.setStruct(field2Data);

        // performing the test to check if we can serialize holders, the initial data for the previous test
        // should match to the result
        byte[] serialized = parser.serialize(Arrays.asList(holder1, holder2));
        assertArrayEquals(data.toByteArray(), serialized);
    }

    private void assertFieldsExist(Object value, String... fieldNames) {
        Map<String, FieldHolder> values = parser.parse(characteristic, new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0});
        assertEquals(fieldNames.length, values.size());
        assertTrue(values.keySet().containsAll(Arrays.asList(fieldNames)));
        for (FieldHolder fieldHolder : values.values()) {
            assertEquals(value, fieldHolder.getRawValue());
        }
    }

    private void assertParseFormat(Object expected, String format, byte[] bytes) throws CharacteristicFormatException {
        assertParseFormat(expected, format, bytes, null);
    }

    private void assertParseFormat(Object expected, String format, byte[] bytes, Integer exponent)
            throws CharacteristicFormatException {
        List<Field> fields = new ArrayList<>();
        String name = format + "Field";
        Field field = MockUtils.mockFieldFormat(name, format);
        when(field.getDecimalExponent()).thenReturn(exponent);
        fields.add(field);
        when(reader.getFields(characteristic)).thenReturn(fields);
        //when(characteristic.getValue().getFlags()).thenReturn(null);
        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);
        Map<String, FieldHolder> values = parser.parse(characteristic, bytes);
        assertEquals(1, values.size());
        assertTrue(values.containsKey(name));
        assertEquals(expected, values.get(name).getRawValue());
    }

    private void assertParseAndSerializeStringFields(String format, String encoding) throws Exception {
        // Testing that string fields that go after another field can be parsed correctly

        // mocking test data
        // mocking number formatter to parse/serialize integers returning 5 bits set to 1
        int dummyNumber = 0b11111;
        when(twosComplementNumberFormatter.deserializeInteger(Matchers.<BitSet>any(), anyByte(), anyBoolean()))
                .thenReturn(dummyNumber);
        BitSet oneBit = new BitSet();
        oneBit.set(0, 5);
        when(twosComplementNumberFormatter.serialize(any(Integer.class), anyInt(), anyBoolean())).thenReturn(oneBit);

        // mocking fields, first field is a dummy field with a length of 1 bit, next one is our target field
        List<Field> fields = new ArrayList<>();
        Field field1 = MockUtils.mockFieldFormat("Field1", "uint5");
        fields.add(field1);
        Field field2 = MockUtils.mockFieldFormat("Field2", format);
        fields.add(field2);
        when(reader.getFields(characteristic)).thenReturn(fields);
        when(characteristic.getValue().getFields()).thenReturn(fields);
        when(characteristic.isValidForRead()).thenReturn(true);

        // data that we are testing
        String field2Text = "Test!";
        byte[] field2Data = field2Text.getBytes(encoding);
        BitSet data = new BitSet();
        data.set(0, 5);
        parser.concat(data, BitSet.valueOf(field2Data), 5, field2Data.length * 8);

        // performing the test to check if we can parse data
        LinkedHashMap<String, FieldHolder> response = parser.parse(characteristic, data.toByteArray());
        assertEquals(2, response.size());
        assertEquals(0b11111, (int) response.get("Field1").getInteger());
        assertEquals(field2Text, response.get("Field2").getString());

        // Now, do it in reverse

        // mocking field holders
        FieldHolder holder1 = new FieldHolder(field1);
        holder1.setInteger(0b11111);
        FieldHolder holder2 = new FieldHolder(field2);
        holder2.setString(field2Text);

        // performing the test to check if we can serialize holders, the initial data for the previous test
        // should match to the result
        byte[] serialized = parser.serialize(Arrays.asList(holder1, holder2));
        assertArrayEquals(data.toByteArray(), serialized);
    }

}
