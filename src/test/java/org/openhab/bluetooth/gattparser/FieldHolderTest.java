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
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openhab.bluetooth.gattparser.FieldHolder;
import org.openhab.bluetooth.gattparser.spec.Enumeration;
import org.openhab.bluetooth.gattparser.spec.Field;
import org.openhab.bluetooth.gattparser.spec.FieldType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FieldHolderTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Field field;

    @Before
    public void setUp() {
        when(field.getFormat().isStruct()).thenReturn(false);
    }

    @Test
    public void testGetField() throws Exception {
        FieldHolder fieldHolder = new FieldHolder(field, new Object());
        assertEquals(field, fieldHolder.getField());
    }

    @Test
    public void testGetBigDecimalExactDecimalScaling() {
        // 1234 * 10^-2 must be exactly 12.34, not 12.340000000000000... — the BigDecimal path
        // uses scaleByPowerOfTen, not Math.pow(10, e).
        FieldHolder fieldHolder = new FieldHolder(field, 1234);
        mockField(-2, null, null, null);
        assertEquals(0, fieldHolder.getBigDecimal(null).compareTo(new java.math.BigDecimal("12.34")));
        assertEquals("12.34", fieldHolder.getBigDecimal(null).stripTrailingZeros().toPlainString());
    }

    @Test
    public void testGetBigDecimalExactBinaryScaling() {
        // 32768 * 2^-6 = 512 exactly (e.g. the GATT Voltage characteristic, 1/64 V resolution).
        FieldHolder fieldHolder = new FieldHolder(field, 32768);
        mockField(null, -6, null, null);
        assertEquals(0, fieldHolder.getBigDecimal(null).compareTo(new java.math.BigDecimal("512")));
    }

    @Test
    public void testIsNumber() throws Exception {
        FieldHolder fieldHolder = new FieldHolder(field, new Object());
        when(field.getFormat().isNumber()).thenReturn(true);
        assertTrue(fieldHolder.isNumber());
        when(field.getFormat().isNumber()).thenReturn(false);
        assertFalse(fieldHolder.isNumber());
        verify(field.getFormat(), times(2)).isNumber();
    }

    @Test
    public void testIsBoolean() throws Exception {
        FieldHolder fieldHolder = new FieldHolder(field, new Object());
        when(field.getFormat().isBoolean()).thenReturn(true);
        assertTrue(fieldHolder.isBoolean());
        when(field.getFormat().isBoolean()).thenReturn(false);
        assertFalse(fieldHolder.isBoolean());
        verify(field.getFormat(), times(2)).isBoolean();
    }

    @Test
    public void testIsString() throws Exception {
        FieldHolder fieldHolder = new FieldHolder(field, new Object());
        when(field.getFormat().isString()).thenReturn(true);
        assertTrue(fieldHolder.isString());
        when(field.getFormat().isString()).thenReturn(false);
        assertFalse(fieldHolder.isString());
        verify(field.getFormat(), times(2)).isString();
    }

    @Test
    public void testIsStruct() throws Exception {
        FieldHolder fieldHolder = new FieldHolder(field, new Object());
        when(field.getFormat().isStruct()).thenReturn(true);
        assertTrue(fieldHolder.isStruct());
        when(field.getFormat().isStruct()).thenReturn(false);
        assertFalse(fieldHolder.isStruct());
        verify(field.getFormat(), times(2)).isStruct();
    }

    @Test
    public void testGetInteger() throws Exception {
        assertGetInteger(0, null, null, null, null, 0);
        assertGetInteger(1, null, null, null, null, 1);
        assertGetInteger(10, null, null, null, null, 10L);
        assertGetInteger(1, null, null, null, null, BigInteger.ONE);
        assertGetInteger(1, null, null, null, null, 1.9F);
        assertGetInteger(1, null, null, null, null, 1.4F);
        assertGetInteger(1, null, null, null, null, 1.6D);

        assertGetInteger(1000000000, 9, null, null, null, 1);
        assertGetInteger((int) 10000000000L, 10, null, null, null, 1);
        assertGetInteger(1, null, 0, null, null, 1);
        assertGetInteger(Integer.MIN_VALUE, null, 31, null, null, 1);
        assertGetInteger(1000000000, null, null, 1000000000, null, 1);

        assertGetInteger(0, -1, null, null, null, 4);
        assertGetInteger(1, -1, null, null, null, 5);

        assertGetInteger(1, null, -1, null, null, 1);
        assertGetInteger(2, null, -1, null, null, 4);

        assertGetInteger(-1, null, null, -1, null, 1);

        assertGetInteger(null, 3, 4, 2, null, "test");

        assertGetInteger(2120, 1, 2, 3, 2000.0, 1);
    }

    @Test
    public void testGetLong() throws Exception {
        assertGetLong(1L, null, null, null, null, 1);
        assertGetLong(10L, null, null, null,  null,10L);
        assertGetLong(1L, null, null, null, null, BigInteger.ONE);
        assertGetLong(1L, null, null, null, null, 1.9F);
        assertGetLong(1L, null, null, null, null, 1.4F);
        assertGetLong(1L, null, null, null, null, 1.6D);

        assertGetLong(1000000000000000000L, 18, null, null, null, 1);
        assertGetLong(1L, null, 0, null, null, 1);

        assertGetLong(Long.MAX_VALUE, null, 63, null, null, 1);
        assertGetLong(1000000000L, null, null, 1000000000, null, 1);

        assertGetLong(0L, -1, null, null, null, 4);
        assertGetLong(1L, -1, null, null, null, 5);

        assertGetLong(1L, null, -1, null, null, 1);
        assertGetLong(2L, null, -1, null, null, 4);

        assertGetLong(-1L, null, null, -1, null, 1);

        assertGetLong(null, null, null, null, null, "test");

        assertGetLong(2120L, 1, 2, 3, 2000.0, 1);
    }

    @Test
    public void testGetBigInteger() throws Exception {
        assertGetBigInteger(BigInteger.ZERO, null, null, null, null, 0);
        assertGetBigInteger(BigInteger.valueOf(10), null, null, null, null, 10L);
        assertGetBigInteger(BigInteger.valueOf(1), null, null, null, null, BigInteger.ONE);
        //TODO rounding differs from getInteger and getLong
        assertGetBigInteger(BigInteger.valueOf(2), null, null, null, null, 1.9F);
        assertGetBigInteger(BigInteger.valueOf(1), null, null, null, null, 1.4F);
        assertGetBigInteger(BigInteger.valueOf(2), null, null, null, null, 1.6D);

        assertGetBigInteger(new BigInteger("1000000000000000000"), 18, null, null, null, BigInteger.ONE);
        assertGetBigInteger(BigInteger.ONE, null, 0, null, null, 1);

        assertGetBigInteger(BigInteger.ZERO, -1, null, null, null, new BigInteger("4"));
        assertGetBigInteger(BigInteger.ONE, -1, null, null, null, new BigInteger("5"));

        assertGetBigInteger(BigInteger.ONE, null, -1, null, null, 1);
        assertGetBigInteger(new BigInteger("2"), null, -1, null, null, 4);

        assertGetBigInteger(new BigInteger("-1"), null, null, -1, null, 1);

        assertGetBigInteger(null, null, null, null, null, "test");

        assertGetBigInteger(new BigInteger("2120"), 1, 2, 3, 2000.0, 1);
    }

    @Test
    public void testGetBoolean() {
        assertGetBoolean(Boolean.TRUE, Boolean.TRUE);
        assertGetBoolean(Boolean.FALSE, Boolean.FALSE);
        assertGetBoolean(Boolean.FALSE, 0);
        assertGetBoolean(Boolean.TRUE, 1);
        assertGetBoolean(null, 2);
        assertGetBoolean(null, -1);

        assertGetBoolean(Boolean.TRUE, "true");
        assertGetBoolean(Boolean.FALSE, "false");
    }

    @Test
    public void testGetString() {
        assertGetString("test", "test");
        assertGetString("-1", -1);
        assertGetString("100", 100);
        assertGetString("123456.789", 123456.789D);
        assertGetString("1.0E100", Math.pow(10, 100));
        assertGetString("5.5", 5.5F);
        assertGetString("100", 100L);
        assertGetString("-123456789", new BigInteger("-123456789"));
    }

    @Test
    public void testGetStruct() {
        when(field.getFormat().isStruct()).thenReturn(true);
        byte[] raw = { 0x6b, 0x65, 0x79, 0x20, 0x73, 0x75, 0x63 };
        FieldHolder fieldHolder = new FieldHolder(field, raw);
        assertEquals(BigInteger.valueOf(27995160020870507L), fieldHolder.getBigInteger());
        assertEquals(27995160020870507L, (long) fieldHolder.getLong());

        raw = new byte[] { 0x6b, 0x65, 0x79, 0x20 };
        fieldHolder = new FieldHolder(field, raw);
        assertEquals(544826731, (int) fieldHolder.getInteger());

        raw = new byte[] { 0x6b, 0x65, 0x79, 0x20 };
        fieldHolder = new FieldHolder(field, raw);
        assertEquals("544826731", fieldHolder.getString());
    }

    @Test
    public void testSetStruct() {
        when(field.getMinimum()).thenReturn(-Double.MAX_VALUE);
        when(field.getMaximum()).thenReturn(Double.MAX_VALUE);

        when(field.getFormat().isStruct()).thenReturn(true);
        byte[] expected = { 0x6b, 0x65, 0x79, 0x20, 0x73, 0x75, 0x63 };
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setStruct(expected);
        assertArrayEquals(expected, (byte[]) fieldHolder.getRawValue());
        assertEquals(BigInteger.valueOf(27995160020870507L), fieldHolder.getBigInteger());

        fieldHolder.setBigInteger(BigInteger.valueOf(27995160020870507L));
        assertArrayEquals(expected, (byte[]) fieldHolder.getRawValue());
    }

    @Test
    public void testGetSetEnumerationString() {
        when(field.getFormat().isString()).thenReturn(true);
        when(field.getFormat().getType()).thenReturn(FieldType.UTF8S);

        List<Enumeration> enums = new ArrayList<>();

        BigInteger fail = new BigInteger("7811882119910221163");
        BigInteger suc = new BigInteger("27995160020870507");

        Enumeration enumerationFail = mock(Enumeration.class);
        when(enumerationFail.getKey()).thenReturn(fail);
        Enumeration enumerationSuc = mock(Enumeration.class);
        when(enumerationSuc.getKey()).thenReturn(suc);

        enums.add(enumerationFail);
        enums.add(enumerationSuc);
        when(field.getEnumerations().getEnumerations()).thenReturn(enums);

        // testing the Set method
        FieldHolder fieldHolderSet = new FieldHolder(field);
        fieldHolderSet.setEnumeration(enumerationFail);
        assertEquals("key fail", fieldHolderSet.getRawValue());
        assertEquals("key fail", fieldHolderSet.getString());

        // testing the Get method
        FieldHolder fieldHolderGet = new FieldHolder(field);
        fieldHolderGet.setString("key fail");
        assertEquals(fail, fieldHolderGet.getEnumeration().getKey());
        fieldHolderGet.setString("key suc");
        assertEquals(suc, fieldHolderGet.getEnumeration().getKey());
    }

    @Test
    public void testGetSetEnumerationStruct() {
        when(field.getFormat().isStruct()).thenReturn(true);

        List<Enumeration> enums = new ArrayList<>();

        BigInteger fail = new BigInteger("7811882119910221163");
        BigInteger suc = new BigInteger("27995160020870507");

        Enumeration enumerationFail = mock(Enumeration.class);
        when(enumerationFail.getKey()).thenReturn(fail);
        Enumeration enumerationSuc = mock(Enumeration.class);
        when(enumerationSuc.getKey()).thenReturn(suc);

        enums.add(enumerationFail);
        enums.add(enumerationSuc);
        when(field.getEnumerations().getEnumerations()).thenReturn(enums);

        byte[] failData = { 0x6b, 0x65, 0x79, 0x20, 0x66, 0x61, 0x69, 0x6c };
        byte[] sucData = { 0x6b, 0x65, 0x79, 0x20, 0x73, 0x75, 0x63 };
        // testing the Set method
        FieldHolder fieldHolderSet = new FieldHolder(field);
        fieldHolderSet.setEnumeration(enumerationFail);
        assertArrayEquals(failData, (byte[]) fieldHolderSet.getRawValue());
        assertArrayEquals(failData, fieldHolderSet.getBytes());

        // testing the Get method
        FieldHolder fieldHolderGet = new FieldHolder(field);
        fieldHolderGet.setStruct(failData);
        assertEquals(fail, fieldHolderGet.getEnumeration().getKey());
        fieldHolderGet.setRawValue(sucData);
        assertEquals(suc, fieldHolderGet.getEnumeration().getKey());
    }

    @Test
    public void testGetSetEnumerationLong() {
        when(field.getMinimum()).thenReturn(-Double.MAX_VALUE);
        when(field.getMaximum()).thenReturn(Double.MAX_VALUE);
        when(field.getFormat().isStruct()).thenReturn(false);
        when(field.getFormat().isString()).thenReturn(false);
        when(field.getFormat().getType()).thenReturn(FieldType.SINT);
        when(field.getFormat().getSize()).thenReturn(64);

        List<Enumeration> enums = new ArrayList<>();

        BigInteger fail = new BigInteger("7811882119910221163");
        BigInteger suc = new BigInteger("27995160020870507");

        Enumeration enumerationFail = mock(Enumeration.class);
        when(enumerationFail.getKey()).thenReturn(fail);
        Enumeration enumerationSuc = mock(Enumeration.class);
        when(enumerationSuc.getKey()).thenReturn(suc);

        enums.add(enumerationFail);
        enums.add(enumerationSuc);
        when(field.getEnumerations().getEnumerations()).thenReturn(enums);

        // testing the Set method
        FieldHolder fieldHolderSet = new FieldHolder(field);
        fieldHolderSet.setEnumeration(enumerationSuc);
        assertEquals(27995160020870507L, (long) fieldHolderSet.getRawValue());
        assertEquals(27995160020870507L, (long) fieldHolderSet.getLong());

        // testing the Get method
        FieldHolder fieldHolderGet = new FieldHolder(field);
        fieldHolderGet.setLong(27995160020870507L);
        assertEquals(suc, fieldHolderGet.getEnumeration().getKey());
        assertEquals(27995160020870507L, fieldHolderGet.getRawValue());
    }

    @Test
    public void testGetRawValue() {
        Object value = new Object();
        assertEquals(value, new FieldHolder(field, value).getRawValue());
    }

    @Test
    public void testGetFloat() {
        assertGetFloat(0.1F, null, null, null, null, 0.1);
        assertGetFloat(0.1F, null, null, null, null, 0.1D);
        assertGetFloat(5F, null, null, null, null, 5);
        assertGetFloat(5F, null, null, null, null, 5L);
        assertGetFloat(5.5F, null, null, null, null, "5.5");
        assertGetFloat(12345F, null, null, null, null, new BigInteger("12345"));

        assertGetFloat(Float.MAX_VALUE, 38, null, null, null, 3.4028235);
        assertGetFloat(1F, null, 0, null, null, 1);

        assertGetFloat(Float.MAX_VALUE, null, 127, null, null, 0x1.fffffeP+0f);
        assertGetFloat(1000000000F, null, null, 1000000000, null, 1);

        assertGetFloat(0.4F, -1, null, null, null, 4);
        assertGetFloat(0.5F, -1, null, null, null, 5);

        assertGetFloat(0.5F, null, -1, null, null, 1);
        assertGetFloat(2F, null, -1, null, null, 4);

        assertGetFloat(-1F, null, null, -1, null, 1);

        assertGetFloat(null, null, null, null, null, "test");

        assertGetFloat(2120.0F, 1, 2, 3, 2000.0, 1);

    }

    @Test
    public void testGetDouble() {
        assertGetDouble(0.1D, null, null, null, null, 0.1);
        assertGetDouble(0.1D, null, null, null, null, 0.1D);
        assertGetDouble(5D, null, null, null, null, 5);
        assertGetDouble(5D, null, null, null, null, 5L);
        assertGetDouble(5.5D, null, null, null, null, "5.5");
        assertGetDouble(12345D, null, null, null, null, new BigInteger("12345"));

        assertGetDouble(Double.MAX_VALUE, 308, null, null, null, 1.7976931348623157);
        assertGetDouble(1D, null, 0, null, null, 1);

        assertGetDouble(Double.MAX_VALUE, null, 1023, null, null, 0x1.fffffffffffffP+0);
        assertGetDouble(1000000000D, null, null, 1000000000, null, 1);

        assertGetDouble(0.4D, -1, null, null, null, 4);
        assertGetDouble(0.5D, -1, null, null, null, 5);

        assertGetDouble(0.5D, null, -1, null, null, 1);
        assertGetDouble(2D, null, -1, null, null, 4);

        assertGetDouble(-1D, null, null, -1, null, 1);

        assertGetDouble(null, null, null, null, null, "test");

        assertGetDouble(2120.0D, 1, 2, 3, 2000.0, 1);
    }

    @Test
    public void testSetInteger() throws Exception {
        mockField(1, 2, 3, 0.0);

        assertSet(FieldHolder::setInteger, 0, FieldType.SINT, 32, 0);
        assertSet(FieldHolder::setInteger, 0L, FieldType.SINT, 33, 0);
        assertSet(FieldHolder::setInteger, 0L, FieldType.SINT, 64, 0);
        assertSet(FieldHolder::setInteger, BigInteger.ZERO, FieldType.SINT, 65, 0);

        assertSet(FieldHolder::setInteger, 0, FieldType.UINT, 31, 0);
        assertSet(FieldHolder::setInteger, 0L, FieldType.UINT, 32, 0);
        assertSet(FieldHolder::setInteger, 0L, FieldType.UINT, 63, 0);
        assertSet(FieldHolder::setInteger, BigInteger.ZERO, FieldType.UINT, 64, 0);

        mockField(1, 2, 3, 2000.0);
        assertSet(FieldHolder::setInteger, 1, FieldType.SINT, 32, 2120);
        assertSet(FieldHolder::setInteger, 1L, FieldType.UINT, 32, 2120);
    }

    @Test
    public void testSetLong() throws Exception {
        mockField(1, 2, 3, 0.0);

        assertSet(FieldHolder::setLong, 0, FieldType.SINT, 32, 0L);
        assertSet(FieldHolder::setLong, 0L, FieldType.SINT, 33, 0L);
        assertSet(FieldHolder::setLong, 0L, FieldType.SINT, 64, 0L);
        assertSet(FieldHolder::setLong, BigInteger.ZERO, FieldType.SINT, 65, 0L);

        assertSet(FieldHolder::setLong, 0, FieldType.UINT, 31, 0L);
        assertSet(FieldHolder::setLong, 0L, FieldType.UINT, 32, 0L);
        assertSet(FieldHolder::setLong, 0L, FieldType.UINT, 63, 0L);
        assertSet(FieldHolder::setLong, BigInteger.ZERO, FieldType.UINT, 64, 0L);

        mockField(1, 2, 3, 2000.0);
        assertSet(FieldHolder::setLong, 1, FieldType.SINT, 32, 2120L);
        assertSet(FieldHolder::setLong, 1L, FieldType.UINT, 32, 2120L);
    }

    @Test
    public void testSetBigInteger() throws Exception {
        mockField(1, 2, 3, 0.0);

        assertSet(FieldHolder::setBigInteger, 0, FieldType.SINT, 32, BigInteger.ZERO);
        assertSet(FieldHolder::setBigInteger, 0L, FieldType.SINT, 33, BigInteger.ZERO);
        assertSet(FieldHolder::setBigInteger, 0L, FieldType.SINT, 64, BigInteger.ZERO);
        assertSet(FieldHolder::setBigInteger, BigInteger.ZERO, FieldType.SINT, 65, BigInteger.ZERO);

        assertSet(FieldHolder::setBigInteger, 0, FieldType.UINT, 31, BigInteger.ZERO);
        assertSet(FieldHolder::setBigInteger, 0L, FieldType.UINT, 32, BigInteger.ZERO);
        assertSet(FieldHolder::setBigInteger, 0L, FieldType.UINT, 63, BigInteger.ZERO);
        assertSet(FieldHolder::setBigInteger, BigInteger.ZERO, FieldType.UINT, 64, BigInteger.ZERO);

        mockField(1, 2, 3, 2000.0);
        assertSet(FieldHolder::setBigInteger, 1, FieldType.SINT, 32, BigInteger.valueOf(2120));
        assertSet(FieldHolder::setBigInteger, 1L, FieldType.UINT, 32, BigInteger.valueOf(2120));
    }

    @Test
    public void testSetFloat() throws Exception {
        mockField(1, 2, 3, 0.0);

        assertSet(FieldHolder::setFloat, 0.0F, FieldType.FLOAT_IEE754, 32, 0.0F);
        assertSet(FieldHolder::setFloat, 0.0D, FieldType.FLOAT_IEE754, 33, 0.0F);
        assertSet(FieldHolder::setFloat, 0.0D, FieldType.FLOAT_IEE754, 64, 0.0F);
        assertSet(FieldHolder::setFloat, 0.0D, FieldType.FLOAT_IEE754, 65, 0.0F);

        assertSet(FieldHolder::setFloat, 0.0F, FieldType.FLOAT_IEE11073, 32, 0.0F);
        assertSet(FieldHolder::setFloat, 0.0D, FieldType.FLOAT_IEE11073, 63, 0.0F);
        assertSet(FieldHolder::setFloat, 0.0D, FieldType.FLOAT_IEE11073, 64, 0.0F);
        assertSet(FieldHolder::setFloat, 0.0D, FieldType.FLOAT_IEE11073, 65, 0.0F);

        mockField(1, 2, 3, 2000.0);
        assertSet(FieldHolder::setFloat, 1.0F, FieldType.FLOAT_IEE754, 32, 2120.0F);
        assertSet(FieldHolder::setFloat, 1.0F, FieldType.FLOAT_IEE11073, 32, 2120.0F);
        assertSet(FieldHolder::setFloat, 1.0D, FieldType.FLOAT_IEE754, 33, 2120.0F);
        assertSet(FieldHolder::setFloat, 1.0D, FieldType.FLOAT_IEE11073, 33, 2120.0F);
    }

    @Test
    public void testSetDouble() throws Exception {
        mockField(1, 2, 3, 0.0);

        assertSet(FieldHolder::setDouble, 0.0F, FieldType.FLOAT_IEE754, 32, 0.0D);
        assertSet(FieldHolder::setDouble, 0.0D, FieldType.FLOAT_IEE754, 33, 0.0D);
        assertSet(FieldHolder::setDouble, 0.0D, FieldType.FLOAT_IEE754, 64, 0.0D);
        assertSet(FieldHolder::setDouble, 0.0D, FieldType.FLOAT_IEE754, 65, 0.0D);

        assertSet(FieldHolder::setDouble, 0.0F, FieldType.FLOAT_IEE11073, 32, 0.0D);
        assertSet(FieldHolder::setDouble, 0.0D, FieldType.FLOAT_IEE11073, 63, 0.0D);
        assertSet(FieldHolder::setDouble, 0.0D, FieldType.FLOAT_IEE11073, 64, 0.0D);
        assertSet(FieldHolder::setDouble, 0.0D, FieldType.FLOAT_IEE11073, 65, 0.0D);

        mockField(1, 2, 3, 2000.0);
        assertSet(FieldHolder::setDouble, 1.0F, FieldType.FLOAT_IEE754, 32, 2120.0D);
        assertSet(FieldHolder::setDouble, 1.0F, FieldType.FLOAT_IEE11073, 32, 2120.0D);
        assertSet(FieldHolder::setDouble, 1.0D, FieldType.FLOAT_IEE754, 33, 2120.0D);
        assertSet(FieldHolder::setDouble, 1.0D, FieldType.FLOAT_IEE11073, 33, 2120.0D);
    }

    @Test
    public void testSetMinMax() {
        when(field.getFormat().getType()).thenReturn(FieldType.SINT);
        when(field.getMinimum()).thenReturn(76.9);
        when(field.getMaximum()).thenReturn(77.1);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setInteger(77);
        fieldHolder.setLong(77L);
        fieldHolder.setBigInteger(BigInteger.valueOf(77));
        fieldHolder.setFloat(76.9F);
        fieldHolder.setDouble(76.9D);

        fieldHolder.setFloat(77.1F);
        fieldHolder.setDouble(77.1D);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMinimumInteger() {
        when(field.getMinimum()).thenReturn(76.9);
        when(field.getMaximum()).thenReturn(Double.MAX_VALUE);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setInteger(76);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMinimumLong() {
        when(field.getMinimum()).thenReturn(76.9);
        when(field.getMaximum()).thenReturn(Double.MAX_VALUE);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setLong(76L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMinimumBigInteger() {
        when(field.getMinimum()).thenReturn(76.9);
        when(field.getMaximum()).thenReturn(Double.MAX_VALUE);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setBigInteger(BigInteger.valueOf(76));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMinimumFloat() {
        when(field.getMinimum()).thenReturn(76.9);
        when(field.getMaximum()).thenReturn(Double.MAX_VALUE);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setFloat(76.89F);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMinimumDouble() {
        when(field.getMinimum()).thenReturn(76.9);
        when(field.getMaximum()).thenReturn(Double.MAX_VALUE);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setDouble(76.89D);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaximumInteger() {
        when(field.getMinimum()).thenReturn(-Double.MAX_VALUE);
        when(field.getMaximum()).thenReturn(77.1);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setInteger(78);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaximumLong() {
        when(field.getMinimum()).thenReturn(-Double.MAX_VALUE);
        when(field.getMaximum()).thenReturn(77.1);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setLong(78L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaximumBigInteger() {
        when(field.getMinimum()).thenReturn(-Double.MAX_VALUE);
        when(field.getMaximum()).thenReturn(77.1);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setBigInteger(BigInteger.valueOf(78));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaximumFloat() {
        when(field.getMinimum()).thenReturn(-Double.MAX_VALUE);
        when(field.getMaximum()).thenReturn(77.1);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setFloat(77.11F);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetMaximumDouble() {
        when(field.getMinimum()).thenReturn(-Double.MAX_VALUE);
        when(field.getMaximum()).thenReturn(77.1);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setDouble(77.11D);
    }

    private void assertGetInteger(Integer expected, Integer decimalExponent, Integer binaryExponent,
            Integer multiplier, Double offset, Object value) {
        FieldHolder fieldHolder = new FieldHolder(field, value);
        mockField(decimalExponent, binaryExponent, multiplier, offset);
        assertEquals(expected, fieldHolder.getInteger(null));
    }

    private <T> void assertSet(BiConsumer<FieldHolder, T> setter, Object expected, FieldType fieldType, int size, T value) {
        when(field.getFormat().getType()).thenReturn(fieldType);
        when(field.getFormat().getSize()).thenReturn(size);
        when(field.getMinimum()).thenReturn(-Double.MAX_VALUE);
        when(field.getMaximum()).thenReturn(Double.MAX_VALUE);
        FieldHolder fieldHolder = new FieldHolder(field);
        setter.accept(fieldHolder, value);
        assertEquals(expected, fieldHolder.getRawValue());
    }

    private void assertSetLong(Object expected, FieldType fieldType, int size, Long value) {
        when(field.getFormat().getType()).thenReturn(fieldType);
        when(field.getFormat().getSize()).thenReturn(size);
        when(field.getMinimum()).thenReturn(-Double.MAX_VALUE);
        when(field.getMaximum()).thenReturn(Double.MAX_VALUE);
        FieldHolder fieldHolder = new FieldHolder(field);
        fieldHolder.setLong(value);
        assertEquals(expected, fieldHolder.getRawValue());
    }

    private void assertGetLong(Long expected, Integer decimalExponent, Integer binaryExponent,
            Integer multiplier, Double offset, Object value) {
        FieldHolder fieldHolder = new FieldHolder(field, value);
        mockField(decimalExponent, binaryExponent, multiplier, offset);
        assertEquals(expected, fieldHolder.getLong(null));
    }

    private void assertGetFloat(Float expected, Integer decimalExponent, Integer binaryExponent,
            Integer multiplier, Double offset, Object value) {
        FieldHolder fieldHolder = new FieldHolder(field, value);
        mockField(decimalExponent, binaryExponent, multiplier, offset);
        if (expected == null) {
            assertNull(fieldHolder.getFloat(null));
        } else {
            assertEquals(expected, fieldHolder.getFloat(null), 0.00001);
        }
    }

    private void assertGetDouble(Double expected, Integer decimalExponent, Integer binaryExponent,
            Integer multiplier, Double offset, Object value) {
        FieldHolder fieldHolder = new FieldHolder(field, value);
        mockField(decimalExponent, binaryExponent, multiplier, offset);
        if (expected == null) {
            assertNull(fieldHolder.getDouble(null));
        } else {
            assertEquals(expected, fieldHolder.getDouble(null), 0.00001);
        }
    }

    private void assertGetBigInteger(BigInteger expected, Integer decimalExponent, Integer binaryExponent,
            Integer multiplier, Double offset, Object value) {
        FieldHolder fieldHolder = new FieldHolder(field, value);
        mockField(decimalExponent, binaryExponent, multiplier, offset);
        assertEquals(expected, fieldHolder.getBigInteger(null));
    }

    private void assertGetBoolean(Boolean expected, Object value) {
        FieldHolder fieldHolder = new FieldHolder(field, value);
        assertEquals(expected, fieldHolder.getBoolean(null));
    }

    private void assertGetString(String expected, Object value) {
        FieldHolder fieldHolder = new FieldHolder(field, value);
        assertEquals(expected, fieldHolder.getString(null));
    }

    private void mockField(Integer decimalExponent, Integer binaryExponent, Integer multiplier, Double offset) {
        when(field.getDecimalExponent()).thenReturn(decimalExponent);
        when(field.getBinaryExponent()).thenReturn(binaryExponent);
        when(field.getMultiplier()).thenReturn(multiplier);
        when(field.getOffset()).thenReturn(offset);
    }

}
