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
package dev.notrobots.authenticator.google

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * View for a circular countdown indicator.
 *
 *
 * The indicator is a filled arc which starts as a full circle (`360` degrees) and shrinks
 * to `0` degrees the less time is remaining.
 */
class CountdownIndicator @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val mRemainingSectorPaint: Paint
    private var mDrawingRect: RectF? = null

    /**
     * Countdown phase starting with `1` when a full cycle is remaining and shrinking to `0` the closer the countdown is to zero.
     */
    private var mPhase = 0.0
    override fun onDraw(canvas: Canvas) {
        val remainingSectorSweepAngle = (mPhase * 360).toFloat()
        val remainingSectorStartAngle = 270 - remainingSectorSweepAngle

        // Draw the sector/filled arc
        // We need to leave the leftmost column and the topmost row out of the drawingRect because
        // in anti-aliased mode drawArc and drawOval use these areas for some reason.

        // Just create the object once, we should prevent creating multiple objects on this function
        if (mDrawingRect == null) {
            mDrawingRect = RectF(1F, 1F, (width - 1).toFloat(), (height - 1).toFloat())
        }
        if (remainingSectorStartAngle < 360) {
            canvas.drawArc(
                mDrawingRect!!,
                remainingSectorStartAngle,
                remainingSectorSweepAngle,
                true,
                mRemainingSectorPaint
            )
        } else {
            // 360 degrees is equivalent to 0 degrees for drawArc, hence the drawOval below.
            canvas.drawOval(mDrawingRect!!, mRemainingSectorPaint)
        }
    }

    /**
     * Sets the phase of this indicator.
     *
     * @param phase phase `[0, 1]`: `1` when the maximum amount of time is remaining,
     * `0` when no time is remaining.
     */
    fun setPhase(phase: Double) {
        require(!(phase < 0 || phase > 1)) { "phase: $phase" }
        mPhase = phase
        invalidate()
    }

    /**
     * Set color for the indicator.
     */
    fun setIndicatorColor(color: Int) {
        mRemainingSectorPaint.color = color
    }

    companion object {
        private const val DEFAULT_COLOR = -0xcf9f40
    }

    init {
//        var color = DEFAULT_COLOR
//        val theme = context.theme
//        val appearance = theme.obtainStyledAttributes(
//            attrs, R.styleable.CountdownIndicator, 0, 0
//        )
//        if (appearance != null) {
//            val n = appearance.indexCount
//            for (i in 0 until n) {
//                val attr = appearance.getIndex(i)
//                if (attr == R.styleable.CountdownIndicator_countdownIndicatorColor) {
//                    color = appearance.getColor(attr, DEFAULT_COLOR)
//                }
//            }
//        }
        mRemainingSectorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mRemainingSectorPaint.color = DEFAULT_COLOR
    }
}