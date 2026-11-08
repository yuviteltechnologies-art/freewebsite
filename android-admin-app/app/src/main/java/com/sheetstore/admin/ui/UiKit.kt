package com.sheetstore.admin.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

object UiKit {

    // DP to PX Converter
    fun dp(context: Context, value: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (value * scale + 0.5f).toInt()
    }

    // Modern Native Card Drawable (Rounded + Sleek Dark Gradient / Surface)
    fun cardDrawable(
        bgColor: Int = Color.parseColor("#121826"),
        strokeColor: Int = Color.parseColor("#1F2D48"),
        strokeWidthDp: Int = 1,
        radiusDp: Float = 18f,
        context: Context? = null
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            val strokePx = if (context != null) dp(context, strokeWidthDp) else strokeWidthDp * 2
            setStroke(strokePx, strokeColor)
            val radiusPx = if (context != null) dp(context, radiusDp.toInt()).toFloat() else radiusDp * 2.5f
            cornerRadius = radiusPx
        }
    }

    // Modern Native Gradient Button Drawable
    fun gradientButtonDrawable(
        startColor: Int = Color.parseColor("#2563EB"),
        endColor: Int = Color.parseColor("#1D4ED8"),
        radiusDp: Float = 14f,
        context: Context? = null
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            val radiusPx = if (context != null) dp(context, radiusDp.toInt()).toFloat() else radiusDp * 2.5f
            cornerRadius = radiusPx
        }
    }

    // Modern Native Input Drawable (Dark Navy + Subtle Outline)
    fun inputDrawable(context: Context, isFocused: Boolean = false): GradientDrawable {
        val strokeColor = if (isFocused) Color.parseColor("#38BDF8") else Color.parseColor("#253450")
        return cardDrawable(
            bgColor = Color.parseColor("#0B0F19"),
            strokeColor = strokeColor,
            strokeWidthDp = if (isFocused) 2 else 1,
            radiusDp = 14f,
            context = context
        )
    }

    // Pill Badge Widget
    fun createPillBadge(
        context: Context,
        text: String,
        textColor: Int = Color.parseColor("#10B981"),
        bgColor: Int = Color.parseColor("#064E3B"),
        borderColor: Int = Color.parseColor("#10B981")
    ): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            gravity = Gravity.CENTER
            background = cardDrawable(
                bgColor = bgColor,
                strokeColor = borderColor,
                strokeWidthDp = 1,
                radiusDp = 50f,
                context = context
            )
            setPadding(dp(context, 14), dp(context, 5), dp(context, 14), dp(context, 5))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    // Modern Native Text Input Field
    fun createStyledInput(
        context: Context,
        hintText: String,
        initialText: String = "",
        singleLine: Boolean = true
    ): EditText {
        return EditText(context).apply {
            hint = hintText
            setText(initialText)
            textSize = 13.5f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#64748B"))
            background = inputDrawable(context, false)
            isSingleLine = singleLine
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 14)
            }
            setOnFocusChangeListener { _, hasFocus ->
                background = inputDrawable(context, hasFocus)
            }
        }
    }

    // Primary High-Conversion Action Button
    fun createPrimaryButton(
        context: Context,
        btnText: String,
        startColor: String = "#2563EB",
        endColor: String = "#1D4ED8",
        onClick: () -> Unit
    ): Button {
        return Button(context).apply {
            text = btnText
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = gradientButtonDrawable(
                startColor = Color.parseColor(startColor),
                endColor = Color.parseColor(endColor),
                radiusDp = 14f,
                context = context
            )
            elevation = dp(context, 4).toFloat()
            setPadding(dp(context, 20), dp(context, 14), dp(context, 20), dp(context, 14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { onClick() }
        }
    }

    // Secondary Glass / Dark Action Button
    fun createSecondaryButton(
        context: Context,
        btnText: String,
        textColorHex: String = "#CBD5E1",
        onClick: () -> Unit
    ): Button {
        return Button(context).apply {
            text = btnText
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(textColorHex))
            background = cardDrawable(
                bgColor = Color.parseColor("#1A2234"),
                strokeColor = Color.parseColor("#2E3D5B"),
                strokeWidthDp = 1,
                radiusDp = 12f,
                context = context
            )
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))
            setOnClickListener { onClick() }
        }
    }

    // Label TextView
    fun createFieldLabel(context: Context, label: String): TextView {
        return TextView(context).apply {
            text = label
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(dp(context, 2), 0, 0, dp(context, 6))
        }
    }

    // Section Container Card (for Grouped Settings)
    fun createSectionContainer(
        context: Context,
        title: String,
        badgeText: String? = null
    ): Pair<LinearLayout, LinearLayout> {
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#1E2D48"),
                strokeWidthDp = 1,
                radiusDp = 20f,
                context = context
            )
            setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 20))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 20)
            }
        }

        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(context, 14))
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(titleView)

        if (badgeText != null) {
            headerRow.addView(
                createPillBadge(
                    context = context,
                    text = badgeText,
                    textColor = Color.parseColor("#38BDF8"),
                    bgColor = Color.parseColor("#0C2A4A"),
                    borderColor = Color.parseColor("#1D4ED8")
                )
            )
        }
        card.addView(headerRow)

        val contentBody = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        card.addView(contentBody)

        return Pair(card, contentBody)
    }
}

