package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LanguageManager
import com.example.ui.MainViewModel
import com.example.ui.screens.HighlightsHubScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerDashboardScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val currentPalette by viewModel.currentPalette.collectAsState()
    val currentLangState by viewModel.currentLanguage.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Request Notification permission launcher safely
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle result elegantly
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(310.dp)
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(24.dp)
                ) {
                    // Header Brand
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = LanguageManager.getString("app_title", currentLangState),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Premium Private Offline Audiobook",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Language Selection Section
                    Text(
                        text = LanguageManager.getString("select_lang", currentLangState),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        LanguageManager.Language.values().forEach { language ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (currentLangState == language) MaterialTheme.colorScheme.primaryContainer 
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.setLanguage(language)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentLangState == language,
                                    onClick = { viewModel.setLanguage(language) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = language.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (currentLangState == language) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Dark/Light toggle option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageManager.getString("dark_mode", currentLangState),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleDarkMode() }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Customize Pastel Theme Palette Picker selection
                    Text(
                        text = LanguageManager.getString("theme", currentLangState),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Scrollable palette options
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.theme.ReaderThemePalette.values().take(4).forEach { palette ->
                            val isSelected = currentPalette == palette
                            AssistChip(
                                onClick = { viewModel.setPalette(palette) },
                                label = { Text(palette.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                     else Color.Transparent
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.theme.ReaderThemePalette.values().drop(4).forEach { palette ->
                            val isSelected = currentPalette == palette
                            AssistChip(
                                onClick = { viewModel.setPalette(palette) },
                                label = { Text(palette.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                                     else Color.Transparent
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Notification Activation Access Button
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Notification Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(32.dp))

                    // "Made with love by Editingcells" developer credits
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = LanguageManager.getString("made_with_love", currentLangState),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "ReaderMe Premium • Offline Secured",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                // Hide bottom navigation completely when reading
                if (activeTab != 4) {
                    NavigationBar(
                        modifier = Modifier.testTag("app_bottom_nav_bar")
                    ) {
                        val tabs = listOf(
                            BottomTabDetails(0, LanguageManager.getString("library", currentLangState), Icons.Filled.Book, Icons.Outlined.Book),
                            BottomTabDetails(1, LanguageManager.getString("player", currentLangState), Icons.Filled.Headphones, Icons.Outlined.Headphones),
                            BottomTabDetails(2, LanguageManager.getString("stats", currentLangState), Icons.Filled.BarChart, Icons.Outlined.BarChart),
                            BottomTabDetails(3, LanguageManager.getString("memos", currentLangState), Icons.Filled.StickyNote2, Icons.Outlined.StickyNote2)
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
                            onOpenReader = { viewModel.setActiveTab(4) },
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
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
}

data class BottomTabDetails(
    val index: Int,
    val title: String,
    val activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector
)
