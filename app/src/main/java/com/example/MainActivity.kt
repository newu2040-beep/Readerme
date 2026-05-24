package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.MainViewModel
import com.example.ui.screens.HighlightsHubScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerDashboardScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Collect theme configurations from unified ViewModel
            val activePalette by mainViewModel.currentPalette.collectAsState()
            val isDarkTheme by mainViewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(
                darkTheme = isDarkTheme,
                palette = activePalette,
                dynamicColor = false
            ) {
                MainAppScaffold(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedBook by viewModel.selectedBook.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Hide bottom navigation completely when reading to offer standard full-bleed focus immersion!
            if (activeTab != 4) {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_nav_bar")
                ) {
                    val tabs = listOf(
                        BottomTabDetails(0, "Library", Icons.Filled.Book, Icons.Outlined.Book),
                        BottomTabDetails(1, "Player", Icons.Filled.Headphones, Icons.Outlined.Headphones),
                        BottomTabDetails(2, "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
                        BottomTabDetails(3, "Memos", Icons.Filled.StickyNote2, Icons.Outlined.StickyNote2)
                    )

                    tabs.forEach { tab ->
                        val isSelected = activeTab == tab.index
                        NavigationBarItem(
                            selected = isSelected,
                            label = { Text(tab.title) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                    contentDescription = tab.title
                                )
                            },
                            onClick = { viewModel.setActiveTab(tab.index) },
                            modifier = Modifier.testTag("nav_item_${tab.title.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (activeTab) {
                0 -> {
                    LibraryScreen(
                        viewModel = viewModel,
                        onOpenReader = { viewModel.setActiveTab(4) }
                    )
                }
                1 -> {
                    PlayerDashboardScreen(viewModel = viewModel)
                }
                2 -> {
                    StatsScreen(viewModel = viewModel)
                }
                3 -> {
                    HighlightsHubScreen(
                        viewModel = viewModel,
                        onJumpToBook = { viewModel.setActiveTab(4) }
                    )
                }
                4 -> {
                    ReaderScreen(viewModel = viewModel)
                }
            }
        }
    }
}

data class BottomTabDetails(
    val index: Int,
    val title: String,
    val activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector
)
