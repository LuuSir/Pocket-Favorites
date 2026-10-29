package com.example.resouretree.overlay

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

/** Only a short tap collects; holding may continue into a drag and never triggers a click. */
internal class FloatingEntryTouchListener(
    private val view: View,
    private val onDown: () -> Unit,
    private val onDrag: (Float, Float) -> Unit,
    private val onDragEnd: () -> Unit
) : View.OnTouchListener {
    private val slop = ViewConfiguration.get(view.context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var active = false
    private var dragging = false
    fun cancel() { active = false }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancel()
                downX = event.rawX; downY = event.rawY
                active = true; dragging = false
                onDown()
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                val dx = event.rawX - downX; val dy = event.rawY - downY
                if (abs(dx) > slop || abs(dy) > slop) {
                    dragging = true
                }
                if (dragging) onDrag(dx, dy)
            }
            MotionEvent.ACTION_UP -> {
                if (active) {
                    if (dragging) onDragEnd()
                    else if (event.eventTime - event.downTime < ViewConfiguration.getLongPressTimeout() && view.isClickable) view.performClick()
                }
                active = false
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> cancel()
        }
        return true
    }
}
