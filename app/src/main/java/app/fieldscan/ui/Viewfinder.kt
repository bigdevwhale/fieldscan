package app.fieldscan.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.fieldscan.render.TrailCam

/**
 * The trail camera's viewfinder at the end of a rite: the real place through the phone's camera, grey and
 * green like the IR sensor, a pair of eyes somewhere low in it. The shutter hands the frame (W × H) on;
 * without a camera the drawn scene is used instead ([onShot] with null).
 */
@Composable
fun Viewfinder(toPlace: Double?, reach: Double, onShot: (Bitmap?) -> Unit) {
    val ru = LocalRu.current
    // the frame is of the place itself: away from it the shutter waits
    val away = toPlace != null && toPlace > reach
    val ctx = LocalContext.current
    var allowed by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var asked by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it; asked = true }
    val hasCamera = remember { ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    Column(Modifier.fillMaxWidth().border(2.dp, C.red).padding(9.dp)) {
        T(tr("Фотоловушка", "Trail camera"), Ty.head)
        T(tr("Обряд пройден — оно здесь. Наведи камеру на место и снимай: существо проявится на настоящем кадре.",
            "The rite is done: it's here. Point the camera at the place and shoot: the creature will come out in a real frame."), Ty.small)
        if (away) {
            Gap(4.dp)
            T(tr("Ты отошёл: до места ", "You've walked off: the place is ") + Words.dist(toPlace!!, ru) + tr(". Вернись, чтобы снять.", " away. Come back to shoot."), Ty.small.copy(color = C.red))
        }
        Gap(8.dp)
        if (!hasCamera) {
            T(tr("Камеры нет — прибор нарисует место сам.", "No camera: the device will draw the place itself."), Ty.small)
            Gap(6.dp); Btn(tr("Снять", "Shoot"), red = true, enabled = !away) { onShot(null) }
        } else if (!allowed) {
            T(tr("Нужна камера: снимок остаётся только в телефоне.", "The camera is needed: the frame stays on the phone only."), Ty.small)
            Gap(6.dp); Btn(tr("Разрешить камеру", "Allow the camera"), red = true) { ask.launch(Manifest.permission.CAMERA) }
            if (asked) { Gap(6.dp); Btn(tr("Снять без камеры", "Shoot without the camera"), outline = true, enabled = !away) { onShot(null) } }
        } else Live(away, onShot)
    }
}

@Composable
private fun Live(away: Boolean, onShot: (Bitmap?) -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = remember {
        PreviewView(ctx).apply {
            // a TextureView, so the shutter can take the picture straight from the preview
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // the IR sensor sees no colour: grey with a green cast, on the camera's own layer so every phone shows it
            val m = android.graphics.ColorMatrix().apply { setSaturation(0f) }
            m.postConcat(android.graphics.ColorMatrix(floatArrayOf(.88f, 0f, 0f, 0f, 6f, 0f, .95f, 0f, 0f, 10f, 0f, 0f, .84f, 0f, 4f, 0f, 0f, 0f, 1f, 0f)))
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(m) })
        }
    }
    var failed by remember { mutableStateOf(false) }
    DisposableEffect(owner) {
        val cam = LifecycleCameraController(ctx)
        runCatching {
            cam.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cam.bindToLifecycle(owner)
            view.controller = cam
        }.onFailure { failed = true }
        onDispose { runCatching { cam.unbind(); view.controller = null } }
    }
    // the eyes glow low in the middle: that's where the creature will stand in the frame
    val ex = .5f
    val ey = .62f
    var notReady by remember { mutableStateOf(false) }
    val t = rememberInfiniteTransition(label = "eyes")
    val glow by t.animateFloat(.35f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse), label = "g")
    Box(Modifier.fillMaxWidth().aspectRatio(TrailCam.W / TrailCam.H.toFloat()).border(2.dp, C.ink).clipToBounds()) {
        AndroidView({ view }, Modifier.fillMaxSize())
        // the IR sensor: grey-green wash, dark corners, the eyes
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0x22000000))
            drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xAA000000)), center = center, radius = size.maxDimension * .62f))
            val p = Offset(size.width * ex, size.height * ey)
            val r = size.minDimension * .022f
            for (dx in listOf(-r * 1.6f, r * 1.6f)) {
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = glow), Color(0x00E8F4E2)), center = p + Offset(dx, 0f), radius = r * 2.4f), r * 2.4f, p + Offset(dx, 0f))
            }
            drawRect(Color.Black, Offset(0f, size.height - 18.dp.toPx()), Size(size.width, 18.dp.toPx()))
        }
        T("● REC  IR", Ty.small.copy(color = Color(0xFFE6E6DE)), Modifier.padding(start = 8.dp, top = 6.dp))
    }
    Gap(8.dp)
    if (failed) {
        T(tr("Камера не открылась — прибор нарисует место сам.", "The camera didn't open: the device will draw the place itself."), Ty.small.copy(color = C.red))
        Gap(6.dp); Btn(tr("Снять", "Shoot"), red = true, enabled = !away) { onShot(null) }
    } else {
        if (notReady) { T(tr("Камера ещё не проснулась — секунду и снимай снова.", "The camera isn't awake yet: a second, then shoot again."), Ty.small.copy(color = C.red)); Gap(4.dp) }
        Btn(tr("Снять", "Shoot"), red = true, enabled = !away) {
            // the picture straight off the preview; none yet means the camera is still starting, not "draw it instead"
            val b = view.bitmap
            if (b == null) notReady = true else onShot(TrailCam.crop(b))
        }
        Gap(6.dp)
        Btn(tr("Снять без камеры", "Shoot without the camera"), outline = true, enabled = !away) { onShot(null) }
    }
}
