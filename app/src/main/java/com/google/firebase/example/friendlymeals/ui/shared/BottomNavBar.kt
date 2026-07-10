package com.google.firebase.example.friendlymeals.ui.shared

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.google.firebase.example.friendlymeals.R
import com.google.firebase.example.friendlymeals.ui.generate.GenerateRoute
import com.google.firebase.example.friendlymeals.ui.groceryList.GroceryListRoute
import com.google.firebase.example.friendlymeals.ui.recipeList.RecipeListRoute
import com.google.firebase.example.friendlymeals.ui.scanMeal.ScanMealRoute
import com.google.firebase.example.friendlymeals.ui.theme.Teal

sealed class BottomNavItem(val route: Any, val icon: Int, val label: Int) {
    object ScanMeal : BottomNavItem(ScanMealRoute, R.drawable.ic_camera, R.string.nav_bar_scan_meal)
    object Generate : BottomNavItem(GenerateRoute, R.drawable.ic_generate, R.string.nav_bar_generate)
    object RecipeList : BottomNavItem(RecipeListRoute, R.drawable.ic_dine, R.string.nav_bar_recipe_list)
    object GroceryList : BottomNavItem(GroceryListRoute, R.drawable.ic_check, R.string.nav_bar_grocery_list)
}

@Composable
fun BottomNavBar(
    isFirestoreAvailable: Boolean = true,
    onDisabledItemClicked: () -> Unit = {},
    navigateTo: (Any) -> Unit
) {
    var selectedItemIndex by remember { mutableIntStateOf(1) }

    val items = listOf(
        BottomNavItem.ScanMeal,
        BottomNavItem.Generate,
        BottomNavItem.RecipeList,
        BottomNavItem.GroceryList
    )

    NavigationBar {
        items.forEachIndexed { index, item ->
            val label = stringResource(item.label)
            val isEnabled = isFirestoreAvailable || (item != BottomNavItem.RecipeList && item != BottomNavItem.GroceryList)

            val itemColors = if (isEnabled) {
                NavigationBarItemDefaults.colors(
                    selectedIconColor = Teal,
                    indicatorColor = Color.Transparent,
                    selectedTextColor = Teal
                )
            } else {
                NavigationBarItemDefaults.colors(
                    unselectedIconColor = Color.Gray.copy(alpha = 0.4f),
                    unselectedTextColor = Color.Gray.copy(alpha = 0.4f),
                    selectedIconColor = Color.Gray.copy(alpha = 0.4f),
                    selectedTextColor = Color.Gray.copy(alpha = 0.4f),
                    indicatorColor = Color.Transparent
                )
            }

            NavigationBarItem(
                selected = selectedItemIndex == index,
                onClick = {
                    if (isEnabled) {
                        selectedItemIndex = index
                        navigateTo(item.route)
                    } else {
                        onDisabledItemClicked()
                    }
                },
                icon = { Icon(
                    painter = painterResource(item.icon),
                    contentDescription = label
                ) },
                colors = itemColors,
                label = { Text(label) }
            )
        }
    }
}
