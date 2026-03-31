package com.example.onitama.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.onitama.R

val Quattrocento = FontFamily(
    Font(R.font.quattrocento_regular, FontWeight.Normal),
    Font(R.font.quattrocento_bold, FontWeight.Bold)
)
val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = Quattrocento,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),

    labelMedium = TextStyle(
        fontFamily = Quattrocento,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp
    ),

    labelLarge = TextStyle(
        fontFamily = Quattrocento,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp
    )
)