package com.dfa

import org.junit.Assert.*
import org.junit.Test

/**
 * MainActivity 单元测试
 */
class MainActivityTest {

    @Test
    fun `MainActivity should extend ComponentActivity`() {
        // Verify that MainActivity is a subclass of ComponentActivity
        assertTrue(androidx.activity.ComponentActivity::class.java.isAssignableFrom(MainActivity::class.java))
    }

    @Test
    fun `MainActivity should have AndroidEntryPoint annotation`() {
        // Verify that MainActivity has the @AndroidEntryPoint annotation
        val annotation = MainActivity::class.java.getAnnotation(dagger.hilt.android.AndroidEntryPoint::class.java)
        assertNotNull(annotation)
    }
}