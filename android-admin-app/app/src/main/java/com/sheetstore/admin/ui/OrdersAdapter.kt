package com.sheetstore.admin.ui

import android.graphics.Color
import android.graphics.Typeface
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
        return OrderViewHolder(card)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position], onApproveClicked)
    }

    override fun getItemCount(): Int = orders.size

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(order: Order, onApproveClicked: (Order) -> Unit) {
            val layout = itemView as LinearLayout
            layout.removeAllViews()

            // Header Row: Order ID + Status Badge
            val headerRow = LinearLayout(layout.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val orderIdView = TextView(layout.context).apply {
                text = order.orderId
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerRow.addView(orderIdView)

            val statusBadge = TextView(layout.context).apply {
                val isPaid = order.status.equals("PAID", ignoreCase = true) || order.verified
                text = if (isPaid) "PAID ✓" else "PENDING ⏳"
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isPaid) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
                setBackgroundColor(if (isPaid) Color.parseColor("#064E3B") else Color.parseColor("#78350F"))
                setPadding(20, 8, 20, 8)
            }
            headerRow.addView(statusBadge)
            layout.addView(headerRow)

            // Date & Amount Row
            val metaView = TextView(layout.context).apply {
                text = "${order.date} • ₹${order.amount}"
                textSize = 12f
                setTextColor(Color.parseColor("#38BDF8"))
                setPadding(0, 8, 0, 16)
            }
            layout.addView(metaView)

            // Customer Name & Product
            val detailsView = TextView(layout.context).apply {
                text = "👤 ${order.customerName} (${order.customerPhone})\n📦 ${order.product}"
                textSize = 13f
                setTextColor(Color.parseColor("#CBD5E1"))
                setLineSpacing(6f, 1f)
            }
            layout.addView(detailsView)

            // UTR Number Display
            val utrView = TextView(layout.context).apply {
                text = "🔢 UTR: ${order.utr.ifEmpty { "Not Provided" }}"
                textSize = 12f
                typeface = Typeface.MONOSPACE
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, 12, 0, 16)
            }
            layout.addView(utrView)

            // If order is PENDING, show 1-Tap Approve Button
            val isPaid = order.status.equals("PAID", ignoreCase = true) || order.verified
            if (!isPaid) {
                val approveBtn = Button(layout.context).apply {
                    text = "Approve Order [✓]"
                    textSize = 12f
                    setBackgroundColor(Color.parseColor("#10B981"))
                    setTextColor(Color.parseColor("#022C22"))
                    typeface = Typeface.DEFAULT_BOLD
                    setOnClickListener {
                        onApproveClicked(order)
                    }
                }
                layout.addView(approveBtn)
            }
        }
    }
}

