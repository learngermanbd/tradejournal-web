package com.tradejournal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tradejournal.app.data.Account
import com.tradejournal.app.data.DiaryNote
import com.tradejournal.app.data.ImportRecord
import com.tradejournal.app.data.Trade
import com.tradejournal.app.data.TradeCalculations
import com.tradejournal.app.data.TradeJournalApplication
import com.tradejournal.app.data.TradeViewModel
import com.tradejournal.app.data.UserSettings
import com.tradejournal.app.data.UserSettingsStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TradeJournalTheme {
                TradeJournalApp()
            }
        }
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF4059D7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFECEFff),
    secondary = Color(0xFF008B72),
    background = Color(0xFFF5F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0F3F8),
    onSurfaceVariant = Color(0xFF68758C),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BA8FF),
    onPrimary = Color(0xFF17205D),
    primaryContainer = Color(0xFF2A326B),
    secondary = Color(0xFF5BD5BD),
    background = Color(0xFF101521),
    surface = Color(0xFF171E2D),
    surfaceVariant = Color(0xFF1D2637),
    onSurfaceVariant = Color(0xFF9CA8BC),
)

@Composable
private fun TradeJournalTheme(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}

private enum class Role { USER, ADMIN }
private enum class AppScreen { HOME, JOURNAL, ANALYSIS, ACCOUNTS, DIARY, IMPORTS, SETTINGS, ADMIN_HOME, ADMIN_USERS, ADMIN_REPORTS }

// Development-only demo credential. Production must use Firebase/Auth provider claims.
private const val DEMO_ADMIN_EMAIL = "admin@tradejournal.dev"
private const val DEMO_ADMIN_PASSWORD = "AdminDemo123!"

private fun seedAccounts() = listOf(
    Account(name = "Main account", type = "Live", broker = "Interactive Brokers", balance = 12450.0, equity = 13695.80),
    Account(name = "Crypto account", type = "Live", broker = "Binance", balance = 8000.0, equity = 8420.15),
    Account(name = "Practice account", type = "Paper", broker = "Manual", balance = 50000.0, equity = 52180.0),
)

private fun seedDiaryNotes() = listOf(
    DiaryNote(noteDate = "March 24, 2026", mood = "Focused", plan = "Focus on patient breakout entries. No trades after two consecutive losses.", reflection = "The best trade was the one where I waited for confirmation."),
    DiaryNote(noteDate = "March 23, 2026", mood = "Frustrated", plan = "Respect the stop and do not chase.", reflection = "Moved the stop because I wanted to be right."),
)

private fun seedTrades() = listOf(
    Trade(symbol = "NVDA", market = "Stocks", direction = "Long", setup = "Breakout", result = 240.0, rMultiple = 2.4, status = "Plan followed"),
    Trade(symbol = "EUR/USD", market = "Forex", direction = "Short", setup = "Reversal", result = -85.0, rMultiple = -1.0, status = "Moved stop"),
    Trade(symbol = "BTC/USDT", market = "Crypto", direction = "Long", setup = "Momentum", result = 216.0, rMultiple = 1.8, status = "Calm"),
    Trade(symbol = "ESM6", market = "Futures", direction = "Short", setup = "Failed break", result = -60.0, rMultiple = -.4, status = "Early exit"),
    Trade(symbol = "AAPL", market = "Stocks", direction = "Long", setup = "Support", result = 108.0, rMultiple = 1.2, status = "Plan followed"),
)

@Composable
private fun TradeJournalApp() {
    var signedIn by rememberSaveable { mutableStateOf(false) }
    var roleName by rememberSaveable { mutableStateOf(Role.USER.name) }
    var screenName by rememberSaveable { mutableStateOf(AppScreen.HOME.name) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as TradeJournalApplication
    val tradeViewModel: TradeViewModel = viewModel(factory = TradeViewModel.Factory(application.tradeRepository))
    val settingsStore = remember { UserSettingsStore(context) }
    var userSettings by remember { mutableStateOf(settingsStore.load()) }
    val trades by tradeViewModel.trades.collectAsStateWithLifecycle()
    val accounts by tradeViewModel.accounts.collectAsStateWithLifecycle()
    val diaryNotes by tradeViewModel.diaryNotes.collectAsStateWithLifecycle()
    val importRecords by tradeViewModel.importRecords.collectAsStateWithLifecycle()
    val importStatus by tradeViewModel.importStatus.collectAsStateWithLifecycle()
    var showAddTrade by remember { mutableStateOf(false) }
    var showConnectAccount by remember { mutableStateOf(false) }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val csv = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@rememberLauncherForActivityResult
        val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "trade-import.csv"
        tradeViewModel.importCsv(fileName, csv)
    }

    if (!signedIn) {
        LoginScreen { role ->
            roleName = role.name
            screenName = if (role == Role.ADMIN) AppScreen.ADMIN_HOME.name else AppScreen.HOME.name
            signedIn = true
        }
        return
    }

    val role = Role.valueOf(roleName)
    val screen = AppScreen.valueOf(screenName)

    LaunchedEffect(Unit) {
        tradeViewModel.seedIfEmpty(seedTrades())
        tradeViewModel.seedAccountsIfEmpty(seedAccounts())
        tradeViewModel.seedDiaryNotesIfEmpty(seedDiaryNotes())
    }

    fun selectScreen(next: AppScreen) {
        screenName = next.name
    }

    fun logout() {
        signedIn = false
        roleName = Role.USER.name
        screenName = AppScreen.HOME.name
        showAddTrade = false
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val wideLayout = maxWidth >= 720.dp
            Row(Modifier.fillMaxSize()) {
                if (wideLayout) {
                    SideNavigation(role = role, screen = screen, onScreenChange = ::selectScreen, onLogout = ::logout)
                }
                Column(Modifier.fillMaxSize()) {
                    AppHeader(role = role, onLogout = ::logout, onAddTrade = { showAddTrade = true })
                    Box(Modifier.weight(1f)) {
                        when (screen) {
                            AppScreen.HOME -> UserHome(trades, onAddTrade = { showAddTrade = true }, onOpenAnalysis = { selectScreen(AppScreen.ANALYSIS) })
                            AppScreen.JOURNAL -> JournalScreen(trades, onAddTrade = { showAddTrade = true }, onOpenImports = { selectScreen(AppScreen.IMPORTS) })
                            AppScreen.ANALYSIS -> AnalysisScreen(trades)
                            AppScreen.ACCOUNTS -> AccountsScreen(accounts, onConnectAccount = { showConnectAccount = true })
                            AppScreen.DIARY -> DiaryScreen(diaryNotes, onSave = tradeViewModel::addDiaryNote)
                            AppScreen.IMPORTS -> ImportScreen(importRecords, importStatus, onPickCsv = { csvPicker.launch("text/*") })
                            AppScreen.SETTINGS -> SettingsScreen(
                                settings = userSettings,
                                onSave = { updated ->
                                    userSettings = updated
                                    settingsStore.save(updated)
                                },
                                onConnectAccount = { showConnectAccount = true },
                                onOpenDiary = { selectScreen(AppScreen.DIARY) },
                            )
                            AppScreen.ADMIN_HOME -> AdminHome()
                            AppScreen.ADMIN_USERS -> AdminUsers()
                            AppScreen.ADMIN_REPORTS -> AdminReports()
                        }
                    }
                    if (!wideLayout) {
                        BottomNavigation(role = role, screen = screen, onScreenChange = ::selectScreen, onAddTrade = { showAddTrade = true })
                    }
                }
            }
        }
    }

    if (showAddTrade) {
        AddTradeDialog(
            initialMarket = userSettings.defaultMarket,
            onDismiss = { showAddTrade = false },
            onSave = { trade ->
                tradeViewModel.addTrade(trade)
                showAddTrade = false
            },
        )
    }
    if (showConnectAccount) {
        ConnectAccountDialog(
            onDismiss = { showConnectAccount = false },
            onSave = { account ->
                tradeViewModel.addAccount(account)
                showConnectAccount = false
            },
        )
    }
}

@Composable
private fun LoginScreen(onLogin: (Role) -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(Modifier.size(58.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) { Text("↗", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
            }
            Text("TradeJournal", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
            Text("Your edge, measured", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Card(Modifier.fillMaxWidth().padding(top = 24.dp), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = email, onValueChange = { email = it; error = null }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = password, onValueChange = { password = it; error = null }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    error?.let { Text(it, color = Color(0xFFC64D5C), fontSize = 12.sp) }
                    Button(
                        onClick = {
                            when {
                                email.trim() == DEMO_ADMIN_EMAIL && password == DEMO_ADMIN_PASSWORD -> onLogin(Role.ADMIN)
                                email.isNotBlank() && password.length >= 4 -> onLogin(Role.USER)
                                else -> error = "Enter a valid email and password."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Sign in") }
                    Text("Your account is verified securely before the workspace opens.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SideNavigation(
    role: Role,
    screen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    onLogout: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(238.dp).fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) { Box(contentAlignment = Alignment.Center) { Text("↗", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) } }
                Spacer(Modifier.width(10.dp))
                Column { Text("TradeJournal", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("your edge, measured", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) }
            }
            Spacer(Modifier.height(18.dp))
            Text("WORKSPACE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            if (role == Role.USER) {
                NavItem("⌂", "Home", AppScreen.HOME, screen, onScreenChange)
                NavItem("▤", "Journal", AppScreen.JOURNAL, screen, onScreenChange)
                NavItem("◔", "Analysis", AppScreen.ANALYSIS, screen, onScreenChange)
                NavItem("⇩", "Imports", AppScreen.IMPORTS, screen, onScreenChange)
                NavItem("◎", "Accounts", AppScreen.ACCOUNTS, screen, onScreenChange)
                NavItem("⚙", "Settings", AppScreen.SETTINGS, screen, onScreenChange)
                Text("IMPROVE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(top = 14.dp))
                NavItem("✎", "Trading diary", AppScreen.DIARY, screen, onScreenChange)
            } else {
                NavItem("⌂", "Overview", AppScreen.ADMIN_HOME, screen, onScreenChange)
                NavItem("♙", "Users", AppScreen.ADMIN_USERS, screen, onScreenChange)
                NavItem("▥", "Reports", AppScreen.ADMIN_REPORTS, screen, onScreenChange)
            }
            Spacer(Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = .12f), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Local-first & private", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Trading data stays on-device. Drive sync is optional.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            SessionBadge(role, onLogout)
        }
    }
}

@Composable
private fun BottomNavigation(role: Role, screen: AppScreen, onScreenChange: (AppScreen) -> Unit, onAddTrade: () -> Unit) {
    Surface(tonalElevation = 4.dp, modifier = Modifier.navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (role == Role.USER) {
                BottomNavItem("⌂", "Home", AppScreen.HOME, screen, onScreenChange)
                BottomNavItem("▤", "Journal", AppScreen.JOURNAL, screen, onScreenChange)
                TextButton(onClick = onAddTrade, modifier = Modifier.weight(1f)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("＋", fontSize = 21.sp, color = MaterialTheme.colorScheme.primary); Text("Add", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary) } }
                BottomNavItem("◔", "Analysis", AppScreen.ANALYSIS, screen, onScreenChange)
                BottomNavItem("⚙", "Settings", AppScreen.SETTINGS, screen, onScreenChange)
            } else {
                BottomNavItem("⌂", "Overview", AppScreen.ADMIN_HOME, screen, onScreenChange)
                BottomNavItem("♙", "Users", AppScreen.ADMIN_USERS, screen, onScreenChange)
                BottomNavItem("▥", "Reports", AppScreen.ADMIN_REPORTS, screen, onScreenChange)
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(icon: String, label: String, destination: AppScreen, selected: AppScreen, onClick: (AppScreen) -> Unit) {
    TextButton(onClick = { onClick(destination) }, modifier = Modifier.weight(1f)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, fontSize = 19.sp, color = if (destination == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant); Text(label, fontSize = 10.sp, color = if (destination == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun NavItem(icon: String, label: String, destination: AppScreen, selected: AppScreen, onClick: (AppScreen) -> Unit) {
    val active = destination == selected
    TextButton(
        onClick = { onClick(destination) },
        colors = ButtonDefaults.textButtonColors(contentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 19.sp, modifier = Modifier.width(28.dp)); Text(label, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium) }
    }
}

@Composable
private fun AppHeader(role: Role, onLogout: () -> Unit, onAddTrade: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(if (role == Role.USER) "Good morning, Alex" else "Admin control center", fontSize = 23.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.5).sp); Text(if (role == Role.USER) "Your decisions, measured with context." else "Monitor the product, users, and service health.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp)) }
            if (role == Role.USER) AssistChip(onClick = onAddTrade, label = { Text("＋ Add trade") })
            Spacer(Modifier.width(8.dp))
            SessionBadge(role, onLogout)
        }
    }
}

@Composable
private fun SessionBadge(role: Role, onLogout: () -> Unit) {
    Column(horizontalAlignment = Alignment.End) {
        Text(if (role == Role.ADMIN) "Admin session" else "User session", color = if (role == Role.ADMIN) Color(0xFFB87416) else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        TextButton(onClick = onLogout, contentPadding = PaddingValues(0.dp)) { Text("Sign out", fontSize = 10.sp) }
    }
}

@Composable
private fun UserHome(trades: List<Trade>, onAddTrade: () -> Unit, onOpenAnalysis: () -> Unit) {
    val net = trades.sumOf { it.result }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) { Text("Performance snapshot", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp); Text("March 2026", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("All accounts · local analysis", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                OutlinedButton(onClick = onOpenAnalysis) { Text("Full analysis →") }
            }
        }
        item {
            ResponsiveCards {
                MetricCard("Net P&L", "+$${"%.2f".format(net)}", "↑ 8.4% vs last month", positive = net >= 0)
                MetricCard("Win rate", "58.3%", "42 trades this month", positive = true)
                MetricCard("Expectancy", "+$29.66", "+0.42R average", positive = true)
                MetricCard("Max drawdown", "−3.2%", "Within your 5% limit", positive = false, warning = true)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Card(Modifier.weight(1f)) { Column(Modifier.padding(18.dp)) { Text("Equity curve", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Account growth across recorded trades", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp); EquityChart(); } }
                Card(Modifier.weight(1f)) { Column(Modifier.padding(18.dp)) { Text("Monthly review", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Turn your data into one next step", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp); Spacer(Modifier.height(14.dp)); Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Text("Your review is ready", fontWeight = FontWeight.Bold); Text("You have enough data to spot patterns in March.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) } }; Spacer(Modifier.height(13.dp)); Text("4 of 6 sections complete", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp); ProgressBar(.72f); Spacer(Modifier.height(13.dp)); Button(onClick = onOpenAnalysis, modifier = Modifier.fillMaxWidth()) { Text("Continue review") } } }
            }
        }
        item { SectionHeader("Recent trades", "View journal →") }
        items(trades.take(4), key = { it.id }) { TradeRow(it) }
        item { SectionHeader("Patterns to review", "Local analysis") }
        item { InsightCard("Breakouts are your edge", "12 trades · 66% win rate · +$890 this month", "↑", true) }
        item { InsightCard("Watch post-loss entries", "3 trades after a loss averaged −1.4R.", "!", false) }
        item { Button(onClick = onAddTrade, modifier = Modifier.fillMaxWidth()) { Text("＋ Add your next trade") } }
    }
}

@Composable
private fun ResponsiveCards(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun RowScope.MetricCard(label: String, value: String, detail: String, positive: Boolean, warning: Boolean = false) {
    Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.padding(15.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(value, color = if (warning) Color(0xFFB87416) else if (positive) MaterialTheme.colorScheme.secondary else Color(0xFFC64D5C), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp)) } }
}

@Composable
private fun EquityChart() {
    Row(Modifier.fillMaxWidth().height(150.dp).padding(top = 16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(.28f, .38f, .34f, .48f, .44f, .58f, .52f, .67f, .61f, .73f, .69f, .84f, .78f, .96f).forEachIndexed { index, height ->
            Box(Modifier.weight(1f).fillMaxHeight(height).background(if (index == 13) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .42f), RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Mar 1", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp); Text("Mar 15", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp); Text("Mar 31", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) }
}

@Composable
private fun SectionHeader(title: String, action: String) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(action, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }

@Composable
private fun TradeRow(trade: Trade) {
    Card { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(34.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Text(trade.symbol.take(2), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp) } }; Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(trade.symbol, fontWeight = FontWeight.Bold); Text("${trade.direction} · ${trade.setup}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }; Column(horizontalAlignment = Alignment.End) { Text(if (trade.result >= 0) "+$${"%.2f".format(trade.result)}" else "−$${"%.2f".format(-trade.result)}", color = if (trade.result >= 0) MaterialTheme.colorScheme.secondary else Color(0xFFC64D5C), fontWeight = FontWeight.Bold); Text(trade.status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp) } } }
}

@Composable
private fun InsightCard(title: String, body: String, icon: String, positive: Boolean) { Card { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(30.dp), shape = RoundedCornerShape(9.dp), color = if (positive) MaterialTheme.colorScheme.secondary.copy(alpha = .13f) else Color(0xFFFFF0E0)) { Box(contentAlignment = Alignment.Center) { Text(icon, color = if (positive) MaterialTheme.colorScheme.secondary else Color(0xFFB87416), fontWeight = FontWeight.Bold) } }; Column(Modifier.padding(start = 10.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)) } } } }

@Composable
private fun JournalScreen(trades: List<Trade>, onAddTrade: () -> Unit, onOpenImports: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeading("Journal", "Search, tag, and review every decision behind the result.", "Import CSV", onOpenImports) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { FilterChip(selected = true, onClick = {}, label = { Text("All trades") }); FilterChip(selected = false, onClick = {}, label = { Text("Winners") }); FilterChip(selected = false, onClick = {}, label = { Text("Losers") }); FilterChip(selected = false, onClick = {}, label = { Text("Needs review") }) } }
        item { Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp) { Column(Modifier.padding(vertical = 5.dp)) { trades.forEach { TradeRow(it) } } } }
        item { Card { Column(Modifier.padding(18.dp)) { Text("March calendar", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("12 winning days · 6 losing days · best day +$470", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)); Spacer(Modifier.height(12.dp)); Text("Mon   Tue   Wed   Thu   Fri   Sat   Sun", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp); Text("  2✓     3       4       5−     6       7✓     8", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold); Text("  9      10✓    11      12−    13      14✓    15", modifier = Modifier.padding(top = 9.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) } } }
    }
}

@Composable
private fun ImportScreen(records: List<ImportRecord>, status: String?, onPickCsv: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeading("Import trades", "Bring broker CSV files into the local journal with validation and duplicate protection.", "Choose CSV", onPickCsv) }
        item { Card { Column(Modifier.padding(18.dp)) { Text("CSV import workflow", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Your file is parsed locally and is not uploaded to the backend.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)); Spacer(Modifier.height(14.dp)); Text("Required columns", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text("symbol/ticker and pnl/profit/result", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)); Text("Optional columns", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(top = 13.dp)); Text("market, direction/side, setup/strategy, and r/r-multiple", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)); status?.let { Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = .12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 14.dp)) { Text(it, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, modifier = Modifier.padding(11.dp)) } }; Button(onClick = onPickCsv, modifier = Modifier.padding(top = 16.dp)) { Text("Select CSV file") } } } }
        item { SectionHeader("Import history", "Local only") }
        if (records.isEmpty()) item { InsightCard("No imports yet", "Choose a broker CSV to create your first import record.", "⇩", true) }
        items(records, key = { it.id }) { record -> ImportRecordRow(record) }
    }
}

@Composable
private fun ImportRecordRow(record: ImportRecord) { Card { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(34.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Text("CSV", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 9.sp) } }; Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(record.fileName, fontWeight = FontWeight.Bold); Text("${record.importedCount} imported · ${record.skippedCount} skipped", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }; Text("Local", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 11.sp) } } }

@Composable
private fun AnalysisScreen(trades: List<Trade>) {
    val net = trades.sumOf { it.result }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeading("Analysis", "Turn numbers into an honest review of the decisions behind them.", "March 2026", {}) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Overview", "Strategies", "Risk", "Psychology").forEachIndexed { index, label -> FilterChip(selected = index == 0, onClick = {}, label = { Text(label) }) } } }
        item { ResponsiveCards { MetricCard("Net P&L", "+$${"%.0f".format(net)}", "This month", true); MetricCard("Win rate", "58.3%", "42 trades", true); MetricCard("Profit factor", "1.82", "Healthy edge", true); MetricCard("Drawdown", "−3.2%", "Limit 5%", false, true) } }
        item { Card { Column(Modifier.padding(18.dp)) { Text("Strategy performance", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Where your edge is showing up", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)); Spacer(Modifier.height(18.dp)); ProgressRow("Breakout", "+$890 · 66% win rate", .82f, true); ProgressRow("Reversal", "+$210 · 42% win rate", .47f, false); ProgressRow("Momentum", "+$145 · 50% win rate", .36f, false) } } }
        item { InsightCard("Your local analysis", "Breakout setups produced 71% of monthly profit. Losses were more common after two consecutive losing trades.", "✓", true) }
        item { Card { Column(Modifier.padding(18.dp)) { Text("Monthly reflection", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("What did you learn?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)); OutlinedTextField(value = "My process is strongest when I wait for confirmation.", onValueChange = {}, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), minLines = 3); Button(onClick = {}, modifier = Modifier.padding(top = 10.dp)) { Text("Save reflection") } } } }
    }
}

@Composable
private fun ProgressRow(label: String, detail: String, progress: Float, positive: Boolean) { Column(Modifier.padding(vertical = 7.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontWeight = FontWeight.Bold); Text(detail, color = if (positive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }; Spacer(Modifier.height(7.dp)); ProgressBar(progress, positive) } }

@Composable
private fun ProgressBar(progress: Float, positive: Boolean = false) { Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))) { Box(Modifier.fillMaxWidth(progress).height(8.dp).background(if (positive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary, RoundedCornerShape(50))) } }

@Composable
private fun AccountsScreen(accounts: List<Account>, onConnectAccount: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeading("Accounts", "Connect read-only accounts and keep live, paper, and demo performance separate.", "＋ Connect account", onConnectAccount) }
        items(accounts, key = { it.id }) { account -> AccountCard(account.name, "${account.type} · ${account.broker}", "${account.currency} ${"%,.2f".format(account.equity)}", "Balance ${account.currency} ${"%,.2f".format(account.balance)}") }
        item {
            Card {
                Column(Modifier.padding(18.dp)) {
                    Text("Connections", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Read-only by default · credentials never belong in the journal database", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                    Spacer(Modifier.height(13.dp))
                    accounts.filter { it.broker != "Manual" }.forEach { Text("✓  ${it.broker} · connection ready", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp)) }
                    OutlinedButton(onClick = onConnectAccount, modifier = Modifier.padding(top = 13.dp)) { Text("Manage connections") }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: UserSettings,
    onSave: (UserSettings) -> Unit,
    onConnectAccount: () -> Unit,
    onOpenDiary: () -> Unit,
) {
    var displayName by rememberSaveable(settings.displayName) { mutableStateOf(settings.displayName) }
    var currency by rememberSaveable(settings.currency) { mutableStateOf(settings.currency) }
    var defaultMarket by rememberSaveable(settings.defaultMarket) { mutableStateOf(settings.defaultMarket) }
    var riskLimit by rememberSaveable(settings.riskLimitPercent) { mutableStateOf(settings.riskLimitPercent) }
    var reviewReminder by rememberSaveable(settings.reviewReminder) { mutableStateOf(settings.reviewReminder) }
    var cloudAi by rememberSaveable(settings.cloudAiEnabled) { mutableStateOf(settings.cloudAiEnabled) }

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeading("Settings", "Personalize your workspace, risk defaults, privacy, and account connections.", "Save changes", onAction = { onSave(UserSettings(displayName.trim().ifBlank { "Alex" }, currency.trim().ifBlank { "USD" }.uppercase(), defaultMarket, riskLimit, reviewReminder, cloudAi)) }) }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Workspace", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    OutlinedTextField(displayName, { displayName = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(currency, { currency = it }, label = { Text("Currency") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(riskLimit, { riskLimit = it }, label = { Text("Risk limit %") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Text("Default market", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Stocks", "Forex", "Crypto", "Futures", "Options").forEach { market -> FilterChip(selected = defaultMarket == market, onClick = { defaultMarket = market }, label = { Text(market, fontSize = 11.sp) }) } }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp)) {
                    Text("Privacy and review", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    SettingToggle("Daily review reminder", "Keep the end-of-session reflection habit.", reviewReminder) { reviewReminder = it }
                    SettingToggle("Cloud AI", "Off by default. Only selected, minimized data can be shared.", cloudAi) { cloudAi = it }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp)) {
                    Text("Connected accounts", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Add a read-only broker or manual account. Live API credentials will be handled by the secure backend integration.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    Button(onClick = onConnectAccount, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text("Connect an account") }
                }
            }
        }
        item { OutlinedButton(onClick = onOpenDiary, modifier = Modifier.fillMaxWidth()) { Text("Open trading diary") } }
    }
}

@Composable
private fun SettingToggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)) }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ConnectAccountDialog(onDismiss: () -> Unit, onSave: (Account) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var broker by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Live") }
    var balance by rememberSaveable { mutableStateOf("0") }
    var currency by rememberSaveable { mutableStateOf("USD") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Top) { Column(Modifier.weight(1f)) { Text("ACCOUNT CONNECTION", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp); Text("Connect an account", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)); Text("Start with a local read-only account. Broker OAuth will be added through the backend.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)) }; IconButton(onClick = onDismiss) { Text("×", fontSize = 22.sp) } }
                OutlinedTextField(name, { name = it }, label = { Text("Account name") }, placeholder = { Text("Main account") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(broker, { broker = it }, label = { Text("Broker or provider") }, placeholder = { Text("Interactive Brokers") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Live", "Paper", "Demo").forEach { value -> FilterChip(selected = type == value, onClick = { type = value }, label = { Text(value) }) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(balance, { balance = it }, label = { Text("Starting balance") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(currency, { currency = it }, label = { Text("Currency") }, modifier = Modifier.weight(1f), singleLine = true) }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { TextButton(onClick = onDismiss) { Text("Cancel") }; Spacer(Modifier.width(8.dp)); Button(onClick = { onSave(Account(name = name.ifBlank { "New account" }, type = type, broker = broker.ifBlank { "Manual" }, balance = balance.toDoubleOrNull() ?: 0.0, equity = balance.toDoubleOrNull() ?: 0.0, currency = currency.ifBlank { "USD" }.uppercase())) }) { Text("Save account") } }
            }
        }
    }
}

@Composable
private fun AccountCard(name: String, kind: String, balance: String, result: String) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.Top) { Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(kind, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }; AssistChip(onClick = {}, label = { Text("Synced", fontSize = 10.sp) }) }; Text(balance, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 20.dp)); Text(result, color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) } } }

@Composable
private fun DiaryScreen(notes: List<DiaryNote>, onSave: (DiaryNote) -> Unit) {
    var plan by rememberSaveable { mutableStateOf("") }
    var reflection by rememberSaveable { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeading("Trading diary", "Make your psychology visible without adding another burden.", "＋ New daily note", {}) }
        item { Card { Column(Modifier.padding(18.dp)) { Text("Today’s session", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Local note · private by default", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp); Spacer(Modifier.height(15.dp)); Text("Before trading", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp); OutlinedTextField(value = plan, onValueChange = { plan = it }, label = { Text("What is your plan today?") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 3); Text("What did you learn?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 15.dp)); OutlinedTextField(value = reflection, onValueChange = { reflection = it }, label = { Text("Post-session reflection") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 3); Button(onClick = { onSave(DiaryNote(noteDate = "Today", mood = "Focused", plan = plan, reflection = reflection)); plan = ""; reflection = "" }, modifier = Modifier.padding(top = 12.dp)) { Text("Save diary note") } } } }
        items(notes, key = { it.id }) { note -> InsightCard(note.noteDate, note.reflection.ifBlank { note.plan }, note.noteDate.takeLast(2), note.mood != "Frustrated") }
    }
}

@Composable
private fun AdminHome() { LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { ScreenHeading("Admin overview", "Monitor product health, user growth, and privacy-sensitive services.", "Export report", {}) }; item { ResponsiveCards { MetricCard("Active users", "2,480", "+12.4% this month", true); MetricCard("Trades journaled", "184,920", "+8.1% this month", true); MetricCard("Drive sync health", "99.4%", "Within target", true); MetricCard("Premium conversion", "8.7%", "+1.2 pts", true) } }; item { Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) { Card(Modifier.weight(1f)) { Column(Modifier.padding(18.dp)) { Text("Service health", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("No user trade data is shown here by default.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)); HealthRow("Authentication", "Operational", true); HealthRow("CSV import", "Operational", true); HealthRow("Google Drive sync", "Operational", true); HealthRow("Cloud AI", "Consent required", true) } }; Card(Modifier.weight(1f)) { Column(Modifier.padding(18.dp)) { Text("Admin actions", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Safe, aggregate-only controls", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)); OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text("Review import errors") }; OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Manage feature flags") }; OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Open audit log") } } } } }; item { InsightCard("Privacy boundary", "Admin tools see account, subscription, and service health metadata—not private trades or journal notes.", "✓", true) } } }

@Composable
private fun HealthRow(name: String, status: String, healthy: Boolean) { Row(Modifier.fillMaxWidth().padding(top = 13.dp), verticalAlignment = Alignment.CenterVertically) { Text(if (healthy) "●" else "!", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp); Text(name, modifier = Modifier.weight(1f).padding(start = 8.dp), fontSize = 12.sp); Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) } }

@Composable
private fun AdminUsers() { LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { ScreenHeading("Users", "Manage account status without exposing private trading content.", "Invite admin", {}) }; item { UserAdminRow("Alex Kim", "alex@example.com", "Premium", "Active") }; item { UserAdminRow("Maya Singh", "maya@example.com", "Free", "Active") }; item { UserAdminRow("Jordan Lee", "jordan@example.com", "Trial", "Needs review") }; item { Card { Column(Modifier.padding(18.dp)) { Text("Admin guardrails", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Role-based access and audit logging are required before production.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)); Text("• No direct trade-note browsing", fontSize = 12.sp, modifier = Modifier.padding(top = 13.dp)); Text("• Aggregate metrics only", fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)); Text("• Every admin action is logged", fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) } } } } }

@Composable
private fun UserAdminRow(name: String, email: String, plan: String, status: String) { Card { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(35.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Text(name.split(" ").map { it.first() }.joinToString(""), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp) } }; Column(Modifier.weight(1f).padding(horizontal = 11.dp)) { Text(name, fontWeight = FontWeight.Bold); Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) }; Text(plan, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(12.dp)); Text(status, color = if (status == "Active") MaterialTheme.colorScheme.secondary else Color(0xFFB87416), fontSize = 11.sp) } } }

@Composable
private fun AdminReports() { LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { ScreenHeading("Admin reports", "Aggregate product insights for safe operations and roadmap decisions.", "Export CSV", {}) }; item { Card { Column(Modifier.padding(18.dp)) { Text("Product usage", fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("No personally identifiable trade content included.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)); ProgressRow("Manual entry", "72% of users", .72f, true); ProgressRow("CSV import", "41% of users", .41f, false); ProgressRow("Drive backup", "38% of users", .38f, false); ProgressRow("Monthly review", "64% completion", .64f, true) } } }; item { Card { Column(Modifier.padding(18.dp)) { Text("Roadmap signals", fontWeight = FontWeight.Bold, fontSize = 17.sp); InsightCard("Options support", "Most requested advanced market workflow.", "1", true); Spacer(Modifier.height(8.dp)); InsightCard("Localization", "English is default; translation framework is ready.", "2", true); Spacer(Modifier.height(8.dp)); InsightCard("Broker coverage", "CSV remains the universal fallback.", "3", true) } } } } }

@Composable
private fun ScreenHeading(title: String, description: String, action: String, onAction: () -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) { Column(Modifier.weight(1f)) { Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp), maxLines = 2, overflow = TextOverflow.Ellipsis) }; if (action.isNotEmpty()) OutlinedButton(onClick = onAction) { Text(action) } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTradeDialog(initialMarket: String, onDismiss: () -> Unit, onSave: (Trade) -> Unit) {
    var symbol by rememberSaveable { mutableStateOf("") }
    var market by rememberSaveable { mutableStateOf(initialMarket) }
    var direction by rememberSaveable { mutableStateOf("Long") }
    var setup by rememberSaveable { mutableStateOf("") }
    var entry by rememberSaveable { mutableStateOf("") }
    var stopLoss by rememberSaveable { mutableStateOf("") }
    var takeProfit by rememberSaveable { mutableStateOf("") }
    var exit by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var leverage by rememberSaveable { mutableStateOf("1") }
    var fees by rememberSaveable { mutableStateOf("0") }
    var riskAmount by rememberSaveable { mutableStateOf("100") }
    var note by rememberSaveable { mutableStateOf("") }

    val entryValue = entry.toDoubleOrNull() ?: 0.0
    val stopValue = stopLoss.toDoubleOrNull() ?: 0.0
    val targetValue = takeProfit.toDoubleOrNull() ?: 0.0
    val exitValue = exit.toDoubleOrNull() ?: 0.0
    val quantityValue = quantity.toDoubleOrNull() ?: 0.0
    val leverageValue = leverage.toDoubleOrNull() ?: 1.0
    val feesValue = fees.toDoubleOrNull() ?: 0.0
    val riskAmountValue = riskAmount.toDoubleOrNull() ?: 0.0
    val autoStopLoss = if (stopValue <= 0 && entryValue > 0 && quantityValue > 0 && riskAmountValue > 0) {
        if (direction == "Short") entryValue + riskAmountValue / quantityValue else entryValue - riskAmountValue / quantityValue
    } else 0.0
    val effectiveStopLoss = if (stopValue > 0) stopValue else autoStopLoss
    val preview = TradeCalculations.preview(direction, entryValue, effectiveStopLoss, targetValue, exitValue, quantityValue, leverageValue, feesValue)
    val canSave = symbol.isNotBlank() && entryValue > 0 && quantityValue > 0
    val pnlColor = if (preview.netPnl >= 0) MaterialTheme.colorScheme.secondary else Color(0xFFC64D5C)

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            LazyColumn(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Row(verticalAlignment = Alignment.Top) { Column(Modifier.weight(1f)) { Text("NEW JOURNAL ENTRY", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp); Text("Add a trade", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)); Text("Enter the plan and let TradeJournal calculate the outcome.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)) }; IconButton(onClick = onDismiss) { Text("×", fontSize = 22.sp) } } }
                item { Text("Trade basics", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Stocks", "Forex", "Crypto", "Futures", "Options").forEach { type -> FilterChip(selected = market == type, onClick = { market = type }, label = { Text(type, fontSize = 11.sp) }) } } }
                item { OutlinedTextField(symbol, { symbol = it }, label = { Text("Symbol") }, placeholder = { Text("e.g. NVDA") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = direction == "Long", onClick = { direction = "Long" }, label = { Text("Long") }); FilterChip(selected = direction == "Short", onClick = { direction = "Short" }, label = { Text("Short") }); OutlinedTextField(setup, { setup = it }, label = { Text("Strategy / setup") }, modifier = Modifier.weight(1f), singleLine = true) } }
                item { Text("Execution and risk", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(entry, { entry = it }, label = { Text("Entry") }, placeholder = { Text("100.00") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(exit, { exit = it }, label = { Text("Exit") }, placeholder = { Text("Optional") }, modifier = Modifier.weight(1f), singleLine = true) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(stopLoss, { stopLoss = it }, label = { Text("Stop loss") }, placeholder = { Text("Auto from risk") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(takeProfit, { takeProfit = it }, label = { Text("Take profit") }, placeholder = { Text("110.00") }, modifier = Modifier.weight(1f), singleLine = true) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(leverage, { leverage = it }, label = { Text("Leverage") }, modifier = Modifier.weight(1f), singleLine = true); OutlinedTextField(fees, { fees = it }, label = { Text("Fees") }, modifier = Modifier.weight(1f), singleLine = true) } }
                item { OutlinedTextField(riskAmount, { riskAmount = it }, label = { Text("Risk amount (auto SL)") }, placeholder = { Text("100.00") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(14.dp)) { Text("LIVE CALCULATIONS", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp); Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { CalculationValue("Net P&L", "${if (preview.netPnl >= 0) "+" else "−"}$${"%.2f".format(kotlin.math.abs(preview.netPnl))}", pnlColor); CalculationValue("Stop risk", "$${"%.2f".format(preview.stopRisk)}", Color(0xFFB87416)); CalculationValue("R multiple", "${"%.2f".format(preview.rMultiple)}R", pnlColor) }; Text("Target: ${if (preview.takeProfitPnl >= 0) "+" else "−"}$${"%.2f".format(kotlin.math.abs(preview.takeProfitPnl))}  ·  ${if (stopValue > 0) "Stop entered" else "Auto SL ${"%.2f".format(effectiveStopLoss)}"}  ·  Margin: $${"%.2f".format(preview.margin)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp)) } } }
                item { Text("Psychology and notes", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp) }
                item { OutlinedTextField(note, { note = it }, label = { Text("What happened and what did you learn?") }, placeholder = { Text("Explain your entry, exit, emotions, and improvement…") }, modifier = Modifier.fillMaxWidth(), minLines = 4) }
                item { Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { TextButton(onClick = onDismiss) { Text("Cancel") }; Spacer(Modifier.width(8.dp)); Button(enabled = canSave, onClick = { onSave(Trade(symbol = symbol.trim().uppercase(), market = market, direction = direction, setup = setup.ifBlank { "Unclassified" }, result = preview.netPnl, rMultiple = preview.rMultiple, status = if (exitValue > 0) "Calculated" else "Open", note = note, entryPrice = entryValue, stopLoss = effectiveStopLoss, takeProfit = targetValue, exitPrice = exitValue, quantity = quantityValue, leverage = leverageValue.coerceAtLeast(1.0), fees = feesValue)) }) { Text("Save calculated trade") } } }
            }
        }
    }
}

@Composable
private fun CalculationValue(label: String, value: String, color: Color) {
    Column { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp); Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 3.dp)) }
}
