package com.dfa

import org.junit.Assert.*
import org.junit.Test

/**
 * DfaApplication 单元测试
 */
class DfaApplicationTest {

    @Test
    fun `DfaApplication should extend Application`() {
        // Verify that DfaApplication is a subclass of Application
        assertTrue(android.app.Application::class.java.isAssignableFrom(DfaApplication::class.java))
    }

    @Test
    fun `DfaApplication should have HiltAndroidApp annotation`() {
        // Verify that DfaApplication has the @HiltAndroidApp annotation
        val annotation = DfaApplication::class.java.getAnnotation(dagger.hilt.android.HiltAndroidApp::class.java)
        assertNotNull(annotation)
    }
}