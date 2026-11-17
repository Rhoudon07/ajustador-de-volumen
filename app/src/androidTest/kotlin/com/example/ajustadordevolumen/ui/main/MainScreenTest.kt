package com.example.ajustadordevolumen.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun appTitle_exists() {
    composeTestRule.setContent { MainScreen() }
    composeTestRule.onNodeWithText("Ajustador de Volumen").assertExists()
  }
}
