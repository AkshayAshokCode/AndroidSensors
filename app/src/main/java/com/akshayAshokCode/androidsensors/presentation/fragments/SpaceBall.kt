package com.akshayAshokCode.androidsensors.presentation.fragments

import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
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

    // Ball physics state - start at center for initial drop demo
    private var ballX by mutableStateOf(0.5f) // Normalized position (0-1)
    private var ballY by mutableStateOf(0.5f) // Start at center
    private var velocityX by mutableStateOf(0f)
    private var velocityY by mutableStateOf(0f)

    // Drag & drop state
    private var isDragging by mutableStateOf(false)
    private var lastDragX = 0f
    private var lastDragY = 0f
    private var dragVelocityX = 0f
    private var dragVelocityY = 0f
    private var lastDragTime = System.currentTimeMillis()

    // Particle system for sparkle effects
    private val particles = mutableStateListOf<Particle>()

    // Background particle field
    private val backgroundParticles = mutableStateListOf<BackgroundParticle>()

    // Trail system for ball path
    private val trailPoints = mutableStateListOf<TrailPoint>()
    private var lastTrailTime = System.currentTimeMillis()

    // Track previous position to detect actual bounces (separate for each edge)
    private var wasAtLeftEdge = false
    private var wasAtRightEdge = false
    private var wasAtTopEdge = false
    private var wasAtBottomEdge = false

    // Physics constants
    private val friction = 0.98f
    private val sensitivity = 0.15f
    private val bounceDampening = 0.7f

    private var lastUpdateTime = System.currentTimeMillis()

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
                ballX = ballX,
                ballY = ballY,
                particles = particles,
                trailPoints = trailPoints,
                backgroundParticles = backgroundParticles,
                isDragging = isDragging,
                isAvailable = isAvailable,
                onDragStart = { x, y, width, height -> handleDragStart(x, y, width, height) },
                onDrag = { x, y -> handleDrag(x, y) },
                onDragEnd = { handleDragEnd() },
                onBottomSheetToggleClick = {
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

        // Add menu to toolbar using MenuProvider
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.sensor_info_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_info -> {
                        AnalyticsManager.logBottomSheetOpened(AnalyticsManager.Features.SPACE_BALL)
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

        // Lock screen orientation to current orientation
        activity?.let { activity ->
            originalOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }

        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

        if (gravitySensor == null) {
            isAvailable = false
            return
        }

        // Initialize background particles if empty (post to ensure view has dimensions)
        if (backgroundParticles.isEmpty()) {
            view?.post {
                initializeBackgroundParticles()
            }
        }

        sensorManager.registerListener(
            this,
            gravitySensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)

        // Restore original orientation
        activity?.requestedOrientation = originalOrientation
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor?.type == Sensor.TYPE_GRAVITY) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = (currentTime - lastUpdateTime) / 1000f
            lastUpdateTime = currentTime

            // Get gravity values (m/s²)
            val gravityX = event.values[0] // Left/Right tilt
            val gravityY = event.values[1] // Forward/Backward tilt

            // Update background particles with parallax effect (always, even when dragging)
            updateBackgroundParticles(gravityX, gravityY, deltaTime)

            // Skip ball physics update if ball is being dragged
            if (isDragging) {
                return
            }

            // Update velocity based on gravity (X inverted to match tilt direction)
            velocityX -= gravityX * deltaTime * sensitivity
            velocityY += gravityY * deltaTime * sensitivity

            // Apply friction
            velocityX *= friction
            velocityY *= friction

            // Stop very small velocities to prevent micro-bounces
            val velocityThreshold = 0.001f
            if (abs(velocityX) < velocityThreshold) velocityX = 0f
            if (abs(velocityY) < velocityThreshold) velocityY = 0f

            // Update position
            ballX += velocityX
            ballY += velocityY

            // Bounce off edges with dampening and sparkle effects
            // Left edge - Yellow sparkles
            if (ballX < 0f) {
                ballX = 0f
                val collisionVelocity = abs(velocityX)
                velocityX = -velocityX * bounceDampening
                // Stop if velocity too small after bounce
                if (abs(velocityX) < velocityThreshold) velocityX = 0f

                // Create sparkles and haptic feedback only on NEW collision with sufficient impact
                if (!wasAtLeftEdge && collisionVelocity > 0.01f) {
                    createSparkleParticles(ballX, ballY, collisionVelocity)
                    triggerHapticFeedback(collisionVelocity)
                }
                wasAtLeftEdge = true
            } else {
                wasAtLeftEdge = false
            }

            // Right edge - Yellow sparkles
            if (ballX > 1f) {
                ballX = 1f
                val collisionVelocity = abs(velocityX)
                velocityX = -velocityX * bounceDampening
                // Stop if velocity too small after bounce
                if (abs(velocityX) < velocityThreshold) velocityX = 0f

                // Create sparkles and haptic feedback only on NEW collision with sufficient impact
                if (!wasAtRightEdge && collisionVelocity > 0.01f) {
                    createSparkleParticles(ballX, ballY, collisionVelocity)
                    triggerHapticFeedback(collisionVelocity)
                }
                wasAtRightEdge = true
            } else {
                wasAtRightEdge = false
            }

            // Top edge - Yellow sparkles
            if (ballY < 0f) {
                ballY = 0f
                val collisionVelocity = abs(velocityY)
                velocityY = -velocityY * bounceDampening
                // Stop if velocity too small after bounce
                if (abs(velocityY) < velocityThreshold) velocityY = 0f

                // Create sparkles and haptic feedback only on NEW collision with sufficient impact
                if (!wasAtTopEdge && collisionVelocity > 0.01f) {
                    createSparkleParticles(ballX, ballY, collisionVelocity)
                    triggerHapticFeedback(collisionVelocity)
                }
                wasAtTopEdge = true
            } else {
                wasAtTopEdge = false
            }

            // Bottom edge - Yellow sparkles
            if (ballY > 1f) {
                ballY = 1f
                val collisionVelocity = abs(velocityY)
                velocityY = -velocityY * bounceDampening
                // Stop if velocity too small after bounce
                if (abs(velocityY) < velocityThreshold) velocityY = 0f

                // Create sparkles and haptic feedback only on NEW collision with sufficient impact
                if (!wasAtBottomEdge && collisionVelocity > 0.01f) {
                    createSparkleParticles(ballX, ballY, collisionVelocity)
                    triggerHapticFeedback(collisionVelocity)
                }
                wasAtBottomEdge = true
            } else {
                wasAtBottomEdge = false
            }

            // Update existing particles
            updateParticles(deltaTime)

            // Update trail
            updateTrail(currentTime)
        }
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
        particles.removeAll { particle ->
            particle.lifetime += deltaTime
            particle.x += particle.velocityX * deltaTime
            particle.y += particle.velocityY * deltaTime
            particle.alpha = 1f - (particle.lifetime / particle.maxLifetime)

            // Remove if lifetime exceeded
            particle.lifetime >= particle.maxLifetime
        }
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
            lastDragTime = System.currentTimeMillis()
            dragVelocityX = 0f
            dragVelocityY = 0f
        }
    }

    private fun handleDrag(x: Float, y: Float) {
        if (isDragging) {
            val currentTime = System.currentTimeMillis()
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

            // Reset all edge tracking to allow sparkles on next impact
            wasAtLeftEdge = false
            wasAtRightEdge = false
            wasAtTopEdge = false
            wasAtBottomEdge = false
        }
    }

    private fun triggerHapticFeedback(impactVelocity: Float) {
        rootView?.let { view ->
            // Use different haptic feedback based on impact force
            if (impactVelocity > 0.02f) {
                // Hard impact - stronger feedback
                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            } else {
                // Soft impact - lighter feedback
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    private fun initializeBackgroundParticles() {
        // Get screen dimensions from the view
        val view = view ?: return
        val width = view.width.toFloat()
        val height = view.height.toFloat()

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
        val view = view ?: return
        val width = view.width.toFloat()
        val height = view.height.toFloat()

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