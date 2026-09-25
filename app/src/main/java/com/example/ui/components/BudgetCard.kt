package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.BudgetEntity
import com.example.ui.theme.BudgetExceededBgDark
import com.example.ui.theme.BudgetExceededBgLight
import com.example.ui.theme.BudgetExceededRed
import com.example.ui.theme.BudgetNearBgDark
import com.example.ui.theme.BudgetNearBgLight
import com.example.ui.theme.BudgetNearOrange
import com.example.ui.theme.BudgetSafeBgDark
import com.example.ui.theme.BudgetSafeBgLight
import com.example.ui.theme.BudgetSafeGreen
import com.example.util.CurrencyFormatter

enum class BudgetStatus {
    WITHIN_LIMIT,
    NEAR_LIMIT,
    OVER_LIMIT
}

@Composable
fun BudgetCard(
    budget: BudgetEntity,
    currentSpent: Double,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val limit = budget.monthlyLimit
    val percentage = if (limit > 0) (currentSpent / limit) * 100.0 else 0.0

    val status = when {
        percentage > 100.0 -> BudgetStatus.OVER_LIMIT
        percentage >= budget.warningThresholdPercent -> BudgetStatus.NEAR_LIMIT
        else -> BudgetStatus.WITHIN_LIMIT
    }

    // Dynamic background colors as strictly requested
    val (backgroundColor, accentColor, statusLabel) = when (status) {
        BudgetStatus.OVER_LIMIT -> Triple(
            if (isDark) BudgetExceededBgDark else BudgetExceededBgLight,
            BudgetExceededRed,
            "OVER BUDGET"
        )
        BudgetStatus.NEAR_LIMIT -> Triple(
            if (isDark) BudgetNearBgDark else BudgetNearBgLight,
            BudgetNearOrange,
            "NEAR LIMIT"
        )
        BudgetStatus.WITHIN_LIMIT -> Triple(
            if (isDark) BudgetSafeBgDark else BudgetSafeBgLight,
            BudgetSafeGreen,
            "ON TRACK"
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_card_${budget.category}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = budget.category,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Monthly Budget",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("delete_budget_${budget.category}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatINR(currentSpent),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = accentColor
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Utilization",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.1f", percentage)}%",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (status == BudgetStatus.OVER_LIMIT) "Exceeded By" else "Remaining",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val diff = kotlin.math.abs(limit - currentSpent)
                    Text(
                        text = CurrencyFormatter.formatINR(diff),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (status == BudgetStatus.OVER_LIMIT) BudgetExceededRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
