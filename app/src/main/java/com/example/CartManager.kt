package com.example

import android.content.Context
import com.example.model.CartItem
import com.example.model.Product
import com.example.model.getMinQuantityRule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CartManager {
    private const val PREFS_NAME = "SahatwarMartCartPrefs"
    private const val KEY_CART = "cart_items"
    
    private val _cartState = MutableStateFlow<List<CartItem>>(emptyList())
    val cartState: StateFlow<List<CartItem>> = _cartState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CART, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<CartItem>>() {}.type
                val items: List<CartItem> = Gson().fromJson(json, type)
                _cartState.value = items
            } catch (e: Exception) {
                _cartState.value = emptyList()
            }
        } else {
            _cartState.value = emptyList()
        }
    }

    private fun save(context: Context, items: List<CartItem>) {
        _cartState.value = items
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(items)
        prefs.edit().putString(KEY_CART, json).apply()
    }

    fun addToCart(context: Context, product: Product, quantity: Int = -1): CartItem {
        val currentList = _cartState.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        
        // Check minimum quantity rule
        val reqMinQty = getMinQuantityRule(product.price)
        val targetQty = if (quantity > 0) quantity else reqMinQty
        
        // Coerce targetQty with stock limit
        val stockLimit = product.stockQuantity
        val finalQty = targetQty.coerceAtMost(stockLimit).coerceAtLeast(reqMinQty)
        
        val item = if (existingIndex >= 0) {
            val existingItem = currentList[existingIndex]
            val newQty = (existingItem.quantity + 1).coerceAtMost(stockLimit).coerceAtLeast(reqMinQty)
            val updated = existingItem.copy(quantity = newQty)
            currentList[existingIndex] = updated
            updated
        } else {
            val newItem = CartItem(
                id = product.id,
                product = product,
                quantity = finalQty
            )
            currentList.add(newItem)
            newItem
        }
        
        save(context, currentList)
        return item
    }

    fun updateQuantity(context: Context, productId: String, newQuantity: Int) {
        val currentList = _cartState.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = currentList[index]
            val minRule = getMinQuantityRule(item.product.price)
            val stockLimit = item.product.stockQuantity
            val coercedQty = newQuantity.coerceAtMost(stockLimit).coerceAtLeast(minRule)
            
            if (coercedQty <= 0) {
                currentList.removeAt(index)
            } else {
                currentList[index] = item.copy(quantity = coercedQty)
            }
            save(context, currentList)
        }
    }

    fun removeFromCart(context: Context, productId: String) {
        val currentList = _cartState.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentList.removeAt(index)
            save(context, currentList)
        }
    }

    fun clearCart(context: Context) {
        save(context, emptyList())
    }

    fun getCartTotal(): Double {
        return _cartState.value.sumOf { it.product.price * it.quantity }
    }

    fun isRuleSatisfied(): Pair<Boolean, String?> {
        for (item in _cartState.value) {
            val minRule = getMinQuantityRule(item.product.price)
            if (item.quantity < minRule) {
                return Pair(false, "Product '${item.product.name}' requires a minimum quantity of $minRule.")
            }
        }
        return Pair(true, null)
    }
}
