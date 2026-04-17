package com.mofit.app.ui.tracking

import android.Manifest
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mofit.app.service.JointPoint
import com.mofit.app.service.PoseAnalyzer
import java.util.concurrent.Executors

private fun findJoint(joints: List<JointPoint>, type: Int): JointPoint? =
    joints.firstOrNull { it.name.contains(type.toString()) && it.confidence > 0.5 }

@Composable
fun JointOverlay(joints: List<JointPoint>, modifier: Modifier = Modifier) {
    val connections = listOf(
        11 to 13, 13 to 15, 12 to 14, 14 to 16,
        23 to 25, 25 to 27, 24 to 26, 26 to 28,
        11 to 12, 23 to 24
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        connections.forEach { (a, b) ->
            val jA = findJoint(joints, a)
            val jB = findJoint(joints, b)
            if (jA != null && jB != null) {
                drawLine(
                    color = Color.White,
                    start = Offset(jA.x * w / 480f, jA.y * h / 640f),
                    end = Offset(jB.x * w / 480f, jB.y * h / 640f),
                    strokeWidth = 3f
                )
            }
        }
        joints.filter { it.confidence > 0.5 }.forEach { joint ->
            drawCircle(
                color = Color(0xFF33CC66),
                radius = 8f,
                center = Offset(joint.x * w / 480f, joint.y * h / 640f)
            )
        }
    }
}

@Composable
fun TrackingScreen(onClose: () -> Unit, viewModel: TrackingViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val poseAnalyzer = remember { PoseAnalyzer { result -> viewModel.onPoseResult(result) } }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val preview = remember { Preview.Builder().build() }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
        viewModel.startSession()
    }

    if (hasCameraPermission) {
        DisposableEffect(Unit) {
            val cameraProvider = cameraProviderFuture.get()
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), poseAnalyzer)
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            onDispose {
                cameraProvider.unbindAll()
                poseAnalyzer.close()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { pv ->
                    preview.setSurfaceProvider(pv.surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (viewModel.phase == WorkoutPhase.Tracking) {
            JointOverlay(joints = viewModel.currentJoints)
        }

        when (val phase = viewModel.phase) {
            WorkoutPhase.PalmDetection -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "손목을 머리 위로 올려주세요",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "1초 유지",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    if (viewModel.wristHoldProgress > 0f) {
                        CircularProgressIndicator(
                            color = Color(0xFF33CC66),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            is WorkoutPhase.Countdown -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = phase.seconds, label = "countdown") { seconds ->
                        Text(
                            text = "$seconds",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 80.sp
                        )
                    }
                }
            }
            WorkoutPhase.Tracking -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Set ${viewModel.completedSets.size + 1}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.endWorkout() }) {
                            Text("종료", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${viewModel.currentRepCount}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 64.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "손목을 올리면 다음 세트",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            WorkoutPhase.Finished -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "운동 완료!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                        Text(
                            text = "총 ${viewModel.totalReps}회 · ${viewModel.completedSets.size}세트",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Button(
                            onClick = { viewModel.saveAndClose(onClose) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF33CC66)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 16.dp)
                        ) {
                            Text("저장하고 닫기", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            WorkoutPhase.Idle -> {}
        }
    }
}
