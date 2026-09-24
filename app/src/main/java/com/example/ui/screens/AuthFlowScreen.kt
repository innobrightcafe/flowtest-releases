package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ui.viewmodel.VpnViewModel
import com.example.ui.motion.MotionTransitions

enum class AuthScreenState(val order: Int) {
    SPLASH(0),
    ONBOARDING(1),
    LOGIN(2),
    REGISTER(3)
}

@Composable
fun AuthFlowScreen(
    viewModel: VpnViewModel,
    onAuthenticated: () -> Unit
) {
    val isUserRegistered by viewModel.isUserRegistered.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    var currentAuthState by remember { mutableStateOf(AuthScreenState.SPLASH) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentAuthState,
            transitionSpec = {
                val isForward = targetState.order >= initialState.order
                if (initialState == AuthScreenState.SPLASH) {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                } else if (isForward) {
                    // Forward slide: enter from right, exit to left
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 3 },
                        animationSpec = MotionTransitions.PageOffsetSpec
                    ) + fadeIn(animationSpec = tween(MotionTransitions.FADE_MS)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 4 },
                        animationSpec = MotionTransitions.PageOffsetSpec
                    ) + fadeOut(animationSpec = tween(MotionTransitions.FADE_MS))
                } else {
                    // Backward slide: enter from left, exit to right
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 4 },
                        animationSpec = MotionTransitions.PageOffsetSpec
                    ) + fadeIn(animationSpec = tween(MotionTransitions.FADE_MS)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = MotionTransitions.PageOffsetSpec
                    ) + fadeOut(animationSpec = tween(MotionTransitions.FADE_MS))
                }
            },
            label = "AuthFlowTransition"
        ) { screen ->
            when (screen) {
                AuthScreenState.SPLASH -> {
                    SplashScreen(
                        onSplashFinished = {
                            val isFirebaseLoggedIn = viewModel.firebaseUser.value != null
                            if (isFirebaseLoggedIn) {
                                onAuthenticated()
                            }
                            // Always advance state machine away from SPLASH so screen is never stuck
                            if (isUserRegistered) {
                                currentAuthState = AuthScreenState.LOGIN
                            } else if (hasCompletedOnboarding) {
                                currentAuthState = AuthScreenState.REGISTER
                            } else {
                                currentAuthState = AuthScreenState.ONBOARDING
                            }
                        }
                    )
                }

                AuthScreenState.ONBOARDING -> {
                    OnboardingScreen(
                        onFinishOnboarding = {
                            viewModel.setCompletedOnboarding(true)
                            currentAuthState = AuthScreenState.LOGIN
                        },
                        onNavigateToRegister = {
                            viewModel.setCompletedOnboarding(true)
                            currentAuthState = AuthScreenState.REGISTER
                        }
                    )
                }

                AuthScreenState.LOGIN -> {
                    LoginScreen(
                        viewModel = viewModel,
                        onNavigateToRegister = {
                            currentAuthState = AuthScreenState.REGISTER
                        },
                        onLoginSuccess = onAuthenticated
                    )
                }

                AuthScreenState.REGISTER -> {
                    RegisterScreen(
                        viewModel = viewModel,
                        onNavigateToLogin = {
                            currentAuthState = AuthScreenState.LOGIN
                        },
                        onRegisterSuccess = onAuthenticated
                    )
                }
            }
        }
    }
}
