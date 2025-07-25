package com.north.mobile.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyTest {
    
    @Test
    fun testMoneyCreation() {
        val money = Money(12345, Currency.CAD)
        assertEquals(12345L, money.amount)
        assertEquals(Currency.CAD, money.currency)
        assertEquals(123.45, money.dollars)
    }
    
    @Test
    fun testFromDollars() {
        val money = Money.fromDollars(123.45, Currency.CAD)
        assertEquals(12345L, money.amount)
        assertEquals(Currency.CAD, money.currency)
        assertEquals(123.45, money.dollars)
    }
    
    @Test
    fun testZero() {
        val zero = Money.zero()
        assertEquals(0L, zero.amount)
        assertEquals(Currency.CAD, zero.currency)
        assertTrue(zero.isZero)
        assertFalse(zero.isPositive)
        assertFalse(zero.isNegative)
    }
    
    @Test
    fun testPositiveNegativeZero() {
        val positive = Money(1000)
        val negative = Money(-1000)
        val zero = Money(0)
        
        assertTrue(positive.isPositive)
        assertFalse(positive.isNegative)
        assertFalse(positive.isZero)
        
        assertFalse(negative.isPositive)
        assertTrue(negative.isNegative)
        assertFalse(negative.isZero)
        
        assertFalse(zero.isPositive)
        assertFalse(zero.isNegative)
        assertTrue(zero.isZero)
    }
    
    @Test
    fun testAbsoluteValue() {
        val positive = Money(1000)
        val negative = Money(-1000)
        
        assertEquals(Money(1000), positive.absoluteValue)
        assertEquals(Money(1000), negative.absoluteValue)
    }
    
    @Test
    fun testAddition() {
        val money1 = Money(1000, Currency.CAD)
        val money2 = Money(2000, Currency.CAD)
        val result = money1 + money2
        
        assertEquals(3000L, result.amount)
        assertEquals(Currency.CAD, result.currency)
    }
    
    @Test
    fun testSubtraction() {
        val money1 = Money(3000, Currency.CAD)
        val money2 = Money(1000, Currency.CAD)
        val result = money1 - money2
        
        assertEquals(2000L, result.amount)
        assertEquals(Currency.CAD, result.currency)
    }
    
    @Test
    fun testMultiplication() {
        val money = Money(1000, Currency.CAD)
        val result = money * 2.5
        
        assertEquals(2500L, result.amount)
        assertEquals(Currency.CAD, result.currency)
    }
    
    @Test
    fun testDivision() {
        val money = Money(1000, Currency.CAD)
        val result = money / 2.0
        
        assertEquals(500L, result.amount)
        assertEquals(Currency.CAD, result.currency)
    }
    
    @Test
    fun testComparison() {
        val money1 = Money(1000, Currency.CAD)
        val money2 = Money(2000, Currency.CAD)
        val money3 = Money(1000, Currency.CAD)
        
        assertTrue(money2 > money1)
        assertTrue(money1 < money2)
        assertTrue(money1 == money3)
        assertTrue(money1 <= money3)
        assertTrue(money1 >= money3)
    }
    
    @Test
    fun testFormatCAD() {
        assertEquals("$0.00", Money(0).formatCAD())
        assertEquals("$10.50", Money(1050).formatCAD())
        assertEquals("$1,234.56", Money(123456).formatCAD())
        assertEquals("-$1,234.56", Money(-123456).formatCAD())
        assertEquals("$1.0M", Money(100000000).formatCAD()) // 1 million
    }
    
    @Test
    fun testFormat() {
        val cadMoney = Money(1050, Currency.CAD)
        val usdMoney = Money(1050, Currency.USD)
        
        assertEquals("$10.50", cadMoney.format())
        assertEquals("US$10.50", usdMoney.format())
    }
    
    @Test
    fun testParse() {
        assertEquals(Money(1050), Money.parse("$10.50"))
        assertEquals(Money(1050), Money.parse("10.50"))
        assertEquals(Money(123456), Money.parse("$1,234.56"))
        assertEquals(Money(-1050), Money.parse("-$10.50"))
        assertNull(Money.parse("invalid"))
        assertNull(Money.parse(""))
    }
    
    @Test
    fun testToString() {
        val money = Money(1050, Currency.CAD)
        assertEquals("$10.50", money.toString())
    }
}