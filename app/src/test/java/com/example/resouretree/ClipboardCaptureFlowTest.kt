package com.example.resouretree

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.resouretree.overlay.FloatingEntryService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ClipboardCaptureFlowTest {
    @get:Rule val compose = createComposeRule()
    private fun waitText(text: String) = compose.waitUntil(15_000) {
        compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }

    @Test fun focusedCaptureKeepsEditsAcrossRecreationAndReturnsWithoutOpeningHomeOrSaving() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as ResourceTreeApplication
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val prefs = context.getSharedPreferences("clipboard-import", 0)
        val handled = prefs.getString("handled", null)
        val before = runBlocking { app.repository.all.first() }.associateBy { it.id }
        compose.setContent { Text("来源页面") }
        var previous: ClipData? = null
        compose.runOnIdle {
            previous = clipboard.primaryClip
            clipboard.setPrimaryClip(ClipData.newPlainText("capture-test", "BV1meMS6rE6Z\n悬浮收集验证"))
        }
        var scenario: ActivityScenario<ClipboardCaptureActivity>? = null
        try {
            scenario = ActivityScenario.launch(FloatingEntryService.captureIntent(context))
            waitText("新建条目")
            compose.onNodeWithText("文本内容").assertTextContains("BV1meMS6rE6Z\n悬浮收集验证")
            compose.onNodeWithContentDescription("切换到图视角").assertDoesNotExist()
            compose.onNodeWithText("名称 *").performTextReplacement("尚未保存的悬浮草稿")
            compose.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("capture-test", "【淘宝】新的剪贴板")) }
            scenario.recreate()
            waitText("新建条目")
            compose.onNodeWithText("名称 *").assertTextContains("尚未保存的悬浮草稿")
            compose.onNodeWithText("文本内容").assertTextContains("BV1meMS6rE6Z\n悬浮收集验证")
            compose.onNodeWithText("返回", substring = false).performClick()
            compose.onNodeWithText("放弃", substring = false).performClick()
            compose.waitUntil(10_000) {
                compose.waitForIdle()
                var finishing = false
                scenario.onActivity { finishing = it.isFinishing }
                finishing
            }
            assertEquals(before, runBlocking { app.repository.all.first() }.associateBy { it.id })
        } finally {
            scenario?.close()
            // Restore from a focused host after the capture window has finished.
            compose.runOnIdle {
                if (previous != null) clipboard.setPrimaryClip(requireNotNull(previous))
                else if (android.os.Build.VERSION.SDK_INT >= 28) clipboard.clearPrimaryClip()
                else clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                prefs.edit().apply { if (handled == null) remove("handled") else putString("handled", handled) }.commit()
                app.clipboardDrafts.clearPending()
            }
        }
    }

    @Test fun explicitSaveCreatesOneTextItemAtHomeAndClosesCapture() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as ResourceTreeApplication
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        compose.setContent { Text("来源页面") }
        val before = runBlocking { app.repository.all.first() }.size
        compose.runOnIdle { clipboard.setPrimaryClip(ClipData.newPlainText("capture-test", "BV_SAVE_FROM_BUBBLE")) }
        ActivityScenario.launch<ClipboardCaptureActivity>(FloatingEntryService.captureIntent(context)).use { scenario ->
            waitText("新建条目")
            assertEquals(before, runBlocking { app.repository.all.first() }.size)
            compose.onNodeWithText("名称 *").performTextReplacement("悬浮保存验证")
            compose.onNodeWithText("保存", substring = false).performClick()
            compose.waitUntil(15_000) {
                compose.waitForIdle()
                var finishing = false
                scenario.onActivity { finishing = it.isFinishing }
                finishing
            }
            val nodes = runBlocking { app.repository.all.first() }
            assertEquals(before + 1, nodes.size)
            val saved = nodes.single { it.name == "悬浮保存验证" }
            assertNull(saved.parentId)
            assertEquals("BV_SAVE_FROM_BUBBLE", saved.content.text)
            assertEquals(com.example.resouretree.domain.model.ActionType.COPY_AND_LAUNCH, saved.action.type)
            assertTrue(saved.action.target.isNotBlank())
        }
    }
}
