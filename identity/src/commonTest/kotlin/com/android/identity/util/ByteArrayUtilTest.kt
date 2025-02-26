package com.android.identity.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ByteArrayUtilTest {

    // PutXXX methods.
    @Test
    fun putInt8_valid() {
        val byteArray = ByteArray(1)
        byteArray.putInt8(0, 0x01)
        assertEquals("01", byteArray.toHex())
    }

    @Test
    fun putInt8_outOfRange() {
        val byteArray = ByteArray(1)
        assertFailsWith<IllegalArgumentException> { byteArray.putInt8(0, Byte.MIN_VALUE - 1) }
        assertFailsWith<IllegalArgumentException> { byteArray.putInt8(0, Byte.MAX_VALUE + 1) }
    }

    @Test
    fun putInt16_valid() {
        val byteArray = ByteArray(2)
        byteArray.putInt16(0, 0x0102)
        assertEquals("0102", byteArray.toHex())
        byteArray.putInt16Le(0, 0x0102)
        assertEquals("0201", byteArray.toHex())
    }

    @Test
    fun putInt16_outOfRange() {
        val byteArray = ByteArray(2)
        assertFailsWith<IllegalArgumentException> { byteArray.putInt16(0, Short.MIN_VALUE - 1) }
        assertFailsWith<IllegalArgumentException> { byteArray.putInt16(0, Short.MAX_VALUE + 1) }
        assertFailsWith<IllegalArgumentException> { byteArray.putInt16Le(0, Short.MIN_VALUE - 1) }
        assertFailsWith<IllegalArgumentException> { byteArray.putInt16Le(0, Short.MAX_VALUE + 1) }
    }

    @Test
    fun putInt32_valid() {
        val byteArray = ByteArray(4)
        byteArray.putInt32(0, 0x01020304)
        assertEquals("01020304", byteArray.toHex())
        byteArray.putInt32Le(0, 0x01020304)
        assertEquals("04030201", byteArray.toHex())
    }

    @Test
    fun putInt64_valid() {
        val byteArray = ByteArray(8)
        byteArray.putInt64(0, 0x0102030405060708)
        assertEquals("0102030405060708", byteArray.toHex())
        byteArray.putInt64Le(0, 0x0102030405060708)
        assertEquals("0807060504030201", byteArray.toHex())
    }

    @Test
    fun putUInt8_valid() {
        val byteArray = ByteArray(1)
        byteArray.putUInt8(0, 0x01u)
        assertEquals("01", byteArray.toHex())
    }

    @Test
    fun putUInt8_outOfRange() {
        val byteArray = ByteArray(1)
        assertFailsWith<IllegalArgumentException> { byteArray.putUInt8(0, UByte.MIN_VALUE.toUInt() - 1u) }
        assertFailsWith<IllegalArgumentException> { byteArray.putUInt8(0, UByte.MAX_VALUE.toUInt() + 1u) }
    }

    @Test
    fun putUInt16_valid() {
        val byteArray = ByteArray(2)
        byteArray.putUInt16(0, 0x0102u)
        assertEquals("0102", byteArray.toHex())
        byteArray.putUInt16Le(0, 0x0102u)
        assertEquals("0201", byteArray.toHex())
    }

    @Test
    fun putUInt16_outOfRange() {
        val byteArray = ByteArray(2)
        assertFailsWith<IllegalArgumentException> { byteArray.putUInt16(0, UShort.MIN_VALUE - 1u) }
        assertFailsWith<IllegalArgumentException> { byteArray.putUInt16(0, UShort.MAX_VALUE.toUInt() + 1u) }
        assertFailsWith<IllegalArgumentException> { byteArray.putUInt16Le(0, UShort.MIN_VALUE.toUInt() - 1u) }
        assertFailsWith<IllegalArgumentException> { byteArray.putUInt16Le(0, UShort.MAX_VALUE.toUInt() + 1u) }
    }

    @Test
    fun putUInt32_valid() {
        val byteArray = ByteArray(4)
        byteArray.putUInt32(0, 0x01020304u)
        assertEquals("01020304", byteArray.toHex())
        byteArray.putUInt32Le(0, 0x01020304u)
        assertEquals("04030201", byteArray.toHex())
    }

    @Test
    fun putUInt64_valid() {
        val byteArray = ByteArray(8)
        byteArray.putUInt64(0, 0x0102030405060708u)
        assertEquals("0102030405060708", byteArray.toHex())
        byteArray.putUInt64Le(0, 0x0102030405060708u)
        assertEquals("0807060504030201", byteArray.toHex())
    }

    // GetXXX methods.
    @Test
    fun getInt8_roundtrip() {
        val byteArray = ByteArray(1)
        byteArray.putInt8(0, Byte.MIN_VALUE.toInt())
        assertEquals(Byte.MIN_VALUE, byteArray[0])

        byteArray.putInt8(0, 0)
        assertEquals(0, byteArray[0])

        byteArray.putInt8(0, Byte.MAX_VALUE.toInt())
        assertEquals(Byte.MAX_VALUE, byteArray[0])
    }

    @Test
    fun getInt16_roundtrip() {
        val byteArray = ByteArray(2)
        byteArray.putInt16(0, Short.MIN_VALUE.toInt())
        assertEquals(Short.MIN_VALUE, byteArray.getInt16(0))

        byteArray.putInt16(0, 0)
        assertEquals(0, byteArray.getInt16(0))

        byteArray.putInt16(0, Short.MAX_VALUE.toInt())
        assertEquals(Short.MAX_VALUE, byteArray.getInt16(0))

        byteArray.putInt16Le(0, Short.MIN_VALUE.toInt())
        assertEquals(Short.MIN_VALUE, byteArray.getInt16Le(0))

        byteArray.putInt16Le(0, 0)
        assertEquals(0, byteArray.getInt16Le(0))

        byteArray.putInt16Le(0, Short.MAX_VALUE.toInt())
        assertEquals(Short.MAX_VALUE, byteArray.getInt16Le(0))
    }

    @Test
    fun getInt32_roundtrip() {
        val byteArray = ByteArray(4)
        byteArray.putInt32(0, Int.MIN_VALUE)
        assertEquals(Int.MIN_VALUE, byteArray.getInt32(0))

        byteArray.putInt32(0, 0)
        assertEquals(0, byteArray.getInt32(0))

        byteArray.putInt32(0, Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, byteArray.getInt32(0))

        byteArray.putInt32Le(0, Int.MIN_VALUE)
        assertEquals(Int.MIN_VALUE, byteArray.getInt32Le(0))

        byteArray.putInt32Le(0, 0)
        assertEquals(0, byteArray.getInt32Le(0))

        byteArray.putInt32Le(0, Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, byteArray.getInt32Le(0))
    }

    @Test
    fun getInt64_roundtrip() {
        val byteArray = ByteArray(8)
        byteArray.putInt64(0, Long.MIN_VALUE)
        assertEquals(Long.MIN_VALUE, byteArray.getInt64(0))

        byteArray.putInt64(0, 0)
        assertEquals(0, byteArray.getInt64(0))

        byteArray.putInt64(0, Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, byteArray.getInt64(0))

        byteArray.putInt64Le(0, Long.MIN_VALUE)
        assertEquals(Long.MIN_VALUE, byteArray.getInt64Le(0))

        byteArray.putInt64Le(0, 0)
        assertEquals(0, byteArray.getInt64Le(0))

        byteArray.putInt64Le(0, Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, byteArray.getInt64Le(0))
    }

    @Test
    fun getUInt8_roundtrip() {
        val byteArray = ByteArray(1)
        byteArray.putUInt8(0, UByte.MIN_VALUE.toUInt())
        assertEquals(UByte.MIN_VALUE, byteArray.getUInt8(0))

        byteArray.putUInt8(0, 128u)
        assertEquals(128u, byteArray.getUInt8(0))

        byteArray.putUInt8(0, UByte.MAX_VALUE.toUInt())
        assertEquals(UByte.MAX_VALUE, byteArray.getUInt8(0))
    }

    @Test
    fun getUInt16_roundtrip() {
        val byteArray = ByteArray(2)
        byteArray.putUInt16(0, UShort.MIN_VALUE.toUInt())
        assertEquals(UShort.MIN_VALUE, byteArray.getUInt16(0))

        byteArray.putUInt16(0, 32768u)
        assertEquals(32768u, byteArray.getUInt16(0))

        byteArray.putUInt16(0, UShort.MAX_VALUE.toUInt())
        assertEquals(UShort.MAX_VALUE, byteArray.getUInt16(0))

        byteArray.putUInt16Le(0, UShort.MIN_VALUE.toUInt())
        assertEquals(UShort.MIN_VALUE, byteArray.getUInt16Le(0))

        byteArray.putUInt16Le(0, 32768u)
        assertEquals(32768u, byteArray.getUInt16Le(0))

        byteArray.putUInt16Le(0, UShort.MAX_VALUE.toUInt())
        assertEquals(UShort.MAX_VALUE, byteArray.getUInt16Le(0))
    }

    @Test
    fun getUInt32_roundtrip() {
        val byteArray = ByteArray(4)
        byteArray.putUInt32(0, UInt.MIN_VALUE)
        assertEquals(UInt.MIN_VALUE, byteArray.getUInt32(0))

        byteArray.putUInt32(0, 2147483647u)
        assertEquals(2147483647u, byteArray.getUInt32(0))

        byteArray.putUInt32Le(0, UInt.MIN_VALUE)
        assertEquals(UInt.MIN_VALUE, byteArray.getUInt32Le(0))

        byteArray.putUInt32Le(0, 2147483647u)
        assertEquals(2147483647u, byteArray.getUInt32Le(0))
    }

    @Test
    fun getUInt64_roundtrip() {
        val byteArray = ByteArray(8)
        byteArray.putUInt64(0, ULong.MIN_VALUE)
        assertEquals(ULong.MIN_VALUE, byteArray.getUInt64(0))

        byteArray.putUInt64(0, 4294967295u)
        assertEquals(4294967295u, byteArray.getUInt64(0))

        byteArray.putUInt64Le(0, ULong.MIN_VALUE)
        assertEquals(ULong.MIN_VALUE, byteArray.getUInt64Le(0))

        byteArray.putUInt64Le(0, 4294967295u)
        assertEquals(4294967295u, byteArray.getUInt64Le(0))
    }

    @Test
    fun getInt8_offset() {
        val byteArray = ByteArray(3)
        byteArray[1] = 100
        assertEquals(100, byteArray.getInt8(1))
    }

    @Test
    fun getInt16_offset() {
        val byteArray = ByteArray(4)
        byteArray.putInt16Le(2, 256)
        assertEquals(256, byteArray.getInt16Le(2))
        assertEquals(1, byteArray.getInt16(2))
    }

    @Test
    fun getInt32_offset() {
        val byteArray = ByteArray(6)
        byteArray.putInt32Le(2, 1000000)
        assertEquals(1000000, byteArray.getInt32Le(2))
        assertEquals(1078071040, byteArray.getInt32(2))
    }

    @Test
    fun getInt64_offset() {
        val byteArray = ByteArray(10)
        byteArray.putInt64Le(2, 10000000000)
        assertEquals(10000000000, byteArray.getInt64Le(2))
        assertEquals(64188750128742400, byteArray.getInt64(2))
    }

    @Test
    fun getUInt8_offset() {
        val byteArray = ByteArray(3)
        byteArray.putUInt8(1, 200u)
        assertEquals(200u, byteArray.getUInt8(1))
    }

    @Test
    fun getUInt16_offset() {
        val byteArray = ByteArray(4)
        byteArray.putUInt16Le(2, 60000u)
        assertEquals(60000u, byteArray.getUInt16Le(2))
        assertEquals(24810u, byteArray.getUInt16(2))
    }

    @Test
    fun getUInt32_offset() {
        val byteArray = ByteArray(6)
        byteArray.putUInt32Le(2, 3000000u)
        assertEquals(3000000u, byteArray.getUInt32Le(2))
        assertEquals(3234213120u, byteArray.getUInt32(2))
    }

    @Test
    fun getUInt64_offset() {
        val byteArray = ByteArray(10)
        byteArray.putUInt64Le(2, 10000000000u)
        assertEquals(10000000000u, byteArray.getUInt64Le(2))
        assertEquals(64188750128742400u, byteArray.getUInt64(2))
    }
}