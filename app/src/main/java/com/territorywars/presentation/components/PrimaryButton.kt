package com.territorywars.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.territorywars.presentation.theme.PlusJakartaSans

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryDim = MaterialTheme.colorScheme.primaryContainer
    val outline = MaterialTheme.colorScheme.outline
    val onSurfVar = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(14.dp)
    val isActive = enabled && !isLoading

    val gradient = if (isActive)
        Brush.linearGradient(listOf(primary, primaryDim))
    else
        Brush.linearGradient(listOf(outline, outline))

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(gradient)
            .clickable(enabled = isActive, onClick = onClick),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = onSurfVar,
                strokeWidth = 2.dp,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = if (isActive) Color.White else onSurfVar,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = PlusJakartaSans,
                    letterSpacing = 0.2.sp,
                )
            }
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    enabled: Boolean = true,
) {
    val errorColor = MaterialTheme.colorScheme.error
    val errorCont = MaterialTheme.colorScheme.errorContainer
    val outline = MaterialTheme.colorScheme.outline
    val onBg = MaterialTheme.colorScheme.onBackground
    val shape = RoundedCornerShape(14.dp)

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor      = if (danger) errorColor else onBg,
            containerColor    = if (danger) errorCont else Color.Transparent,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = if (danger) errorColor.copy(alpha = 0.5f) else outline,
        ),
        modifier = modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PlusJakartaSans,
        )
    }
}
