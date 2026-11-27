package com.remotepad.client.data

import android.database.sqlite.SQLiteConstraintException
import com.remotepad.client.data.local.DefaultMacros
import com.remotepad.client.data.local.DeviceSession
import com.remotepad.client.data.local.HostPCProfile
import com.remotepad.client.data.local.MacroPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HostPCProfileDaoTest : DatabaseTestBase() {

    private fun host(ip: String = "192.168.1.24", time: Long = 1) =
        HostPCProfile(hostName = "WORK-PC", ipAddress = ip, port = 8080, lastConnected = time)

    @Test
    fun `create, read, update and delete`() = runTest {
        val dao = db.hosts()
        val id = dao.insert(host())
        assertEquals("WORK-PC", dao.getById(id)?.hostName)

        assertEquals(1, dao.update(dao.getById(id)!!.copy(hostName = "LAB-PC")))
        assertEquals("LAB-PC", dao.find("192.168.1.24", 8080)?.hostName)

        assertEquals(1, dao.delete(dao.getById(id)!!))
        assertNull(dao.getById(id))
    }

    @Test
    fun `same address and port cannot be stored twice`() = runTest {
        db.hosts().insert(host())
        assertFailsWith<SQLiteConstraintException> { db.hosts().insert(host()) }
    }

    @Test
    fun `hosts are listed from the most recent`() = runTest {
        db.hosts().insert(host("10.0.0.1", time = 1))
        db.hosts().insert(host("10.0.0.2", time = 5))
        assertEquals(listOf("10.0.0.2", "10.0.0.1"), db.hosts().observeAll().first().map { it.ipAddress })
    }

    @Test
    fun `deleting host removes its sessions`() = runTest {
        val id = db.hosts().insert(host())
        db.sessions().insert(DeviceSession("t1", id, "Pixel", authTime = 1, isActive = true))
        db.hosts().deleteById(id)
        assertNull(db.sessions().getActive())
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeviceSessionDaoTest : DatabaseTestBase() {

    @Test
    fun `ending active session stores end time`() = runTest {
        val hostId = db.hosts().insert(HostPCProfile(hostName = "PC", ipAddress = "10.0.0.1", port = 8080))
        db.sessions().insert(DeviceSession("t1", hostId, "Pixel", authTime = 100, isActive = true))

        assertEquals(1, db.sessions().endActive(now = 500))
        assertNull(db.sessions().getActive())
        val stored = db.sessions().observeForHost(hostId).first().single()
        assertEquals(500L, stored.endTime)
    }

    @Test
    fun `old closed sessions are cleaned up, active stay`() = runTest {
        val hostId = db.hosts().insert(HostPCProfile(hostName = "PC", ipAddress = "10.0.0.1", port = 8080))
        db.sessions().insert(DeviceSession("old", hostId, "Pixel", authTime = 10, isActive = false))
        db.sessions().insert(DeviceSession("now", hostId, "Pixel", authTime = 20, isActive = true))

        assertEquals(1, db.sessions().deleteOlderThan(before = 50))
        assertEquals(listOf("now"), db.sessions().observeForHost(hostId).first().map { it.sessionToken })
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MacroPresetDaoTest : DatabaseTestBase() {

    @Test
    fun `create, read, update and delete`() = runTest {
        val dao = db.macros()
        val macro = MacroPreset("m1", "Знімок", "CTRL+SHIFT+S", "custom", "Мої")
        dao.insert(macro)
        assertNotNull(dao.getById("m1"))

        dao.update(macro.copy(title = "Знімок екрана"))
        assertEquals("Знімок екрана", dao.getById("m1")?.title)

        assertEquals(1, dao.delete(macro))
        assertNull(dao.getById("m1"))
    }

    @Test
    fun `seeding defaults twice keeps user changes`() = runTest {
        val dao = db.macros()
        dao.insertAll(DefaultMacros.all)
        dao.update(DefaultMacros.all.first().copy(title = "Мій заголовок"))
        dao.insertAll(DefaultMacros.all)
        assertEquals(DefaultMacros.all.size, dao.observeAll().first().size)
        assertEquals("Мій заголовок", dao.getById(DefaultMacros.all.first().macroId)?.title)
    }

    @Test
    fun `frequently used macros go first`() = runTest {
        val dao = db.macros()
        dao.insertAll(DefaultMacros.all)
        repeat(3) { dao.incrementUsage("save") }
        dao.incrementUsage("copy")
        val order = dao.observeAll().first().map { it.macroId }
        assertEquals(listOf("save", "copy"), order.take(2))
        assertTrue(dao.getById("save")!!.usageCount == 3)
    }
}
