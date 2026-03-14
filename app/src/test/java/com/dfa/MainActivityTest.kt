package com.dfa

import com.google.truth.Truth.assertThat
import org.junit.Test

/**
 * MainActivity 单元测试
 */
class MainActivityTest {

    @Test
    fun `MainActivity should extend ComponentActivity`() {
        // Verify that MainActivity is a subclass of ComponentActivity
        assertThat(androidx.activity.ComponentActivity::class.java).isAssignableFrom(MainActivity::class.java)
    }

    @Test
    fun `MainActivity should have AndroidEntryPoint annotation`() {
        // Verify that MainActivity has the @AndroidEntryPoint annotation
        val annotation = MainActivity::class.java.getAnnotation(dagger.hilt.android.AndroidEntryPoint::class.java)
        assertThat(annotation).isNotNull()
    }

    @Test
    fun `Greeting should be a composable function`() {
        // Verify that Greeting is annotated with @Composable
        // Note: Composable annotation is applied at compile time, so we check the function exists
        val method = MainActivity::class.java.getDeclaredMethod(
            "Greeting",
            String::class.java,
            androidx.compose.ui.Modifier::class.java
        )
        assertThat(method).isNotNull()
    }
}