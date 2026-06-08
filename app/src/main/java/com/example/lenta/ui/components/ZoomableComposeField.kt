package com.example.lenta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ZoomableComposeField(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Состояние трансформации
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Фон поля
            .clipToBounds() // Обрезаем контент по границам экрана
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(0.5f, 5f) // Ограничение зума
                    
                    // Математика корректного смещения относительно точки касания (centroid)
                    val actualZoom = scale / oldScale
                    offset = (offset * actualZoom) + pan - (centroid * (actualZoom - 1f))
                }
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    transformOrigin = TransformOrigin(0f, 0f)
                )
        ) {
            content()
        }
    }
}

@Composable
fun ZoomableComposeFieldExample() {
    ZoomableComposeField {
        // ПРИМЕР: Сетка для визуализации масштаба
        repeat(10) { x ->
            repeat(20) { y ->
                Box(
                    modifier = Modifier
                        .offset(x = (x * 200).dp, y = (y * 200).dp)
                        .size(195.dp)
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }
        }

        // ПРИМЕР: Интерактивный контент (стандартные кнопки)
        Button(
            onClick = { /* Логика нажатия */ },
            modifier = Modifier.offset(150.dp, 150.dp)
        ) {
            Text("Рабочая кнопка")
        }
    }
}
