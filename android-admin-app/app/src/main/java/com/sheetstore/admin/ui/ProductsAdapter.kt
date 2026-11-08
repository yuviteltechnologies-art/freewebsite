package com.sheetstore.admin.ui

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.sheetstore.admin.models.Product

class ProductsAdapter(
    private var products: List<Product>,
    private val onSaveProduct: (Product, String, String, Boolean) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    fun updateData(newProducts: List<Product>) {
        this.products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val context = parent.context
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, UiKit.dp(context, 16))
            }
            setPadding(
                UiKit.dp(context, 18),
                UiKit.dp(context, 18),
                UiKit.dp(context, 18),
                UiKit.dp(context, 18)
            )
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#1F2D48"),
                strokeWidthDp = 1,
                radiusDp = 18f,
                context = context
            )
        }
        return ProductViewHolder(card)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position], onSaveProduct)
    }

    override fun getItemCount(): Int = products.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(product: Product, onSaveProduct: (Product, String, String, Boolean) -> Unit) {
            val layout = itemView as LinearLayout
            val context = layout.context
            layout.removeAllViews()

            // 1. Top Header: Product Title + Category Pill
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val titleView = TextView(context).apply {
                text = product.title
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerRow.addView(titleView)

            val catPill = UiKit.createPillBadge(
                context = context,
                text = product.category.uppercase(),
                textColor = Color.parseColor("#38BDF8"),
                bgColor = Color.parseColor("#0C2A4A"),
                borderColor = Color.parseColor("#1E40AF")
            )
            headerRow.addView(catPill)
            layout.addView(headerRow)

            // 2. Subtitle / Badge
            val badgeView = TextView(context).apply {
                text = product.badge ?: "Standard Listing"
                textSize = 11f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, UiKit.dp(context, 4), 0, UiKit.dp(context, 12))
            }
            layout.addView(badgeView)

            // 3. Dual Price Inputs Box (Selling Price & Original Price)
            val priceCard = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#0B0F19"),
                    strokeColor = Color.parseColor("#1C2638"),
                    strokeWidthDp = 1,
                    radiusDp = 12f,
                    context = context
                )
                setPadding(
                    UiKit.dp(context, 12),
                    UiKit.dp(context, 10),
                    UiKit.dp(context, 12),
                    UiKit.dp(context, 10)
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = UiKit.dp(context, 12)
                }
            }

            // Selling Price Column
            val sellingCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val sellingLabel = TextView(context).apply {
                text = "Offer Price (₹)"
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#10B981"))
            }
            sellingCol.addView(sellingLabel)

            val sellingInput = EditText(context).apply {
                setText(product.priceSelling)
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                background = null
                setPadding(0, UiKit.dp(context, 4), 0, 0)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            sellingCol.addView(sellingInput)
            priceCard.addView(sellingCol)

            // Original / MRP Price Column
            val originalCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val originalLabel = TextView(context).apply {
                text = "Original / MRP (₹)"
                textSize = 10.5f
                setTextColor(Color.parseColor("#94A3B8"))
            }
            originalCol.addView(originalLabel)

            val originalInput = EditText(context).apply {
                setText(product.priceOriginal)
                setTextColor(Color.parseColor("#CBD5E1"))
                textSize = 13f
                background = null
                setPadding(0, UiKit.dp(context, 4), 0, 0)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            originalCol.addView(originalInput)
            priceCard.addView(originalCol)

            layout.addView(priceCard)

            // 4. Action Row: Active Switch + Save Button
            val actionRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val isCurrentlyActive = (product.isActive == null || product.isActive.equals("TRUE", ignoreCase = true))
            val activeSwitch = Switch(context).apply {
                text = "Live On Site"
                setTextColor(Color.parseColor("#CBD5E1"))
                textSize = 11.5f
                isChecked = isCurrentlyActive
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            actionRow.addView(activeSwitch)

            val saveBtn = Button(context).apply {
                text = "💾 Update Price"
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = UiKit.gradientButtonDrawable(
                    startColor = Color.parseColor("#2563EB"),
                    endColor = Color.parseColor("#1D4ED8"),
                    radiusDp = 10f,
                    context = context
                )
                setPadding(UiKit.dp(context, 16), UiKit.dp(context, 8), UiKit.dp(context, 16), UiKit.dp(context, 8))
                setOnClickListener {
                    val newSelling = sellingInput.text.toString().trim()
                    val newOriginal = originalInput.text.toString().trim()
                    val isActive = activeSwitch.isChecked
                    if (newSelling.isNotEmpty()) {
                        onSaveProduct(product, newSelling, newOriginal, isActive)
                    }
                }
            }
            actionRow.addView(saveBtn)

            layout.addView(actionRow)
        }
    }
}
