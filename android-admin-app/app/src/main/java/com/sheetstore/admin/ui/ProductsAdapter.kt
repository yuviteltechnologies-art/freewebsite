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
        val card = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            setPadding(36, 36, 36, 36)
            setBackgroundColor(Color.parseColor("#1E293B"))
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
            layout.removeAllViews()

            // Title
            val titleView = TextView(layout.context).apply {
                text = product.title
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }
            layout.addView(titleView)

            // Category & Badge
            val catView = TextView(layout.context).apply {
                text = "${product.category.uppercase()} • ${product.badge ?: "Digital File"}"
                textSize = 11f
                setTextColor(Color.parseColor("#38BDF8"))
                setPadding(0, 4, 0, 16)
            }
            layout.addView(catView)

            // Price Row
            val priceRow = LinearLayout(layout.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Selling Price Input
            val sellingLayout = LinearLayout(layout.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val sellingLabel = TextView(layout.context).apply {
                text = "Offer Price (₹):"
                textSize = 11f
                setTextColor(Color.parseColor("#94A3B8"))
            }
            sellingLayout.addView(sellingLabel)

            val sellingInput = EditText(layout.context).apply {
                setText(product.priceSelling)
                setTextColor(Color.WHITE)
                textSize = 14f
                setBackgroundColor(Color.parseColor("#0F172A"))
                setPadding(20, 16, 20, 16)
            }
            sellingLayout.addView(sellingInput)
            priceRow.addView(sellingLayout)

            // Space
            val spacer = View(layout.context).apply {
                layoutParams = LinearLayout.LayoutParams(24, 1)
            }
            priceRow.addView(spacer)

            // Original Price Input
            val originalLayout = LinearLayout(layout.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val originalLabel = TextView(layout.context).apply {
                text = "MRP (₹):"
                textSize = 11f
                setTextColor(Color.parseColor("#94A3B8"))
            }
            originalLayout.addView(originalLabel)

            val originalInput = EditText(layout.context).apply {
                setText(product.priceOriginal)
                setTextColor(Color.parseColor("#CBD5E1"))
                textSize = 14f
                setBackgroundColor(Color.parseColor("#0F172A"))
                setPadding(20, 16, 20, 16)
            }
            originalLayout.addView(originalInput)
            priceRow.addView(originalLayout)

            layout.addView(priceRow)

            // Active Toggle
            val activeSwitch = Switch(layout.context).apply {
                text = "Show on Website (Active)"
                setTextColor(Color.parseColor("#E2E8F0"))
                textSize = 12f
                isChecked = product.isActive?.equals("TRUE", ignoreCase = true) ?: true
                setPadding(0, 16, 0, 16)
            }
            layout.addView(activeSwitch)

            // Save Product Button
            val saveBtn = Button(layout.context).apply {
                text = "Update Product Price 💾"
                textSize = 12f
                setBackgroundColor(Color.parseColor("#2563EB"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    val newSelling = sellingInput.text.toString().trim()
                    val newOriginal = originalInput.text.toString().trim()
                    val isActive = activeSwitch.isChecked
                    onSaveProduct(product, newSelling, newOriginal, isActive)
                }
            }
            layout.addView(saveBtn)
        }
    }
}

