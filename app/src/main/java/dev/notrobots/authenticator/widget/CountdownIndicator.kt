/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.notrobots.authenticator.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.authenticator.R

/**
 * View for a circular countdown indicator.
 *
 * The indicator is a filled arc which starts as a full circle (`360` degrees) and shrinks
 * to `0` degrees the less time is remaining.
 */
class CountdownIndicator(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : View(context, attrs, defStyleAttr){
    private val paint: Paint
    private var rect: RectF? = null

    /**
     * Countdown phase starting with `1` when a full cycle is remaining and shrinking to `0` the closer the countdown is to zero.
     */
    private var phase = 0.0

    init {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = context.resolveColorAttribute(R.attr.colorPrimary)
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    override fun onDraw(canvas: Canvas) {
        val angle = (phase * 360).toFloat()
        val startAngle = 270 - angle

        // Draw the sector/filled arc
        // We need to leave the leftmost column and the topmost row out of the drawingRect because
        // in anti-aliased mode drawArc and drawOval use these areas for some reason.

        // Just create the object once, we should prevent creating multiple objects on this function
        if (rect == null) {
            rect = RectF(
                1F,
                1F,
                (width - 1).toFloat(),
                (height - 1).toFloat()
            )
        }

        if (startAngle < 360) {
            canvas.drawArc(
                rect!!,
                startAngle,
                angle,
                true,
                paint
            )
        } else {
            // 360 degrees is equivalent to 0 degrees for drawArc, hence the drawOval below.
            canvas.drawOval(rect!!, paint)
        }
    }

    /**
     * Sets the phase of this indicator.
     *
     * @param phase phase `[0, 1]`: `1` when the maximum amount of time is remaining,
     * `0` when no time is remaining.
     */
    fun setPhase(phase: Double) {
        require(phase in 0.0..1.0) {
            "Phase must be a value between 0 and 1"
        }
        this.phase = phase
        invalidate()
    }

    /**
     * Sets color for the indicator.
     */
    fun setIndicatorColor(color: Int) {
        paint.color = color
        invalidate()
    }
}