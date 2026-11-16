package com.example.resouretree

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.resouretree.localization.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], qualifiers = "en")
class LanguageScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun menuAndEditorAreEnglishAndLanguageChoiceSurvivesRecreation() {
        compose.waitUntil(15000) { compose.onAllNodesWithText("＋ New").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Pocket Favorites").assertExists()
        compose.onNodeWithText("Menu").performClick()
        compose.onNodeWithText("App language").performClick()
        compose.onNodeWithText("Follow system").assertExists()
        compose.onNodeWithText("简体中文").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("跟随系统").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("返回").performClick()
        compose.onNodeWithText("外置收藏夹").assertExists()
        compose.onNodeWithText("菜单").performClick()
        compose.onNodeWithText("界面语言").performClick()
        compose.onNodeWithText("English").performClick()
        compose.waitUntil(15000) { compose.onAllNodesWithText("Follow system").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("＋ New").performClick()
        compose.onNodeWithText("New item").performClick()
        compose.onNodeWithText("Text content").assertExists()
        compose.onNodeWithText("Name *").performTextInput("My favorite")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Name *").assertTextContains("My favorite")
        assertEquals(AppLanguage.ENGLISH, AppLanguages.selected(compose.activity))
    }
}
