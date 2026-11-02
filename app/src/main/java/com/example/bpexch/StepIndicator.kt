package com.example.bpexch

import android.view.View
import android.widget.TextView

/** Two-step flow state (layouts + "1 — 2" header) shared by the deposit and withdraw flows. */
class StepIndicator(
    private val ctx: AppContext,
    private val step1Layout: View,
    private val step2Layout: View,
    private val step1Icon: TextView,
    private val line: View,
    private val step2Icon: TextView,
    private val step2Label: TextView,
) {
    fun show(step: Int) {
        step1Layout.visibility = if (step == 1) View.VISIBLE else View.GONE
        step2Layout.visibility = if (step == 1) View.GONE else View.VISIBLE
        if (step == 1) {
            step1Icon.text = "1"
            step1Icon.backgroundTintList = ctx.colorStateList(R.color.deposit_green_light)
            step1Icon.setTextColor(ctx.color(R.color.deposit_green_dark))

            line.setBackgroundColor(ctx.color(R.color.border_subtle))

            step2Icon.text = "2"
            step2Icon.backgroundTintList = ctx.colorStateList(R.color.bg_input)
            step2Icon.setTextColor(ctx.color(R.color.text_secondary))
            step2Label.setTextColor(ctx.color(R.color.text_secondary))
        } else {
            step1Icon.text = "✓"
            step1Icon.backgroundTintList = ctx.colorStateList(R.color.deposit_green)
            step1Icon.setTextColor(ctx.color(R.color.text_white))

            line.setBackgroundColor(ctx.color(R.color.deposit_green))

            step2Icon.text = "2"
            step2Icon.backgroundTintList = ctx.colorStateList(R.color.deposit_green_light)
            step2Icon.setTextColor(ctx.color(R.color.deposit_green_dark))
            step2Label.setTextColor(ctx.color(R.color.text_primary))
        }
        ctx.binding.mainScrollView.scrollTo(0, 0)
    }
}
