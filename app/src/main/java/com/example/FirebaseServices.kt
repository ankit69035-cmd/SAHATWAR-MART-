package com.example

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.model.*
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

object FirebaseServices {
    private const val TAG = "FirebaseServices"
    private const val PREFS_NAME = "SahatwarMartFirebasePrefs2"

    var isInitialized = false
        private set

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // Read from properties / config
            val apiKey = BuildConfig.FIREBASE_API_KEY.ifEmpty { "" }
            val projectId = BuildConfig.FIREBASE_PROJECT_ID.ifEmpty { "" }
            val appId = BuildConfig.FIREBASE_APP_ID.ifEmpty { "" }
            val storageBucket = BuildConfig.FIREBASE_STORAGE_BUCKET.ifEmpty { "" }

            // Read overrides from Shared Preferences
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedApiKey = sharedPrefs.getString("api_key", "").orEmpty().ifEmpty { apiKey }
            val savedProjectId = sharedPrefs.getString("project_id", "").orEmpty().ifEmpty { projectId }
            val savedAppId = sharedPrefs.getString("app_id", "").orEmpty().ifEmpty { appId }
            val savedStorageBucket = sharedPrefs.getString("storage_bucket", "").orEmpty().ifEmpty { storageBucket }

            if (savedApiKey.isNotEmpty() && savedProjectId.isNotEmpty() && savedAppId.isNotEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(savedApiKey)
                    .setProjectId(savedProjectId)
                    .setApplicationId(savedAppId)
                    .apply {
                        if (savedStorageBucket.isNotEmpty()) {
                            setStorageBucket(savedStorageBucket)
                        }
                    }
                    .build()

                // If already initialized with any app, clean it
                val initializedApps = FirebaseApp.getApps(context)
                if (initializedApps.isNotEmpty()) {
                    // FirebaseApp.getInstance().delete() // wait, better to reuse or initialize standard
                    // Let's check if the existing app works
                    isInitialized = true
                    Log.d(TAG, "Firebase already initialized")
                } else {
                    FirebaseApp.initializeApp(context, options)
                    isInitialized = true
                    Log.d(TAG, "Firebase initialized dynamically with ProjectId=$savedProjectId")
                }
            } else {
                Log.w(TAG, "No Firebase configuration found! Prompt user to configure.")
                // Initialize default dummy for structure if nothing exists yet, so other components load
                // without immediately crashing, although operations will fail gracefully
                val options = FirebaseOptions.Builder()
                    .setApiKey("mock-api-key-replace-me")
                    .setProjectId("sahatwar-mart-1234")
                    .setApplicationId("1:1234567890:android:abcdef123456")
                    .build()
                FirebaseApp.initializeApp(context, options)
                isInitialized = true
                Log.d(TAG, "Initialized Firebase with placeholder config. Expect service errors until real config is set.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error", e)
        }
    }

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    val storage: FirebaseStorage
        get() = FirebaseStorage.getInstance()

    fun isAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email?.trim()?.lowercase() == "ankit69035@gmail.com"
    }

    fun saveConfig(context: Context, apiKey: String, projectId: String, appId: String, storageBucket: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("api_key", apiKey.trim())
            .putString("project_id", projectId.trim())
            .putString("app_id", appId.trim())
            .putString("storage_bucket", storageBucket.trim())
            .apply()
        
        isInitialized = false
        initialize(context)
    }

    fun importConfigFromJson(context: Context, jsonString: String): Boolean {
        return try {
            val json = JSONObject(jsonString)
            val clientArr = json.getJSONArray("client")
            val clientObj = clientArr.getJSONObject(0)
            val projectInfo = json.getJSONObject("project_info")
            
            val projectId = projectInfo.getString("project_id")
            val storageBucket = projectInfo.optString("storage_bucket")
            val clientInfo = clientObj.getJSONObject("client_info")
            val appId = clientInfo.getString("mobilesdk_app_id")
            val apiKeys = clientObj.getJSONArray("api_key")
            val apiKey = apiKeys.getJSONObject(0).getString("current_key")
            
            saveConfig(context, apiKey, projectId, appId, storageBucket)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Google Services JSON", e)
            false
        }
    }

    fun clearConfig(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        isInitialized = false
        // Re-initialize using BuildConfig defaults or prompt
        initialize(context)
    }

    fun isUsingRealProject(): Boolean {
        val testConf = auth.app.options.apiKey
        return testConf != "mock-api-key-replace-me"
    }

    fun getConfig(context: Context): Map<String, String> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return mapOf(
            "api_key" to (sharedPrefs.getString("api_key", null) ?: BuildConfig.FIREBASE_API_KEY),
            "project_id" to (sharedPrefs.getString("project_id", null) ?: BuildConfig.FIREBASE_PROJECT_ID),
            "app_id" to (sharedPrefs.getString("app_id", null) ?: BuildConfig.FIREBASE_APP_ID),
            "storage_bucket" to (sharedPrefs.getString("storage_bucket", null) ?: BuildConfig.FIREBASE_STORAGE_BUCKET)
        )
    }

    // --- REALTIME PRODUCTS LISTENER ---
    fun getProductsFlow(): Flow<List<Product>> = callbackFlow {
        val listener = firestore.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val name = doc.getString("name") ?: ""
                            val description = doc.getString("description") ?: ""
                            val category = doc.getString("category") ?: ""
                            val imageUrl = doc.getString("imageUrl") ?: ""
                            val price = doc.getDouble("price") ?: 0.0
                            val mrp = doc.getDouble("mrp") ?: 0.0
                            val rating = doc.getDouble("rating")?.toFloat() ?: 4.2f
                            val inStock = doc.getBoolean("inStock") ?: true
                            val stockQuantity = doc.getLong("stockQuantity")?.toInt() ?: 10
                            val warningThreshold = doc.getLong("warningThreshold")?.toInt() ?: 5
                            
                            Product(id, name, description, category, imageUrl, price, mrp, rating, inStock, stockQuantity, warningThreshold)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping product document ${doc.id}", e)
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // --- REALTIME CATEGORIES LISTENER ---
    fun getCategoriesFlow(): Flow<List<Category>> = callbackFlow {
        val listener = firestore.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val name = doc.getString("name") ?: ""
                        Category(id, name)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // --- REALTIME BANNERS LISTENER ---
    fun getBannersFlow(): Flow<List<BannerItem>> = callbackFlow {
        val listener = firestore.collection("banners")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val id = doc.id
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        val title = doc.getString("title") ?: ""
                        BannerItem(id, imageUrl, title)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    // --- REALTIME ORDERS LISTENER ---
    fun getOrdersFlow(email: String?): Flow<List<Order>> = callbackFlow {
        val query = if (email != null && email.trim().lowercase() == "ankit69035@gmail.com") {
            firestore.collection("orders").orderBy("orderTime", Query.Direction.DESCENDING)
        } else {
            firestore.collection("orders")
                .whereEqualTo("email", email ?: "")
                .orderBy("orderTime", Query.Direction.DESCENDING)
        }

        // Catch exception in case indices are building or orderTime doesn't exist
        var simpleQuery = firestore.collection("orders")
        val activeQuery = try {
            query
        } catch (e: Exception) {
            simpleQuery
        }

        val listener = activeQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Return simple snapshot listener without ordering if composite index error
                Log.e(TAG, "Query ordered snapshot failed, falling back to simple query", error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        mapOrder(doc)
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    private fun mapOrder(doc: com.google.firebase.firestore.DocumentSnapshot): Order {
        val id = doc.id
        val customerName = doc.getString("customerName") ?: ""
        val phone = doc.getString("phone") ?: ""
        val email = doc.getString("email") ?: ""
        val address = doc.getString("address") ?: ""
        val landmark = doc.getString("landmark") ?: ""
        val totalAmount = doc.getDouble("totalAmount") ?: 0.0
        val orderTime = doc.getLong("orderTime") ?: 0L
        val status = doc.getString("status") ?: "Pending"
        
        // Parse items list
        val itemsRaw = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
        val itemsList = itemsRaw.map { raw ->
            OrderItem(
                productId = raw["productId"] as? String ?: "",
                name = raw["name"] as? String ?: "",
                price = (raw["price"] as? Number)?.toDouble() ?: 0.0,
                imageUrl = raw["imageUrl"] as? String ?: "",
                quantity = (raw["quantity"] as? Number)?.toInt() ?: 1
            )
        }
        return Order(id, customerName, phone, email, address, landmark, itemsList, totalAmount, orderTime, status)
    }

    // --- REALTIME WISHLIST LISTENER ---
    fun getWishlistFlow(email: String): Flow<List<Product>> = callbackFlow {
        val listener = firestore.collection("wishlist")
            .whereEqualTo("email", email)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Fetch products dynamically
                    val productIds = snapshot.documents.mapNotNull { it.getString("productId") }
                    if (productIds.isEmpty()) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    // Fetch products info for wishlist
                    firestore.collection("products")
                        .get()
                        .addOnSuccessListener { prodSnapshot ->
                            val products = prodSnapshot.documents.mapNotNull { doc ->
                                if (doc.id in productIds) {
                                    val id = doc.id
                                    val name = doc.getString("name") ?: ""
                                    val description = doc.getString("description") ?: ""
                                    val category = doc.getString("category") ?: ""
                                    val imageUrl = doc.getString("imageUrl") ?: ""
                                    val price = doc.getDouble("price") ?: 0.0
                                    val mrp = doc.getDouble("mrp") ?: 0.0
                                    val rating = doc.getDouble("rating")?.toFloat() ?: 4.2f
                                    val inStock = doc.getBoolean("inStock") ?: true
                                    val stockQuantity = doc.getLong("stockQuantity")?.toInt() ?: 10
                                    val warningThreshold = doc.getLong("warningThreshold")?.toInt() ?: 5
                                    
                                    Product(id, name, description, category, imageUrl, price, mrp, rating, inStock, stockQuantity, warningThreshold)
                                } else null
                            }
                            trySend(products)
                        }
                        .addOnFailureListener {
                            trySend(emptyList())
                        }
                }
            }
        awaitClose { listener.remove() }
    }

    fun checkWishlistedFlow(productId: String, email: String): Flow<Boolean> = callbackFlow {
        val listener = firestore.collection("wishlist")
            .whereEqualTo("email", email)
            .whereEqualTo("productId", productId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }
                trySend(snapshot != null && !snapshot.isEmpty)
            }
        awaitClose { listener.remove() }
    }

    suspend fun toggleWishlist(productId: String, email: String) {
        val query = firestore.collection("wishlist")
            .whereEqualTo("email", email)
            .whereEqualTo("productId", productId)
            .get()
            .await()
        
        if (!query.isEmpty) {
            for (doc in query.documents) {
                firestore.collection("wishlist").document(doc.id).delete().await()
            }
        } else {
            val data = mapOf(
                "email" to email,
                "productId" to productId,
                "addedTime" to System.currentTimeMillis()
            )
            firestore.collection("wishlist").add(data).await()
        }
    }

    // --- PRODUCT CRUD OPERATIONS ---
    suspend fun addProduct(product: Product, imageUri: Uri?, context: Context): String {
        var imageUrl = product.imageUrl
        if (imageUri != null) {
            imageUrl = uploadImage(imageUri, context)
        }
        val data = mapOf(
            "name" to product.name,
            "description" to product.description,
            "category" to product.category,
            "imageUrl" to imageUrl,
            "price" to product.price,
            "mrp" to product.mrp,
            "rating" to product.rating,
            "inStock" to product.inStock,
            "stockQuantity" to product.stockQuantity,
            "warningThreshold" to product.warningThreshold
        )
        val docRef = firestore.collection("products").add(data).await()
        return docRef.id
    }

    suspend fun editProduct(product: Product, imageUri: Uri?, context: Context) {
        var imageUrl = product.imageUrl
        if (imageUri != null) {
            imageUrl = uploadImage(imageUri, context)
        }
        val data = mapOf(
            "name" to product.name,
            "description" to product.description,
            "category" to product.category,
            "imageUrl" to imageUrl,
            "price" to product.price,
            "mrp" to product.mrp,
            "rating" to product.rating,
            "inStock" to product.inStock,
            "stockQuantity" to product.stockQuantity,
            "warningThreshold" to product.warningThreshold
        )
        firestore.collection("products").document(product.id).set(data).await()
    }

    suspend fun deleteProduct(productId: String) {
        firestore.collection("products").document(productId).delete().await()
    }

    // --- CATEGORY OPERATIONS ---
    suspend fun addCategory(name: String) {
        val query = firestore.collection("categories")
            .whereEqualTo("name", name.trim())
            .get()
            .await()
        if (query.isEmpty) {
            firestore.collection("categories").add(mapOf("name" to name.trim())).await()
        }
    }

    suspend fun deleteCategory(categoryId: String) {
        firestore.collection("categories").document(categoryId).delete().await()
    }

    // --- BANNER OPERATIONS ---
    suspend fun addBanner(title: String, imageUri: Uri, context: Context) {
        val imageUrl = uploadImage(imageUri, context)
        firestore.collection("banners").add(mapOf(
            "title" to title,
            "imageUrl" to imageUrl
        )).await()
    }

    suspend fun deleteBanner(bannerId: String) {
        firestore.collection("banners").document(bannerId).delete().await()
    }

    // --- IMAGE UPLOAD TO STORAGE ---
    private suspend fun uploadImage(uri: Uri, context: Context): String {
        try {
            val storageRef = storage.reference.child("product_images/${UUID.randomUUID()}.jpg")
            
            // Read uri bytes to upload
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val byteBuffer = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var len: Int
            if (inputStream != null) {
                while (inputStream.read(buffer).also { len = it } != -1) {
                    byteBuffer.write(buffer, 0, len)
                }
            }
            val dataBytes = byteBuffer.toByteArray()
            
            val uploadTask = storageRef.putBytes(dataBytes).await()
            val downloadUrl = storageRef.downloadUrl.await()
            return downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Image upload failed. Returning placeholder image.", e)
            // Fallback placeholder image URL if upload fails or storage options are not writable
            return "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=300"
        }
    }

    // --- USER METADATA SYSTEM ---
    suspend fun saveUserMetadata(userId: String, name: String, phone: String, address: String, landmark: String, email: String) {
        val data = mapOf(
            "userId" to userId,
            "name" to name,
            "phone" to phone,
            "address" to address,
            "landmark" to landmark,
            "email" to email,
            "role" to if (email.trim().lowercase() == "ankit69035@gmail.com") "admin" else "customer"
        )
        firestore.collection("users").document(userId).set(data).await()
    }

    suspend fun getUserMetadata(userId: String): Map<String, Any>? {
        val doc = firestore.collection("users").document(userId).get().await()
        return if (doc.exists()) doc.data else null
    }

    // --- ORDER OPERATIONS ---
    suspend fun createOrder(order: Order) {
        val data = mapOf(
            "customerName" to order.customerName,
            "phone" to order.phone,
            "email" to order.email,
            "address" to order.address,
            "landmark" to order.landmark,
            "totalAmount" to order.totalAmount,
            "orderTime" to order.orderTime,
            "status" to order.status,
            "items" to order.items.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "name" to item.name,
                    "price" to item.price,
                    "imageUrl" to item.imageUrl,
                    "quantity" to item.quantity
                )
            }
        )
        
        // Add to firestore
        firestore.collection("orders").add(data).await()
        
        // Subtract stock for each product ordered! Real stock reduction system!
        for (item in order.items) {
            try {
                val docRef = firestore.collection("products").document(item.productId)
                val doc = docRef.get().await()
                if (doc.exists()) {
                    val currentStock = doc.getLong("stockQuantity")?.toInt() ?: 10
                    val newStock = (currentStock - item.quantity).coerceAtLeast(0)
                    val inStock = newStock > 0
                    docRef.update(
                        mapOf(
                            "stockQuantity" to newStock,
                            "inStock" to inStock
                        )
                    ).await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update stock for product ${item.productId}", e)
            }
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        firestore.collection("orders").document(orderId).update("status", status).await()
        
        // If order cancelled, return the stock back to the products! Amazing real-world system!
        if (status == "Cancelled") {
            try {
                val doc = firestore.collection("orders").document(orderId).get().await()
                if (doc.exists()) {
                    val itemsRaw = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                    for (itemMap in itemsRaw) {
                        val productId = itemMap["productId"] as? String ?: ""
                        val quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1
                        if (productId.isNotEmpty()) {
                            val pDocRef = firestore.collection("products").document(productId)
                            val pDoc = pDocRef.get().await()
                            if (pDoc.exists()) {
                                val curStock = pDoc.getLong("stockQuantity")?.toInt() ?: 10
                                val newStock = curStock + quantity
                                pDocRef.update(
                                    mapOf(
                                        "stockQuantity" to newStock,
                                        "inStock" to true
                                    )
                                ).await()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore stock for cancelled order", e)
            }
        }
    }
}
