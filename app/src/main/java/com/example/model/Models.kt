package com.example.model

import java.io.Serializable

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val mrp: Double = 0.0,
    val rating: Float = 4.05f,
    val inStock: Boolean = true,
    val stockQuantity: Int = 10,
    val warningThreshold: Int = 5
) : Serializable {
    val discount: Int
        get() = if (mrp > price && mrp > 0) (((mrp - price) / mrp) * 100).toInt() else 0

    // Smart Minimum Quantity Rule (Requirements: "For products below ₹50: ₹10 => minimum 5 quantity, ₹15 => minimum 4 quantity, ₹20 => minimum 3 quantity, ₹25 => minimum 2 quantity")
    val minQuantity: Int
        get() = getMinQuantityRule(price)
}

fun getMinQuantityRule(price: Double): Int {
    if (price >= 50.0) return 1
    return when {
        price <= 10.0 -> 5
        price <= 15.0 -> 4
        price <= 20.0 -> 3
        price <= 25.0 -> 2
        else -> 1 // Default for other prices between 25 and 50
    }
}

data class CartItem(
    val id: String = "",
    val product: Product = Product(),
    val quantity: Int = 1
)

data class OrderItem(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val quantity: Int = 1
)

data class Order(
    val id: String = "",
    val customerName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val landmark: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val orderTime: Long = 0,
    val status: String = "Pending" // "Pending", "Accepted", "Out For Delivery", "Delivered", "Cancelled"
)

data class Category(
    val id: String = "",
    val name: String = ""
)

data class WishlistItem(
    val id: String = "",
    val productId: String = "",
    val addedTime: Long = 0
)

data class BannerItem(
    val id: String = "",
    val imageUrl: String = "",
    val title: String = ""
)
