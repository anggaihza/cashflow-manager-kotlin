package com.app.biztrack.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.biztrack.presentation.CategoryTotal
import com.app.biztrack.presentation.EnrichedTransaction
import com.app.biztrack.utils.formatDate
import com.app.biztrack.utils.formatMoney

val PremiumEmerald = Color(0xFF083F3A)
val PremiumEmeraldDeep = Color(0xFF062A27)
val PremiumEmeraldSoft = Color(0xFFF1F5F2)
val PremiumGold = Color(0xFFD7B16A)
val PremiumGoldDark = Color(0xFF8C6A32)
val PremiumGoldLine = Color(0xFFEAD6B4)
val PremiumIvory = Color(0xFFFBF8F1)
val PremiumIncome = Color(0xFF0D6258)
val PremiumExpense = Color(0xFF8A5A37)

enum class FinanceGlyph {
    Ledger,
    Home,
    Store,
    Income,
    Expense,
    Wallet,
    Report,
    Settings,
    Bell,
    Plus,
    Chevron,
    Back,
    Empty,
}

@Composable
fun PremiumPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.64f)),
        content = { content() },
    )
}

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    tint: Color = PremiumGold,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(
                Brush.linearGradient(
                    listOf(PremiumEmeraldDeep, PremiumEmerald.copy(alpha = 0.92f)),
                ),
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, tint.copy(alpha = 0.92f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(34.dp)) {
            val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            val gold = tint
            drawLine(gold, Offset(7.dp.toPx(), 7.dp.toPx()), Offset(7.dp.toPx(), 27.dp.toPx()), stroke.width, StrokeCap.Round)
            drawLine(gold, Offset(7.dp.toPx(), 8.dp.toPx()), Offset(22.dp.toPx(), 8.dp.toPx()), stroke.width, StrokeCap.Round)
            drawLine(gold, Offset(7.dp.toPx(), 17.dp.toPx()), Offset(20.dp.toPx(), 17.dp.toPx()), stroke.width, StrokeCap.Round)
            drawLine(gold, Offset(7.dp.toPx(), 26.dp.toPx()), Offset(22.dp.toPx(), 26.dp.toPx()), stroke.width, StrokeCap.Round)
            drawArc(
                color = gold,
                startAngle = -90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(14.dp.toPx(), 8.dp.toPx()),
                size = Size(13.dp.toPx(), 9.dp.toPx()),
                style = stroke,
            )
            drawArc(
                color = gold,
                startAngle = -90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(14.dp.toPx(), 17.dp.toPx()),
                size = Size(13.dp.toPx(), 9.dp.toPx()),
                style = stroke,
            )
        }
    }
}

@Composable
fun GlyphBadge(
    glyph: FinanceGlyph,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    dark: Boolean = false,
) {
    val badgeBrush = if (dark) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f)))
    } else {
        Brush.linearGradient(listOf(color.copy(alpha = 0.15f), PremiumIvory.copy(alpha = 0.55f), MaterialTheme.colorScheme.surface))
    }
    val border = if (dark) PremiumGold.copy(alpha = 0.45f) else color.copy(alpha = 0.20f)
    Box(
        modifier = modifier
            .size(size)
            .background(badgeBrush, RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        FinanceGlyphCanvas(glyph = glyph, color = if (dark) PremiumGold else color)
    }
}

@Composable
private fun FinanceGlyphCanvas(glyph: FinanceGlyph, color: Color) {
    Canvas(Modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round)
        when (glyph) {
            FinanceGlyph.Ledger -> {
                drawRoundRect(color, Offset(4.dp.toPx(), 4.dp.toPx()), Size(16.dp.toPx(), 16.dp.toPx()), CornerRadius(4.dp.toPx()), style = stroke)
                drawLine(color, Offset(8.dp.toPx(), 9.dp.toPx()), Offset(16.dp.toPx(), 9.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(8.dp.toPx(), 14.dp.toPx()), Offset(16.dp.toPx(), 14.dp.toPx()), stroke.width, StrokeCap.Round)
            }
            FinanceGlyph.Home -> {
                drawLine(color, Offset(4.dp.toPx(), 12.dp.toPx()), Offset(12.dp.toPx(), 5.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(12.dp.toPx(), 5.dp.toPx()), Offset(20.dp.toPx(), 12.dp.toPx()), stroke.width, StrokeCap.Round)
                drawRoundRect(color, Offset(6.dp.toPx(), 11.dp.toPx()), Size(16.dp.toPx(), 12.dp.toPx()), CornerRadius(3.dp.toPx()), style = stroke)
                drawLine(color, Offset(12.dp.toPx(), 23.dp.toPx()), Offset(12.dp.toPx(), 17.dp.toPx()), stroke.width, StrokeCap.Round)
            }
            FinanceGlyph.Store -> {
                drawRoundRect(color, Offset(5.dp.toPx(), 10.dp.toPx()), Size(14.dp.toPx(), 11.dp.toPx()), CornerRadius(2.dp.toPx()), style = stroke)
                drawLine(color, Offset(4.dp.toPx(), 10.dp.toPx()), Offset(6.dp.toPx(), 5.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(6.dp.toPx(), 5.dp.toPx()), Offset(18.dp.toPx(), 5.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(18.dp.toPx(), 5.dp.toPx()), Offset(20.dp.toPx(), 10.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(8.dp.toPx(), 10.dp.toPx()), Offset(8.dp.toPx(), 7.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(12.dp.toPx(), 10.dp.toPx()), Offset(12.dp.toPx(), 5.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(16.dp.toPx(), 10.dp.toPx()), Offset(16.dp.toPx(), 7.dp.toPx()), stroke.width, StrokeCap.Round)
            }
            FinanceGlyph.Income -> {
                drawCircle(color, radius = 8.5.dp.toPx(), center = Offset(12.dp.toPx(), 12.dp.toPx()), style = stroke)
                drawLine(color, Offset(12.dp.toPx(), 17.dp.toPx()), Offset(12.dp.toPx(), 7.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(8.dp.toPx(), 11.dp.toPx()), Offset(12.dp.toPx(), 7.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(16.dp.toPx(), 11.dp.toPx()), Offset(12.dp.toPx(), 7.dp.toPx()), stroke.width, StrokeCap.Round)
            }
            FinanceGlyph.Expense -> {
                drawCircle(color, radius = 8.5.dp.toPx(), center = Offset(12.dp.toPx(), 12.dp.toPx()), style = stroke)
                drawLine(color, Offset(12.dp.toPx(), 7.dp.toPx()), Offset(12.dp.toPx(), 17.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(8.dp.toPx(), 13.dp.toPx()), Offset(12.dp.toPx(), 17.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(16.dp.toPx(), 13.dp.toPx()), Offset(12.dp.toPx(), 17.dp.toPx()), stroke.width, StrokeCap.Round)
            }
            FinanceGlyph.Wallet -> {
                drawRoundRect(color, Offset(3.dp.toPx(), 7.dp.toPx()), Size(19.dp.toPx(), 13.dp.toPx()), CornerRadius(4.dp.toPx()), style = stroke)
                drawLine(color, Offset(7.dp.toPx(), 7.dp.toPx()), Offset(17.dp.toPx(), 4.dp.toPx()), stroke.width, StrokeCap.Round)
                drawCircle(color, radius = 1.4.dp.toPx(), center = Offset(17.dp.toPx(), 14.dp.toPx()))
            }
            FinanceGlyph.Report -> {
                drawRoundRect(color, Offset(4.dp.toPx(), 4.dp.toPx()), Size(16.dp.toPx(), 18.dp.toPx()), CornerRadius(3.dp.toPx()), style = stroke)
                drawLine(color, Offset(8.dp.toPx(), 17.dp.toPx()), Offset(8.dp.toPx(), 13.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(12.dp.toPx(), 17.dp.toPx()), Offset(12.dp.toPx(), 9.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(16.dp.toPx(), 17.dp.toPx()), Offset(16.dp.toPx(), 12.dp.toPx()), stroke.width, StrokeCap.Round)
            }
            FinanceGlyph.Settings -> {
                drawCircle(color, radius = 4.dp.toPx(), center = Offset(12.dp.toPx(), 12.dp.toPx()), style = stroke)
                drawCircle(color, radius = 8.5.dp.toPx(), center = Offset(12.dp.toPx(), 12.dp.toPx()), style = stroke)
            }
            FinanceGlyph.Bell -> {
                drawArc(
                    color = color,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(6.dp.toPx(), 5.dp.toPx()),
                    size = Size(12.dp.toPx(), 13.dp.toPx()),
                    style = stroke,
                )
                drawLine(color, Offset(6.dp.toPx(), 17.dp.toPx()), Offset(18.dp.toPx(), 17.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(8.dp.toPx(), 17.dp.toPx()), Offset(8.dp.toPx(), 11.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(16.dp.toPx(), 17.dp.toPx()), Offset(16.dp.toPx(), 11.dp.toPx()), stroke.width, StrokeCap.Round)
                drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(12.dp.toPx(), 21.dp.toPx()))
            }
            FinanceGlyph.Plus -> {
                drawLine(color, Offset(12.dp.toPx(), 6.dp.toPx()), Offset(12.dp.toPx(), 18.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(6.dp.toPx(), 12.dp.toPx()), Offset(18.dp.toPx(), 12.dp.toPx()), stroke.width, StrokeCap.Round)
            }
            FinanceGlyph.Chevron -> {
                drawLine(color, Offset(9.dp.toPx(), 6.dp.toPx()), Offset(15.dp.toPx(), 12.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(15.dp.toPx(), 12.dp.toPx()), Offset(9.dp.toPx(), 18.dp.toPx()), stroke.width, StrokeCap.Round)
            }
            FinanceGlyph.Back -> {
                drawLine(color, Offset(15.dp.toPx(), 6.dp.toPx()), Offset(9.dp.toPx(), 12.dp.toPx()), stroke.width, StrokeCap.Round)
                drawLine(color, Offset(9.dp.toPx(), 12.dp.toPx()), Offset(15.dp.toPx(), 18.dp.toPx()), stroke.width, StrokeCap.Round)
            }
            FinanceGlyph.Empty -> {
                drawRoundRect(color, Offset(4.dp.toPx(), 5.dp.toPx()), Size(16.dp.toPx(), 15.dp.toPx()), CornerRadius(4.dp.toPx()), style = stroke)
                drawLine(color, Offset(8.dp.toPx(), 12.dp.toPx()), Offset(16.dp.toPx(), 12.dp.toPx()), stroke.width, StrokeCap.Round)
            }
        }
    }
}

@Composable
fun KpiCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val glyph = when {
        label.contains("pemasukan", ignoreCase = true) || label.contains("income", ignoreCase = true) -> FinanceGlyph.Income
        label.contains("pengeluaran", ignoreCase = true) || label.contains("expense", ignoreCase = true) -> FinanceGlyph.Expense
        label.contains("saldo", ignoreCase = true) -> FinanceGlyph.Wallet
        else -> FinanceGlyph.Report
    }
    PremiumPanel(modifier = modifier) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.surface, accent.copy(alpha = 0.035f)),
                    ),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlyphBadge(glyph = glyph, color = accent, size = 32.dp)
                Text(
                    label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    PremiumPanel(modifier = modifier) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlyphBadge(FinanceGlyph.Empty, PremiumEmerald, size = 38.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun TypeFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) Brush.linearGradient(listOf(PremiumEmerald, PremiumEmerald))
                else Brush.linearGradient(listOf(colors.surface, colors.surface)),
                RoundedCornerShape(8.dp),
            )
            .border(
                1.dp,
                if (selected) PremiumEmerald.copy(alpha = 0.86f) else colors.outline.copy(alpha = 0.72f),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.White else colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MiniIncomeExpenseChart(income: Long, expense: Long, modifier: Modifier = Modifier) {
    val max = maxOf(income, expense, 1L).toFloat()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ChartRow("Pemasukan", income, max, PremiumIncome)
        ChartRow("Pengeluaran", expense, max, PremiumExpense)
    }
}

@Composable
private fun ChartRow(label: String, value: Long, max: Float, color: Color) {
    val percent = ((value.toFloat() / max).coerceIn(0f, 1f) * 100).toInt()
    val progress = if (value <= 0) 0f else (value / max).coerceIn(0.04f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(8.dp).background(color, RoundedCornerShape(8.dp)))
                Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
            }
            Text("$percent%", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp),
        ) {
            val corner = 7.dp.toPx()
            drawRoundRect(
                color = color.copy(alpha = 0.10f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(corner, corner),
            )
            if (progress > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.82f))),
                    size = Size(size.width * progress, size.height),
                    cornerRadius = CornerRadius(corner, corner),
                )
            }
        }
    }
}

@Composable
fun CategoryTotalBars(items: List<CategoryTotal>, currency: String, modifier: Modifier = Modifier) {
    val max = items.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items.forEachIndexed { index, item ->
            val color = item.category?.color?.toColorOrNull() ?: PremiumEmerald
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(color.copy(alpha = 0.13f), RoundedCornerShape(8.dp))
                        .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.category?.name ?: "Tanpa kategori", fontWeight = FontWeight.SemiBold)
                        Text(formatMoney(item.amount, currency), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((item.amount.toFloat() / max.toFloat()).coerceIn(0.05f, 1f))
                                .height(8.dp)
                                .background(color, RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    item: EnrichedTransaction,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isIncome = item.transaction.type == "income"
    val isTransfer = item.transaction.type == "transfer"
    val amountColor = when {
        isIncome -> PremiumIncome
        isTransfer -> PremiumGoldDark
        else -> PremiumExpense
    }
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.58f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlyphBadge(
                glyph = when {
                    isIncome -> FinanceGlyph.Income
                    isTransfer -> FinanceGlyph.Wallet
                    else -> FinanceGlyph.Expense
                },
                color = amountColor,
                size = 34.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    when {
                        isTransfer -> "Transfer"
                        isIncome -> item.category?.name ?: "Pemasukan"
                        else -> item.category?.name ?: "Pengeluaran"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (isTransfer) {
                        listOfNotNull(
                            item.cashAccount?.name,
                            item.targetCashAccount?.name?.let { "ke $it" },
                            formatDate(item.transaction.date),
                        ).joinToString(" - ")
                    } else {
                        listOfNotNull(
                            formatDate(item.transaction.date),
                            item.cashAccount?.name,
                        ).joinToString(" - ")
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                modifier = Modifier.widthIn(max = 132.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "${if (isIncome) "+" else if (isTransfer) "" else "-"}${formatMoney(item.transaction.amount, currency)}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun HeroBand(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(PremiumEmeraldDeep, Color(0xFF00453E), Color(0xFF082F2B)),
                ),
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, PremiumGold.copy(alpha = 0.85f), RoundedCornerShape(8.dp)),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val lineColor = PremiumGold.copy(alpha = 0.12f)
            val strokeWidth = 1.dp.toPx()
            repeat(14) { index ->
                val startX = size.width * 0.62f + index * 17.dp.toPx()
                val path = Path().apply {
                    moveTo(startX, -18.dp.toPx())
                    cubicTo(
                        startX - size.width * 0.16f,
                        size.height * 0.35f,
                        startX + size.width * 0.02f,
                        size.height * 0.62f,
                        startX - size.width * 0.08f,
                        size.height + 24.dp.toPx(),
                    )
                }
                drawPath(path = path, color = lineColor, style = Stroke(width = strokeWidth))
            }
            drawCircle(
                color = PremiumGold.copy(alpha = 0.12f),
                radius = size.width * 0.28f,
                center = Offset(size.width * 0.84f, size.height * 0.02f),
            )
        }
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BrandMark()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.bodyMedium)
                }
                trailing?.invoke()
            }
            content()
        }
    }
}

private fun monthLabelShort(): String = "Mei 2026"

fun String.toColorOrNull(): Color? =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrNull()
