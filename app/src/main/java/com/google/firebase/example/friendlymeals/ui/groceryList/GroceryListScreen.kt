package com.google.firebase.example.friendlymeals.ui.groceryList

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.example.friendlymeals.R
import com.google.firebase.example.friendlymeals.data.model.GroceryItem
import com.google.firebase.example.friendlymeals.data.schema.LocalStore
import com.google.firebase.example.friendlymeals.ui.theme.BorderColor
import com.google.firebase.example.friendlymeals.ui.theme.FriendlyMealsTheme
import com.google.firebase.example.friendlymeals.ui.theme.LightTeal
import com.google.firebase.example.friendlymeals.ui.theme.Teal
import com.google.firebase.example.friendlymeals.ui.theme.TextColor
import kotlinx.serialization.Serializable

@Serializable
object GroceryListRoute

@Composable
fun GroceryListScreen(
    viewModel: GroceryListViewModel = hiltViewModel()
) {
    val groceries = viewModel.groceries.collectAsStateWithLifecycle()
    val localizerState = viewModel.localizerState.collectAsStateWithLifecycle()

    GroceryListScreenContent(
        groceries = groceries.value,
        localizerState = localizerState.value,
        onAddItem = viewModel::addItem,
        onToggleItem = viewModel::toggleItem,
        onDeleteItem = viewModel::deleteItem,
        onLocalize = viewModel::localizeGroceryList,
        onResetLocalizer = viewModel::resetLocalizer
    )
}

@Composable
fun GroceryListScreenContent(
    groceries: List<GroceryItem>,
    localizerState: LocalizerUiState = LocalizerUiState.Idle,
    onAddItem: (String) -> Unit = {},
    onToggleItem: (GroceryItem) -> Unit = {},
    onDeleteItem: (GroceryItem) -> Unit = {},
    onLocalize: (Double, Double) -> Unit = { _, _ -> },
    onResetLocalizer: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            showBottomSheet = true
            getCurrentLocation(fusedLocationClient,
                onSuccess = { lat, lng ->
                    onLocalize(lat, lng)
                },
                onFailure = {
                    //TODO: handle failure
                }
            )
        }
    }

    fun startLocalizer() {
        if (hasLocationPermission(context)) {
            showBottomSheet = true
            getCurrentLocation(fusedLocationClient,
                onSuccess = { lat, lng ->
                    onLocalize(lat, lng)
                },
                onFailure = {
                    //TODO: handle failure
                }
            )
        } else {
            permissionLauncher.launch(
                arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
            )
        }
    }

    fun handleAdd() {
        if (inputText.isNotBlank()) {
            onAddItem(inputText)
            inputText = ""
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.grocery_list_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { startLocalizer() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = stringResource(R.string.store_localizer_button_content_description),
                        tint = Teal,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(
                        stringResource(R.string.add_grocery_item_hint),
                        color = Color.Gray
                    ) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Teal,
                        unfocusedBorderColor = BorderColor
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { handleAdd() }),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { handleAdd() },
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(54.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_button),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (groceries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.grocery_list_empty_message),
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = groceries, key = { it.id }) { item ->
                        GroceryCard(
                            item = item,
                            onToggle = { onToggleItem(item) },
                            onDelete = { onDeleteItem(item) }
                        )
                    }
                }
            }
            if (showBottomSheet) {
                LocalizerBottomSheet(
                    uiState = localizerState,
                    onDismiss = {
                        showBottomSheet = false
                        onResetLocalizer()
                    },
                    onRetry = {
                        getCurrentLocation(fusedLocationClient,
                            onSuccess = { lat, lng ->
                                onLocalize(lat, lng)
                            },
                            onFailure = {
                                //TODO: handle failure
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun GroceryCard(
    item: GroceryItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.checked) 0.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(if (item.checked) Teal else LightTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.checked) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color.White, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = item.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.checked) Color.Gray else TextColor,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                    lineHeight = 22.sp
                )
            }

            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.delete_button_content_description),
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalizerBottomSheet(
    uiState: LocalizerUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF7F9FB),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.store_localizer_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = stringResource(R.string.store_localizer_subtitle),
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            when (uiState) {
                is LocalizerUiState.Idle -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Teal,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.store_localizer_determining_location),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextColor
                        )
                    }
                }
                is LocalizerUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Teal,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.store_localizer_locating_stores),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextColor
                        )
                    }
                }
                is LocalizerUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.message,
                            fontSize = 16.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = Teal)
                        ) {
                            Text(stringResource(R.string.store_localizer_retry))
                        }
                    }
                }
                is LocalizerUiState.Success -> {
                    if (uiState.stores.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.store_localizer_no_stores),
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.stores) { store ->
                                StoreCard(store = store)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreCard(store: LocalStore) {
    val uriHandler = LocalUriHandler.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = store.mapUrl.isNotBlank()) {
                uriHandler.openUri(store.mapUrl)
            }
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = store.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = store.address,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Place icon",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = store.distance,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (store.openNow) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = stringResource(R.string.store_localizer_open_now),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFE57373), CircleShape)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = stringResource(R.string.store_localizer_closed),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC62828)
                        )
                    }
                }

                if (store.closingSoon) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF9800), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.store_localizer_closing_soon),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF6C00)
                        )
                    }
                }

                val parkingColor = if (store.hasParking) Color(0xFFE3F2FD) else Color(0xFFECEFF1)
                val parkingTextColor = if (store.hasParking) Color(0xFF1565C0) else Color(0xFF455A64)
                val text = if (store.hasParking) R.string.store_localizer_parking else R.string.store_localizer_no_parking

                Text(
                    text = stringResource(text),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = parkingTextColor,
                    modifier = Modifier
                        .background(parkingColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            if (store.parkingDetails.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = store.parkingDetails,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
fun getCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onSuccess: (Double, Double) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val priority = Priority.PRIORITY_HIGH_ACCURACY
    val cancellationTokenSource = CancellationTokenSource()

    fusedLocationClient.getCurrentLocation(priority, cancellationTokenSource.token)
        .addOnSuccessListener { location ->
            if (location != null) {
                onSuccess(location.latitude, location.longitude)
            } else {
                onFailure(Exception("Location is null"))
                //TODO: Fix OnFailure logic
            }
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}

@Preview
@Composable
fun GroceryListScreenPreview() {
    FriendlyMealsTheme {
        GroceryListScreenContent(
            groceries = listOf(
                GroceryItem(id = "1", name = "2 cloves garlic", checked = true),
                GroceryItem(id = "2", name = "400g canned tomatoes", checked = false),
                GroceryItem(id = "3", name = "Fresh basil", checked = false)
            )
        )
    }
}
