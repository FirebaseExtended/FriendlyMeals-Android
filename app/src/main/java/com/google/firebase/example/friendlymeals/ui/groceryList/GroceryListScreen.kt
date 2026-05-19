package com.google.firebase.example.friendlymeals.ui.groceryList

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.example.friendlymeals.R
import com.google.firebase.example.friendlymeals.data.model.GroceryItem
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

    GroceryListScreenContent(
        groceries = groceries.value,
        onAddItem = viewModel::addItem,
        onToggleItem = viewModel::toggleItem,
        onDeleteItem = viewModel::deleteItem
    )
}

@Composable
fun GroceryListScreenContent(
    groceries: List<GroceryItem>,
    onAddItem: (String) -> Unit = {},
    onToggleItem: (GroceryItem) -> Unit = {},
    onDeleteItem: (GroceryItem) -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }

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
