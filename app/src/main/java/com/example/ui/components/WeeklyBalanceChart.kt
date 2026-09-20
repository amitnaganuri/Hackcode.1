package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeeklyBalanceDay
import com.example.ui.theme.FocusOnSurface
import com.example.ui.theme.FocusOnSurfaceVariant
import com.example.ui.theme.FocusPrimary
import com.example.ui.theme.FocusSurfaceContainer
import com.example.ui.theme.FocusSurfaceVariant

@Composable
fun WeeklyBalanceChart(
    days: List<WeeklyBalanceDay>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusSurfaceContainer)
            .padding(16.dp)
    ) {
        // Header & Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weekly Balance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FocusOnSurface
                )
                Text(
                    text = "Focus vs Impulse Traps",
                    fontSize = 12.sp,
                    color = FocusOnSurfaceVariant
                )
            }

            // Legend
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FocusPrimary)
                    )
                    Text(
                        text = "Focus",
                        fontSize = 12.sp,
                        color = FocusOnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FocusSurfaceVariant)
                    )
                    Text(
                        text = "Impulse",
                        fontSize = 12.sp,
                        color = FocusOnSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bars Container (height 120dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Two side-by-side bars
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.height(100.dp)
                    ) {
                        // Focus Bar
                        Box(
                            modifier = Modifier
                                .width(9.dp)
                                .fillMaxHeight(day.focusFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (day.isHighlighted) FocusPrimary
                                    else FocusPrimary.copy(alpha = 0.85f)
                                )
                        )

                        // Impulse Bar
                        Box(
                            modifier = Modifier
                                .width(9.dp)
                                .fillMaxHeight(day.impulseFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(FocusSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = day.dayLabel,
                        fontSize = 12.sp,
                        fontWeight = if (day.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        color = if (day.isHighlighted) FocusPrimary else FocusOnSurfaceVariant
                    )
                }
            }
        }
    }
}
