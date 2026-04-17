package com.example.expirationtracker

// ============================================================
// RED PHASE — These tests define the parsing contract for
// Portuguese-BR voice commands BEFORE any implementation.
// ============================================================

import com.example.expirationtracker.voice.ParsedCommand
import com.example.expirationtracker.voice.VoiceCommandParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class VoiceCommandParserTest {

    private lateinit var parser: VoiceCommandParser

    @Before
    fun setUp() {
        parser = VoiceCommandParser()
    }

    // ── Keyword variations ────────────────────────────────────

    // RED: "adicionar" trigger word
    @Test
    fun `parses command starting with adicionar`() {
        val result = parser.parse("adicionar leite vence 20 de maio")
        assertNotNull(result)
        assertEquals("leite", result!!.productName)
    }

    // RED: "registrar" trigger word
    @Test
    fun `parses command starting with registrar`() {
        val result = parser.parse("registrar frango vence 25 de junho")
        assertNotNull(result)
        assertEquals("frango", result!!.productName)
    }

    // RED: "cadastrar" trigger word
    @Test
    fun `parses command starting with cadastrar`() {
        val result = parser.parse("cadastrar iogurte vence 10 de julho")
        assertNotNull(result)
        assertEquals("iogurte", result!!.productName)
    }

    // ── Date keyword variations ───────────────────────────────

    // RED: "validade" keyword instead of "vence"
    @Test
    fun `parses command using validade keyword`() {
        val result = parser.parse("adicionar queijo validade 15 de agosto")
        assertNotNull(result)
        assertEquals("queijo", result!!.productName)
    }

    // RED: "expira" keyword
    @Test
    fun `parses command using expira keyword`() {
        val result = parser.parse("adicionar manteiga expira 5 de setembro")
        assertNotNull(result)
        assertEquals("manteiga", result!!.productName)
    }

    // RED: "expira em" keyword phrase
    @Test
    fun `parses command using expira em keyword phrase`() {
        val result = parser.parse("registrar nata expira em 3 de outubro")
        assertNotNull(result)
        assertEquals("nata", result!!.productName)
    }

    // ── PT-BR month names ─────────────────────────────────────

    @Test
    fun `parses janeiro correctly`() {
        val result = parser.parse("adicionar arroz vence 10 de janeiro de 2027")
        assertNotNull(result)
        assertEquals(1, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses fevereiro correctly`() {
        val result = parser.parse("adicionar feijão vence 14 de fevereiro de 2027")
        assertNotNull(result)
        assertEquals(2, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses marco correctly`() {
        val result = parser.parse("adicionar óleo vence 1 de março de 2027")
        assertNotNull(result)
        assertEquals(3, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses abril correctly`() {
        val result = parser.parse("adicionar sal vence 2 de abril de 2027")
        assertNotNull(result)
        assertEquals(4, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses maio correctly`() {
        val result = parser.parse("adicionar açúcar vence 3 de maio de 2027")
        assertNotNull(result)
        assertEquals(5, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses junho correctly`() {
        val result = parser.parse("adicionar café vence 4 de junho de 2027")
        assertNotNull(result)
        assertEquals(6, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses julho correctly`() {
        val result = parser.parse("adicionar chá vence 5 de julho de 2027")
        assertNotNull(result)
        assertEquals(7, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses agosto correctly`() {
        val result = parser.parse("adicionar macarrão vence 6 de agosto de 2027")
        assertNotNull(result)
        assertEquals(8, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses setembro correctly`() {
        val result = parser.parse("adicionar farinha vence 7 de setembro de 2027")
        assertNotNull(result)
        assertEquals(9, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses outubro correctly`() {
        val result = parser.parse("adicionar azeite vence 8 de outubro de 2027")
        assertNotNull(result)
        assertEquals(10, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses novembro correctly`() {
        val result = parser.parse("adicionar vinagre vence 9 de novembro de 2027")
        assertNotNull(result)
        assertEquals(11, result!!.expirationDate.monthValue)
    }

    @Test
    fun `parses dezembro correctly`() {
        val result = parser.parse("adicionar molho vence 10 de dezembro de 2027")
        assertNotNull(result)
        assertEquals(12, result!!.expirationDate.monthValue)
    }

    // ── Date format: DD/MM/YYYY ───────────────────────────────

    @Test
    fun `parses numeric date format DD slash MM slash YYYY`() {
        val result = parser.parse("adicionar leite vence 25/05/2026")
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 5, 25), result!!.expirationDate)
    }

    // ── Date format: DD/MM (current year inferred) ────────────

    @Test
    fun `parses numeric date format DD slash MM without year`() {
        val result = parser.parse("adicionar suco vence 15/08")
        assertNotNull(result)
        assertEquals(8, result!!.expirationDate.monthValue)
        assertEquals(15, result!!.expirationDate.dayOfMonth)
    }

    // ── Full command extraction ───────────────────────────────

    @Test
    fun `extracts correct product name with multiple words`() {
        val result = parser.parse("adicionar creme de leite vence 20 de maio de 2026")
        assertNotNull(result)
        assertEquals("creme de leite", result!!.productName)
        assertEquals(LocalDate.of(2026, 5, 20), result!!.expirationDate)
    }

    @Test
    fun `extracts correct day and month`() {
        val result = parser.parse("registrar frango vence 25 de junho de 2026")
        assertNotNull(result)
        assertEquals(25, result!!.expirationDate.dayOfMonth)
        assertEquals(6, result!!.expirationDate.monthValue)
        assertEquals(2026, result!!.expirationDate.year)
    }

    // ── Case insensitivity ────────────────────────────────────

    @Test
    fun `parsing is case insensitive`() {
        val result = parser.parse("ADICIONAR Leite VENCE 20 DE MAIO DE 2026")
        assertNotNull(result)
        assertEquals("leite", result!!.productName)
    }

    // ── Invalid commands ──────────────────────────────────────

    @Test
    fun `returns null for empty input`() {
        assertNull(parser.parse(""))
    }

    @Test
    fun `returns null when trigger word is missing`() {
        assertNull(parser.parse("leite vence 20 de maio"))
    }

    @Test
    fun `returns null when date keyword is missing`() {
        assertNull(parser.parse("adicionar leite 20 de maio"))
    }

    @Test
    fun `returns null when product name is missing`() {
        assertNull(parser.parse("adicionar vence 20 de maio"))
    }

    @Test
    fun `returns null for gibberish input`() {
        assertNull(parser.parse("xyzabc 123"))
    }

    // ── ParsedCommand data class ──────────────────────────────

    @Test
    fun `ParsedCommand holds product name and expiration date`() {
        val date = LocalDate.of(2026, 6, 15)
        val cmd = ParsedCommand(productName = "leite", expirationDate = date)
        assertEquals("leite", cmd.productName)
        assertEquals(date, cmd.expirationDate)
    }
}
