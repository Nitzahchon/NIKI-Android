package com.niki.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF10172A)
private val Purple = Color(0xFF7755DD)
private val LightPurple = Color(0xFFE9E2FF)
private val Background = Color(0xFFF6F5FA)

data class Studio(
    val name: String,
    val icon: String,
    val description: String
)

private val studios = listOf(
    Studio("Voice Studio", "🎙️", "Record and transform your voice"),
    Studio("Subtitle Studio", "✍️", "Create and animate your subtitles"),
    Studio("LipSync Studio", "👄", "Bring photographs to life"),
    Studio("BodyMotion Studio", "🕺", "Create gestures and body movements"),
    Studio("Video Studio", "🎬", "Combine your creations into videos")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                NikiApp()
            }
        }
    }
}

@Composable
fun NikiApp() {
    var selectedStudio by remember {
        mutableStateOf<Studio?>(null)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        if (selectedStudio == null) {
            HomeScreen(
                onStudioClick = { selectedStudio = it }
            )
        } else {
            StudioScreen(
                studio = selectedStudio!!,
                onBack = { selectedStudio = null }
            )
        }
    }
}

@Composable
fun HomeScreen(
    onStudioClick: (Studio) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Navy, Purple)
                    )
                )
                .padding(26.dp)
        ) {
            Text(
                text = "NIKI",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = "Your Voice. Your Story. Your Victory.",
                fontSize = 15.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Create something amazing.",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Your creativity starts here.",
                fontSize = 14.sp,
                color = LightPurple
            )
        }

        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "MY STUDIOS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Purple
            )

            Spacer(modifier = Modifier.height(14.dp))

            studios.forEach { studio ->
                StudioCard(
                    studio = studio,
                    onClick = { onStudioClick(studio) }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Made with NIKI ❤️",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun StudioCard(
    studio: Studio,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = studio.icon,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = studio.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )

                Text(
                    text = studio.description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = "›",
                fontSize = 28.sp,
                color = Purple
            )
        }
    }
}

@Composable
fun StudioScreen(
    studio: Studio,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = Purple)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = studio.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
        }

        Spacer(modifier = Modifier.height(65.dp))

        Text(
            text = studio.icon,
            fontSize = 65.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = studio.name,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Navy
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = studio.description,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Your creative workspace is being prepared.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            color = Purple
        )
    }
}
