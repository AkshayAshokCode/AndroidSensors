package com.akshayAshokCode.androidsensors.presentation.fragments

import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.akshayAshokCode.androidsensors.R
import com.akshayAshokCode.androidsensors.presentation.views.GravityBallScreen
import com.akshayAshokCode.androidsensors.presentation.views.Particle
import com.akshayAshokCode.androidsensors.presentation.views.TrailPoint
import com.akshayAshokCode.androidsensors.presentation.views.BackgroundParticle
import com.akshayAshokCode.androidsensors.presentation.views.SensorDetailsBottomSheet
import com.akshayAshokCode.androidsensors.utils.AnalyticsManager
import com.akshayAshokCode.androidsensors.utils.PreferencesManager
import kotlin.math.abs
import kotlin.random.Random

class SpaceBall : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var isAvailable by mutableStateOf(true)
    private var showBottomSheet by mutableStateOf(false)

    // Store original orientation to restore later
    private var originalOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    // Store view reference for haptic feedback
    private var rootView: View? = null
    private var reviewFlaggedThisVisit = false

    // Ball physics state - start at center for initial drop demo
    private var ballX by mutableStateOf(0.5f) // Normalized position (0-1)
    private var ballY by mutableStateOf(0.5f) // Start at center
    // Velocity is internal physics state only — not displayed, no need for Compose state
    private var velocityX = 0f
    private var velocityY = 0f

    private val mainHandler = Handler(Looper.getMainLooper())

    // Accelerometer fallback when TYPE_GRAVITY is unavailable (~15-20% of devices)
    @Volatile private var usingAccelerometerFallback = false
    private val lowPassGravity = FloatArray(2)
    private val lowPassAlpha = 0.8f

    // Drag & drop state
    private var isDragging by mutableStateOf(false)
    private var lastDragX = 0f
    private var lastDragY = 0f
    private var dragVelocityX = 0f
    private var dragVelocityY = 0f
    private var lastDragTime = SystemClock.elapsedRealtime()

    // Particle system for sparkle effects
    private val particles = mutableStateListOf<Particle>()

    // Background particle field
    private val backgroundParticles = mutableStateListOf<BackgroundParticle>()

    // Trail system for ball path
    private val trailPoints = mutableStateListOf<TrailPoint>()
    private var lastTrailTime = SystemClock.elapsedRealtime()

    // Track previous position to detect actual bounces (separate for each edge)
    private var wasAtLeftEdge = false
    private var wasAtRightEdge = false
    private var wasAtTopEdge = false
    private var wasAtBottomEdge = false

    // Physics constants
    private val friction = 0.98f
    private val sensitivity = 0.15f
    private val bounceDampening = 0.7f

    private var lastUpdateTime = SystemClock.elapsedRealtime()

    // Cached canvas dimensions — updated via layout listener, avoids view.width/height on every frame
    private var cachedWidth  = 0f
    private var cachedHeight = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize sensor manager
        sensorManager = context?.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager

        val composeView = ComposeView(requireContext())
        rootView = composeView

        composeView.setContent {
            GravityBallScreen(
                ballX               = ballX,
                ballY               = ballY,
                particles           = particles,
                trailPoints         = trailPoints,
                backgroundParticles = backgroundParticles,
                isDragging          = isDragging,
                isAvailable         = isAvailable,
                onDragStart         = { x, y, width, height -> handleDragStart(x, y, width, height) },
                onDrag              = { x, y -> handleDrag(x, y) },
                onDragEnd           = { handleDragEnd() },
                onBottomSheetToggle = {
                    AnalyticsManager.logBottomSheetOpened(AnalyticsManager.Features.SPACE_BALL)
                    showBottomSheet = true
                }
            )

            SensorDetailsBottomSheet(
                isVisible = showBottomSheet,
                onDismiss = { showBottomSheet = false },
                title = stringResource(R.string.space_ball_details_title),
                instruction = stringResource(R.string.space_ball_instruction),
                content = stringResource(R.string.space_ball_details)
            )
        }

        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            cachedWidth  = v.width.toFloat()
            cachedHeight = v.height.toFloat()
        }

        // Add menu to toolbar using MenuProvider
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.sensor_info_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_info -> {
                        showBottomSheet = true
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onResume() {
        super.onResume()
        reviewFlaggedThisVisit = false
        lastUpdateTime = SystemClock.elapsedRealtime()

        // Lock screen orientation to current orientation
        activity?.let { activity ->
            originalOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }

        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        when {
            gravitySensor != null -> {
                usingAccelerometerFallback = false
                sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
            }
            accelerometer != null -> {
                usingAccelerometerFallback = true
                lowPassGravity.fill(0f)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            }
            else -> {
                isAvailable = false
                AnalyticsManager.logSensorUnavailable(AnalyticsManager.Features.SPACE_BALL)
                return
            }
        }

        // Initialize background particles if empty (post to ensure view has dimensions)
        if (backgroundParticles.isEmpty()) {
            view?.post {
                initializeBackgroundParticles()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        activity?.requestedOrientation = originalOrientation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
        rootView = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensorType = event.sensor?.type
        val isGravityEvent = sensorType == Sensor.TYPE_GRAVITY
        val isAccelFallback = sensorType == Sensor.TYPE_ACCELEROMETER && usingAccelerometerFallback
        if (!isGravityEvent && !isAccelFallback) return

        // Apply low-pass filter on sensor thread to extract gravity from accelerometer
        val gx: Float
        val gy: Float
        if (isAccelFallback) {
            lowPassGravity[0] = lowPassAlpha * lowPassGravity[0] + (1 - lowPassAlpha) * event.values[0]
            lowPassGravity[1] = lowPassAlpha * lowPassGravity[1] + (1 - lowPassAlpha) * event.values[1]
            gx = lowPassGravity[0]; gy = lowPassGravity[1]
        } else {
            gx = event.values[0]; gy = event.values[1]
        }
        val capturedTime = SystemClock.elapsedRealtime()

        // Post ALL Compose state mutations (ball physics, particles, trail) to main thread.
        // This fixes both the thread-safety crash and the ConcurrentModificationException on
        // the particle/trail SnapshotStateLists.
        mainHandler.post {
            if (!isAdded) return@post
            processPhysics(gx, gy, capturedTime)
        }
    }

    private fun processPhysics(gravityX: Float, gravityY: Float, currentTime: Long) {
        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime

        updateBackgroundParticles(gravityX, gravityY, deltaTime)

        if (isDragging) return

        velocityX -= gravityX * deltaTime * sensitivity
        velocityY += gravityY * deltaTime * sensitivity

        velocityX *= friction
        velocityY *= friction

        val velocityThreshold = 0.001f
        if (abs(velocityX) < velocityThreshold) velocityX = 0f
        if (abs(velocityY) < velocityThreshold) velocityY = 0f

        ballX += velocityX
        ballY += velocityY

        if (ballX < 0f) {
            ballX = 0f
            val collisionVelocity = abs(velocityX)
            velocityX = -velocityX * bounceDampening
            if (abs(velocityX) < velocityThreshold) velocityX = 0f
            if (!wasAtLeftEdge && collisionVelocity > 0.01f) {
                createSparkleParticles(ballX, ballY, collisionVelocity)
                triggerHapticFeedback(collisionVelocity)
            }
            wasAtLeftEdge = true
        } else {
            wasAtLeftEdge = false
        }

        if (ballX > 1f) {
            ballX = 1f
            val collisionVelocity = abs(velocityX)
            velocityX = -velocityX * bounceDampening
            if (abs(velocityX) < velocityThreshold) velocityX = 0f
            if (!wasAtRightEdge && collisionVelocity > 0.01f) {
                createSparkleParticles(ballX, ballY, collisionVelocity)
                triggerHapticFeedback(collisionVelocity)
            }
            wasAtRightEdge = true
        } else {
            wasAtRightEdge = false
        }

        if (ballY < 0f) {
            ballY = 0f
            val collisionVelocity = abs(velocityY)
            velocityY = -velocityY * bounceDampening
            if (abs(velocityY) < velocityThreshold) velocityY = 0f
            if (!wasAtTopEdge && collisionVelocity > 0.01f) {
                createSparkleParticles(ballX, ballY, collisionVelocity)
                triggerHapticFeedback(collisionVelocity)
            }
            wasAtTopEdge = true
        } else {
            wasAtTopEdge = false
        }

        if (ballY > 1f) {
            ballY = 1f
            val collisionVelocity = abs(velocityY)
            velocityY = -velocityY * bounceDampening
            if (abs(velocityY) < velocityThreshold) velocityY = 0f
            if (!wasAtBottomEdge && collisionVelocity > 0.01f) {
                createSparkleParticles(ballX, ballY, collisionVelocity)
                triggerHapticFeedback(collisionVelocity)
            }
            wasAtBottomEdge = true
        } else {
            wasAtBottomEdge = false
        }

        updateParticles(deltaTime)
        updateTrail(currentTime)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    private fun createSparkleParticles(x: Float, y: Float, impactForce: Float) {
        // Number of particles based on impact force (3-8 particles)
        val particleCount = (3 + (impactForce * 50).toInt()).coerceIn(3, 8)

        repeat(particleCount) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            // Reduced speed to 50% - particles travel half as far
            val speed = (0.15f + Random.nextFloat() * 0.2f) * impactForce * 100

            particles.add(
                Particle(
                    x = x,
                    y = y,
                    velocityX = kotlin.math.cos(angle) * speed,
                    velocityY = kotlin.math.sin(angle) * speed,
                    alpha = 1f,
                    lifetime = 0f,
                    // Very short lifetime: 0.1-0.15s for quick flash effect
                    maxLifetime = 0.1f + Random.nextFloat() * 0.05f,
                    color = Color(0xFFFFC107) // Yellow sparkles
                )
            )
        }

        // Limit total particles to prevent performance issues
        while (particles.size > 50) {
            particles.removeAt(0)
        }
    }

    private fun updateParticles(deltaTime: Float) {
        particles.forEach { particle ->
            particle.lifetime += deltaTime
            particle.x += particle.velocityX * deltaTime
            particle.y += particle.velocityY * deltaTime
            particle.alpha = 1f - (particle.lifetime / particle.maxLifetime)
        }
        particles.removeAll { it.lifetime >= it.maxLifetime }
    }

    private fun updateTrail(currentTime: Long) {
        // Add new trail point every 50ms
        if (currentTime - lastTrailTime > 50) {
            trailPoints.add(TrailPoint(ballX, ballY, alpha = 1f))
            lastTrailTime = currentTime

            // Limit trail length to 20 points
            if (trailPoints.size > 20) {
                trailPoints.removeAt(0)
            }
        }

        // Fade out trail points
        trailPoints.forEachIndexed { index, point ->
            // Older points (lower index) fade faster
            val fadeSpeed = 0.05f
            point.alpha = (point.alpha - fadeSpeed).coerceAtLeast(0f)
        }

        // Remove fully faded points
        trailPoints.removeAll { it.alpha <= 0f }
    }

    private fun handleDragStart(x: Float, y: Float, screenWidth: Float, screenHeight: Float) {
        // Ball radius in pixels (40dp, approximately 120px on most devices)
        val ballRadiusPx = 120f

        // Calculate actual ball position in pixels (accounting for the offset)
        val ballRadiusNormalized = ballRadiusPx / screenWidth
        val actualBallX = ballX * (1f - ballRadiusNormalized * 2) + ballRadiusNormalized
        val actualBallY = ballY * (1f - ballRadiusNormalized * 2) + ballRadiusNormalized

        // Convert tap position to actual coordinates
        val tapX = x
        val tapY = y

        // Check if tap is on the ball (with tolerance)
        val distance = kotlin.math.sqrt(
            (tapX - actualBallX) * (tapX - actualBallX) + (tapY - actualBallY) * (tapY - actualBallY)
        )

        // Tolerance in normalized coordinates
        if (distance < ballRadiusNormalized * 1.5f) {
            isDragging = true
            lastDragX = x
            lastDragY = y
            lastDragTime = SystemClock.elapsedRealtime()
            dragVelocityX = 0f
            dragVelocityY = 0f
        }
    }

    private fun handleDrag(x: Float, y: Float) {
        if (isDragging) {
            val currentTime = SystemClock.elapsedRealtime()
            val deltaTime = (currentTime - lastDragTime) / 1000f

            if (deltaTime > 0) {
                // Calculate drag velocity for throw physics
                dragVelocityX = (x - lastDragX) / deltaTime
                dragVelocityY = (y - lastDragY) / deltaTime
            }

            // Update ball position
            ballX = x.coerceIn(0f, 1f)
            ballY = y.coerceIn(0f, 1f)

            lastDragX = x
            lastDragY = y
            lastDragTime = currentTime
        }
    }

    private fun handleDragEnd() {
        if (isDragging) {
            isDragging = false

            // Apply throw velocity (scaled to 15% for gentle throw)
            velocityX = dragVelocityX * 0.15f
            velocityY = dragVelocityY * 0.15f

            if (velocityX != 0f || velocityY != 0f) {
                AnalyticsManager.logBallThrown()
            }

            // Reset all edge tracking to allow sparkles on next impact
            wasAtLeftEdge = false
            wasAtRightEdge = false
            wasAtTopEdge = false
            wasAtBottomEdge = false
        }
    }

    private fun triggerHapticFeedback(impactVelocity: Float) {
        rootView?.let { view ->
            if (impactVelocity > 0.02f) {
                if (!reviewFlaggedThisVisit) {
                    reviewFlaggedThisVisit = true
                    AnalyticsManager.logWinAchieved(AnalyticsManager.Features.SPACE_BALL)
                    PreferencesManager.recordWin(requireContext())
                }
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    private fun initializeBackgroundParticles() {
        val width  = cachedWidth
        val height = cachedHeight
        if (width <= 0 || height <= 0) return

        // Create 80 background particles at random positions
        repeat(80) {
            backgroundParticles.add(
                BackgroundParticle(
                    x = Random.nextFloat() * width,
                    y = Random.nextFloat() * height,
                    velocityX = 0f,
                    velocityY = 0f,
                    size = 2f + Random.nextFloat() * 2f, // Random between 2.0 and 4.0
                    alpha = 0.15f + Random.nextFloat() * 0.15f // Random between 0.15 and 0.30
                )
            )
        }
    }

    private fun updateBackgroundParticles(gravityX: Float, gravityY: Float, deltaTime: Float) {
        val width  = cachedWidth
        val height = cachedHeight
        if (width <= 0 || height <= 0) return

        // Update each background particle with slower movement (parallax effect)
        backgroundParticles.forEach { particle ->
            // Apply gravity with increased sensitivity for more visible movement (50% of ball sensitivity)
            particle.velocityX -= gravityX * deltaTime * sensitivity * 0.5f
            particle.velocityY += gravityY * deltaTime * sensitivity * 0.5f

            // Apply less friction so particles keep moving longer
            particle.velocityX *= 0.98f
            particle.velocityY *= 0.98f

            // Update position (multiply by larger factor for more visible movement)
            particle.x += particle.velocityX * 100f
            particle.y += particle.velocityY * 100f

            // Wrap around edges (particles loop around instead of bouncing)
            if (particle.x < 0) particle.x = width
            if (particle.x > width) particle.x = 0f
            if (particle.y < 0) particle.y = height
            if (particle.y > height) particle.y = 0f
        }
    }
}