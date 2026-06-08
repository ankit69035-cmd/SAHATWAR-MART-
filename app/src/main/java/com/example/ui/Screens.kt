@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.FirebaseServices
import com.example.model.*
import com.example.ui.theme.DeliveryGreen
import com.example.ui.theme.FlipkartBlue
import com.example.ui.theme.FlipkartYellow
import com.example.ui.theme.WarningRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Helper to format timestamps to readable order date
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Helper to select beautiful semantic category icons and colors matching Flipkart/Clean Utility aesthetics
fun getCategoryIconAndColor(categoryName: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    val name = categoryName.trim().lowercase()
    return when {
        name.contains("fruit") || name.contains("veggie") || name.contains("vegetable") || name.contains("farm") ->
            Pair(androidx.compose.material.icons.Icons.Default.Eco, Color(0xFF10B981)) // Emerald
        name.contains("milk") || name.contains("dairy") || name.contains("ghee") || name.contains("butter") ->
            Pair(androidx.compose.material.icons.Icons.Default.WaterDrop, Color(0xFF3B82F6)) // Ocean Blue
        name.contains("snack") || name.contains("bakery") || name.contains("bread") || name.contains("biscuit") || name.contains("cookie") ->
            Pair(androidx.compose.material.icons.Icons.Default.BakeryDining, Color(0xFFD97706)) // Amber
        name.contains("clean") || name.contains("shampoo") || name.contains("detergent") || name.contains("soap") || name.contains("toilet") ->
            Pair(androidx.compose.material.icons.Icons.Default.CleaningServices, Color(0xFF64748B)) // Slate
        name.contains("spice") || name.contains("masala") || name.contains("grocery") || name.contains("oil") || name.contains("flour") || name.contains("powder") ->
            Pair(androidx.compose.material.icons.Icons.Default.Restaurant, Color(0xFFEF4444)) // Crimson
        else ->
            Pair(androidx.compose.material.icons.Icons.Default.Category, Color(0xFF2874F0)) // Brand Blue
    }
}

@Composable
fun MainAppContainer(viewModel: MartViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val cartItemsList by viewModel.cartItems.collectAsState()
    val totalCartCount = cartItemsList.sumOf { it.quantity }
    
    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    Scaffold(
        topBar = {
            Column {
                // Main Header Banner
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SAHATWAR MART",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 21.sp,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(FlipkartYellow, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "VILLAGE",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FlipkartBlue,
                        titleContentColor = Color.White
                    ),
                    actions = {
                        if (!viewModel.isFirebaseConnected) {
                            IconButton(onClick = { viewModel.showConfigScreen = true }) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Config Required",
                                    tint = FlipkartYellow
                                )
                            }
                        }
                        IconButton(onClick = { 
                            if (viewModel.currentUser != null) {
                                viewModel.currentScreen = "account"
                            } else {
                                viewModel.currentScreen = "account"
                            }
                        }) {
                            Icon(
                                imageVector = if (viewModel.currentUser != null) Icons.Default.AccountCircle else Icons.Default.Login,
                                contentDescription = "Profile",
                                tint = Color.White
                            )
                        }
                    }
                )
                // Delivery Restrictions subtitle
                Surface(
                    color = FlipkartYellow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Delivery Location Info",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Delivery in Sahatwar Village",
                                color = Color(0xFF1E293B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = "Express Delivery",
                                    tint = Color(0xFF1E293B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "20-30 MINS",
                                    color = Color(0xFF1E293B),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (viewModel.currentScreen != "admin") {
                Column {
                    // Sticky Smart Rule Banner
                    Surface(
                        color = Color(0xFFEFF6FF), // bg-blue-50
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Color(0xFFDBEAFE))) // border-t border-blue-100 style
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Rule Info Icon",
                                tint = FlipkartBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Items below ₹25 require minimum quantities. Checkout is blocked until rules are met.",
                                color = Color(0xFF1E40AF), // text-blue-800
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                    NavigationBarItem(
                        selected = viewModel.currentScreen == "home",
                        onClick = { viewModel.currentScreen = "home" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FlipkartBlue,
                            selectedTextColor = FlipkartBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = FlipkartYellow.copy(alpha = 0.3f)
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentScreen == "categories",
                        onClick = { viewModel.currentScreen = "categories" },
                        icon = { Icon(Icons.Default.Category, contentDescription = "Categories") },
                        label = { Text("Categories") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FlipkartBlue,
                            selectedTextColor = FlipkartBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = FlipkartYellow.copy(alpha = 0.3f)
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentScreen == "cart",
                        onClick = { viewModel.currentScreen = "cart" },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (totalCartCount > 0) {
                                        Badge(containerColor = WarningRed) {
                                            Text(text = totalCartCount.toString(), color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                            }
                        },
                        label = { Text("Cart") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FlipkartBlue,
                            selectedTextColor = FlipkartBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = FlipkartYellow.copy(alpha = 0.3f)
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentScreen == "orders",
                        onClick = { viewModel.currentScreen = "orders" },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Orders") },
                        label = { Text("Orders") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FlipkartBlue,
                            selectedTextColor = FlipkartBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = FlipkartYellow.copy(alpha = 0.3f)
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentScreen == "account",
                        onClick = { viewModel.currentScreen = "account" },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                        label = { Text("Account") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FlipkartBlue,
                            selectedTextColor = FlipkartBlue,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = FlipkartYellow.copy(alpha = 0.3f)
                        )
                    )
                }
            }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (viewModel.currentScreen) {
                "home" -> HomeScreen(viewModel)
                "categories" -> CategoriesScreen(viewModel)
                "cart" -> CartScreen(viewModel)
                "orders" -> OrdersScreen(viewModel)
                "account" -> AccountScreen(viewModel)
                "admin" -> AdminDashboardScreen(viewModel)
            }

            // Connection advisory banner if Firebase is unconfigured or mock project is in use
            if (viewModel.showConfigScreen) {
                FirebaseConfigDialog(
                    onDismiss = { viewModel.showConfigScreen = false },
                    viewModel = viewModel
                )
            }

            // Product Detail Overlay Modal
            viewModel.activeProductDetail?.let { product ->
                ProductDetailDialog(
                    product = product,
                    onDismiss = { viewModel.activeProductDetail = null },
                    viewModel = viewModel
                )
            }

            // Checkout Overlay modal
            if (viewModel.showCheckoutScreen) {
                CheckoutDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.showCheckoutScreen = false }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: MartViewModel) {
    val productsList by viewModel.products.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val bannersList by viewModel.banners.collectAsState()
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Local filters
    var searchText by remember { mutableStateOf("") }
    viewModel.searchQuery = searchText

    val filteredProducts = remember(productsList, viewModel.selectedCategory, searchText, viewModel.priceSortOrder) {
        var list = productsList
        
        // Categorized filter
        viewModel.selectedCategory?.let { categoryName ->
            list = list.filter { it.category.trim().lowercase() == categoryName.trim().lowercase() }
        }
        
        // Search filter
        if (searchText.isNotEmpty()) {
            list = list.filter { 
                it.name.contains(searchText, ignoreCase = true) || 
                it.description.contains(searchText, ignoreCase = true) ||
                it.category.contains(searchText, ignoreCase = true)
            }
        }
        
        // Price sort
        when (viewModel.priceSortOrder) {
            "low_to_high" -> list = list.sortedBy { it.price }
            "high_to_low" -> list = list.sortedByDescending { it.price }
        }
        
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search fresh groceries, milk, ghee, soaps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlipkartBlue,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )
        }

        // Categories quick carousel horizontal chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Shop by Categories",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    item {
                        val isSelected = viewModel.selectedCategory == null
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clickable { viewModel.selectedCategory = null }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = if (isSelected) FlipkartBlue else Color.White,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) FlipkartBlue else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Category,
                                    contentDescription = "All Products",
                                    tint = if (isSelected) Color.White else FlipkartBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "All Products",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) FlipkartBlue else Color(0xFF1E293B)
                            )
                        }
                    }
                    items(categoriesList) { cat ->
                        val isSelected = viewModel.selectedCategory == cat.name
                        val (icon, tintColor) = getCategoryIconAndColor(cat.name)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clickable { viewModel.selectedCategory = cat.name }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = if (isSelected) FlipkartBlue else Color.White,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) FlipkartBlue else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = cat.name,
                                    tint = if (isSelected) Color.White else tintColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = cat.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) FlipkartBlue else Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }

        // Banners slider (Only shown on "All Products" landing)
        if (viewModel.selectedCategory == null && bannersList.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(bannersList) { banner ->
                        Card(
                            modifier = Modifier
                                .width(310.dp)
                                .height(140.dp)
                                .clickable { },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = banner.imageUrl,
                                    contentDescription = banner.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.25f))
                                )
                                Text(
                                    text = banner.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sort Header and Filters
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (viewModel.selectedCategory != null) viewModel.selectedCategory!! else "All Fresh Items",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sort:", fontSize = 12.sp, color = Color.Gray)
                    TextButton(
                        onClick = {
                            viewModel.priceSortOrder = when (viewModel.priceSortOrder) {
                                "" -> "low_to_high"
                                "low_to_high" -> "high_to_low"
                                else -> ""
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (viewModel.priceSortOrder) {
                                    "low_to_high" -> "Price: Low ➔ High"
                                    "high_to_low" -> "Price: High ➔ Low"
                                    else -> "None"
                                },
                                fontSize = 13.sp,
                                color = FlipkartBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                imageVector = if (viewModel.priceSortOrder == "low_to_high") Icons.Filled.ArrowUpward else if (viewModel.priceSortOrder == "high_to_low") Icons.Filled.ArrowDownward else Icons.Filled.Sort,
                                contentDescription = "Sort direction",
                                modifier = Modifier.size(14.dp),
                                tint = FlipkartBlue
                            )
                        }
                    }
                }
            }
        }

        if (filteredProducts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = "Empty",
                            tint = Color.LightGray,
                            modifier = Modifier.size(70.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No products found in this category or search query.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // Grid representation of products
            item {
                // Jetpack Compose Grid built with simple nested layout to avoid vertical scroll nested problems under LazyColumn
                val columns = 2
                val rows = (filteredProducts.size + columns - 1) / columns
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (row in 0 until rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (col in 0 until columns) {
                                val idx = row * columns + col
                                if (idx < filteredProducts.size) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProductCard(
                                            product = filteredProducts[idx],
                                            viewModel = viewModel,
                                            context = context
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductCard(product: Product, viewModel: MartViewModel, context: Context) {
    val cartList by viewModel.cartItems.collectAsState()
    val cartItem = cartList.find { it.product.id == product.id }
    val isWishlisted = viewModel.isWishlisted(product.id)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { viewModel.activeProductDetail = product },
                onLongClick = { 
                    if (FirebaseServices.isAdmin()) {
                        // Fast navigate to details or editing trigger
                        viewModel.activeProductDetail = product
                    }
                }
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Image Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFAFAFA)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = product.imageUrl.ifEmpty { "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=300" }
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Fast action badge for Stock Status
                    if (!product.inStock) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(WarningRed, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OUT OF STOCK",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Under-50 Min quantity rule notification
                if (product.price < 50.0) {
                    val reqMin = getMinQuantityRule(product.price)
                    Box(
                        modifier = Modifier
                            .background(FlipkartYellow.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Min Qty: $reqMin req.",
                            color = Color(0xFFA05000),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Category tag
                Text(
                    text = product.category.uppercase(),
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Product Title
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )

                // Ratings and stars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF388E3C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", product.rating),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "stars",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("(${10 + product.id.hashCode() % 90})", fontSize = 10.sp, color = Color.Gray)
                }

                // Prices
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "₹${product.price.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (product.mrp > product.price) {
                        Text(
                            text = "₹${product.mrp.toInt()}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = TextDecoration.LineThrough,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${product.discount}% off",
                            color = DeliveryGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cart interaction buttons
                if (!product.inStock) {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Out of Stock", fontSize = 12.sp)
                    }
                } else {
                    if (cartItem != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val rMin = getMinQuantityRule(product.price)
                                    if (cartItem.quantity <= rMin) {
                                        viewModel.removeFromCart(context, product.id)
                                    } else {
                                        viewModel.updateCartQuantity(context, product.id, cartItem.quantity - 1)
                                    }
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(FlipkartBlue.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (cartItem.quantity == getMinQuantityRule(product.price)) Icons.Default.Delete else Icons.Default.Remove,
                                    contentDescription = "Minus",
                                    tint = FlipkartBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Text(
                                text = cartItem.quantity.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            
                            IconButton(
                                onClick = {
                                    if (cartItem.quantity < product.stockQuantity) {
                                        viewModel.updateCartQuantity(context, product.id, cartItem.quantity + 1)
                                    } else {
                                        Toast.makeText(context, "Only ${product.stockQuantity} items in stock!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(FlipkartBlue.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Plus",
                                    tint = FlipkartBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { 
                                viewModel.addToCart(context, product)
                                Toast.makeText(context, "${product.name} Added!", Toast.LENGTH_SHORT).show()
                            },
                            border = BorderStroke(1.dp, FlipkartBlue),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FlipkartBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Icon", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ADD", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Wishlist Heart Icon
            IconButton(
                onClick = { 
                    if (viewModel.currentUser == null) {
                        Toast.makeText(context, "Please login first to wishlist items!", Toast.LENGTH_LONG).show()
                        viewModel.currentScreen = "account"
                    } else {
                        viewModel.toggleWishlist(product)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Wishlist heart",
                    tint = if (isWishlisted) WarningRed else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CategoriesScreen(viewModel: MartViewModel) {
    val categoriesList by viewModel.categories.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Browse by Categories",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (categoriesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "No Categories",
                        tint = Color.LightGray,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No categories available yet. Admin must add some. Go to Account -> Settings to connect Firebase.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categoriesList) { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clickable {
                                viewModel.selectedCategory = category.name
                                viewModel.currentScreen = "home"
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FlipkartBlue.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, FlipkartBlue.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(FlipkartBlue.copy(alpha = 0.15f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = category.name,
                                    tint = FlipkartBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = category.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartScreen(viewModel: MartViewModel) {
    val cartList by viewModel.cartItems.collectAsState()
    val context = LocalContext.current
    
    val totalAmount = viewModel.getCartTotal()
    val isRuleSatisfiedPair = viewModel.isCartRuleSatisfied()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "Your Shopping Cart",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (cartList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Empty Cart",
                        tint = Color.LightGray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your cart is empty!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Add fresh products, flours, spices from the home catalog.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.currentScreen = "home" },
                        colors = ButtonDefaults.buttonColors(containerColor = FlipkartBlue)
                    ) {
                        Text("SHOP NOW", color = Color.White)
                    }
                }
            }
        } else {
            // Cart item list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartList) { item ->
                    val minRule = getMinQuantityRule(item.product.price)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Product Image
                            AsyncImage(
                                model = item.product.imageUrl.ifEmpty { "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=150" },
                                contentDescription = item.product.name,
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFAFAFA))
                            )
                            
                            Spacer(modifier = Modifier.width(10.dp))
                            
                            // Info column
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.product.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Category: ${item.product.category}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "₹${item.product.price.toInt()} each",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.DarkGray
                                )
                                
                                // Price Rule warning
                                if (item.product.price < 50.0) {
                                    Text(
                                        text = "Minimum Quantity Required: $minRule",
                                        fontSize = 10.sp,
                                        color = if (item.quantity < minRule) WarningRed else DeliveryGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // Controls
                            Column(horizontalAlignment = Alignment.End) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (item.quantity <= minRule) {
                                                viewModel.removeFromCart(context, item.product.id)
                                            } else {
                                                viewModel.updateCartQuantity(context, item.product.id, item.quantity - 1)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(Color(0xFFEEEEEE), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (item.quantity == minRule) Icons.Default.Delete else Icons.Default.Remove,
                                            contentDescription = "Minus",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = item.quantity.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            if (item.quantity < item.product.stockQuantity) {
                                                viewModel.updateCartQuantity(context, item.product.id, item.quantity + 1)
                                            } else {
                                                Toast.makeText(context, "Max stock limit!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(Color(0xFFEEEEEE), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Plus",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "₹${(item.product.price * item.quantity).toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = FlipkartBlue,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Price detail overview
                item {
                    val originalMrpSum = cartList.sumOf { (if (it.product.mrp > 0) it.product.mrp else it.product.price) * it.quantity }
                    val savings = originalMrpSum - totalAmount
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Price Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Price (${cartList.size} Items)", color = Color.DarkGray)
                                Text("₹${originalMrpSum.toInt()}")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (savings > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Product Discount", color = DeliveryGreen)
                                    Text("- ₹${savings.toInt()}", color = DeliveryGreen)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Delivery Fee (Sahatwar)", color = Color.DarkGray)
                                Text("FREE", color = DeliveryGreen, fontWeight = FontWeight.Bold)
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Amount Payable", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("₹${totalAmount.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = FlipkartBlue)
                            }
                        }
                    }
                }
            }

            // Checkout CTA
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isRuleSatisfiedPair.first) {
                        Text(
                            text = isRuleSatisfiedPair.second ?: "Rule validation failed",
                            color = WarningRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "✓ All Smart Minimum Quantity Rules are satisfied",
                            color = DeliveryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (viewModel.currentUser == null) {
                                Toast.makeText(context, "Please login to proceed with checkout", Toast.LENGTH_LONG).show()
                                viewModel.currentScreen = "account"
                            } else if (!isRuleSatisfiedPair.first) {
                                Toast.makeText(context, isRuleSatisfiedPair.second ?: "Error", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.showCheckoutScreen = true
                            }
                        },
                        enabled = isRuleSatisfiedPair.first,
                        colors = ButtonDefaults.buttonColors(containerColor = FlipkartYellow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PROCEED TO SECURE CHECKOUT (₹${totalAmount.toInt()})",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrdersScreen(viewModel: MartViewModel) {
    val ordersList by viewModel.orders.collectAsState()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "Your Orders & Tracking",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (viewModel.currentUser == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Lock",
                        tint = Color.LightGray,
                        modifier = Modifier.size(70.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Sign in to see persistent orders",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.currentScreen = "account" },
                        colors = ButtonDefaults.buttonColors(containerColor = FlipkartBlue)
                    ) {
                        Text("LOGIN / SIGN UP", color = Color.White)
                    }
                }
            }
        } else if (ordersList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = "No Orders",
                        tint = Color.LightGray,
                        modifier = Modifier.size(70.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No orders found!",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ordersList) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header ID and Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order ID: #${order.id.takeLast(7).uppercase()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = FlipkartBlue
                                )
                                Text(
                                    text = formatTimestamp(order.orderTime),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // Products list summary
                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.name} (${item.quantity})",
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "₹${(item.price * item.quantity).toInt()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            // Total and status indicators
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Status: ", fontSize = 13.sp, color = Color.Gray)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (order.status) {
                                                    "Pending" -> Color(0xFFFF9800)
                                                    "Accepted" -> FlipkartBlue
                                                    "Out For Delivery" -> Color(0xFF9C27B0)
                                                    "Delivered" -> DeliveryGreen
                                                    else -> Color.Gray
                                                }.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = order.status.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = when (order.status) {
                                                "Pending" -> Color(0xFFFF9800)
                                                "Accepted" -> FlipkartBlue
                                                "Out For Delivery" -> Color(0xFF9C27B0)
                                                "Delivered" -> DeliveryGreen
                                                else -> Color.Gray
                                            }
                                        )
                                    }
                                }
                                
                                Text(
                                    text = "Total Paid: ₹${order.totalAmount.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            // Tracking Stepper Bar
                            Spacer(modifier = Modifier.height(10.dp))
                            TrackingStepper(status = order.status)
                            
                            // Cancel button: enable cancel before Out For Delivery
                            if (order.status == "Pending" || order.status == "Accepted") {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.cancelOrder(order.id)
                                        Toast.makeText(context, "Order Cancelled Successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    border = BorderStroke(1.dp, WarningRed),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRed),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CANCEL ORDER (Full Refund)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingStepper(status: String) {
    val steps = listOf("Pending", "Accepted", "Out For Delivery", "Delivered")
    val currentIndex = steps.indexOf(status)
    if (currentIndex == -1 && status == "Cancelled") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WarningRed.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✕ This order was Cancelled. Returned stock back to storefront catalog.",
                color = WarningRed,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        steps.forEachIndexed { idx, step ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (idx <= currentIndex) DeliveryGreen else Color.LightGray,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when(step) {
                        "Out For Delivery" -> "Out"
                        else -> step
                    },
                    fontSize = 9.sp,
                    fontWeight = if (idx == currentIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (idx == currentIndex) Color.Black else Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            if (idx < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(2.dp)
                        .background(if (idx < currentIndex) DeliveryGreen else Color.LightGray)
                )
            }
        }
    }
}

@Composable
fun AccountScreen(viewModel: MartViewModel) {
    val context = LocalContext.current
    var isSignUpTab by remember { mutableStateOf(false) }

    // Input fields for metadata
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }

    if (viewModel.currentUser != null) {
        // Logged-in profile view
        val userMeta = viewModel.userMetadata
        val userEmail = viewModel.currentUser?.email
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Avatar",
                            tint = FlipkartBlue,
                            modifier = Modifier.size(70.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = (userMeta?.get("name") as? String) ?: "Customer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = userEmail ?: "no_email@example.com",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        
                        if (FirebaseServices.isAdmin()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(WarningRed, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AUTHORIZED ADMIN PORTAL",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // User Profile parameters
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Address & Contact Meta", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Gray)
                        
                        Row {
                            Text("Mobile No: ", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(90.dp))
                            Text((userMeta?.get("phone") as? String) ?: "Empty")
                        }
                        Row {
                            Text("Village/Loc: ", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(90.dp))
                            Text("Sahatwar Village, Ballia, UP", color = DeliveryGreen, fontWeight = FontWeight.Bold)
                        }
                        Row {
                            Text("Address: ", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(90.dp))
                            Text((userMeta?.get("address") as? String) ?: "Empty", modifier = Modifier.weight(1f))
                        }
                        Row {
                            Text("Landmark: ", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(90.dp))
                            Text((userMeta?.get("landmark") as? String) ?: "Empty")
                        }
                    }
                }
            }

            // Navigation trigger to Admin
            if (FirebaseServices.isAdmin()) {
                item {
                    Button(
                        onClick = { viewModel.currentScreen = "admin" },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings icon")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ENTER ADMIN ENGINE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Custom Firebase connection dialog button
            item {
                OutlinedButton(
                    onClick = { viewModel.showConfigScreen = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, FlipkartBlue)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = "Sync option")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("MANAGE FIREBASE CONFIGURATION (API KEYS)", color = FlipkartBlue, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("LOG OUT")
                }
            }
        }
    } else {
        // Logged-out state: integrated Sign In / Sign Up tabs
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Welcome to Sahatwar Mart",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlipkartBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                Text(
                    text = "Save and track groceries instantly. Accessible in Sahatwar Village, Ballia, UP only.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                TabRow(
                    selectedTabIndex = if (isSignUpTab) 1 else 0,
                    containerColor = Color.White
                ) {
                    Tab(
                        selected = !isSignUpTab,
                        onClick = { isSignUpTab = false },
                        text = { Text("Log In") }
                    )
                    Tab(
                        selected = isSignUpTab,
                        onClick = { isSignUpTab = true },
                        text = { Text("Sign Up") }
                    )
                }
            }

            if (isSignUpTab) {
                // Sign up form
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FlipkartBlue)
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Number (10 digit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FlipkartBlue)
                    )
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Delivery Address in Sahatwar") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FlipkartBlue)
                    )
                }
                item {
                    OutlinedTextField(
                        value = landmark,
                        onValueChange = { landmark = it },
                        label = { Text("Near Landmark (School, Mandir, Chauraha)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FlipkartBlue)
                    )
                }
            }

            // Common email / password
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FlipkartBlue)
                )
            }
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (Min 6 digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FlipkartBlue)
                )
            }

            if (viewModel.authState is NetworkState.Error) {
                item {
                    Text(
                        text = (viewModel.authState as NetworkState.Error).message,
                        color = WarningRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please write both email and password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isSignUpTab) {
                            if (name.isBlank() || phone.isBlank() || address.isBlank() || landmark.isBlank()) {
                                Toast.makeText(context, "All address fields are required for village delivery!", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            viewModel.signUp(email, password, name, phone, address, landmark) {
                                Toast.makeText(context, "Account Created Successfully!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            viewModel.login(email, password) {
                                Toast.makeText(context, "Logged In Successfully!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlipkartBlue),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.authState !is NetworkState.Loading
                ) {
                    if (viewModel.authState is NetworkState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(text = if (isSignUpTab) "CREATE NEW ACCOUNT" else "SECURE LOG IN", color = Color.White)
                    }
                }
            }

            // Forgot password feature
            if (!isSignUpTab) {
                item {
                    TextButton(
                        onClick = {
                            if (email.isBlank()) {
                                Toast.makeText(context, "Please input your email ID first, then click reset!", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.resetPassword(email, {
                                    Toast.makeText(context, "Reset password email sent!", Toast.LENGTH_LONG).show()
                                }, { err ->
                                    Toast.makeText(context, "Error: $err", Toast.LENGTH_LONG).show()
                                })
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Forgot Password? Reset password email", color = FlipkartBlue)
                    }
                }
            }

            item {
                Divider()
            }

            // Setup Firebase configuration banner
            item {
                Button(
                    onClick = { viewModel.showConfigScreen = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FlipkartYellow)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = "Sync options", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CONNECT TO MY FIREBASE PROJECT", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FirebaseConfigDialog(onDismiss: () -> Unit, viewModel: MartViewModel) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf("") }
    var projectId by remember { mutableStateOf("") }
    var appId by remember { mutableStateOf("") }
    var storageBucket by remember { mutableStateOf("") }
    var jsonString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val conf = FirebaseServices.getConfig(context)
        apiKey = conf["api_key"].orEmpty()
        projectId = conf["project_id"].orEmpty()
        appId = conf["app_id"].orEmpty()
        storageBucket = conf["storage_bucket"].orEmpty()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Firebase Key Setup",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FlipkartBlue
                )
                Text(
                    text = "Sahatwar Mart connects directly to Firestore/Storage. Set your Firebase project keys here or import your google-services.json context.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Divider()

                Text("Option 1: Paste google-services.json contents", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = jsonString,
                    onValueChange = { jsonString = it },
                    label = { Text("Paste Raw JSON String") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (jsonString.isNotBlank()) {
                            val success = FirebaseServices.importConfigFromJson(context, jsonString)
                            if (success) {
                                Toast.makeText(context, "JSON Configuration Loaded!", Toast.LENGTH_LONG).show()
                                viewModel.init(context)
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Failed to parse JSON. Check format!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Parse and Import JSON")
                }

                Divider()

                Text("Option 2: Add Keys manually", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Firebase API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = projectId,
                    onValueChange = { projectId = it },
                    label = { Text("Project ID") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = appId,
                    onValueChange = { appId = it },
                    label = { Text("Application ID (mobilesdk_app_id)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = storageBucket,
                    onValueChange = { storageBucket = it },
                    label = { Text("Storage Bucket URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            FirebaseServices.clearConfig(context)
                            viewModel.init(context)
                            Toast.makeText(context, "Using default developer sandbox parameters.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset Default")
                    }
                    Button(
                        onClick = {
                            if (apiKey.isBlank() || projectId.isBlank() || appId.isBlank()) {
                                Toast.makeText(context, "API Key, Project ID, and App ID are mandatory!", Toast.LENGTH_SHORT).show()
                            } else {
                                FirebaseServices.saveConfig(context, apiKey, projectId, appId, storageBucket)
                                viewModel.init(context)
                                Toast.makeText(context, "Firebase Config Saved!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FlipkartBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Connect", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailDialog(product: Product, onDismiss: () -> Unit, viewModel: MartViewModel) {
    val context = LocalContext.current
    val cartList by viewModel.cartItems.collectAsState()
    val cartItem = cartList.find { it.product.id == product.id }
    val reqMin = getMinQuantityRule(product.price)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Image container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFAFAFA)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = product.imageUrl.ifEmpty { "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=300" },
                        contentDescription = product.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Category and Name
                Text(
                    text = product.category.uppercase(),
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )

                // Ratings and pricing
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF388E3C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", product.rating),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "stars",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (product.inStock) "IN STOCK (${product.stockQuantity} items left)" else "OUT OF STOCK",
                        fontWeight = FontWeight.Bold,
                        color = if (product.inStock) DeliveryGreen else WarningRed,
                        fontSize = 12.sp
                    )
                }

                // Prices
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "₹${product.price.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = FlipkartBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (product.mrp > product.price) {
                        Text(
                            text = "₹${product.mrp.toInt()}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textDecoration = TextDecoration.LineThrough,
                                color = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${product.discount}% off",
                            color = DeliveryGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Smart Quantity rules
                if (product.price < 50.0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = FlipkartYellow.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, FlipkartYellow)
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Under-₹50 Smart Rule: This item costs ₹${product.price.toInt()}. Must order minimum of $reqMin quantities to checkout.",
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = "Description",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = product.description.ifEmpty { "Fresh authentic grocery sourced locally for village homes in Sahatwar. Pure quality guaranteed." },
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Add to Cart & Buy Now (Direct checkout support)
                if (product.inStock) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cart Selector or Add
                        Box(modifier = Modifier.weight(1f)) {
                            if (cartItem != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .border(1.dp, FlipkartBlue, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (cartItem.quantity <= reqMin) {
                                                viewModel.removeFromCart(context, product.id)
                                            } else {
                                                viewModel.updateCartQuantity(context, product.id, cartItem.quantity - 1)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (cartItem.quantity == reqMin) Icons.Default.Delete else Icons.Default.Remove,
                                            contentDescription = "Minus",
                                            tint = FlipkartBlue
                                        )
                                    }
                                    Text(
                                        text = cartItem.quantity.toString(),
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = {
                                        if (cartItem.quantity < product.stockQuantity) {
                                            viewModel.updateCartQuantity(context, product.id, cartItem.quantity + 1)
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Plus", tint = FlipkartBlue)
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.addToCart(context, product) },
                                    border = BorderStroke(1.dp, FlipkartBlue),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FlipkartBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                    ) {
                                    Text("ADD TO CART", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Buy Now Button (Direct Checkout bypassing the standard cart flow)
                        Button(
                            onClick = {
                                onDismiss()
                                // Add item to cart at required quantity first, then proceed to checkout
                                viewModel.addToCart(context, product, reqMin)
                                viewModel.showCheckoutScreen = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FlipkartYellow),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                        ) {
                            Text("BUY NOW (FAST CHECKOUT)", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OUT OF STOCK")
                    }
                }

                // Back arrow CTA
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("GO BACK", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun CheckoutDialog(viewModel: MartViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cartList by viewModel.cartItems.collectAsState()
    val totalAmount = viewModel.getCartTotal()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    val email = viewModel.currentUser?.email.orEmpty()

    // Populate values from user database metadata
    LaunchedEffect(viewModel.userMetadata) {
        viewModel.userMetadata?.let { meta ->
            name = (meta["name"] as? String).orEmpty()
            phone = (meta["phone"] as? String).orEmpty()
            address = (meta["address"] as? String).orEmpty()
            landmark = (meta["landmark"] as? String).orEmpty()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Sahatwar House Delivery",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FlipkartBlue
                )
                Text(
                    text = "Payment Option: Cash On Delivery (COD) - Standard delivery in 20-30 minutes inside Sahatwar Village local boundaries only.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Divider()

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer ID / Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FlipkartBlue)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Active Phone Number (10 digit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FlipkartBlue)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Exact Home Address in Sahatwar") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FlipkartBlue)
                )

                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Famous Landmark near address") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = FlipkartBlue)
                )

                // Bill summary
                Card(colors = CardDefaults.cardColors(containerColor = FlipkartBlue.copy(alpha = 0.05f))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Bill Payable: ₹${totalAmount.toInt()}", fontWeight = FontWeight.Bold)
                        Text("Delivery Radius: Sahatwar Village (FREE delivery)", fontSize = 11.sp, color = DeliveryGreen, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        if (name.isBlank() || phone.isBlank() || address.isBlank() || landmark.isBlank()) {
                            Toast.makeText(context, "All address fields must be filled for successful COD delivery!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        
                        // Map cart items to order items structure
                        val orderItems = cartList.map { item ->
                            OrderItem(
                                productId = item.product.id,
                                name = item.product.name,
                                price = item.product.price,
                                imageUrl = item.product.imageUrl,
                                quantity = item.quantity
                            )
                        }

                        viewModel.placeOrder(
                            customerName = name.trim(),
                            phone = phone.trim(),
                            email = email,
                            address = address.trim(),
                            landmark = landmark.trim(),
                            items = orderItems,
                            totalAmount = totalAmount
                        ) { success ->
                            if (success) {
                                viewModel.clearCart(context)
                                Toast.makeText(context, "Order Placed Successfully! Local Delivery Boy will call your mobile shortly.", Toast.LENGTH_LONG).show()
                                viewModel.currentScreen = "orders"
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Database order error. Ensure your Firebase keys are writable!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlipkartYellow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CONFIRM COD DELIVERY ORDER", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        }
    }
}

// --- ADMIN SYSTEM DASHBOARD VIEWS ---
@Composable
fun AdminDashboardScreen(viewModel: MartViewModel) {
    val context = LocalContext.current
    var activeAdminTab by remember { mutableStateOf(0) } // 0: Products, 1: Categories, 2: Banners, 3: Orders

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sahatwar Mart Admin Portal",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WarningRed
            )
            IconButton(onClick = { viewModel.currentScreen = "account" }) {
                Icon(Icons.Default.Close, contentDescription = "Close admin", tint = Color.Black)
            }
        }

        // Subtabs selection
        TabRow(
            selectedTabIndex = activeAdminTab,
            containerColor = Color.White
        ) {
            Tab(selected = activeAdminTab == 0, onClick = { activeAdminTab = 0 }) { Text("Products", fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
            Tab(selected = activeAdminTab == 1, onClick = { activeAdminTab = 1 }) { Text("Category", fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
            Tab(selected = activeAdminTab == 2, onClick = { activeAdminTab = 2 }) { Text("Banners", fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
            Tab(selected = activeAdminTab == 3, onClick = { activeAdminTab = 3 }) { Text("Orders", fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (activeAdminTab) {
                0 -> AdminProductsTab(viewModel)
                1 -> AdminCategoriesTab(viewModel)
                2 -> AdminBannersTab(viewModel)
                3 -> AdminOrdersTab(viewModel)
            }
        }
    }
}

@Composable
fun AdminProductsTab(viewModel: MartViewModel) {
    val productsList by viewModel.products.collectAsState()
    val categoriesList by viewModel.categories.collectAsState()
    val context = LocalContext.current

    var showFormDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<Product?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Warning triggers
        val lowStockProducts = productsList.filter { it.stockQuantity <= it.warningThreshold }
        if (lowStockProducts.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarningRed.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, WarningRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "⚠ LOW STOCK ALERTS (${lowStockProducts.size} Items):",
                        fontWeight = FontWeight.Bold,
                        color = WarningRed,
                        fontSize = 12.sp
                    )
                    lowStockProducts.forEach {
                        Text(
                            text = "• ${it.name}: only ${it.stockQuantity} items in stock (threshold: ${it.warningThreshold})",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Registered Store Products (${productsList.size})", fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    selectedProductForEdit = null
                    showFormDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlipkartBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Product", color = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(productsList) { prod ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = prod.imageUrl.ifEmpty { "https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&q=80&w=200" },
                            contentDescription = prod.name,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFAFAFA))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "Price: ₹${prod.price.toInt()} | MRP: ₹${prod.mrp.toInt()}", fontSize = 12.sp, color = Color.DarkGray)
                            Text(text = "Stock: ${prod.stockQuantity} pcs (${if (prod.inStock) "In Stock" else "Out of Stock"})", fontSize = 11.sp, color = if (prod.inStock) DeliveryGreen else WarningRed)
                        }
                        Row {
                            IconButton(onClick = {
                                selectedProductForEdit = prod
                                showFormDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = FlipkartBlue)
                            }
                            IconButton(onClick = {
                                viewModel.deleteProduct(prod.id) {
                                    Toast.makeText(context, "Deleted Product ${prod.name}!", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = WarningRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        ProductFormDialog(
            product = selectedProductForEdit,
            categoriesList = categoriesList,
            viewModel = viewModel,
            onDismiss = { showFormDialog = false }
        )
    }
}

@Composable
fun ProductFormDialog(
    product: Product?,
    categoriesList: List<Category>,
    viewModel: MartViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(product?.name ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var mrp by remember { mutableStateOf(product?.mrp?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var stockQuantity by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "10") }
    var warningThreshold by remember { mutableStateOf(product?.warningThreshold?.toString() ?: "5") }
    var inStock by remember { mutableStateOf(product?.inStock ?: true) }
    var imageUrl by remember { mutableStateOf(product?.imageUrl ?: "") }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    // Set fallback default category if empty
    LaunchedEffect(categoriesList) {
        if (category.isEmpty() && categoriesList.isNotEmpty()) {
            category = categoriesList.first().name
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (product == null) "Add Grocery Product" else "Edit ${product.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FlipkartBlue
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                // Category selection dropdown fallback string input
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (e.g. Flour, Rice, Dairy)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Box {
                            var expandedCatMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { expandedCatMenu = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                            DropdownMenu(expanded = expandedCatMenu, onDismissRequest = { expandedCatMenu = false }) {
                                categoriesList.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            category = cat.name
                                            expandedCatMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Selling Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = mrp,
                        onValueChange = { mrp = it },
                        label = { Text("MRP (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("Stock Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = warningThreshold,
                        onValueChange = { warningThreshold = it },
                        label = { Text("Low Stock Alert Limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // In stock toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Stock Availability Status:")
                    Switch(checked = inStock, onCheckedChange = { inStock = it })
                }

                // Image visual picker
                Text("Product Image Media Source:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Button(
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Photo")
                    Spacer(Modifier.width(6.dp))
                    Text(if (selectedImageUri != null) "✓ Image Selected from Gallery" else "Pick Product Photo from Gallery")
                }

                if (selectedImageUri == null) {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Or input direct Image URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        if (name.isBlank() || price.isBlank() || mrp.isBlank() || category.isBlank()) {
                            Toast.makeText(context, "Fill name, prices, and category!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        val cleanPrice = price.toDoubleOrNull() ?: 0.0
                        val cleanMrp = mrp.toDoubleOrNull() ?: 0.0
                        val stockQty = stockQuantity.toIntOrNull() ?: 10
                        val warningLim = warningThreshold.toIntOrNull() ?: 5
                        
                        val payload = Product(
                            id = product?.id ?: "",
                            name = name.trim(),
                            description = description.trim(),
                            category = category.trim(),
                            imageUrl = imageUrl.trim(),
                            price = cleanPrice,
                            mrp = cleanMrp,
                            rating = product?.rating ?: (4.0f + (3..9).random() / 10f),
                            inStock = inStock && (stockQty > 0),
                            stockQuantity = stockQty,
                            warningThreshold = warningLim
                        )

                        if (product == null) {
                            viewModel.addProduct(payload, selectedImageUri, context) { success ->
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Added Product successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        } else {
                            viewModel.editProduct(payload, selectedImageUri, context) { success ->
                                isLoading = false
                                if (success) {
                                    Toast.makeText(context, "Edited Product successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FlipkartBlue),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (product == null) "SAVE PRODUCT" else "APPLY CHANGES", color = Color.White)
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AdminCategoriesTab(viewModel: MartViewModel) {
    val categoriesList by viewModel.categories.collectAsState()
    val context = LocalContext.current
    var newCatName by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Create Category", fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newCatName,
                onValueChange = { newCatName = it },
                label = { Text("Category Name (Fruits, Flour, etc.)") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newCatName.isNotBlank()) {
                        viewModel.addCategory(newCatName) { success ->
                            if (success) {
                                Toast.makeText(context, "Category added!", Toast.LENGTH_SHORT).show()
                                newCatName = ""
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlipkartBlue)
            ) {
                Text("Create")
            }
        }

        Divider()

        Text("Active Categories List (${categoriesList.size})", fontWeight = FontWeight.Bold)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(categoriesList) { cat ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = cat.name, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = WarningRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBannersTab(viewModel: MartViewModel) {
    val bannersList by viewModel.banners.collectAsState()
    val context = LocalContext.current

    var bannerTitle by remember { mutableStateOf("") }
    var selectedImgUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val bannerSelector = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImgUri = uri
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Upload New Promo Banner", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = bannerTitle,
            onValueChange = { bannerTitle = it },
            label = { Text("Banner Message / Title") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = {
                bannerSelector.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Image, contentDescription = "Media")
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (selectedImgUri != null) "✓ Banner Selected" else "Choose Banner Graphic from Gallery")
        }

        Button(
            onClick = {
                if (bannerTitle.isBlank() || selectedImgUri == null) {
                    Toast.makeText(context, "Title and Image are required!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isUploading = true
                viewModel.addBanner(bannerTitle, selectedImgUri!!, context) { success ->
                    isUploading = false
                    if (success) {
                        Toast.makeText(context, "Promo Banner Uploaded Successfully!", Toast.LENGTH_SHORT).show()
                        bannerTitle = ""
                        selectedImgUri = null
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = FlipkartBlue),
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isUploading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("UPLOAD BANNER", color = Color.White)
            }
        }

        Divider()

        Text("Active Promotions Flow (${bannersList.size})", fontWeight = FontWeight.Bold)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bannersList) { ban ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ban.imageUrl,
                            contentDescription = ban.title,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = ban.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteBanner(ban.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = WarningRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOrdersTab(viewModel: MartViewModel) {
    val ordersList by viewModel.orders.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Customer Orders Logbook (${ordersList.size})",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (ordersList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No customer orders received yet in database.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ordersList) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Customer: ${order.customerName}", fontWeight = FontWeight.Bold)
                                    Text(text = "Phone: ${order.phone} | email: ${order.email}", fontSize = 12.sp, color = Color.DarkGray)
                                    Text(text = "Deliv: ${order.address} (Landmark: ${order.landmark})", fontSize = 12.sp, color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = order.status.uppercase(), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 6.dp))

                            // Order items
                            order.items.forEach { item ->
                                Text(
                                    text = "➔ ${item.name} x ${item.quantity} (₹${(item.price * item.quantity).toInt()})",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Order Total: ₹${order.totalAmount.toInt()}", fontWeight = FontWeight.ExtraBold, color = FlipkartBlue)
                                Text(text = formatTimestamp(order.orderTime), fontSize = 11.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Status controller buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (order.status == "Pending") {
                                    Button(
                                        onClick = { viewModel.updateOrderStatus(order.id, "Accepted") },
                                        colors = ButtonDefaults.buttonColors(containerColor = FlipkartBlue),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Accept", fontSize = 11.sp, color = Color.White)
                                    }
                                    Button(
                                        onClick = { viewModel.updateOrderStatus(order.id, "Cancelled") },
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningRed),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                                
                                if (order.status == "Accepted") {
                                    Button(
                                        onClick = { viewModel.updateOrderStatus(order.id, "Out For Delivery") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Mark Out For Delivery", fontSize = 12.sp, color = Color.White)
                                    }
                                }
                                
                                if (order.status == "Out For Delivery") {
                                    Button(
                                        onClick = { viewModel.updateOrderStatus(order.id, "Delivered") },
                                        colors = ButtonDefaults.buttonColors(containerColor = DeliveryGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Mark Delivered", fontSize = 12.sp, color = Color.White)
                                    }
                                }

                                if (order.status == "Delivered") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DeliveryGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✓ Order Successfully Delivered", color = DeliveryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                if (order.status == "Cancelled") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(WarningRed.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✕ Order Cancelled / Rejected", color = WarningRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
