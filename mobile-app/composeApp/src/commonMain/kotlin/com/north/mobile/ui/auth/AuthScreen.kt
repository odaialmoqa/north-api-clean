package com.north.mobile.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Authentication screen for login and registration with enhanced UX
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordMessage by remember { mutableStateOf<String?>(null) }
    
    // Form validation states
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Optimized scroll state
    val scrollState = rememberScrollState()
    
    // Animation states
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    
    val formAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.7f else 1f,
        animationSpec = tween(300),
        label = "formAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState)
            .semantics {
                contentDescription = "Authentication screen"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo and Title - Enhanced Professional Design with Animation
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(800)) + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .semantics {
                        contentDescription = "North logo"
                        role = Role.Image
                    },
                contentAlignment = Alignment.Center
            ) {
                // Enhanced North star/diamond logo
                NorthLogo()
            }
        }
        
        // Animated title
        AnimatedContent(
            targetState = "North",
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) with fadeOut(animationSpec = tween(300))
            },
            label = "titleAnimation"
        ) { title ->
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics {
                        contentDescription = "North app title"
                    }
            )
        }
        
        // Animated subtitle
        AnimatedContent(
            targetState = if (isLogin) "Welcome back!" else "Join North today",
            transitionSpec = {
                slideInVertically { height -> height } + fadeIn() with
                slideOutVertically { height -> -height } + fadeOut()
            },
            label = "subtitleAnimation"
        ) { subtitle ->
            Text(
                text = subtitle,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .semantics {
                        contentDescription = "Authentication screen subtitle"
                    }
            )
        }
        
        // Auth Form with enhanced animations and accessibility
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = formAlpha
                }
                .semantics {
                    contentDescription = "Authentication form"
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Toggle between Login/Register with enhanced accessibility
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Authentication mode selector"
                        },
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { 
                            isLogin = true
                            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isLogin) MaterialTheme.colorScheme.primary 
                                         else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Switch to login mode"
                            role = Role.Tab
                            selected = isLogin
                        }
                    ) {
                        Text(
                            "Login",
                            fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    
                    Text(
                        " | ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    )
                    
                    TextButton(
                        onClick = { 
                            isLogin = false
                            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (!isLogin) MaterialTheme.colorScheme.primary 
                                         else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = "Switch to registration mode"
                            role = Role.Tab
                            selected = !isLogin
                        }
                    ) {
                        Text(
                            "Register",
                            fontWeight = if (!isLogin) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                
                // Registration fields with enhanced animations and accessibility
                AnimatedVisibility(
                    visible = !isLogin,
                    enter = slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(animationSpec = tween(400)),
                    exit = slideOutVertically(
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Name input fields"
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { newValue ->
                                firstName = newValue.capitalizeWords()
                                firstNameError = validateName(firstName, "First Name")
                            },
                            label = { Text("First Name") },
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = "Enter your first name"
                                    if (firstNameError != null) {
                                        error(firstNameError!!)
                                    }
                                },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            isError = firstNameError != null,
                            supportingText = firstNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                        )
                        
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { newValue ->
                                lastName = newValue.capitalizeWords()
                                lastNameError = validateName(lastName, "Last Name")
                            },
                            label = { Text("Last Name") },
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = "Enter your last name"
                                    if (lastNameError != null) {
                                        error(lastNameError!!)
                                    }
                                },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            shape = RoundedCornerShape(12.dp),
                            isError = lastNameError != null,
                            supportingText = lastNameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                        )
                    }
                }
                
                // Email field with enhanced accessibility
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = validateEmail(email)
                    },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Enter your email address"
                            if (emailError != null) {
                                error(emailError!!)
                            }
                        },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp),
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
                
                // Password field with enhanced accessibility
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        passwordError = validatePassword(password, !isLogin)
                    },
                    label = { Text("Password") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (isLogin) "Enter your password" else "Create a password"
                            if (passwordError != null) {
                                error(passwordError!!)
                            }
                        },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isLoading) {
                                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                scope.launch {
                                    handleAuth(
                                        isLogin = isLogin,
                                        email = email,
                                        password = password,
                                        firstName = firstName,
                                        lastName = lastName,
                                        onLoading = { isLoading = it },
                                        onError = { 
                                            errorMessage = it
                                            if (it != null) {
                                                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            }
                                        },
                                        onSuccess = {
                                            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            onAuthSuccess()
                                        }
                                    )
                                }
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    isError = passwordError != null,
                    supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
                
                // Forgot Password link (only show for login) with enhanced accessibility
                AnimatedVisibility(
                    visible = isLogin,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { 
                                showForgotPasswordDialog = true
                                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Open forgot password dialog"
                                role = Role.Button
                            }
                        ) {
                            Text(
                                "Forgot Password?",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Error message with animation
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeIn(animationSpec = tween(300)),
                    exit = slideOutVertically(
                        animationSpec = tween(200)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    contentDescription = "Authentication error: $error"
                                }
                        )
                    }
                }
                
                // Auth button with enhanced UX
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        scope.launch {
                            handleAuth(
                                isLogin = isLogin,
                                email = email,
                                password = password,
                                firstName = firstName,
                                lastName = lastName,
                                onLoading = { isLoading = it },
                                onError = { 
                                    errorMessage = it
                                    if (it != null) {
                                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    }
                                },
                                onSuccess = {
                                    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onAuthSuccess()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics {
                            contentDescription = if (isLogin) "Login to your account" else "Create new account"
                            role = Role.Button
                        },
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank() &&
                            (isLogin || (firstName.isNotBlank() && lastName.isNotBlank())),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AnimatedContent(
                        targetState = isLoading,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) with fadeOut(animationSpec = tween(200))
                        },
                        label = "buttonContentAnimation"
                    ) { loading ->
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (isLogin) "Login" else "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Enhanced Forgot Password Dialog with animations and accessibility
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showForgotPasswordDialog = false
                forgotPasswordEmail = ""
                forgotPasswordMessage = null
            },
            title = { 
                Text(
                    "Reset Password",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics {
                        contentDescription = "Reset password dialog"
                    }
                ) 
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Enter your email address and we'll send you a reset link.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "Password reset instructions"
                        }
                    )
                    
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Enter email for password reset"
                            },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Show message if any with animation
                    AnimatedVisibility(
                        visible = forgotPasswordMessage != null,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(),
                        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically()
                    ) {
                        forgotPasswordMessage?.let { message ->
                            Text(
                                text = message,
                                color = if (message.contains("sent", ignoreCase = true)) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                modifier = Modifier.semantics {
                                    contentDescription = "Password reset status: $message"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            handleForgotPassword(
                                email = forgotPasswordEmail,
                                onMessage = { forgotPasswordMessage = it }
                            )
                        }
                    },
                    enabled = forgotPasswordEmail.isNotBlank(),
                    modifier = Modifier.semantics {
                        contentDescription = "Send password reset link"
                        role = Role.Button
                    }
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showForgotPasswordDialog = false
                        forgotPasswordEmail = ""
                        forgotPasswordMessage = null
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Cancel password reset"
                        role = Role.Button
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NorthLogo() {
    // Enhanced North star/diamond with vibrant colors and visible background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2563EB), // Blue-600
                        Color(0xFF1D4ED8)  // Blue-700
                    )
                ),
                shape = androidx.compose.foundation.shape.CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.7f)) {
            val center = size.width / 2f
            val outerStarSize = size.width * 0.4f
            val midStarSize = size.width * 0.3f
            val innerStarSize = size.width * 0.2f
            
            // Shadow layer (slightly offset)
            val shadowPath = androidx.compose.ui.graphics.Path().apply {
                val shadowCenter = center + 1f
                moveTo(shadowCenter, shadowCenter - outerStarSize)
                lineTo(shadowCenter + outerStarSize, shadowCenter)
                lineTo(shadowCenter, shadowCenter + outerStarSize)
                lineTo(shadowCenter - outerStarSize, shadowCenter)
                close()
            }
            
            drawPath(
                path = shadowPath,
                color = Color.Black.copy(alpha = 0.15f)
            )
            
            // Main outer diamond - bright white
            val outerPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(center, center - outerStarSize)
                lineTo(center + outerStarSize, center)
                lineTo(center, center + outerStarSize)
                lineTo(center - outerStarSize, center)
                close()
            }
            
            drawPath(
                path = outerPath,
                color = Color.White
            )
            
            // Middle layer with subtle gradient
            val midPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(center, center - midStarSize)
                lineTo(center + midStarSize, center)
                lineTo(center, center + midStarSize)
                lineTo(center - midStarSize, center)
                close()
            }
            
            drawPath(
                path = midPath,
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC), // Very light gray
                        Color(0xFFE2E8F0)  // Light gray
                    ),
                    radius = midStarSize
                )
            )
            
            // Inner highlight diamond
            val innerPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(center, center - innerStarSize)
                lineTo(center + innerStarSize, center)
                lineTo(center, center + innerStarSize)
                lineTo(center - innerStarSize, center)
                close()
            }
            
            drawPath(
                path = innerPath,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            // Central highlight dot for extra polish
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = 2f,
                center = androidx.compose.ui.geometry.Offset(center, center)
            )
        }
    }
}

/**
 * Extension function to capitalize the first letter of each word
 */
private fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        if (word.isNotEmpty()) {
            word.lowercase().replaceFirstChar { it.uppercase() }
        } else {
            word
        }
    }
}

/**
 * Validate email format
 */
private fun validateEmail(email: String): String? {
    return when {
        email.isBlank() -> "Email is required"
        !email.contains("@") -> "Invalid email format"
        !email.contains(".") -> "Invalid email format"
        email.count { it == '@' } != 1 -> "Invalid email format"
        email.startsWith("@") || email.endsWith("@") -> "Invalid email format"
        else -> null
    }
}

/**
 * Validate password strength
 */
private fun validatePassword(password: String, isRegistration: Boolean): String? {
    return when {
        password.isBlank() -> "Password is required"
        isRegistration && password.length < 6 -> "Password must be at least 6 characters"
        isRegistration && !password.any { it.isDigit() } -> "Password must contain at least one number"
        else -> null
    }
}

/**
 * Validate name fields
 */
private fun validateName(name: String, fieldName: String): String? {
    return when {
        name.isBlank() -> "$fieldName is required"
        name.length < 2 -> "$fieldName must be at least 2 characters"
        else -> null
    }
}

/**
 * Handle forgot password request
 */
private suspend fun handleForgotPassword(
    email: String,
    onMessage: (String) -> Unit
) {
    try {
        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            onMessage("Please enter a valid email address")
            return
        }
        
        // Create API client and auth service
        val apiClient = com.north.mobile.data.api.ApiClient()
        val authApiService = com.north.mobile.data.api.AuthApiService(apiClient)
        val authRepository = com.north.mobile.data.repository.AuthRepository(authApiService)
        
        // Call the password reset API
        val result = authRepository.requestPasswordReset(email)
        
        result.fold(
            onSuccess = { message ->
                println("✅ Password reset request successful")
                onMessage(message)
            },
            onFailure = { error ->
                println("❌ Password reset request failed: ${error.message}")
                onMessage(error.message ?: "Failed to send reset link. Please try again.")
            }
        )
    } catch (e: Exception) {
        println("❌ Password reset error: ${e.message}")
        onMessage("Failed to send reset link. Please try again.")
    }
}

/**
 * Handle authentication (login or register) with enhanced UX
 */
private suspend fun handleAuth(
    isLogin: Boolean,
    email: String,
    password: String,
    firstName: String,
    lastName: String,
    onLoading: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onSuccess: () -> Unit
) {
    onLoading(true)
    onError(null)
    
    try {
        // Create API client and auth service
        val apiClient = com.north.mobile.data.api.ApiClient()
        val authApiService = com.north.mobile.data.api.AuthApiService(apiClient)
        val authRepository = com.north.mobile.data.repository.AuthRepository(authApiService)
        
        val result = if (isLogin) {
            authRepository.login(email, password)
        } else {
            authRepository.register(email, password, firstName, lastName)
        }
        
        result.fold(
            onSuccess = { user ->
                println("✅ Authentication successful: ${user.email}")
                onSuccess()
            },
            onFailure = { error ->
                println("❌ Authentication failed: ${error.message}")
                onError(error.message ?: "Authentication failed")
            }
        )
    } catch (e: Exception) {
        println("❌ Authentication error: ${e.message}")
        onError(e.message ?: "Authentication failed")
    } finally {
        onLoading(false)
    }
}