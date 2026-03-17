package com.cdroid

import org.junit.Assert.*
import org.junit.Test

/**
 * CdroidApplication 单元测试
 */
class CdroidApplicationTest {

    @Test
    fun `CdroidApplication should extend Application`() {
        // Verify that CdroidApplication is a subclass of Application
        assertTrue(android.app.Application::class.java.isAssignableFrom(CdroidApplication::class.java))
    }

    @Test
    fun `CdroidApplication should have HiltAndroidApp annotation`() {
        // Verify that CdroidApplication has the @HiltAndroidApp annotation
        val annotation = CdroidApplication::class.java.getAnnotation(dagger.hilt.android.HiltAndroidApp::class.java)
        assertNotNull(annotation)
    }
}
