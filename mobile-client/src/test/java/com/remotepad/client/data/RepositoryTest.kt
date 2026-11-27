package com.remotepad.client.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionHistoryRepositoryTest : DatabaseTestBase() {

    private val repo by lazy { ConnectionHistoryRepository(db.hosts(), db.sessions()) }

    @Test
    fun `repeated login updates host instead of duplicating`() = runTest {
        val first = repo.recordLogin("10.0.0.5", 8080, "PC", "t1", "Pixel", now = 1_000)
        val second = repo.recordLogin("10.0.0.5", 8080, "PC-RENAMED", "t2", "Pixel", now = 2_000)
        assertEquals(first, second)
        val host = repo.savedHosts.first().single()
        assertEquals("PC-RENAMED", host.hostName)
        assertEquals(2_000, host.lastConnected)
    }

    @Test
    fun `new login closes previous session`() = runTest {
        repo.recordLogin("10.0.0.5", 8080, "PC", "t1", "Pixel", now = 1_000)
        repo.recordLogin("10.0.0.5", 8080, "PC", "t2", "Pixel", now = 2_000)
        assertEquals("t2", db.sessions().getActive()?.sessionToken)
    }

    @Test
    fun `end session leaves no active one`() = runTest {
        repo.recordLogin("10.0.0.5", 8080, "PC", "t1", "Pixel", now = 1_000)
        repo.endSession(now = 3_000)
        assertNull(db.sessions().getActive())
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MacroRepositoryTest : DatabaseTestBase() {

    private val repo by lazy { MacroRepository(db.macros()) }

    @Test
    fun `new macro is normalized and stored`() = runTest {
        val saved = repo.save(" Знімок ", "ctrl+shift+s")
        assertEquals("Знімок", saved.title)
        assertEquals("CTRL+SHIFT+S", saved.keySequence)
        assertEquals(saved, db.macros().getById(saved.macroId))
    }

    @Test
    fun `invalid key sequence is rejected before saving`() = runTest {
        assertFailsWith<InvalidMacroException> { repo.save("Щось", "CTRL+F13") }
        assertFailsWith<InvalidMacroException> { repo.save("  ", "CTRL+C") }
        assertEquals(0, repo.macros.first().size)
    }

    @Test
    fun `existing macro is updated in place`() = runTest {
        val saved = repo.save("Знімок", "CTRL+S")
        repo.save("Зберегти все", "CTRL+SHIFT+S", existing = saved)
        val all = repo.macros.first()
        assertEquals(1, all.size)
        assertEquals("Зберегти все", all.single().title)
    }
}
