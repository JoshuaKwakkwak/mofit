package com.mofit.app.service

import android.graphics.PointF
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.common.PointF3D
import kotlin.math.acos
import kotlin.math.sqrt

data class JointPoint(val name: String, val x: Float, val y: Float, val z: Float, val confidence: Float)
data class PoseResult(val joints: List<JointPoint>, val repCount: Int, val isWristRaised: Boolean, val squatPhase: SquatPhase)
enum class SquatPhase { STANDING, DESCENDING, BOTTOM, ASCENDING }

class PoseAnalyzer(private val onResult: (PoseResult) -> Unit) : ImageAnalysis.Analyzer {

    private val detector: PoseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
    )
    private var repCount: Int = 0
    private var squatPhase: SquatPhase = SquatPhase.STANDING
    private var wristRaisedStartTime: Long = 0L
    private var isWristCurrentlyRaised: Boolean = false
    val wristRaiseThresholdMs: Long = 1000L

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(inputImage)
            .addOnSuccessListener { pose ->
                processPose(pose)
                imageProxy.close()
            }
            .addOnFailureListener {
                imageProxy.close()
            }
    }

    fun processPose(pose: Pose) {
        val joints = pose.allPoseLandmarks.map { landmark ->
            JointPoint(
                name = landmark.landmarkType.toString(),
                x = landmark.position3D.x,
                y = landmark.position3D.y,
                z = landmark.position3D.z,
                confidence = landmark.inFrameLikelihood
            )
        }

        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val hip = if ((leftHip?.inFrameLikelihood ?: 0f) >= 0.5f) leftHip else rightHip
        val knee = if ((leftKnee?.inFrameLikelihood ?: 0f) >= 0.5f) leftKnee else rightKnee
        val ankle = if ((leftAnkle?.inFrameLikelihood ?: 0f) >= 0.5f) leftAnkle else rightAnkle

        if (hip != null && knee != null && ankle != null &&
            hip.inFrameLikelihood > 0.5f && knee.inFrameLikelihood > 0.5f && ankle.inFrameLikelihood > 0.5f
        ) {
            val angle = angleBetween(hip.position3D, knee.position3D, ankle.position3D)
            squatPhase = when (squatPhase) {
                SquatPhase.STANDING -> if (angle < 140f) SquatPhase.DESCENDING else SquatPhase.STANDING
                SquatPhase.DESCENDING -> if (angle < 100f) SquatPhase.BOTTOM else SquatPhase.DESCENDING
                SquatPhase.BOTTOM -> if (angle > 120f) SquatPhase.ASCENDING else SquatPhase.BOTTOM
                SquatPhase.ASCENDING -> if (angle > 155f) { repCount++; SquatPhase.STANDING } else SquatPhase.ASCENDING
            }
        }

        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val wrist = if ((leftWrist?.inFrameLikelihood ?: 0f) >= (rightWrist?.inFrameLikelihood ?: 0f)) leftWrist else rightWrist
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)

        if (wrist != null && nose != null) {
            val isRaised = wrist.position.y < nose.position.y
            if (isRaised) {
                if (wristRaisedStartTime == 0L) wristRaisedStartTime = System.currentTimeMillis()
                val held = System.currentTimeMillis() - wristRaisedStartTime
                isWristCurrentlyRaised = held >= wristRaiseThresholdMs
            } else {
                wristRaisedStartTime = 0L
                isWristCurrentlyRaised = false
            }
        }

        onResult(PoseResult(joints, repCount, isWristCurrentlyRaised, squatPhase))
    }

    fun resetReps() {
        repCount = 0
        squatPhase = SquatPhase.STANDING
        wristRaisedStartTime = 0L
        isWristCurrentlyRaised = false
    }

    fun close() {
        detector.close()
    }

    private fun angleBetween(a: PointF3D, vertex: PointF3D, b: PointF3D): Float {
        val v1x = a.x - vertex.x; val v1y = a.y - vertex.y
        val v2x = b.x - vertex.x; val v2y = b.y - vertex.y
        val dot = v1x * v2x + v1y * v2y
        val mag1 = sqrt((v1x * v1x + v1y * v1y).toDouble())
        val mag2 = sqrt((v2x * v2x + v2y * v2y).toDouble())
        if (mag1 == 0.0 || mag2 == 0.0) return 180f
        return Math.toDegrees(acos((dot / (mag1 * mag2)).coerceIn(-1.0, 1.0))).toFloat()
    }
}
