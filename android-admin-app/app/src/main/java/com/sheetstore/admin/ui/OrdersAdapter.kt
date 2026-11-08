package com.sheetstore.admin.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sheetstore.admin.models.Order

class OrdersAdapter(
    private var orders: List<Order>,
    private val onApproveClicked: (Order) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    fun updateData(newOrders: List<Order>) {
        this.orders = newOrders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
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
        return OrderViewHolder(card)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position], onApproveClicked)
    }

    override fun getItemCount(): Int = orders.size

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(order: Order, onApproveClicked: (Order) -> Unit) {
            val layout = itemView as LinearLayout
            val context = layout.context
            layout.removeAllViews()

            val isPaid = order.status.equals("PAID", ignoreCase = true) || order.verified

            // 1. Top Header Row: Order ID + Status Badge
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val orderIdView = TextView(context).apply {
                text = order.orderId
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerRow.addView(orderIdView)

            val statusBadge = UiKit.createPillBadge(
                context = context,
                text = if (isPaid) "✓ PAID" else "⏳ PENDING",
                textColor = if (isPaid) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"),
                bgColor = if (isPaid) Color.parseColor("#064E3B") else Color.parseColor("#451A03"),
                borderColor = if (isPaid) Color.parseColor("#10B981") else Color.parseColor("#D97706")
            )
            headerRow.addView(statusBadge)
            layout.addView(headerRow)

            // 2. Date & Amount Row
            val metaRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, UiKit.dp(context, 6), 0, UiKit.dp(context, 12))
            }

            val dateView = TextView(context).apply {
                text = order.date
                textSize = 11f
                setTextColor(Color.parseColor("#94A3B8"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            metaRow.addView(dateView)

            val amountView = TextView(context).apply {
                text = "₹${order.amount.toInt()}"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#10B981"))
            }
            metaRow.addView(amountView)
            layout.addView(metaRow)

            // 3. Customer Info Box
            val customerBox = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#0B0F19"),
                    strokeColor = Color.parseColor("#1C2638"),
                    strokeWidthDp = 1,
                    radiusDp = 12f,
                    context = context
                )
                setPadding(
                    UiKit.dp(context, 14),
                    UiKit.dp(context, 12),
                    UiKit.dp(context, 14),
                    UiKit.dp(context, 12)
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = UiKit.dp(context, 12)
                }
            }

            val custNameView = TextView(context).apply {
                text = "👤 ${order.customerName} (${order.customerPhone})"
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }
            customerBox.addView(custNameView)

            val productView = TextView(context).apply {
                text = "📦 ${order.product}"
                textSize = 11.5f
                setTextColor(Color.parseColor("#38BDF8"))
                setPadding(0, UiKit.dp(context, 4), 0, 0)
            }
            customerBox.addView(productView)

            if (order.utr.isNotEmpty() && order.utr != "PRE-VERIFIED") {
                val utrView = TextView(context).apply {
                    text = "🔢 UPI Ref / UTR: ${order.utr}"
                    textSize = 11f
                    setTextColor(Color.parseColor("#CBD5E1"))
                    setPadding(0, UiKit.dp(context, 4), 0, 0)
                }
                customerBox.addView(utrView)
            }
            layout.addView(customerBox)

            // 4. Action Buttons Row: WhatsApp Chat + 1-Tap Approve
            val actionRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val waBtn = Button(context).apply {
                text = "💬 WhatsApp"
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#34D399"))
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#062E22"),
                    strokeColor = Color.parseColor("#059669"),
                    strokeWidthDp = 1,
                    radiusDp = 12f,
                    context = context
                )
                layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(context, 40), 1f).apply {
                    rightMargin = UiKit.dp(context, 8)
                }
                setOnClickListener {
                    val phone = order.customerPhone.replace("+", "").replace(" ", "").trim()
                    val targetPhone = if (phone.startsWith("91") || phone.length > 10) phone else "91$phone"
                    val msg = "Namaste ${order.customerName}! Aapke order ${order.orderId} (₹${order.amount.toInt()}) ke update ke silsile me sampark kiya gaya hai."
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$targetPhone?text=${Uri.encode(msg)}"))
                    context.startActivity(intent)
                }
            }
            actionRow.addView(waBtn)

            if (!isPaid) {
                val approveBtn = Button(context).apply {
                    text = "⚡ Approve Order"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.WHITE)
                    background = UiKit.gradientButtonDrawable(
                        startColor = Color.parseColor("#059669"),
                        endColor = Color.parseColor("#047857"),
                        radiusDp = 12f,
                        context = context
                    )
                    layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(context, 40), 1.2f)
                    setOnClickListener {
                        onApproveClicked(order)
                    }
                }
                actionRow.addView(approveBtn)
            } else {
                val approvedBadge = TextView(context).apply {
                    text = "✓ Payment Verified"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor("#10B981"))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(context, 40), 1.2f)
                }
                actionRow.addView(approvedBadge)
            }

            layout.addView(actionRow)
        }
    }
}
