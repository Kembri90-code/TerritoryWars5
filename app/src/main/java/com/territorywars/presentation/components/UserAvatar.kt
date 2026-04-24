package com.territorywars.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.territorywars.presentation.theme.PlusJakartaSans

private const val BASE_URL = "http://93.183.74.141"

@Composable
fun UserAvatar(
    username: String,
    avatarUrl: String?,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    borderModifier: Modifier = Modifier,
) {
    val fullUrl = remember(avatarUrl) {
        when {
            avatarUrl.isNullOrBlank() -> null
            avatarUrl.startsWith("http") -> avatarUrl
            else -> "$BASE_URL$avatarUrl"
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.2f))
            .then(borderModifier),
    ) {
        if (fullUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(fullUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = username,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                content = {
                    when (painter.state) {
                        is coil.compose.AsyncImagePainter.State.Success ->
                            SubcomposeAsyncImageContent(contentScale = ContentScale.Crop)
                        else ->
                            Initials(username = username, color = color, size = size)
                    }
                }
            )
        } else {
            Initials(username = username, color = color, size = size)
        }
    }
}

@Composable
private fun Initials(username: String, color: Color, size: Dp) {
    Text(
        text = username.take(2).uppercase(),
        fontSize = (size.value * 0.33f).sp,
        fontWeight = FontWeight.Bold,
        fontFamily = PlusJakartaSans,
        color = color,
    )
}
