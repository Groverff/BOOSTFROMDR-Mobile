package com.boostfromdr.mobile

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt
import kotlin.math.max

private val Bg = Color(0xFF080D14)
private val Panel = Color(0xFF111B27)
private val Cyan = Color(0xFF24D8E8)
private val Muted = Color(0xFF91A4B7)
private val Green = Color(0xFF55D6A0)

data class MotionSample(
    val rawX: Float = 0f, val rawY: Float = 0f, val rawZ: Float = 0f,
    val filteredX: Float = 0f, val filteredY: Float = 0f, val filteredZ: Float = 0f,
    val jitter: Float = 0f, val sensorAvailable: Boolean = false,
    val sampleRateHz: Float = 0f, val status: String = "Waiting for sensor"
)

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var gyro: Sensor? = null
    private var lastTimestamp = 0L
    private var sampleCount = 0
    private var rateStart = 0L

    private val motion = mutableStateOf(MotionSample())
    private val recentJitter = ArrayDeque<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        motion.value = motion.value.copy(
            sensorAvailable = gyro != null,
            status = if (gyro != null) "Gyroscope ready" else "Gyroscope not available"
        )

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Cyan,
                    background = Bg,
                    surface = Panel,
                    onPrimary = Bg,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                BoostApp(motion.value, onReset = { resetCalibration() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gyro?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        val now = event.timestamp
        val dt = if (lastTimestamp == 0L) 1f / 60f
        else ((now - lastTimestamp) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        lastTimestamp = now

        val raw = event.values
        val filtered = try {
            NativeFilter.process(raw[0], raw[1], raw[2], dt)
        } catch (_: Throwable) {
            floatArrayOf(raw[0], raw[1], raw[2])
        }

        val dx = raw[0] - filtered[0]
        val dy = raw[1] - filtered[1]
        val dz = raw[2] - filtered[2]
        val jitter = sqrt(dx * dx + dy * dy + dz * dz)

        recentJitter.addLast(jitter)
        while (recentJitter.size > 120) recentJitter.removeFirst()

        sampleCount++
        if (rateStart == 0L) rateStart = now
        var rate = motion.value.sampleRateHz
        val elapsed = now - rateStart
        if (elapsed >= 1_000_000_000L) {
            rate = sampleCount * 1_000_000_000f / elapsed
            sampleCount = 0
            rateStart = now
        }

        motion.value = MotionSample(
            rawX = raw[0], rawY = raw[1], rawZ = raw[2],
            filteredX = filtered[0], filteredY = filtered[1], filteredZ = filtered[2],
            jitter = if (recentJitter.isEmpty()) 0f else recentJitter.average().toFloat(),
            sensorAvailable = true,
            sampleRateHz = rate,
            status = "Live sensor stream"
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun resetCalibration() {
        NativeFilter.reset()
        recentJitter.clear()
        lastTimestamp = 0L
        motion.value = motion.value.copy(
            rawX = 0f, rawY = 0f, rawZ = 0f,
            filteredX = 0f, filteredY = 0f, filteredZ = 0f,
            jitter = 0f,
            status = "Filter reset — keep phone still"
        )
    }
}

@Composable
private fun BoostApp(data: MotionSample, onReset: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Monitor") }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg)
            .verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("BOOSTFROMDR", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("MOBILE  /  DELTA FORCE EDITION", color = Cyan, fontSize = 10.sp, letterSpacing = 1.5.sp)
            }
            Box(Modifier.size(11.dp).background(if (data.sensorAvailable) Green else Color.Red, RoundedCornerShape(50)))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Monitor", "Training", "Profiles").forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = { Text(tab) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Cyan,
                        selectedLabelColor = Bg,
                        labelColor = Muted
                    )
                )
            }
        }

        when (selectedTab) {
            "Monitor" -> MonitorScreen(data, onReset)
            "Training" -> TrainingScreen()
            else -> ProfilesScreen()
        }

        Text(
            "Standalone sensor analysis • No game injection • No root",
            color = Muted, fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
        )
    }
}

@Composable
private fun MonitorScreen(data: MotionSample, onReset: () -> Unit) {
    SectionCard("SENSOR STATUS") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(data.status, color = if (data.sensorAvailable) Green else Color.Red)
            Text(if (data.sensorAvailable) "CONNECTED" else "UNAVAILABLE", color = Cyan, fontSize = 10.sp)
        }
        Text("Measured sampling: ${"%.1f".format(data.sampleRateHz)} Hz", color = Muted, fontSize = 12.sp)
    }

    SectionCard("LIVE GYROSCOPE") {
        AxisLine("X", data.rawX, data.filteredX)
        AxisLine("Y", data.rawY, data.filteredY)
        AxisLine("Z", data.rawZ, data.filteredZ)
        Text("Raw values are in rad/s. Filtered values are processed locally.", color = Muted, fontSize = 10.sp)
    }

    SectionCard("JITTER ANALYSIS") {
        Text("%.5f".format(data.jitter), color = Cyan, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Mean raw-to-filtered difference (rad/s)", color = Muted, fontSize = 11.sp)
        Text("This is a preliminary indicator, not a validated measure of in-game aim stability.", color = Muted, fontSize = 10.sp)
    }

    Button(
        onClick = onReset,
        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Bg),
        modifier = Modifier.fillMaxWidth()
    ) { Text("RESET FILTER / CALIBRATION") }
}

@Composable
private fun AxisLine(axis: String, raw: Float, filtered: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("AXIS $axis", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("Raw ${"%.3f".format(raw)}  |  Filtered ${"%.3f".format(filtered)}", color = Muted, fontSize = 10.sp)
        }
        val scale = max(0.05f, max(kotlin.math.abs(raw), kotlin.math.abs(filtered)))
        Canvas(Modifier.fillMaxWidth().height(18.dp)) {
            val mid = size.width / 2f
            drawLine(Color(0xFF344454), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2f)
            drawLine(Color(0xFF536A7F), Offset(mid, 0f), Offset(mid, size.height), 1f)
            val rawX = mid + (raw / scale) * (size.width * 0.42f)
            val filteredX = mid + (filtered / scale) * (size.width * 0.42f)
            drawLine(Color(0xFF71879B), Offset(mid, 5f), Offset(rawX, 5f), 4f)
            drawLine(Cyan, Offset(mid, 13f), Offset(filteredX, 13f), 4f)
        }
    }
}

@Composable
private fun TrainingScreen() {
    SectionCard("TRACKING LAB — PREVIEW") {
        Text("A self-contained practice module will measure touch tracking inside this app.", color = Color.White)
        Text("Planned tests:", color = Cyan, fontWeight = FontWeight.Bold)
        listOf("Slow target tracking", "Fast direction changes", "Fine movement control", "Overshoot and recovery analysis").forEach {
            Text("• $it", color = Muted, fontSize = 12.sp)
        }
        Text("No game screen capture or automated aiming is used.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun ProfilesScreen() {
    SectionCard("DELTA FORCE PROFILE") {
        Text("Profile status: Not calibrated", color = Cyan, fontWeight = FontWeight.Bold)
        Text("V1.0 will store suggested sensitivity notes and sensor calibration results locally. It will not write settings into the game.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            content()
        }
    }
}
