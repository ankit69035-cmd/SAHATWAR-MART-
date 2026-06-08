package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.CartManager
import com.example.FirebaseServices
import com.example.model.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class NetworkState<out T> {
    object Idle : NetworkState<Nothing>()
    object Loading : NetworkState<Nothing>()
    data class Success<out T>(val data: T) : NetworkState<T>()
    data class Error(val message: String) : NetworkState<Nothing>()
}

class MartViewModel : ViewModel() {
    private val TAG = "MartViewModel"

    // Authentication States
    var currentUser by mutableStateOf<FirebaseUser?>(null)
        private set
    
    var userMetadata by mutableStateOf<Map<String, Any>?>(null)
        private set

    var authState by mutableStateOf<NetworkState<FirebaseUser?>>(NetworkState.Idle)
        private set

    // Firebase Connection Status
    var isFirebaseConnected by mutableStateOf(false)
        private set

    // Navigation and UI state
    var currentScreen by mutableStateOf("home")
    var selectedCategory by mutableStateOf<String?>(null)
    var searchQuery by mutableStateOf("")
    var priceSortOrder by mutableStateOf("") // "", "low_to_high", "high_to_low"
    
    // Dialog / Sheet states
    var activeProductDetail by mutableStateOf<Product?>(null)
    var showCheckoutScreen by mutableStateOf(false)
    var showConfigScreen by mutableStateOf(false)
    
    // Firestore Flow Containers
    var products = MutableStateFlow<List<Product>>(emptyList())
        private set
    var categories = MutableStateFlow<List<Category>>(emptyList())
        private set
    var banners = MutableStateFlow<List<BannerItem>>(emptyList())
        private set
    var orders = MutableStateFlow<List<Order>>(emptyList())
        private set
    var wishlist = MutableStateFlow<List<Product>>(emptyList())
        private set

    // Local reactive Cart flow
    val cartItems: StateFlow<List<CartItem>> = CartManager.cartState

    fun init(context: Context) {
        FirebaseServices.initialize(context)
        CartManager.init(context)
        
        currentUser = FirebaseServices.auth.currentUser
        isFirebaseConnected = FirebaseServices.isUsingRealProject()
        
        viewModelScope.launch {
            if (currentUser != null) {
                fetchUserMetadata(currentUser!!.uid)
            }
            startRealtimeListeners()
        }
    }

    private fun startRealtimeListeners() {
        if (!FirebaseServices.isInitialized) return
        
        viewModelScope.launch {
            try {
                FirebaseServices.getProductsFlow().collect {
                    products.value = it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Products flow collection failed", e)
            }
        }

        viewModelScope.launch {
            try {
                FirebaseServices.getCategoriesFlow().collect {
                    categories.value = it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Categories flow collection failed", e)
            }
        }

        viewModelScope.launch {
            try {
                FirebaseServices.getBannersFlow().collect {
                    banners.value = it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Banners flow collection failed", e)
            }
        }

        viewModelScope.launch {
            try {
                // If user logged in, listen to their orders or all orders if admin
                val email = currentUser?.email
                if (email != null) {
                    FirebaseServices.getOrdersFlow(email).collect {
                        orders.value = it
                    }
                } else {
                    orders.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Orders flow collection failed", e)
            }
        }

        viewModelScope.launch {
            try {
                val email = currentUser?.email
                if (email != null) {
                    FirebaseServices.getWishlistFlow(email).collect {
                        wishlist.value = it
                    }
                } else {
                    wishlist.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Wishlist flow collection failed", e)
            }
        }
    }

    private suspend fun fetchUserMetadata(userId: String) {
        try {
            userMetadata = FirebaseServices.getUserMetadata(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user metadata", e)
        }
    }

    // --- AUTHENTICATION INTERFACES ---
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authState = NetworkState.Loading
            try {
                val result = FirebaseServices.auth.signInWithEmailAndPassword(email.trim(), password).await()
                currentUser = result.user
                if (currentUser != null) {
                    fetchUserMetadata(currentUser!!.uid)
                    // Restart orders/wishlist listeners
                    startRealtimeListeners()
                }
                authState = NetworkState.Success(currentUser)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                authState = NetworkState.Error(e.localizedMessage ?: "Unknown authentication error")
            }
        }
    }

    fun signUp(email: String, password: String, name: String, phone: String, address: String, landmark: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authState = NetworkState.Loading
            try {
                val result = FirebaseServices.auth.createUserWithEmailAndPassword(email.trim(), password).await()
                currentUser = result.user
                if (currentUser != null) {
                    // Save additional details
                    FirebaseServices.saveUserMetadata(currentUser!!.uid, name.trim(), phone.trim(), address.trim(), landmark.trim(), email.trim())
                    fetchUserMetadata(currentUser!!.uid)
                    startRealtimeListeners()
                }
                authState = NetworkState.Success(currentUser)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Signup error", e)
                authState = NetworkState.Error(e.localizedMessage ?: "Unknown signup error")
            }
        }
    }

    fun resetPassword(email: String, onSent: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseServices.auth.sendPasswordResetEmail(email.trim()).await()
                onSent()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Reset failed")
            }
        }
    }

    fun logout() {
        FirebaseServices.auth.signOut()
        currentUser = null
        userMetadata = null
        orders.value = emptyList()
        wishlist.value = emptyList()
        authState = NetworkState.Idle
    }

    // --- CART ACTIONS ---
    fun addToCart(context: Context, product: Product, quantity: Int = -1) {
        CartManager.addToCart(context, product, quantity)
    }

    fun updateCartQuantity(context: Context, productId: String, newQty: Int) {
        CartManager.updateQuantity(context, productId, newQty)
    }

    fun removeFromCart(context: Context, productId: String) {
        CartManager.removeFromCart(context, productId)
    }

    fun clearCart(context: Context) {
        CartManager.clearCart(context)
    }

    fun getCartTotal(): Double {
        return CartManager.getCartTotal()
    }

    fun isCartRuleSatisfied(): Pair<Boolean, String?> {
        return CartManager.isRuleSatisfied()
    }

    // --- WISHLIST ACTION ---
    fun toggleWishlist(product: Product) {
        val email = currentUser?.email ?: return
        viewModelScope.launch {
            try {
                FirebaseServices.toggleWishlist(product.id, email)
            } catch (e: Exception) {
                Log.e(TAG, "Wishlist toggle failed", e)
            }
        }
    }

    fun isWishlisted(productId: String): Boolean {
        return wishlist.value.any { it.id == productId }
    }

    // --- ADMIN ACTIONS ---
    fun addProduct(product: Product, imageUri: Uri?, context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseServices.addProduct(product, imageUri, context)
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Add product failed", e)
                onComplete(false)
            }
        }
    }

    fun editProduct(product: Product, imageUri: Uri?, context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseServices.editProduct(product, imageUri, context)
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Edit product failed", e)
                onComplete(false)
            }
        }
    }

    fun deleteProduct(productId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseServices.deleteProduct(productId)
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Delete product failed", e)
                onComplete(false)
            }
        }
    }

    fun addCategory(name: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseServices.addCategory(name)
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Add category failed", e)
                onComplete(false)
            }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            try {
                FirebaseServices.deleteCategory(id)
            } catch (e: Exception) {
                Log.e(TAG, "Delete category failed", e)
            }
        }
    }

    fun addBanner(title: String, imageUri: Uri, context: Context, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseServices.addBanner(title, imageUri, context)
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Add banner failed", e)
                onComplete(false)
            }
        }
    }

    fun deleteBanner(id: String) {
        viewModelScope.launch {
            try {
                FirebaseServices.deleteBanner(id)
            } catch (e: Exception) {
                Log.e(TAG, "Delete banner failed", e)
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            try {
                FirebaseServices.updateOrderStatus(orderId, status)
            } catch (e: Exception) {
                Log.e(TAG, "Update order status failed", e)
            }
        }
    }

    // --- CLIENT CHECKOUT & ORDERS ---
    fun placeOrder(
        customerName: String,
        phone: String,
        email: String,
        address: String,
        landmark: String,
        items: List<OrderItem>,
        totalAmount: Double,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val order = Order(
                    customerName = customerName,
                    phone = phone,
                    email = email,
                    address = address,
                    landmark = landmark,
                    items = items,
                    totalAmount = totalAmount,
                    orderTime = System.currentTimeMillis(),
                    status = "Pending"
                )
                FirebaseServices.createOrder(order)
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Checkout order failed", e)
                onComplete(false)
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                FirebaseServices.updateOrderStatus(orderId, "Cancelled")
            } catch (e: Exception) {
                Log.e(TAG, "Order cancellation failed", e)
            }
        }
    }
}
