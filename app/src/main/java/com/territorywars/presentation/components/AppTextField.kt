package com.territorywars.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.territorywars.presentation.theme.PlusJakartaSans

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    error: String? = null,
    hint: String? = null,
    isValid: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val hasValue = value.isNotEmpty()
    val lifted = isFocused || hasValue

    val primary    = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val outline    = MaterialTheme.colorScheme.outline
    val bgAlt      = MaterialTheme.colorScheme.surfaceContainerLow
    val onBg       = MaterialTheme.colorScheme.onBackground
    val onSurfVar  = MaterialTheme.colorScheme.onSurfaceVariant

    val borderColor by animateColorAsState(
        targetValue = when {
            error != null -> errorColor.copy(alpha = 0.8f)
            isFocused     -> primary.copy(alpha = 0.9f)
            else          -> outline
        },
        animationSpec = tween(180),
        label = "border",
    )
    val labelColor by animateColorAsState(
        targetValue = when {
            error != null -> errorColor
            isFocused     -> primary
            else          -> onSurfVar
        },
        animationSpec = tween(180),
        label = "label",
    )
    val iconColor by animateColorAsState(
        targetValue = if (isFocused) primary else onSurfVar,
        animationSpec = tween(180),
        label = "icon",
    )
    // Label font size: small when lifted, normal when idle
    val labelFontSize by animateFloatAsState(
        targetValue = if (lifted) 10f else 15f,
        animationSpec = tween(180),
        label = "labelSize",
    )
    // Label Y offset: near top when lifted, vertically centred when idle
    val labelOffsetY by animateDpAsState(
        targetValue = if (lifted) 6.dp else 16.dp,
        animationSpec = tween(180),
        label = "labelOffset",
    )

    val shape = RoundedCornerShape(14.dp)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bgAlt)
                .border(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = borderColor,
                    shape = shape,
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                // Fixed-height box so label and input never overlap
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    // Floating label — moves via offset animation
                    Text(
                        text = if (lifted) label.uppercase() else label,
                        color = labelColor,
                        fontSize = labelFontSize.sp,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = if (lifted) 0.8.sp else 0.sp,
                        modifier = Modifier.offset(y = labelOffsetY),
                    )

                    // Input field — always anchored to the bottom of the box
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = true,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = if (isPassword && !passwordVisible)
                            PasswordVisualTransformation() else VisualTransformation.None,
                        textStyle = TextStyle(
                            color = onBg,
                            fontSize = 15.sp,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Normal,
                        ),
                        cursorBrush = SolidColor(primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .padding(bottom = 8.dp)
                            .onFocusChanged { isFocused = it.isFocused },
                    )
                }

                // Trailing: password toggle or custom
                when {
                    trailingContent != null -> trailingContent()
                    isPassword -> {
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff
                                              else Icons.Outlined.Visibility,
                                contentDescription = null,
                                tint = onSurfVar,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        // Error / hint caption
        val caption = error ?: hint
        if (caption != null) {
            Text(
                text = caption,
                fontSize = 12.sp,
                fontFamily = PlusJakartaSans,
                color = if (error != null) errorColor else onSurfVar,
                modifier = Modifier.padding(start = 14.dp, top = 5.dp),
            )
        }
    }
}
