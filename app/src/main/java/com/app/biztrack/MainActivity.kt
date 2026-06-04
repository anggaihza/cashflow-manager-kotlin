package com.app.biztrack

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.biztrack.data.local.database.BizTrackDatabase
import com.app.biztrack.data.local.entity.CashAccountEntity
import com.app.biztrack.data.local.entity.TransactionEntity
import com.app.biztrack.data.preferences.AppPreferencesDataSource
import com.app.biztrack.data.repository.FinanceRepository
import com.app.biztrack.domain.model.CategoryTypes
import com.app.biztrack.domain.model.ContactTypes
import com.app.biztrack.domain.model.DebtStatus
import com.app.biztrack.domain.model.DebtTypes
import com.app.biztrack.domain.model.InvoiceStatus
import com.app.biztrack.domain.model.TransactionTypes
import com.app.biztrack.presentation.BizTrackUiState
import com.app.biztrack.presentation.BizTrackViewModel
import com.app.biztrack.presentation.BizTrackViewModelFactory
import com.app.biztrack.presentation.CashflowChartPoint
import com.app.biztrack.presentation.EnrichedTransaction
import com.app.biztrack.presentation.EnrichedInvoice
import com.app.biztrack.presentation.GlobalSearchResult
import com.app.biztrack.presentation.components.CategoryTotalBars
import com.app.biztrack.presentation.components.EmptyState
import com.app.biztrack.presentation.components.FinanceGlyph
import com.app.biztrack.presentation.components.GlyphBadge
import com.app.biztrack.presentation.components.HeroBand
import com.app.biztrack.presentation.components.KpiCard
import com.app.biztrack.presentation.components.MiniIncomeExpenseChart
import com.app.biztrack.presentation.components.PremiumEmerald
import com.app.biztrack.presentation.components.PremiumEmeraldSoft
import com.app.biztrack.presentation.components.PremiumExpense
import com.app.biztrack.presentation.components.PremiumGold
import com.app.biztrack.presentation.components.PremiumGoldDark
import com.app.biztrack.presentation.components.PremiumIncome
import com.app.biztrack.presentation.components.PremiumIvory
import com.app.biztrack.presentation.components.PremiumPanel
import com.app.biztrack.presentation.components.SectionHeader
import com.app.biztrack.presentation.components.TransactionRow
import com.app.biztrack.presentation.components.TypeFilterChip
import com.app.biztrack.presentation.components.toColorOrNull
import com.app.biztrack.ui.theme.BizTrackTheme
import com.app.biztrack.utils.currentMonth
import com.app.biztrack.utils.formatDate
import com.app.biztrack.utils.formatMoney
import com.app.biztrack.utils.monthLabel
import com.app.biztrack.utils.parseDateMillis
import com.app.biztrack.utils.parseMoney
import com.app.biztrack.utils.todayMillis
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = BizTrackDatabase.getInstance(applicationContext)
        val preferences = AppPreferencesDataSource(applicationContext)
        val repository = FinanceRepository(database, preferences)
        val factory = BizTrackViewModelFactory(repository)

        setContent {
            val viewModel: BizTrackViewModel = viewModel(factory = factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            BizTrackTheme(darkTheme = state.preferences.themeMode == "dark") {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    BizTrackRoot(state = state, viewModel = viewModel)
                }
            }
        }
    }
}

private sealed class AppScreen {
    data object Dashboard : AppScreen()
    data object Transactions : AppScreen()
    data object Reports : AppScreen()
    data object Settings : AppScreen()
    data object Categories : AppScreen()
    data object CashAccounts : AppScreen()
    data object DebtReceivables : AppScreen()
    data object TransferCash : AppScreen()
    data object MonthlyTargets : AppScreen()
    data object Reminders : AppScreen()
    data object RecurringTemplates : AppScreen()
    data object Inventory : AppScreen()
    data object BusinessContacts : AppScreen()
    data object Invoices : AppScreen()
    data object BackupRestore : AppScreen()
    data class AddTransaction(val initialType: String) : AppScreen()
    data class EditTransaction(val id: Long) : AppScreen()
}

@Composable
private fun BizTrackRoot(state: BizTrackUiState, viewModel: BizTrackViewModel) {
    var pinUnlocked by remember(state.preferences.pinEnabled, state.preferences.pinHash) { mutableStateOf(!state.preferences.pinEnabled) }

    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } },
            title = { Text("Terjadi kendala") },
            text = { Text(state.errorMessage) },
        )
    }

    if (!state.preferences.onboardingCompleted) {
        OnboardingScreen(viewModel)
    } else if (state.preferences.pinEnabled && !pinUnlocked) {
        PinLockScreen(
            onUnlock = { pin ->
                val success = viewModel.verifyPin(pin, state.preferences.pinHash)
                if (success) {
                    pinUnlocked = true
                }
                success
            },
        )
    } else {
        AppShell(state, viewModel)
    }
}

@Composable
private fun PinLockScreen(onUnlock: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        PremiumPanel {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GlyphBadge(FinanceGlyph.Settings, PremiumEmerald, size = 58.dp)
                Text("Masukkan PIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(8)
                        error = null
                    },
                    label = { Text("PIN") },
                    colors = premiumTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(
                        error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                PremiumPrimaryButton(
                    text = "Buka aplikasi",
                    enabled = pin.length >= 4,
                    onClick = {
                        if (!onUnlock(pin)) {
                            error = "PIN tidak sesuai."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OnboardingScreen(viewModel: BizTrackViewModel) {
    var businessName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("IDR") }
    var accountName by remember { mutableStateOf("Cash") }
    var initialBalance by remember { mutableStateOf("") }
    var useDarkMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            HeroBand(
                title = "BizTrack",
                subtitle = "Pencatatan bisnis lokal untuk pemasukan, pengeluaran, saldo, dan laporan.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FeaturePill("Offline-first", FinanceGlyph.Wallet, Modifier.weight(1f))
                        FeaturePill("Tanpa login", FinanceGlyph.Settings, Modifier.weight(1f))
                    }
                    Text(
                        "Data tetap berada di perangkat Anda.",
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
        item {
            PremiumPanel {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        GlyphBadge(FinanceGlyph.Store, PremiumEmerald, size = 54.dp)
                        Column {
                            Text("Setup bisnis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Profil awal dan akun kas utama",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Nama bisnis") },
                        placeholder = { Text("Masukkan nama bisnis Anda") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Store) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase().take(3) },
                        label = { Text("Mata uang") },
                        leadingIcon = { CurrencyPrefix(currency.ifBlank { "Rp" }) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("Akun kas pertama") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Wallet) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = initialBalance,
                        onValueChange = { initialBalance = it },
                        label = { Text("Saldo awal") },
                        leadingIcon = { CurrencyPrefix("Rp") },
                        colors = premiumTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            GlyphBadge(FinanceGlyph.Settings, PremiumGoldDark, size = 46.dp)
                            Column {
                                Text("Dark mode", fontWeight = FontWeight.Bold)
                                Text(
                                    "Bisa diubah lagi dari pengaturan",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        Switch(
                            checked = useDarkMode,
                            onCheckedChange = { useDarkMode = it },
                            colors = premiumSwitchColors(),
                        )
                    }
                }
            }
        }
        item {
            PremiumPrimaryButton(
                text = "Mulai gunakan BizTrack",
                onClick = {
                    viewModel.completeOnboarding(
                        businessName = businessName.ifBlank { "Bisnis Saya" },
                        currency = currency.ifBlank { "IDR" },
                        accountName = accountName.ifBlank { "Cash" },
                        initialBalance = parseMoney(initialBalance) ?: 0,
                        useDarkMode = useDarkMode,
                    )
                },
                leadingGlyph = FinanceGlyph.Plus,
                trailingGlyph = FinanceGlyph.Chevron,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FeaturePill(label: String, glyph: FinanceGlyph, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.dp, PremiumGold.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlyphBadge(glyph, PremiumGold, size = 28.dp, dark = true)
            Text(
                label,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FieldGlyph(glyph: FinanceGlyph) {
    GlyphBadge(glyph, PremiumEmerald, size = 32.dp)
}

@Composable
private fun CurrencyPrefix(label: String) {
    val display = if (label.equals("IDR", ignoreCase = true) || label.equals("Rp", ignoreCase = true)) {
        "Rp"
    } else {
        label.take(3).uppercase()
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(PremiumEmeraldSoft, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(display, color = PremiumEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

private fun formatMoneyInput(raw: String): String {
    val amount = parseMoney(raw) ?: return ""
    return NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).apply {
        maximumFractionDigits = 0
    }.format(amount)
}

@Composable
private fun MoneyInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    currency: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(formatMoneyInput(it)) },
        label = { Text(label) },
        leadingIcon = { CurrencyPrefix(currency) },
        colors = premiumTextFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Tanggal",
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = parseDateMillis(value) ?: todayMillis())
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { FieldGlyph(FinanceGlyph.Report) },
        trailingIcon = {
            TextButton(onClick = { showPicker = true }) { Text("Pilih") }
        },
        colors = premiumTextFieldColors(),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onValueChange(formatDate(it)) }
                        showPicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Batal") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun premiumTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PremiumGoldDark,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = PremiumEmerald,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = PremiumEmerald,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
)

@Composable
private fun premiumSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = PremiumEmerald,
    checkedBorderColor = PremiumGold,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
)

@Composable
private fun PremiumPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingGlyph: FinanceGlyph? = null,
    trailingGlyph: FinanceGlyph? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PremiumEmerald,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = BorderStroke(1.dp, if (enabled) PremiumGold.copy(alpha = 0.82f) else MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 13.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leadingGlyph?.let {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFFE7A8), PremiumGoldDark)),
                            RoundedCornerShape(8.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = Color(0xFF15302B), fontSize = 22.sp, fontWeight = FontWeight.Normal)
                }
            }
            Text(
                text,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailingGlyph?.let {
                GlyphBadge(it, PremiumGold, size = 28.dp, dark = true)
            }
        }
    }
}

@Composable
private fun PremiumOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.92f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = PremiumEmerald,
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HeroActionPill(label: String, glyph: FinanceGlyph, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(8.dp))
            .border(1.dp, PremiumGold.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlyphBadge(glyph, PremiumGold, size = 26.dp, dark = true)
            Text(
                label,
                color = PremiumGold,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.76f), fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Medium)
        Text(
            value,
            color = Color.White,
            fontSize = if (compact) 20.sp else 30.sp,
            lineHeight = if (compact) 24.sp else 34.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(state: BizTrackUiState, viewModel: BizTrackViewModel) {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }
    var selectedTransactionId by remember { mutableStateOf<Long?>(null) }
    var returnAfterEdit by remember { mutableStateOf<AppScreen>(AppScreen.Transactions) }
    val transactionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedTransaction = selectedTransactionId?.let { id ->
        state.enrichedTransactions.firstOrNull { it.transaction.id == id }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingAddButton { screen = AppScreen.AddTransaction(TransactionTypes.INCOME) }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BottomItem("Home", FinanceGlyph.Home, screen is AppScreen.Dashboard) {
                        screen = AppScreen.Dashboard
                    }
                    BottomItem("Transaksi", FinanceGlyph.Wallet, screen is AppScreen.Transactions) {
                        screen = AppScreen.Transactions
                    }
                    BottomItem("Laporan", FinanceGlyph.Report, screen is AppScreen.Reports) {
                        screen = AppScreen.Reports
                    }
                    BottomItem("Akun", FinanceGlyph.Settings, screen is AppScreen.Settings) {
                        screen = AppScreen.Settings
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            when (val current = screen) {
                AppScreen.Dashboard -> DashboardScreen(
                    state = state,
                    onAddIncome = { screen = AppScreen.AddTransaction(TransactionTypes.INCOME) },
                    onAddExpense = { screen = AppScreen.AddTransaction(TransactionTypes.EXPENSE) },
                    onOpenTransaction = { selectedTransactionId = it },
                    onOpenTransactions = { screen = AppScreen.Transactions },
                    onOpenReports = { screen = AppScreen.Reports },
                    onSeedDemo = { viewModel.seedDemoData() },
                    onGlobalSearch = viewModel::updateGlobalSearch,
                )
                AppScreen.Transactions -> TransactionListScreen(
                    state = state,
                    viewModel = viewModel,
                    onOpenTransaction = { selectedTransactionId = it },
                    onAdd = { screen = AppScreen.AddTransaction(TransactionTypes.INCOME) },
                )
                AppScreen.Reports -> ReportScreen(state, viewModel) { selectedTransactionId = it }
                AppScreen.Settings -> SettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    onCategories = { screen = AppScreen.Categories },
                    onCashAccounts = { screen = AppScreen.CashAccounts },
                    onDebtReceivables = { screen = AppScreen.DebtReceivables },
                    onTransferCash = { screen = AppScreen.TransferCash },
                    onTargets = { screen = AppScreen.MonthlyTargets },
                    onReminders = { screen = AppScreen.Reminders },
                    onRecurring = { screen = AppScreen.RecurringTemplates },
                    onInventory = { screen = AppScreen.Inventory },
                    onContacts = { screen = AppScreen.BusinessContacts },
                    onInvoices = { screen = AppScreen.Invoices },
                    onBackup = { screen = AppScreen.BackupRestore },
                )
                AppScreen.Categories -> CategoriesScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                )
                AppScreen.CashAccounts -> CashAccountsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                )
                AppScreen.DebtReceivables -> DebtReceivablesScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                )
                AppScreen.TransferCash -> TransferCashScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                    onSaved = { screen = AppScreen.Dashboard },
                )
                AppScreen.MonthlyTargets -> MonthlyTargetsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                )
                AppScreen.Reminders -> RemindersScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                )
                AppScreen.RecurringTemplates -> RecurringTemplatesScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                )
                AppScreen.Inventory -> InventoryScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                )
                AppScreen.BusinessContacts -> BusinessContactsScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                )
                AppScreen.Invoices -> InvoicesScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                )
                AppScreen.BackupRestore -> BackupRestoreScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { screen = AppScreen.Settings },
                )
                is AppScreen.AddTransaction -> TransactionFormScreen(
                    state = state,
                    initialType = current.initialType,
                    existing = null,
                    onBack = { screen = AppScreen.Dashboard },
                    onSaved = { screen = AppScreen.Dashboard },
                    viewModel = viewModel,
                )
                is AppScreen.EditTransaction -> {
                    val existing = state.transactions.firstOrNull { it.id == current.id }
                    TransactionFormScreen(
                        state = state,
                        initialType = existing?.type ?: TransactionTypes.INCOME,
                        existing = existing,
                        onBack = { screen = returnAfterEdit },
                        onSaved = { screen = returnAfterEdit },
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    if (selectedTransactionId != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedTransactionId = null },
            modifier = Modifier.fillMaxHeight(),
            sheetState = transactionSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        ) {
            TransactionDetailSheet(
                item = selectedTransaction,
                state = state,
                onClose = { selectedTransactionId = null },
                onEdit = {
                    selectedTransactionId?.let { id ->
                        returnAfterEdit = screen
                        selectedTransactionId = null
                        screen = AppScreen.EditTransaction(id)
                    }
                },
                onDeleted = {
                    selectedTransactionId = null
                },
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun FloatingAddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(172.dp)
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(
                    listOf(PremiumEmerald, Color(0xFF003A34)),
                ),
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, PremiumGold.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFFFFE7A8), PremiumGoldDark)),
                        RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                GlyphBadge(FinanceGlyph.Plus, Color(0xFF15302B), size = 26.dp)
            }
            Text("Tambah", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RowScope.BottomItem(label: String, glyph: FinanceGlyph, selected: Boolean, onClick: () -> Unit) {
    val contentColor = if (selected) PremiumEmerald else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .height(66.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (selected) PremiumEmeraldSoft else Color.Transparent, RoundedCornerShape(8.dp))
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        GlyphBadge(
            glyph = glyph,
            color = if (selected) PremiumGold else contentColor,
            size = 32.dp,
            dark = selected,
        )
        Text(
            label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    description: String,
    glyph: FinanceGlyph,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumPanel(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(color, color.copy(alpha = 0.78f))),
                        RoundedCornerShape(8.dp),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                GlyphBadge(glyph, Color.White, size = 34.dp, dark = true)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GlyphBadge(FinanceGlyph.Chevron, MaterialTheme.colorScheme.onSurfaceVariant, size = 28.dp)
        }
    }
}

@Composable
private fun DashboardScreen(
    state: BizTrackUiState,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onOpenTransactions: () -> Unit,
    onOpenReports: () -> Unit,
    onSeedDemo: () -> Unit,
    onGlobalSearch: (String) -> Unit,
) {
    val currency = state.preferences.currency
    val businessName = state.preferences.businessName.ifBlank { "Demo Store" }
    val primaryCashAccount = state.cashAccounts.firstOrNull()?.name ?: "Cash"
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        businessName,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                HeroActionPill("Laporan", FinanceGlyph.Report, onOpenReports)
            }
        }
        item {
            HeroBand(
                title = "Saldo kas",
                subtitle = "Ringkasan ${monthLabel(currentMonth())}",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    HeroMetric(
                        label = "Total saldo",
                        value = formatMoney(state.totalCashBalance, currency),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                        ) {
                            Text(
                                "$primaryCashAccount · Profit ${formatMoney(state.monthlyProfit, currency)}",
                                color = Color.White.copy(alpha = 0.86f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
        item {
            GlobalSearchPanel(
                query = state.globalSearchQuery,
                results = state.globalSearchResults,
                currency = currency,
                onQueryChange = onGlobalSearch,
            )
        }
        item {
            ExecutiveDashboardPanel(state = state)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeActionButton(
                    label = "Pemasukan",
                    hint = "",
                    glyph = FinanceGlyph.Income,
                    color = PremiumEmerald,
                    onClick = onAddIncome,
                    modifier = Modifier.weight(1f),
                )
                HomeActionButton(
                    label = "Pengeluaran",
                    hint = "",
                    glyph = FinanceGlyph.Expense,
                    color = PremiumEmerald,
                    onClick = onAddExpense,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            SectionHeader(
                title = "Transaksi terbaru",
                trailing = { TextButton(onClick = onOpenTransactions) { Text("Lihat") } },
            )
        }
        if (state.recentTransactions.isEmpty()) {
            item {
                EmptyState(
                    title = "Belum ada transaksi.",
                    message = "Mulai catat transaksi pertama bisnis Anda.",
                )
                Spacer(Modifier.height(10.dp))
                PremiumPrimaryButton("Isi data contoh", onClick = onSeedDemo, modifier = Modifier.fillMaxWidth())
            }
        } else {
            items(state.recentTransactions, key = { it.transaction.id }) { item ->
                TransactionRow(item, currency, onClick = { onOpenTransaction(item.transaction.id) })
            }
        }
    }
}

@Composable
private fun GlobalSearchPanel(
    query: String,
    results: List<GlobalSearchResult>,
    currency: String,
    onQueryChange: (String) -> Unit,
) {
    PremiumPanel {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Cari transaksi, invoice, kontak") },
                leadingIcon = { FieldGlyph(FinanceGlyph.Report) },
                colors = premiumTextFieldColors(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (query.trim().length >= 2) {
                if (results.isEmpty()) {
                    Text(
                        "Tidak ada hasil.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        results.forEach { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(Modifier.size(7.dp).background(PremiumGoldDark, RoundedCornerShape(8.dp)))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(result.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${result.type} · ${result.subtitle}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                result.amount?.let {
                                    Text(formatMoney(it, currency), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
private fun ExecutiveDashboardPanel(state: BizTrackUiState) {
    val currency = state.preferences.currency
    PremiumPanel {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            PremiumEmeraldSoft.copy(alpha = 0.35f),
                            PremiumGold.copy(alpha = 0.08f),
                        ),
                    ),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Executive view", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Arus kas 6 bulan", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .background(PremiumEmerald.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        if (state.monthlyProfit >= 0) "Profit" else "Rugi",
                        color = PremiumEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExecutiveMiniMetric("Profit", formatMoney(state.monthlyProfit, currency), Modifier.weight(1f))
                ExecutiveMiniMetric("Invoice", state.unpaidInvoiceCount.toString(), Modifier.weight(1f))
                ExecutiveMiniMetric("Stok", state.lowStockCount.toString(), Modifier.weight(1f))
            }
            CashflowChart(points = state.cashflowChart)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowStat(
                    label = "Masuk",
                    value = formatMoney(state.monthlyIncome, currency),
                    glyph = FinanceGlyph.Income,
                    color = PremiumIncome,
                    modifier = Modifier.weight(1f),
                )
                FlowStat(
                    label = "Keluar",
                    value = formatMoney(state.monthlyExpense, currency),
                    glyph = FinanceGlyph.Expense,
                    color = PremiumExpense,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ExecutiveMiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
        Text(value, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
    }
}

@Composable
private fun CashflowChart(points: List<CashflowChartPoint>) {
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val incomeColor = PremiumIncome
    val expenseColor = PremiumExpense.copy(alpha = 0.84f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.68f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            val maxValue = points.flatMap { listOf(it.income, it.expense) }.maxOrNull()?.takeIf { it > 0 } ?: 1L
            val slotWidth = size.width / points.size.coerceAtLeast(1)
            val baseline = size.height - 18f
            drawLine(axisColor, Offset(0f, baseline), Offset(size.width, baseline), strokeWidth = 1.4f)
            points.forEachIndexed { index, point ->
                val center = slotWidth * index + slotWidth / 2f
                val barWidth = (slotWidth * 0.22f).coerceIn(8f, 18f)
                val incomeHeight = (baseline - 14f) * (point.income.toFloat() / maxValue)
                val expenseHeight = (baseline - 14f) * (point.expense.toFloat() / maxValue)
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(center - barWidth - 2f, baseline - incomeHeight),
                    size = Size(barWidth, incomeHeight.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(10f, 10f),
                )
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(center + 2f, baseline - expenseHeight),
                    size = Size(barWidth, expenseHeight.coerceAtLeast(3f)),
                    cornerRadius = CornerRadius(10f, 10f),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { point ->
                Text(
                    monthLabel(point.month).take(3),
                    color = labelColor,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MonthlyFlowPanel(
    income: Long,
    expense: Long,
    currency: String,
) {
    PremiumPanel {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            PremiumEmeraldSoft.copy(alpha = 0.42f),
                            PremiumGold.copy(alpha = 0.08f),
                        ),
                    ),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Bulan ini", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(currency.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowStat(
                    label = "Masuk",
                    value = formatMoney(income, currency),
                    glyph = FinanceGlyph.Income,
                    color = PremiumIncome,
                    modifier = Modifier.weight(1f),
                )
                FlowStat(
                    label = "Keluar",
                    value = formatMoney(expense, currency),
                    glyph = FinanceGlyph.Expense,
                    color = PremiumExpense,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SmartInsightPanel(state: BizTrackUiState) {
    val currency = state.preferences.currency
    val incomeTarget = state.preferences.monthlyIncomeTarget
    val expenseLimit = state.preferences.monthlyExpenseLimit
    val insightText = when {
        state.overdueInvoiceCount > 0 ->
            "${state.overdueInvoiceCount} invoice melewati jatuh tempo."
        state.unpaidInvoiceCount > 0 ->
            "${state.unpaidInvoiceCount} invoice masih menunggu pembayaran."
        state.lowStockCount > 0 ->
            "${state.lowStockCount} produk stoknya menipis."
        incomeTarget > 0 && state.monthlyIncome < incomeTarget ->
            "Target pemasukan kurang ${formatMoney(incomeTarget - state.monthlyIncome, currency)}."
        expenseLimit > 0 && state.monthlyExpense > expenseLimit ->
            "Pengeluaran melewati limit ${formatMoney(state.monthlyExpense - expenseLimit, currency)}."
        state.receivableTotal > 0 ->
            "Ada piutang ${formatMoney(state.receivableTotal, currency)} yang perlu ditagih."
        state.upcomingReminderCount > 0 ->
            "${state.upcomingReminderCount} reminder pembayaran aktif."
        else -> "Arus kas bulan ini masih dalam kendali."
    }
    PremiumPanel {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlyphBadge(FinanceGlyph.Report, PremiumEmerald, size = 34.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Insight", fontWeight = FontWeight.Bold)
                Text(insightText, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FlowStat(
    label: String,
    value: String,
    glyph: FinanceGlyph,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(8.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    label: String,
    hint: String,
    glyph: FinanceGlyph,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.surface, color.copy(alpha = 0.055f)),
                ),
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, PremiumGold.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(color.copy(alpha = 0.16f), PremiumIvory.copy(alpha = 0.62f))),
                        RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                GlyphBadge(glyph, color, size = 30.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hint.isNotBlank()) {
                    Text(
                        hint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeInsightPanel(
    title: String,
    subtitle: String,
    glyph: FinanceGlyph,
    accent: Color,
    content: @Composable () -> Unit,
) {
    PremiumPanel {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(42.dp)
                        .background(accent, RoundedCornerShape(8.dp)),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                GlyphBadge(glyph, accent, size = 38.dp)
            }
            content()
        }
    }
}

@Composable
private fun TransactionListScreen(
    state: BizTrackUiState,
    viewModel: BizTrackViewModel,
    onOpenTransaction: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    val currency = state.preferences.currency
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader("Transaksi")
        }
        item {
            OutlinedTextField(
                value = state.filters.search,
                onValueChange = viewModel::updateSearch,
                label = { Text("Cari catatan, kategori, akun") },
                leadingIcon = { FieldGlyph(FinanceGlyph.Report) },
                colors = premiumTextFieldColors(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeFilterChip("Semua", state.filters.type == "all") { viewModel.updateTypeFilter("all") }
                TypeFilterChip("Pemasukan", state.filters.type == TransactionTypes.INCOME) {
                    viewModel.updateTypeFilter(TransactionTypes.INCOME)
                }
                TypeFilterChip("Pengeluaran", state.filters.type == TransactionTypes.EXPENSE) {
                    viewModel.updateTypeFilter(TransactionTypes.EXPENSE)
                }
            }
        }
        if (state.filteredTransactions.isEmpty()) {
            item {
                EmptyState("Tidak ada transaksi pada periode ini.", "Gunakan tombol tambah untuk membuat transaksi baru.")
                Spacer(Modifier.height(8.dp))
                PremiumPrimaryButton("Tambah transaksi", onClick = onAdd, modifier = Modifier.fillMaxWidth())
            }
        } else {
            items(state.filteredTransactions, key = { it.transaction.id }) { item ->
                TransactionRow(item, currency, onClick = { onOpenTransaction(item.transaction.id) })
            }
        }
    }
}

@Composable
private fun TransactionFormScreen(
    state: BizTrackUiState,
    initialType: String,
    existing: TransactionEntity?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: BizTrackViewModel,
) {
    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: initialType) }
    var amount by remember(existing?.id) { mutableStateOf(existing?.amount?.toString() ?: "") }
    var date by remember(existing?.id) { mutableStateOf(formatDate(existing?.date ?: todayMillis())) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var selectedCategoryId by remember(existing?.id) { mutableStateOf(existing?.categoryId) }
    var selectedAccountId by remember(existing?.id) { mutableStateOf(existing?.cashAccountId.takeIf { it != 0L }) }
    var selectedMethodId by remember(existing?.id) { mutableStateOf(existing?.paymentMethodId) }

    val categoryOptions = state.categories.filter { it.type == type }
    LaunchedEffect(type, categoryOptions.size) {
        if (selectedCategoryId == null || categoryOptions.none { it.id == selectedCategoryId }) {
            selectedCategoryId = categoryOptions.firstOrNull()?.id
        }
    }
    LaunchedEffect(state.cashAccounts.size) {
        if (selectedAccountId == null || state.cashAccounts.none { it.id == selectedAccountId }) {
            selectedAccountId = state.cashAccounts.firstOrNull()?.id
        }
    }
    LaunchedEffect(state.paymentMethods.size) {
        if (selectedMethodId == null || state.paymentMethods.none { it.id == selectedMethodId }) {
            selectedMethodId = state.paymentMethods.firstOrNull()?.id
        }
    }

    val parsedAmount = parseMoney(amount)
    val parsedDate = parseDateMillis(date)
    val isValid = parsedAmount != null && parsedAmount > 0 &&
        parsedDate != null && selectedCategoryId != null && selectedAccountId != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeaderWithBack(
                title = if (existing == null) "Tambah transaksi" else "Edit transaksi",
                subtitle = "Nominal dan kategori dibuat mudah dijangkau",
                onBack = onBack,
            )
        }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeFilterChip("Pemasukan", type == TransactionTypes.INCOME) {
                            type = TransactionTypes.INCOME
                        }
                        TypeFilterChip("Pengeluaran", type == TransactionTypes.EXPENSE) {
                            type = TransactionTypes.EXPENSE
                        }
                    }
                    MoneyInputField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Nominal",
                        currency = state.preferences.currency,
                    )
                    DatePickerField(
                        value = date,
                        onValueChange = { date = it },
                        label = "Tanggal",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { date = formatDate(todayMillis()) }, label = { Text("Hari ini") })
                        AssistChip(
                            onClick = { date = formatDate(todayMillis() - 86_400_000L) },
                            label = { Text("Kemarin") },
                        )
                    }
                    ChoiceSection(
                        title = "Kategori",
                        options = categoryOptions,
                        selectedId = selectedCategoryId,
                        label = { it.name },
                        color = { it.color.toColorOrNull() ?: MaterialTheme.colorScheme.primary },
                        onSelected = { selectedCategoryId = it },
                    )
                    ChoiceSection(
                        title = "Akun kas",
                        options = state.cashAccounts,
                        selectedId = selectedAccountId,
                        label = { "${it.name} (${formatMoney(it.currentBalance, state.preferences.currency)})" },
                        color = { MaterialTheme.colorScheme.secondary },
                        onSelected = { selectedAccountId = it },
                    )
                    ChoiceSection(
                        title = "Metode pembayaran",
                        options = state.paymentMethods,
                        selectedId = selectedMethodId,
                        label = { it.name },
                        color = { MaterialTheme.colorScheme.tertiary },
                        onSelected = { selectedMethodId = it },
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Ledger) },
                        colors = premiumTextFieldColors(),
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumPrimaryButton(
                        text = if (existing == null) "Simpan transaksi" else "Simpan perubahan",
                        enabled = isValid,
                        onClick = {
                            val cashAccountId = selectedAccountId ?: return@PremiumPrimaryButton
                            if (existing == null) {
                                viewModel.addTransaction(
                                    type = type,
                                    amount = parsedAmount ?: return@PremiumPrimaryButton,
                                    date = parsedDate ?: return@PremiumPrimaryButton,
                                    categoryId = selectedCategoryId,
                                    cashAccountId = cashAccountId,
                                    paymentMethodId = selectedMethodId,
                                    note = note,
                                    onSaved = onSaved,
                                )
                            } else {
                                viewModel.updateTransaction(
                                    existing = existing,
                                    type = type,
                                    amount = parsedAmount ?: return@PremiumPrimaryButton,
                                    date = parsedDate ?: return@PremiumPrimaryButton,
                                    categoryId = selectedCategoryId,
                                    cashAccountId = cashAccountId,
                                    paymentMethodId = selectedMethodId,
                                    note = note,
                                    onSaved = onSaved,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> ChoiceSection(
    title: String,
    options: List<T>,
    selectedId: Long?,
    label: @Composable (T) -> String,
    color: @Composable (T) -> Color,
    onSelected: (Long) -> Unit,
) where T : Any {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        if (options.isEmpty()) {
            Text("Belum ada data aktif.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { item ->
                    val id = when (item) {
                        is com.app.biztrack.data.local.entity.CategoryEntity -> item.id
                        is CashAccountEntity -> item.id
                        is com.app.biztrack.data.local.entity.PaymentMethodEntity -> item.id
                        else -> 0L
                    }
                    FilterChip(
                        selected = selectedId == id,
                        onClick = { onSelected(id) },
                        label = { Text(label(item), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color(item), RoundedCornerShape(10.dp)),
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDetailSheet(
    item: EnrichedTransaction?,
    state: BizTrackUiState,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: BizTrackViewModel,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog && item != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTransaction(item.transaction.id, onDeleted)
                    },
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } },
            title = { Text("Hapus transaksi?") },
            text = { Text("Saldo akun kas akan dikembalikan sesuai transaksi ini.") },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Detail transaksi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Ringkasan dan metadata", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup")
                }
            }
        }
        if (item == null) {
            item { EmptyState("Data tidak ditemukan.", "Transaksi mungkin sudah dihapus.") }
        } else {
            item {
                val isIncome = item.transaction.type == TransactionTypes.INCOME
                val isTransfer = item.transaction.type == TransactionTypes.TRANSFER
                val title = when {
                    isIncome -> "Pemasukan"
                    isTransfer -> "Transfer antar akun"
                    else -> "Pengeluaran"
                }
                val subtitle = when {
                    isTransfer -> listOfNotNull(
                        item.cashAccount?.name,
                        item.targetCashAccount?.name?.let { "ke $it" },
                    ).joinToString(" ")
                    else -> item.category?.name ?: "Tanpa kategori"
                }
                val accent = when {
                    isIncome -> PremiumIncome
                    isTransfer -> PremiumGoldDark
                    else -> PremiumExpense
                }
                PremiumPanel {
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GlyphBadge(
                            glyph = when {
                                isIncome -> FinanceGlyph.Income
                                isTransfer -> FinanceGlyph.Wallet
                                else -> FinanceGlyph.Expense
                            },
                            color = accent,
                            size = 42.dp,
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                subtitle.ifBlank { "Detail akun kas" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(formatDate(item.transaction.date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                        Text(
                            "${if (isIncome) "+" else if (isTransfer) "" else "-"}${formatMoney(item.transaction.amount, state.preferences.currency)}",
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            item {
                PremiumPanel {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailLine("Akun kas", item.cashAccount?.name ?: "-")
                        if (item.transaction.type == TransactionTypes.TRANSFER) {
                            DetailLine("Akun tujuan", item.targetCashAccount?.name ?: "-")
                        } else {
                            DetailLine("Kategori", item.category?.name ?: "-")
                            DetailLine("Metode pembayaran", item.paymentMethod?.name ?: "-")
                        }
                        DetailLine("Catatan", item.transaction.note.ifBlank { "-" })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        DetailLine("Dibuat", formatDate(item.transaction.createdAt))
                        DetailLine("Terakhir diedit", formatDate(item.transaction.updatedAt))
                    }
                }
            }
            item {
                val canEdit = item.transaction.type != TransactionTypes.TRANSFER
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (canEdit) {
                        PremiumOutlinedButton("Edit", onClick = onEdit, modifier = Modifier.weight(1f))
                    }
                    PremiumOutlinedButton("Hapus", onClick = { showDeleteDialog = true }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReportScreen(
    state: BizTrackUiState,
    viewModel: BizTrackViewModel,
    onOpenTransaction: (Long) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currency = state.preferences.currency

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader("Laporan")
        }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        IconButton(onClick = viewModel::selectPreviousReportMonth) {
                            Text("<", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(monthLabel(state.selectedReportMonth), fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = viewModel::selectNextReportMonth) {
                            Text(">", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiCard("Income", formatMoney(state.report.income, currency), PremiumIncome, Modifier.weight(1f))
                        KpiCard("Expense", formatMoney(state.report.expense, currency), PremiumExpense, Modifier.weight(1f))
                    }
                    KpiCard("Profit", formatMoney(state.report.profit, currency), PremiumGoldDark)
                    MiniIncomeExpenseChart(state.report.income, state.report.expense)
                    PremiumPrimaryButton(
                        text = "Export laporan CSV",
                        onClick = {
                            scope.launch {
                                runCatching { viewModel.exportCurrentReportCsv(context) }
                                    .onSuccess { shareFile(context, viewModel, it, "text/csv") }
                                    .onFailure {
                                        Toast.makeText(context, it.message ?: "Export gagal.", Toast.LENGTH_LONG).show()
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumOutlinedButton(
                        text = "Export laporan PDF",
                        onClick = {
                            scope.launch {
                                runCatching { viewModel.exportCurrentReportPdf(context) }
                                    .onSuccess { shareFile(context, viewModel, it, "application/pdf") }
                                    .onFailure {
                                        Toast.makeText(context, it.message ?: "Export PDF gagal.", Toast.LENGTH_LONG).show()
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Kategori dominan")
                    DetailLine("Pemasukan", state.report.topIncomeCategory?.category?.name ?: "-")
                    DetailLine("Pengeluaran", state.report.topExpenseCategory?.category?.name ?: "-")
                    DetailLine("Jumlah transaksi", state.report.transactionCount.toString())
                }
            }
        }
        item {
            SectionHeader("Transaksi laporan")
        }
        if (state.report.transactions.isEmpty()) {
            item { EmptyState("Belum ada data untuk laporan ini.", "Pilih bulan lain atau tambah transaksi baru.") }
        } else {
            items(state.report.transactions, key = { it.transaction.id }) { item ->
                TransactionRow(item, currency, onClick = { onOpenTransaction(item.transaction.id) })
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: BizTrackUiState,
    viewModel: BizTrackViewModel,
    onCategories: () -> Unit,
    onCashAccounts: () -> Unit,
    onDebtReceivables: () -> Unit,
    onTransferCash: () -> Unit,
    onTargets: () -> Unit,
    onReminders: () -> Unit,
    onRecurring: () -> Unit,
    onInventory: () -> Unit,
    onContacts: () -> Unit,
    onInvoices: () -> Unit,
    onBackup: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var businessName by remember(state.preferences.businessName) { mutableStateOf(state.preferences.businessName) }
    var currency by remember(state.preferences.currency) { mutableStateOf(state.preferences.currency) }
    var pin by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        scope.launch { viewModel.deleteAllData() }
                    },
                ) { Text("Hapus semua") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal") } },
            title = { Text("Hapus semua data?") },
            text = { Text("Semua transaksi, kategori, akun kas, dan pengaturan lokal akan dihapus.") },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionHeader("Pengaturan") }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Nama bisnis") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Store) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase().take(3) },
                        label = { Text("Mata uang") },
                        leadingIcon = { CurrencyPrefix(currency.ifBlank { "IDR" }) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumPrimaryButton(
                        text = "Simpan profil bisnis",
                        onClick = { viewModel.updateBusinessInfo(businessName, currency.ifBlank { "IDR" }) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            PremiumPanel {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Dark mode", fontWeight = FontWeight.SemiBold)
                    }
                    Switch(
                        checked = state.preferences.themeMode == "dark",
                        onCheckedChange = { viewModel.updateThemeMode(if (it) "dark" else "light") },
                        colors = premiumSwitchColors(),
                    )
                }
            }
        }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader("Keamanan", if (state.preferences.pinEnabled) "PIN aktif" else "PIN mati")
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                        label = { Text("PIN baru") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Settings) },
                        colors = premiumTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PremiumPrimaryButton(
                            text = "Aktifkan PIN",
                            enabled = pin.length >= 4,
                            onClick = {
                                viewModel.updatePin(pin)
                                pin = ""
                                pinMessage = "PIN diaktifkan."
                            },
                            modifier = Modifier.weight(1f),
                        )
                        PremiumOutlinedButton(
                            text = "Matikan",
                            onClick = {
                                viewModel.updatePin("")
                                pin = ""
                                pinMessage = "PIN dimatikan."
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pinMessage != null) {
                        Text(pinMessage ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            SectionHeader("Keuangan")
            SettingsAction("Isi data contoh", FinanceGlyph.Report) {
                viewModel.seedDemoData { created ->
                    Toast.makeText(
                        context,
                        if (created) "Data contoh berhasil dibuat." else "Data contoh hanya bisa dibuat saat transaksi masih kosong.",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
            SettingsAction("Target bulanan", FinanceGlyph.Report, onTargets)
            SettingsAction("Transfer antar akun", FinanceGlyph.Wallet, onTransferCash)
            SettingsAction("Piutang & hutang", FinanceGlyph.Ledger, onDebtReceivables)
            SettingsAction("Reminder pembayaran", FinanceGlyph.Bell, onReminders)
            SettingsAction("Template transaksi", FinanceGlyph.Report, onRecurring)
        }
        item {
            SectionHeader("Operasional")
            SettingsAction("Invoice / nota", FinanceGlyph.Report, onInvoices)
            SettingsAction("Kontak bisnis", FinanceGlyph.Settings, onContacts)
            SettingsAction("Inventori ringan", FinanceGlyph.Store, onInventory)
            SettingsAction("Kategori transaksi", FinanceGlyph.Ledger, onCategories)
            SettingsAction("Akun kas / dompet", FinanceGlyph.Wallet, onCashAccounts)
        }
        item {
            SectionHeader("Sistem")
            SettingsAction("Backup & restore lokal", FinanceGlyph.Report, onBackup)
            SettingsAction("Hapus semua data", FinanceGlyph.Empty) { confirmDelete = true }
        }
        item {
            Text(
                "Data tersimpan lokal. Backup berkala disarankan.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MonthlyTargetsScreen(state: BizTrackUiState, viewModel: BizTrackViewModel, onBack: () -> Unit) {
    var incomeTarget by remember(state.preferences.monthlyIncomeTarget) {
        mutableStateOf(state.preferences.monthlyIncomeTarget.takeIf { it > 0 }?.toString() ?: "")
    }
    var expenseLimit by remember(state.preferences.monthlyExpenseLimit) {
        mutableStateOf(state.preferences.monthlyExpenseLimit.takeIf { it > 0 }?.toString() ?: "")
    }
    val currency = state.preferences.currency

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeaderWithBack("Target bulanan", "", onBack) }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    TargetProgressLine(
                        label = "Target pemasukan",
                        current = state.monthlyIncome,
                        target = state.preferences.monthlyIncomeTarget,
                        currency = currency,
                        color = PremiumIncome,
                    )
                    TargetProgressLine(
                        label = "Limit pengeluaran",
                        current = state.monthlyExpense,
                        target = state.preferences.monthlyExpenseLimit,
                        currency = currency,
                        color = PremiumExpense,
                    )
                }
            }
        }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyInputField(
                        value = incomeTarget,
                        onValueChange = { incomeTarget = it },
                        label = "Target pemasukan",
                        currency = currency,
                    )
                    MoneyInputField(
                        value = expenseLimit,
                        onValueChange = { expenseLimit = it },
                        label = "Limit pengeluaran",
                        currency = currency,
                    )
                    PremiumPrimaryButton(
                        text = "Simpan target",
                        onClick = {
                            viewModel.updateMonthlyTargets(
                                parseMoney(incomeTarget) ?: 0,
                                parseMoney(expenseLimit) ?: 0,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TargetProgressLine(label: String, current: Long, target: Long, currency: String, color: Color) {
    val progress = if (target <= 0) 0f else (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(
                if (target > 0) "${(progress * 100).toInt()}%" else "Belum diset",
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "${formatMoney(current, currency)} / ${if (target > 0) formatMoney(target, currency) else "-"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceAtLeast(0.03f))
                    .height(9.dp)
                    .background(color, RoundedCornerShape(8.dp)),
            )
        }
    }
}

@Composable
private fun TransferCashScreen(
    state: BizTrackUiState,
    viewModel: BizTrackViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    var fromAccountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(formatDate(todayMillis())) }
    var note by remember { mutableStateOf("") }
    LaunchedEffect(state.cashAccounts.size) {
        if (fromAccountId == null) fromAccountId = state.cashAccounts.firstOrNull()?.id
        if (toAccountId == null) toAccountId = state.cashAccounts.drop(1).firstOrNull()?.id
    }

    val parsedAmount = parseMoney(amount)
    val parsedDate = parseDateMillis(date)
    val isValid = state.cashAccounts.size >= 2 && fromAccountId != null && toAccountId != null &&
        fromAccountId != toAccountId && parsedAmount != null && parsedAmount > 0 && parsedDate != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeaderWithBack("Transfer antar akun", "Pindahkan saldo tanpa mengubah profit", onBack) }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (state.cashAccounts.size < 2) {
                        Text("Minimal butuh dua akun kas aktif.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        ChoiceSection(
                            title = "Dari akun",
                            options = state.cashAccounts,
                            selectedId = fromAccountId,
                            label = { "${it.name} (${formatMoney(it.currentBalance, state.preferences.currency)})" },
                            color = { PremiumEmerald },
                            onSelected = { fromAccountId = it },
                        )
                        ChoiceSection(
                            title = "Ke akun",
                            options = state.cashAccounts,
                            selectedId = toAccountId,
                            label = { it.name },
                            color = { PremiumGoldDark },
                            onSelected = { toAccountId = it },
                        )
                    }
                    MoneyInputField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Nominal",
                        currency = state.preferences.currency,
                    )
                    DatePickerField(
                        value = date,
                        onValueChange = { date = it },
                        label = "Tanggal",
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Ledger) },
                        colors = premiumTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumPrimaryButton(
                        text = "Simpan transfer",
                        enabled = isValid,
                        onClick = {
                            viewModel.transferCash(
                                fromAccountId = fromAccountId ?: return@PremiumPrimaryButton,
                                toAccountId = toAccountId ?: return@PremiumPrimaryButton,
                                amount = parsedAmount ?: return@PremiumPrimaryButton,
                                date = parsedDate ?: return@PremiumPrimaryButton,
                                note = note,
                                onSaved = onSaved,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtReceivablesScreen(state: BizTrackUiState, viewModel: BizTrackViewModel, onBack: () -> Unit) {
    var type by remember { mutableStateOf(DebtTypes.RECEIVABLE) }
    var partyName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var paymentTarget by remember { mutableStateOf<com.app.biztrack.data.local.entity.DebtReceivableEntity?>(null) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentAccountId by remember { mutableStateOf<Long?>(null) }
    val currency = state.preferences.currency
    LaunchedEffect(state.cashAccounts.size) {
        if (paymentAccountId == null) paymentAccountId = state.cashAccounts.firstOrNull()?.id
    }

    paymentTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { paymentTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.recordDebtPayment(
                            target,
                            parseMoney(paymentAmount) ?: 0,
                            paymentAccountId ?: return@TextButton,
                        )
                        paymentAmount = ""
                        paymentTarget = null
                    },
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { paymentTarget = null }) { Text("Batal") } },
            title = { Text("Catat pembayaran") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MoneyInputField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = "Nominal",
                        currency = currency,
                    )
                    ChoiceSection(
                        title = "Masuk/keluar dari akun",
                        options = state.cashAccounts,
                        selectedId = paymentAccountId,
                        label = { it.name },
                        color = { PremiumEmerald },
                        onSelected = { paymentAccountId = it },
                    )
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeaderWithBack("Piutang & hutang", "Pantau tagihan bisnis", onBack) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard("Piutang", formatMoney(state.receivableTotal, currency), PremiumIncome, Modifier.weight(1f))
                KpiCard("Hutang", formatMoney(state.debtTotal, currency), PremiumExpense, Modifier.weight(1f))
            }
        }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeFilterChip("Piutang", type == DebtTypes.RECEIVABLE) { type = DebtTypes.RECEIVABLE }
                        TypeFilterChip("Hutang", type == DebtTypes.DEBT) { type = DebtTypes.DEBT }
                    }
                    OutlinedTextField(
                        value = partyName,
                        onValueChange = { partyName = it },
                        label = { Text("Nama pihak") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Store) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MoneyInputField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Nominal",
                        currency = currency,
                    )
                    DatePickerField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = "Jatuh tempo",
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Ledger) },
                        colors = premiumTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumPrimaryButton(
                        text = "Simpan tagihan",
                        enabled = partyName.isNotBlank() && (parseMoney(amount) ?: 0) > 0,
                        onClick = {
                            viewModel.createDebtReceivable(
                                type = type,
                                partyName = partyName,
                                totalAmount = parseMoney(amount) ?: 0,
                                dueDate = dueDate.takeIf { it.isNotBlank() }?.let { parseDateMillis(it) },
                                note = note,
                            )
                            partyName = ""
                            amount = ""
                            dueDate = ""
                            note = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (state.debtReceivables.isEmpty()) {
            item { EmptyState("Belum ada tagihan.", "Tambahkan piutang atau hutang pertama.") }
        } else {
            items(state.debtReceivables, key = { it.id }) { item ->
                DebtReceivableRow(item, currency, onPay = {
                    paymentAmount = item.remainingAmount.toString()
                    paymentTarget = item
                })
            }
        }
    }
}

@Composable
private fun DebtReceivableRow(
    item: com.app.biztrack.data.local.entity.DebtReceivableEntity,
    currency: String,
    onPay: () -> Unit,
) {
    val isReceivable = item.type == DebtTypes.RECEIVABLE
    val color = if (isReceivable) PremiumIncome else PremiumExpense
    PremiumPanel {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlyphBadge(if (isReceivable) FinanceGlyph.Income else FinanceGlyph.Expense, color, size = 40.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.partyName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        item.dueDate?.let { "Jatuh tempo ${formatDate(it)}" } ?: "Tanpa jatuh tempo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(statusLabel(item.status), color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            DetailLine("Sisa", formatMoney(item.remainingAmount, currency))
            if (item.note.isNotBlank()) {
                Text(item.note, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (item.status != DebtStatus.PAID) {
                PremiumOutlinedButton("Catat pembayaran", onClick = onPay, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    DebtStatus.PAID -> "Lunas"
    DebtStatus.PARTIAL -> "Sebagian"
    DebtStatus.OVERDUE -> "Terlambat"
    else -> "Belum lunas"
}

@Composable
private fun RemindersScreen(state: BizTrackUiState, viewModel: BizTrackViewModel, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(formatDate(todayMillis())) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeaderWithBack("Reminder", "Pengingat pembayaran dan follow-up", onBack) }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul reminder") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Bell) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DatePickerField(
                        value = date,
                        onValueChange = { date = it },
                        label = "Tanggal",
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Catatan") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Ledger) },
                        colors = premiumTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumPrimaryButton(
                        text = "Buat reminder",
                        enabled = title.isNotBlank() && parseDateMillis(date) != null,
                        onClick = {
                            viewModel.createReminder(title, description, parseDateMillis(date) ?: todayMillis())
                            title = ""
                            description = ""
                            date = formatDate(todayMillis())
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (state.activeReminders.isEmpty()) {
            item { EmptyState("Belum ada reminder.", "Tambahkan pengingat untuk tagihan penting.") }
        } else {
            items(state.activeReminders, key = { it.id }) { item ->
                PremiumPanel {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GlyphBadge(FinanceGlyph.Bell, PremiumGoldDark, size = 40.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatDate(item.reminderDate), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (item.description.isNotBlank()) {
                                Text(item.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = { viewModel.deactivateReminder(item) }) { Text("Selesai") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringTemplatesScreen(state: BizTrackUiState, viewModel: BizTrackViewModel, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionTypes.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedMethodId by remember { mutableStateOf<Long?>(null) }
    val categoryOptions = state.categories.filter { it.type == type }

    LaunchedEffect(type, categoryOptions.size) {
        if (selectedCategoryId == null || categoryOptions.none { it.id == selectedCategoryId }) {
            selectedCategoryId = categoryOptions.firstOrNull()?.id
        }
    }
    LaunchedEffect(state.cashAccounts.size) {
        if (selectedAccountId == null) selectedAccountId = state.cashAccounts.firstOrNull()?.id
    }
    LaunchedEffect(state.paymentMethods.size) {
        if (selectedMethodId == null) selectedMethodId = state.paymentMethods.firstOrNull()?.id
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeaderWithBack("Template transaksi", "Untuk biaya dan pemasukan berulang", onBack) }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeFilterChip("Pemasukan", type == TransactionTypes.INCOME) { type = TransactionTypes.INCOME }
                        TypeFilterChip("Pengeluaran", type == TransactionTypes.EXPENSE) { type = TransactionTypes.EXPENSE }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama template") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Report) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MoneyInputField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Nominal",
                        currency = state.preferences.currency,
                    )
                    ChoiceSection(
                        title = "Kategori",
                        options = categoryOptions,
                        selectedId = selectedCategoryId,
                        label = { it.name },
                        color = { it.color.toColorOrNull() ?: PremiumEmerald },
                        onSelected = { selectedCategoryId = it },
                    )
                    ChoiceSection(
                        title = "Akun kas",
                        options = state.cashAccounts,
                        selectedId = selectedAccountId,
                        label = { it.name },
                        color = { PremiumEmerald },
                        onSelected = { selectedAccountId = it },
                    )
                    ChoiceSection(
                        title = "Metode",
                        options = state.paymentMethods,
                        selectedId = selectedMethodId,
                        label = { it.name },
                        color = { PremiumGoldDark },
                        onSelected = { selectedMethodId = it },
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Ledger) },
                        colors = premiumTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumPrimaryButton(
                        text = "Simpan template",
                        enabled = title.isNotBlank() && (parseMoney(amount) ?: 0) > 0 && selectedAccountId != null,
                        onClick = {
                            viewModel.createRecurringTemplate(
                                title = title,
                                type = type,
                                amount = parseMoney(amount) ?: 0,
                                categoryId = selectedCategoryId,
                                cashAccountId = selectedAccountId ?: return@PremiumPrimaryButton,
                                paymentMethodId = selectedMethodId,
                                note = note,
                            )
                            title = ""
                            amount = ""
                            note = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (state.recurringTemplates.isEmpty()) {
            item { EmptyState("Belum ada template.", "Buat template untuk transaksi yang sering berulang.") }
        } else {
            items(state.recurringTemplates, key = { it.id }) { template ->
                PremiumPanel {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val isIncome = template.type == TransactionTypes.INCOME
                            GlyphBadge(if (isIncome) FinanceGlyph.Income else FinanceGlyph.Expense, if (isIncome) PremiumIncome else PremiumExpense, size = 40.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(template.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatMoney(template.amount, state.preferences.currency), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { viewModel.deactivateRecurringTemplate(template.id) }) { Text("Arsip") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PremiumPrimaryButton(
                                text = "Buat hari ini",
                                onClick = { viewModel.createTransactionFromTemplate(template, todayMillis()) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusinessContactsScreen(state: BizTrackUiState, viewModel: BizTrackViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ContactTypes.CUSTOMER) }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<com.app.biztrack.data.local.entity.BusinessContactEntity?>(null) }
    val contactSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    selectedContact?.let { contact ->
        ModalBottomSheet(
            onDismissRequest = { selectedContact = null },
            modifier = Modifier.fillMaxHeight(),
            sheetState = contactSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        ) {
            CustomerDetailSheet(
                contact = contact,
                invoices = state.enrichedInvoices.filter { it.invoice.contactId == contact.id },
                currency = state.preferences.currency,
                onClose = { selectedContact = null },
                onStatement = {
                    scope.launch {
                        runCatching { viewModel.exportCustomerStatementPdf(context, contact.id) }
                            .onSuccess { shareFile(context, viewModel, it, "application/pdf") }
                            .onFailure {
                                Toast.makeText(context, it.message ?: "Export statement gagal.", Toast.LENGTH_LONG).show()
                            }
                    }
                },
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HeaderWithBack("Kontak bisnis", "Pelanggan dan supplier", onBack) }
        item {
            PremiumPanel {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeFilterChip("Customer", type == ContactTypes.CUSTOMER) { type = ContactTypes.CUSTOMER }
                        TypeFilterChip("Supplier", type == ContactTypes.SUPPLIER) { type = ContactTypes.SUPPLIER }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Store) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Telepon") },
                            colors = premiumTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            colors = premiumTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan singkat") },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumPrimaryButton(
                        text = "Tambah kontak",
                        enabled = name.isNotBlank(),
                        onClick = {
                            viewModel.createBusinessContact(name, type, phone, email, "", note)
                            name = ""
                            phone = ""
                            email = ""
                            note = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (state.businessContacts.isEmpty()) {
            item { EmptyState("Belum ada kontak.", "Tambahkan customer atau supplier utama bisnis Anda.") }
        } else {
            items(state.businessContacts, key = { it.id }) { contact ->
                PremiumPanel {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GlyphBadge(
                            FinanceGlyph.Settings,
                            if (contact.type == ContactTypes.CUSTOMER) PremiumIncome else PremiumGoldDark,
                            size = 34.dp,
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { selectedContact = contact },
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(contact.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOf(contactTypeLabel(contact.type), contact.phone.ifBlank { contact.email }).filter { it.isNotBlank() }
                                    .joinToString(" - "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TextButton(onClick = { viewModel.deactivateBusinessContact(contact.id) }) { Text("Arsip") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoicesScreen(state: BizTrackUiState, viewModel: BizTrackViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var invoiceNumber by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(InvoiceStatus.UNPAID) }
    var createDueReminder by remember { mutableStateOf(true) }
    var contactId by remember(state.businessContacts.size) { mutableStateOf(state.businessContacts.firstOrNull { it.type == ContactTypes.CUSTOMER }?.id) }
    var issueDate by remember { mutableStateOf(formatDate(todayMillis())) }
    var dueDate by remember { mutableStateOf(formatDate(todayMillis() + 7 * 86_400_000L)) }
    var selectedProductId by remember { mutableStateOf<Long?>(null) }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var lines by remember { mutableStateOf<List<FinanceRepository.InvoiceLineInput>>(emptyList()) }
    var payTarget by remember { mutableStateOf<EnrichedInvoice?>(null) }
    var paymentAmount by remember { mutableStateOf("") }
    var selectedInvoice by remember { mutableStateOf<EnrichedInvoice?>(null) }
    var paymentAccountId by remember(state.cashAccounts.size) { mutableStateOf(state.cashAccounts.firstOrNull()?.id) }
    var paymentMethodId by remember(state.paymentMethods.size) { mutableStateOf(state.paymentMethods.firstOrNull()?.id) }
    val invoiceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currency = state.preferences.currency
    val currentLineInvalid = description.isNotBlank() && ((parseMoney(quantity) ?: 0) <= 0 || (parseMoney(unitPrice) ?: 0) <= 0)
    val invoiceValidationText = when {
        lines.isEmpty() -> "Tambahkan minimal satu item."
        contactId == null -> "Pilih customer agar invoice mudah dilacak."
        else -> null
    }

    fun shareInvoice(invoice: EnrichedInvoice) {
        scope.launch {
            runCatching { viewModel.exportInvoicePdf(context, invoice.invoice) }
                .onSuccess { shareFile(context, viewModel, it, "application/pdf") }
                .onFailure {
                    Toast.makeText(context, it.message ?: "Export invoice gagal.", Toast.LENGTH_LONG).show()
                }
        }
    }

    if (payTarget != null) {
        val targetInvoice = payTarget?.invoice
        LaunchedEffect(targetInvoice?.id) {
            paymentAmount = targetInvoice?.let { (it.totalAmount - it.paidAmount).coerceAtLeast(0).toString() } ?: ""
        }
        AlertDialog(
            onDismissRequest = { payTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val invoice = payTarget?.invoice ?: return@TextButton
                        viewModel.recordInvoicePayment(
                            invoice = invoice,
                            amount = parseMoney(paymentAmount) ?: 0,
                            cashAccountId = paymentAccountId ?: return@TextButton,
                            paymentMethodId = paymentMethodId,
                        )
                        payTarget = null
                    },
                ) { Text("Catat bayar") }
            },
            dismissButton = { TextButton(onClick = { payTarget = null }) { Text("Batal") } },
            title = { Text("Pembayaran invoice") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(payTarget?.invoice?.invoiceNumber ?: "")
                    MoneyInputField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = "Nominal bayar",
                        currency = currency,
                    )
                    if (state.cashAccounts.isEmpty()) {
                        Text("Tambahkan akun kas terlebih dahulu.", color = MaterialTheme.colorScheme.error)
                    }
                    ChoiceSection(
                        title = "Akun kas",
                        options = state.cashAccounts,
                        selectedId = paymentAccountId,
                        label = { it.name },
                        color = { PremiumEmerald },
                        onSelected = { paymentAccountId = it },
                    )
                    ChoiceSection(
                        title = "Metode pembayaran",
                        options = state.paymentMethods,
                        selectedId = paymentMethodId,
                        label = { it.name },
                        color = { PremiumGoldDark },
                        onSelected = { paymentMethodId = it },
                    )
                }
            },
        )
    }

    selectedInvoice?.let { invoice ->
        ModalBottomSheet(
            onDismissRequest = { selectedInvoice = null },
            modifier = Modifier.fillMaxHeight(),
            sheetState = invoiceSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        ) {
            InvoiceDetailSheet(
                item = invoice,
                currency = currency,
                onClose = { selectedInvoice = null },
                onPaid = {
                    payTarget = invoice
                    selectedInvoice = null
                },
                onSent = {
                    viewModel.markInvoiceSent(invoice.invoice)
                    selectedInvoice = null
                },
                onShare = { shareInvoice(invoice) },
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HeaderWithBack("Invoice / nota", "Tagihan penjualan sederhana", onBack) }
        item {
            PremiumPanel {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it.take(24) },
                        label = { Text("Nomor invoice") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Report) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeFilterChip("Unpaid", status == InvoiceStatus.UNPAID) { status = InvoiceStatus.UNPAID }
                        TypeFilterChip("Sent", status == InvoiceStatus.SENT) { status = InvoiceStatus.SENT }
                        TypeFilterChip("Draft", status == InvoiceStatus.DRAFT) { status = InvoiceStatus.DRAFT }
                    }
                    if (state.businessContacts.isNotEmpty()) {
                        Text("Customer", fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.businessContacts.filter { it.type == ContactTypes.CUSTOMER }, key = { it.id }) { contact ->
                                FilterChip(
                                    selected = contactId == contact.id,
                                    onClick = { contactId = contact.id },
                                    label = { Text(contact.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DatePickerField(issueDate, { issueDate = it }, "Tanggal", Modifier.weight(1f))
                        DatePickerField(dueDate, { dueDate = it }, "Jatuh tempo", Modifier.weight(1f))
                    }
                    if (state.inventoryItems.isNotEmpty()) {
                        Text("Produk", fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedProductId == null,
                                    onClick = {
                                        selectedProductId = null
                                        description = ""
                                        unitPrice = ""
                                    },
                                    label = { Text("Manual") },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                            items(state.inventoryItems, key = { it.id }) { product ->
                                FilterChip(
                                    selected = selectedProductId == product.id,
                                    onClick = {
                                        selectedProductId = product.id
                                        description = product.name
                                        unitPrice = product.salePrice.takeIf { it > 0 }?.toString() ?: ""
                                    },
                                    label = { Text(product.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Item / layanan") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Ledger) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = formatMoneyInput(it) },
                            label = { Text("Qty") },
                            colors = premiumTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(0.7f),
                        )
                        MoneyInputField(unitPrice, { unitPrice = it }, "Harga", currency, Modifier.weight(1.3f))
                    }
                    PremiumOutlinedButton(
                        text = "Tambah item",
                        enabled = description.isNotBlank() && (parseMoney(quantity) ?: 0) > 0 && (parseMoney(unitPrice) ?: 0) > 0,
                        onClick = {
                            lines = lines + FinanceRepository.InvoiceLineInput(
                                inventoryItemId = selectedProductId,
                                description = description,
                                quantity = parseMoney(quantity) ?: 1,
                                unitPrice = parseMoney(unitPrice) ?: 0,
                            )
                            selectedProductId = null
                            description = ""
                            quantity = "1"
                            unitPrice = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (currentLineInvalid) {
                        Text(
                            "Qty dan harga harus lebih dari 0.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (lines.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            lines.forEachIndexed { index, line ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("${line.quantity}x", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    Text(line.description, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatMoney(line.quantity * line.unitPrice, currency), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    TextButton(onClick = { lines = lines.filterIndexed { itemIndex, _ -> itemIndex != index } }) { Text("Hapus") }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumPrimaryButton(
                        text = "Buat invoice",
                        enabled = lines.isNotEmpty(),
                        onClick = {
                            viewModel.createInvoice(
                                invoiceNumber = invoiceNumber,
                                contactId = contactId,
                                issueDate = parseDateMillis(issueDate) ?: todayMillis(),
                                dueDate = parseDateMillis(dueDate) ?: todayMillis(),
                                status = status,
                                lines = lines,
                                note = note,
                                createDueReminder = createDueReminder,
                            )
                            invoiceNumber = ""
                            lines = emptyList()
                            selectedProductId = null
                            description = ""
                            quantity = "1"
                            unitPrice = ""
                            note = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (invoiceValidationText != null) {
                        Text(
                            invoiceValidationText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Reminder jatuh tempo", fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = createDueReminder,
                            onCheckedChange = { createDueReminder = it },
                            colors = premiumSwitchColors(),
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactMetric("Unpaid", state.unpaidInvoiceCount.toString(), PremiumGoldDark, Modifier.weight(1f))
                CompactMetric("Overdue", state.overdueInvoiceCount.toString(), PremiumExpense, Modifier.weight(1f))
            }
        }
        if (state.enrichedInvoices.isEmpty()) {
            item { EmptyState("Belum ada invoice.", "Buat invoice pertama untuk penjualan bisnis.") }
        } else {
            items(state.enrichedInvoices, key = { it.invoice.id }) { invoice ->
                InvoiceRow(
                    item = invoice,
                    currency = currency,
                    onOpen = { selectedInvoice = invoice },
                    onPaid = { payTarget = invoice },
                    onShare = { shareInvoice(invoice) },
                )
            }
        }
    }
}

@Composable
private fun CompactMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    PremiumPanel(modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(8.dp).background(color, RoundedCornerShape(8.dp)))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CustomerDetailSheet(
    contact: com.app.biztrack.data.local.entity.BusinessContactEntity,
    invoices: List<EnrichedInvoice>,
    currency: String,
    onClose: () -> Unit,
    onStatement: () -> Unit,
) {
    val unpaid = invoices.filter { it.invoice.status != InvoiceStatus.PAID }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SheetHeader(contact.name, contactTypeLabel(contact.type), onClose)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactMetric("Invoice", invoices.size.toString(), PremiumEmerald, Modifier.weight(1f))
                CompactMetric("Unpaid", unpaid.size.toString(), PremiumGoldDark, Modifier.weight(1f))
            }
        }
        item {
            PremiumOutlinedButton("Statement PDF", onClick = onStatement, modifier = Modifier.fillMaxWidth())
        }
        item {
            PremiumPanel {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLine("Telepon", contact.phone.ifBlank { "-" })
                    DetailLine("Email", contact.email.ifBlank { "-" })
                    DetailLine("Alamat", contact.address.ifBlank { "-" })
                    DetailLine("Catatan", contact.note.ifBlank { "-" })
                }
            }
        }
        if (invoices.isNotEmpty()) {
            item { SectionHeader("Riwayat invoice") }
            items(invoices, key = { it.invoice.id }) { invoice ->
                PremiumPanel {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(8.dp).background(if (invoice.invoice.status == InvoiceStatus.PAID) PremiumIncome else PremiumGoldDark, RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f)) {
                            Text(invoice.invoice.invoiceNumber, fontWeight = FontWeight.Bold)
                            Text(invoiceStatusLabel(invoice.invoice.status), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                        Text(formatMoney(invoice.invoice.totalAmount, currency), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceDetailSheet(
    item: EnrichedInvoice,
    currency: String,
    onClose: () -> Unit,
    onPaid: () -> Unit,
    onSent: () -> Unit,
    onShare: () -> Unit,
) {
    val isPaid = item.invoice.status == InvoiceStatus.PAID
    val canMarkSent = item.invoice.status == InvoiceStatus.DRAFT || item.invoice.status == InvoiceStatus.UNPAID
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SheetHeader(item.invoice.invoiceNumber, invoiceStatusLabel(item.invoice.status), onClose)
        }
        item {
            PremiumPanel {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLine("Customer", item.contact?.name ?: "-")
                    DetailLine("Tanggal", formatDate(item.invoice.issueDate))
                    DetailLine("Jatuh tempo", formatDate(item.invoice.dueDate))
                    DetailLine("Status", invoiceStatusLabel(item.invoice.status))
                }
            }
        }
        item { SectionHeader("Item") }
        items(item.items, key = { it.id }) { line ->
            PremiumPanel {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("${line.quantity}x", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Column(Modifier.weight(1f)) {
                        Text(line.description, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatMoney(line.unitPrice, currency), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Text(formatMoney(line.totalAmount, currency), fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            PremiumPanel {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailLine("Total", formatMoney(item.invoice.totalAmount, currency))
                    DetailLine("Dibayar", formatMoney(item.invoice.paidAmount, currency))
                    DetailLine("Catatan", item.invoice.note.ifBlank { "-" })
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PremiumOutlinedButton("PDF", onClick = onShare, modifier = Modifier.weight(1f))
                    if (!isPaid) {
                        PremiumPrimaryButton("Catat lunas", onClick = onPaid, modifier = Modifier.weight(1f))
                    }
                }
                if (canMarkSent) {
                    PremiumOutlinedButton("Tandai terkirim", onClick = onSent, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Tutup")
        }
    }
}

@Composable
private fun InvoiceRow(item: EnrichedInvoice, currency: String, onOpen: () -> Unit, onPaid: () -> Unit, onShare: () -> Unit) {
    val isPaid = item.invoice.status == InvoiceStatus.PAID
    val accent = if (isPaid) PremiumIncome else PremiumGoldDark
    PremiumPanel {
        Row(
            modifier = Modifier
                .clickable(onClick = onOpen)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlyphBadge(FinanceGlyph.Report, accent, size = 34.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.invoice.invoiceNumber, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull("${item.items.size} item", item.contact?.name, invoiceStatusLabel(item.invoice.status), formatDate(item.invoice.dueDate))
                        .joinToString(" - "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatMoney(item.invoice.totalAmount, currency), color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = onShare) { Text("PDF") }
                    if (!isPaid) {
                        TextButton(onClick = onPaid) { Text("Lunas") }
                    }
                }
            }
        }
    }
}

private fun contactTypeLabel(type: String): String = when (type) {
    ContactTypes.SUPPLIER -> "Supplier"
    else -> "Customer"
}

private fun invoiceStatusLabel(status: String): String = when (status) {
    InvoiceStatus.PAID -> "Paid"
    InvoiceStatus.PARTIAL -> "Partial"
    InvoiceStatus.OVERDUE -> "Overdue"
    InvoiceStatus.SENT -> "Sent"
    InvoiceStatus.DRAFT -> "Draft"
    else -> "Unpaid"
}

@Composable
private fun InventoryScreen(state: BizTrackUiState, viewModel: BizTrackViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var minStock by remember { mutableStateOf("5") }
    var costPrice by remember { mutableStateOf("") }
    var salePrice by remember { mutableStateOf("") }
    var movementTarget by remember { mutableStateOf<com.app.biztrack.data.local.entity.InventoryItemEntity?>(null) }
    var movementType by remember { mutableStateOf("in") }
    var movementQty by remember { mutableStateOf("") }
    var movementNote by remember { mutableStateOf("") }
    var movementCreatesTransaction by remember { mutableStateOf(true) }
    var movementAccountId by remember { mutableStateOf<Long?>(null) }
    var movementMethodId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.cashAccounts.size) {
        if (movementAccountId == null) movementAccountId = state.cashAccounts.firstOrNull()?.id
    }
    LaunchedEffect(state.paymentMethods.size) {
        if (movementMethodId == null) movementMethodId = state.paymentMethods.firstOrNull()?.id
    }

    movementTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { movementTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (movementCreatesTransaction) {
                            viewModel.recordInventoryMovementWithTransaction(
                                itemId = target.id,
                                type = movementType,
                                quantity = parseMoney(movementQty) ?: 0,
                                note = movementNote,
                                cashAccountId = movementAccountId ?: return@TextButton,
                                paymentMethodId = movementMethodId,
                            )
                        } else {
                            viewModel.recordInventoryMovement(
                                target.id,
                                movementType,
                                parseMoney(movementQty) ?: 0,
                                movementNote,
                            )
                        }
                        movementTarget = null
                        movementQty = ""
                        movementNote = ""
                    },
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { movementTarget = null }) { Text("Batal") } },
            title = { Text("Update stok") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeFilterChip("Masuk", movementType == "in") { movementType = "in" }
                        TypeFilterChip("Keluar", movementType == "out") { movementType = "out" }
                    }
                    OutlinedTextField(
                        value = movementQty,
                        onValueChange = { movementQty = formatMoneyInput(it) },
                        label = { Text("Jumlah") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = movementNote,
                        onValueChange = { movementNote = it },
                        label = { Text("Catatan") },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Buat transaksi kas", fontWeight = FontWeight.Bold)
                            Text(
                                if (movementType == "in") "Stok masuk jadi pengeluaran" else "Stok keluar jadi pemasukan",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = movementCreatesTransaction,
                            onCheckedChange = { movementCreatesTransaction = it },
                            colors = premiumSwitchColors(),
                        )
                    }
                    if (movementCreatesTransaction) {
                        ChoiceSection(
                            title = "Akun kas",
                            options = state.cashAccounts,
                            selectedId = movementAccountId,
                            label = { "${it.name} (${formatMoney(it.currentBalance, state.preferences.currency)})" },
                            color = { PremiumEmerald },
                            onSelected = { movementAccountId = it },
                        )
                        ChoiceSection(
                            title = "Metode pembayaran",
                            options = state.paymentMethods,
                            selectedId = movementMethodId,
                            label = { it.name },
                            color = { PremiumGoldDark },
                            onSelected = { movementMethodId = it },
                        )
                    }
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeaderWithBack("Inventori", "Stok barang sederhana", onBack) }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama produk") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Store) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU / kode") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Ledger) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = formatMoneyInput(it) },
                        label = { Text("Stok awal") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Report) },
                        colors = premiumTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = minStock,
                        onValueChange = { minStock = formatMoneyInput(it) },
                        label = { Text("Minimum stok") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Report) },
                        colors = premiumTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MoneyInputField(costPrice, { costPrice = it }, "Harga modal", state.preferences.currency)
                    MoneyInputField(salePrice, { salePrice = it }, "Harga jual", state.preferences.currency)
                    PremiumPrimaryButton(
                        text = "Tambah produk",
                        enabled = name.isNotBlank(),
                        onClick = {
                            viewModel.createInventoryItem(
                                name = name,
                                sku = sku,
                                stock = parseMoney(stock) ?: 0,
                                minStock = parseMoney(minStock) ?: 0,
                                costPrice = parseMoney(costPrice) ?: 0,
                                salePrice = parseMoney(salePrice) ?: 0,
                            )
                            name = ""
                            sku = ""
                            stock = ""
                            minStock = "5"
                            costPrice = ""
                            salePrice = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        if (state.inventoryItems.isEmpty()) {
            item { EmptyState("Belum ada produk.", "Tambahkan produk pertama untuk mulai memantau stok.") }
        } else {
            items(state.inventoryItems, key = { it.id }) { item ->
                val isLowStock = item.stock <= item.minStock
                PremiumPanel {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlyphBadge(FinanceGlyph.Store, if (isLowStock) PremiumGoldDark else PremiumEmerald, size = 40.dp)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "Stok ${item.stock} · Min ${item.minStock} · ${formatMoney(item.salePrice, state.preferences.currency)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(onClick = { viewModel.deactivateInventoryItem(item.id) }) { Text("Arsip") }
                        }
                        if (isLowStock) {
                            Text(
                                "Perlu restock",
                                color = PremiumGoldDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                        PremiumOutlinedButton(
                            text = "Update stok",
                            onClick = {
                                movementTarget = item
                                movementType = "in"
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesScreen(state: BizTrackUiState, viewModel: BizTrackViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CategoryTypes.INCOME) }
    val colors = listOf("#0E8D70", "#004C43", "#D9A24A", "#E76A2E", "#7C5A2D")
    var selectedColor by remember { mutableStateOf(colors.first()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeaderWithBack("Kategori", "Kelola kategori pemasukan dan pengeluaran", onBack) }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeFilterChip("Pemasukan", type == CategoryTypes.INCOME) { type = CategoryTypes.INCOME }
                        TypeFilterChip("Pengeluaran", type == CategoryTypes.EXPENSE) { type = CategoryTypes.EXPENSE }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama kategori") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Ledger) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        colors.forEach { color ->
                            FilterChip(
                                selected = selectedColor == color,
                                onClick = { selectedColor = color },
                                label = { Text(color) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(color.toColorOrNull() ?: Color.Gray, RoundedCornerShape(10.dp)),
                                    )
                                },
                            )
                        }
                    }
                    PremiumPrimaryButton(
                        text = "Tambah kategori",
                        onClick = {
                            viewModel.createCategory(name, type, selectedColor)
                            name = ""
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        items(state.categories, key = { it.id }) { category ->
            PremiumPanel {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(category.color.toColorOrNull() ?: MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.Bold)
                        Text(category.type, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { viewModel.deactivateCategory(category.id) }) { Text("Nonaktifkan") }
                }
            }
        }
    }
}

@Composable
private fun CashAccountsScreen(state: BizTrackUiState, viewModel: BizTrackViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val currency = state.preferences.currency

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeaderWithBack("Akun kas", "Kelola dompet, bank, QRIS, dan kas kecil", onBack) }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama akun") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Wallet) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = { Text("Saldo awal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { CurrencyPrefix(currency) },
                        colors = premiumTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan") },
                        leadingIcon = { FieldGlyph(FinanceGlyph.Ledger) },
                        colors = premiumTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumPrimaryButton(
                        text = "Tambah akun kas",
                        onClick = {
                            viewModel.createCashAccount(name, parseMoney(balance) ?: 0, note)
                            name = ""
                            balance = ""
                            note = ""
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        items(state.cashAccounts, key = { it.id }) { account ->
            PremiumPanel {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlyphBadge(FinanceGlyph.Wallet, PremiumEmerald)
                    Column(Modifier.weight(1f)) {
                        Text(account.name, fontWeight = FontWeight.Bold)
                        Text(formatMoney(account.currentBalance, currency), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (account.note.isNotBlank()) {
                            Text(account.note, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                    TextButton(onClick = { viewModel.deactivateCashAccount(account.id) }) { Text("Nonaktifkan") }
                }
            }
        }
    }
}

@Composable
private fun BackupRestoreScreen(
    state: BizTrackUiState,
    viewModel: BizTrackViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var latestFile by remember { mutableStateOf<File?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            viewModel.previewBackup(context, uri)
        }
    }

    state.backupPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = {
                pendingRestoreUri = null
                viewModel.clearBackupPreview()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingRestoreUri ?: return@TextButton
                        scope.launch {
                            runCatching { viewModel.restoreBackup(context, uri) }
                                .onSuccess {
                                    Toast.makeText(context, "Backup berhasil dipulihkan.", Toast.LENGTH_LONG).show()
                                    pendingRestoreUri = null
                                    viewModel.clearBackupPreview()
                                }
                                .onFailure {
                                    Toast.makeText(context, it.message ?: "Restore gagal.", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingRestoreUri = null
                        viewModel.clearBackupPreview()
                    },
                ) { Text("Batal") }
            },
            title = { Text("Preview backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailLine("Bisnis", preview.businessName.ifBlank { "-" })
                    DetailLine("Transaksi", preview.transactionCount.toString())
                    DetailLine("Akun kas", preview.cashAccountCount.toString())
                    DetailLine("Piutang/hutang", preview.debtReceivableCount.toString())
                    DetailLine("Reminder", preview.reminderCount.toString())
                    DetailLine("Template", preview.recurringTemplateCount.toString())
                    DetailLine("Inventori", preview.inventoryItemCount.toString())
                    DetailLine("Kontak bisnis", preview.businessContactCount.toString())
                    DetailLine("Invoice", preview.invoiceCount.toString())
                    Text(
                        "Restore akan mengganti seluruh data lokal saat ini.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HeaderWithBack("Backup & restore", "Simpan data lokal ke file JSON", onBack) }
        item {
            PremiumPanel {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DetailLine("Transaksi", state.transactions.size.toString())
                    DetailLine("Kategori", state.categories.size.toString())
                    DetailLine("Akun kas", state.cashAccounts.size.toString())
                    DetailLine(
                        "Backup terakhir",
                        state.preferences.lastBackupAt?.let { formatDate(it) } ?: "-",
                    )
                    PremiumPrimaryButton(
                        text = "Buat backup JSON",
                        onClick = {
                            scope.launch {
                                runCatching { viewModel.createBackup(context) }
                                    .onSuccess {
                                        latestFile = it
                                        Toast.makeText(context, "Backup dibuat: ${it.name}", Toast.LENGTH_LONG).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(context, it.message ?: "Backup gagal.", Toast.LENGTH_LONG).show()
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PremiumOutlinedButton("Restore dari file", onClick = { restoreLauncher.launch("application/json") }, modifier = Modifier.fillMaxWidth())
                    latestFile?.let { file ->
                        PremiumOutlinedButton("Bagikan ${file.name}", onClick = { shareFile(context, viewModel, file, "application/json") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            Text(
                "Restore akan mengganti seluruh data lokal dengan isi file backup. Pastikan file berasal dari BizTrack.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun HeaderWithBack(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.85f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            GlyphBadge(FinanceGlyph.Back, PremiumEmerald, size = 30.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.9f))
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SettingsAction(label: String, glyph: FinanceGlyph, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.surface, PremiumIvory.copy(alpha = 0.45f)),
                ),
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, PremiumGold.copy(alpha = 0.24f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlyphBadge(glyph, PremiumEmerald, size = 30.dp)
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        GlyphBadge(FinanceGlyph.Chevron, MaterialTheme.colorScheme.onSurfaceVariant, size = 24.dp)
    }
}

private fun shareFile(context: Context, viewModel: BizTrackViewModel, file: File, mimeType: String) {
    val uri = viewModel.shareUriForFile(context, file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = ClipData.newUri(context.contentResolver, file.name, uri)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan ${file.name}"))
}
