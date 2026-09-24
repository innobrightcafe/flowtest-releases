package com.example.ui.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/**
 * UI/UX Pro Max Motion & Transition System
 * 
 * Provides ultra-responsive, lightweight, 60fps transitions for:
 * - Pages: Smooth directional slide + alpha crossfade (230ms)
 * - Tabs: Directional horizontal slide + subtle scale/fade (200ms)
 * - Drawers: Smooth slide-in from edge/bottom with scrim backdrop fade (260ms)
 *
 * Eliminates sluggish physics loops and touch interception latency for instant, snappy user feedback.
 */
object MotionTransitions {

    // Easing curves optimized for responsive mobile interaction
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val StandardEasing = FastOutSlowInEasing
    val AccelerationEasing = FastOutLinearInEasing
    val DecelerationEasing = LinearOutSlowInEasing

    // Snappy specs (200-240ms) - ideal for zero-latency response
    const val PAGE_TRANSITION_MS = 240
    const val TAB_TRANSITION_MS = 200
    const val DRAWER_TRANSITION_MS = 260
    const val FADE_MS = 180

    val PageOffsetSpec = tween<IntOffset>(
        durationMillis = PAGE_TRANSITION_MS,
        easing = EmphasizedDecelerate
    )

    val TabOffsetSpec = tween<IntOffset>(
        durationMillis = TAB_TRANSITION_MS,
        easing = StandardEasing
    )

    val FastSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    // Spring-damper physics curves for bottom drawers and sheets (natural deceleration + fluid snap)
    val DrawerSpringFloatSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val DrawerSpringIntOffsetSpec = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Multi-step directional transitions:
     * Next step enters from the right with alpha fade; previous step exits to the left.
     * Back step enters from the left with alpha fade; current step exits to the right.
     */
    object StepTransitions {
        fun directionalSlideFade(isForward: Boolean): ContentTransform {
            return if (isForward) {
                // Moving forward: Slide in from right, fade out to left
                (slideInHorizontally { width -> width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
            } else {
                // Moving backward: Slide in from left, fade out to right
                (slideInHorizontally { width -> -width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
            }
        }
    }

    /**
     * Page Transitions: Forward, Backward, and Pop navigation in NavHost
     */
    object PageTransitions {
        fun enterSlide(direction: Int = 1): EnterTransition {
            return slideInHorizontally(
                initialOffsetX = { fullWidth -> direction * (fullWidth / 3) },
                animationSpec = PageOffsetSpec
            ) + fadeIn(animationSpec = tween(FADE_MS, easing = LinearOutSlowInEasing))
        }

        fun exitSlide(direction: Int = -1): ExitTransition {
            return slideOutHorizontally(
                targetOffsetX = { fullWidth -> direction * (fullWidth / 4) },
                animationSpec = PageOffsetSpec
            ) + fadeOut(animationSpec = tween(FADE_MS, easing = FastOutLinearInEasing))
        }

        fun popEnterSlide(): EnterTransition {
            return slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth / 4) },
                animationSpec = PageOffsetSpec
            ) + fadeIn(animationSpec = tween(FADE_MS, easing = LinearOutSlowInEasing))
        }

        fun popExitSlide(): ExitTransition {
            return slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = PageOffsetSpec
            ) + fadeOut(animationSpec = tween(FADE_MS, easing = FastOutLinearInEasing))
        }
    }

    /**
     * Tab Transitions: Responsive horizontal sliding between primary tabs and in-screen tab segments
     */
    object TabTransitions {
        fun enterTransition(isMovingRight: Boolean): EnterTransition {
            val direction = if (isMovingRight) 1 else -1
            return slideInHorizontally(
                initialOffsetX = { fullWidth -> direction * (fullWidth / 4) },
                animationSpec = TabOffsetSpec
            ) + fadeIn(animationSpec = tween(TAB_TRANSITION_MS - 40, easing = LinearOutSlowInEasing))
        }

        fun exitTransition(isMovingRight: Boolean): ExitTransition {
            val direction = if (isMovingRight) -1 else 1
            return slideOutHorizontally(
                targetOffsetX = { fullWidth -> direction * (fullWidth / 4) },
                animationSpec = TabOffsetSpec
            ) + fadeOut(animationSpec = tween(TAB_TRANSITION_MS - 40, easing = FastOutLinearInEasing))
        }

        fun contentTransform(initialIndex: Int, targetIndex: Int): ContentTransform {
            val isForward = targetIndex > initialIndex
            val enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> if (isForward) fullWidth / 4 else -fullWidth / 4 },
                animationSpec = TabOffsetSpec
            ) + fadeIn(animationSpec = tween(TAB_TRANSITION_MS - 40))

            val exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> if (isForward) -fullWidth / 4 else fullWidth / 4 },
                animationSpec = TabOffsetSpec
            ) + fadeOut(animationSpec = tween(TAB_TRANSITION_MS - 40))

            return enter togetherWith exit
        }
    }

    /**
     * Drawer & Sheet Transitions: Slide in from screen edge or bottom
     */
    object DrawerTransitions {
        val slideInFromEnd: EnterTransition = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DRAWER_TRANSITION_MS, easing = EmphasizedDecelerate)
        ) + fadeIn(animationSpec = tween(FADE_MS))

        val slideOutToEnd: ExitTransition = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DRAWER_TRANSITION_MS - 30, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(FADE_MS))

        val slideInFromBottom: EnterTransition = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(DRAWER_TRANSITION_MS, easing = EmphasizedDecelerate)
        ) + fadeIn(animationSpec = tween(FADE_MS))

        val slideOutToBottom: ExitTransition = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(DRAWER_TRANSITION_MS - 30, easing = FastOutLinearInEasing)
        ) + fadeOut(animationSpec = tween(FADE_MS))

        // Spring-physics bottom drawer animations: natural deceleration curve
        val springSlideInFromBottom: EnterTransition = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(FADE_MS, easing = LinearOutSlowInEasing))

        val springSlideOutToBottom: ExitTransition = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(animationSpec = tween(FADE_MS, easing = FastOutLinearInEasing))
    }
}
