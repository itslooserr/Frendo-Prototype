package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.auth.FirebaseAuthManager
import android.app.Activity
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.crypto.EncryptionUtils
import com.example.data.Contact
import com.example.data.Message
import com.example.data.UserSession
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.example.ui.theme.LocalAppThemePreferences

// Dynamic text size multiplier extension helper
@Composable
fun Float.scaled(session: UserSession?) = (this * LocalAppThemePreferences.current.fontSizeMultiplier).sp

@Composable
fun Int.scaled(session: UserSession?) = (this.toFloat() * LocalAppThemePreferences.current.fontSizeMultiplier).sp

@Composable
fun Float.scaled() = (this * LocalAppThemePreferences.current.fontSizeMultiplier).sp

@Composable
fun Int.scaled() = (this.toFloat() * LocalAppThemePreferences.current.fontSizeMultiplier).sp

// Palette Definitions
val PaletteAzure = "#0288D1"
val PaletteEmerald = "#2E7D32"
val PaletteAmber = "#F57C00"
val PaletteRose = "#D32F2F"
val PaletteOrchid = "#7B1FA2"

val WallpaperWhite = "#FFFFFF"
val WallpaperSand = "#FAF0D7"
val WallpaperMint = "#DCEFE1"
val WallpaperSky = "#DCEBF7"
val WallpaperPeach = "#FFE9D6"
val WallpaperLilac = "#F3E8FF"

@Composable
fun AppNavigator(viewModel: SecureTextViewModel) {
    val userSession by viewModel.userSession.collectAsState()
    val activeContact by viewModel.activeChatContact.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = when {
                userSession == null -> "onboarding"
                activeContact == null -> "dashboard"
                else -> "chat"
            },
            transitionSpec = {
                fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                "onboarding" -> OnboardingScreen(viewModel)
                "dashboard" -> {
                    val session = userSession
                    if (session != null) {
                        DashboardScreen(viewModel, session)
                    }
                }
                "chat" -> {
                    val session = userSession
                    val contact = activeContact
                    if (session != null && contact != null) {
                        ChatScreen(viewModel, session, contact)
                    }
                }
            }
        }
    }
}

/**
 * App Logo Composable: Displays Frendo with custom orange chat bubble containing a heart.
 * Exactly replicates the brand identity of the provided asset.
 */
@Composable
fun FrendoLogoHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(90.dp)) {
                // Outer clean circle
                drawCircle(
                    color = Color(0xFFFF9100).copy(alpha = 0.15f),
                    radius = size.width / 2f
                )
            }
            // Styled Chat icon bubble containing a heart
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF9100),
                modifier = Modifier.size(54.dp)
            )
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Frendo",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Text(
            text = "Ultra-Clean • Peer-Closed • AES Secured",
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

/**
 * OnboardingScreen: WhatsApp structure with phone verification stage,
 * real-time system OTP simulations, and Profile instantiation fields.
 */


/**
 * OnboardingScreen: WhatsApp-structured onboarding featuring:
 * 1. REAL & HIGH-FIDELITY SIMULATED Firebase OTP Phone Authentication flow.
 * 2. Pre-onboarding Quick Customization bar for dynamic dark mode & font-scaling options.
 * 3. SMS simulation banner with auto-fill option.
 * 4. Active Firebase client state with log updates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(viewModel: SecureTextViewModel) {
    var step by remember { mutableStateOf(1) } // 1: Phone, 2: OTP, 3: Profile Setup
    var phoneNumber by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("frendosecret") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isPassphraseVisible by remember { mutableStateOf(false) }

    // Onboarding Visual Customization states (collected from ViewModel)
    val onboardingIsDarkMode by viewModel.onboardingIsDarkMode.collectAsState()
    val onboardingFontSizeMultiplier by viewModel.onboardingFontSizeMultiplier.collectAsState()
    
    // Resolve current preferences (fallback to system defaults if null)
    val activeDark = onboardingIsDarkMode ?: isSystemInDarkTheme()
    val activeSize = onboardingFontSizeMultiplier ?: 1.0f

    // Auth Mode Selection (Simulated fallback vs Real Firebase Auth)
    var isSimulatedMode by remember { mutableStateOf(true) }
    var authLogs by remember { mutableStateOf(listOf("System: Ready for Frendo Authentication.")) }
    var showFirebaseSetupHelp by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    
    // Timer details
    var countdownSeconds by remember { mutableStateOf(30) }
    var isTimerActive by remember { mutableStateOf(false) }

    // Firebase state tracking
    var firebaseVerificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Countdown Timer Hook
    LaunchedEffect(isTimerActive, countdownSeconds) {
        if (isTimerActive && countdownSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            countdownSeconds--
            if (countdownSeconds == 0) {
                isTimerActive = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 1. Accessibility & Dynamic Theme Adjustment Block
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🎨 ONBOARDING EXPERIMENT ENGINE",
                                fontSize = 10.scaled(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Test Accessibility Controls Live",
                                fontSize = 9.scaled(),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Dark Theme Switcher
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (activeDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = "Theme Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Night Mode",
                                    fontSize = 11.scaled(),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Switch(
                                    checked = activeDark,
                                    onCheckedChange = { viewModel.setOnboardingTheme(it) },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (activeDark) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }

                            // Dynamic Font Scaling row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Text Size:",
                                    fontSize = 11.scaled(),
                                    fontWeight = FontWeight.SemiBold
                                )
                                listOf(0.85f to "S", 1.0f to "M", 1.2f to "L", 1.4f to "XL").forEach { (mult, label) ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (activeSize == mult) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .clickable { viewModel.setOnboardingFontSize(mult) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (activeSize == mult) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                FrendoLogoHeader(modifier = Modifier.padding(vertical = 8.dp))

                // Mode Selector Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeModeContainerColor = MaterialTheme.colorScheme.surface
                    val activeModeContentColor = MaterialTheme.colorScheme.primary

                    Button(
                        onClick = { isSimulatedMode = true; errorMsg = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSimulatedMode) activeModeContainerColor else Color.Transparent,
                            contentColor = if (isSimulatedMode) activeModeContentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulated Mode", fontSize = 10.scaled(), fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            if (FirebaseAuthManager.isFirebaseInitialized) {
                                isSimulatedMode = false
                                errorMsg = null
                            } else {
                                authLogs = authLogs + "[Error] Failed to initialize real Firebase Auth Client. Missing google-services.json!"
                                showFirebaseSetupHelp = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSimulatedMode) activeModeContainerColor else Color.Transparent,
                            contentColor = if (!isSimulatedMode) activeModeContentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Real Firebase SDK", fontSize = 10.scaled(), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Inline Help block if standard Firebase unavailable
                if (showFirebaseSetupHelp) {
                    AlertDialog(
                        onDismissRequest = { showFirebaseSetupHelp = false },
                        confirmButton = {
                            TextButton(onClick = { showFirebaseSetupHelp = false }) {
                                Text("Acknowledge & Use Sandbox Mode")
                            }
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9100))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Firebase Setup Required", fontSize = 16.scaled())
                            }
                        },
                        text = {
                            Text(
                                text = "To use the real Firebase SDK, add your 'google-services.json' to the 'app/' directory of your project and configure the Google Services buildscript plugin. Under Frendo's resilient design, we automatically fallback to our developer-friendly Simulated Mode so you can preview all features without any friction.",
                                fontSize = 12.scaled()
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Authentication Panel Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (step) {
                            1 -> {
                                Text(
                                    text = "FIREBASE SMS OTP PORTAL",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.scaled(),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isSimulatedMode) {
                                        "Authenticate with our local secure sandbox. Type any valid phone number below to trigger SMS verification."
                                    } else {
                                        "Authenticate with real Firebase Phone SMS network. Code will be dispatched to your physical phone."
                                    },
                                    fontSize = 12.scaled(),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = { phoneNumber = it },
                                    label = { Text("Phone Number", fontSize = 12.scaled()) },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_phone_input"),
                                    placeholder = { Text("+1 (555) 123-4567", fontSize = 12.scaled()) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                if (isSendingOtp) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Sending Firebase SMS Code...", fontSize = 11.scaled(), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            2 -> {
                                Text(
                                    text = "ENTER 6-DIGIT VERIFICATION CODE",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.scaled(),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                // Real-Time SMS Notification Simulation Auto-Filler (Aesthetic Excellence)
                                if (isSimulatedMode) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9100).copy(alpha = 0.12f)),
                                        border = BorderStroke(1.dp, Color(0xFFFF9100).copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clickable { 
                                                    enteredOtp = "123456"
                                                    authLogs = authLogs + "[AutoFill] Populated 123456 from incoming SMS notification."
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Sms, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "💬 [Simulated SMS Provider] Frendo Code:",
                                                    fontSize = 10.scaled(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF9100)
                                                )
                                                Text(
                                                    text = "Code is 123456. Tap here to auto-fill.",
                                                    fontSize = 12.scaled(),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("A physical Firebase OTP is loading... check your phone messages.", fontSize = 11.scaled(), color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                OutlinedTextField(
                                    value = enteredOtp,
                                    onValueChange = { enteredOtp = it },
                                    label = { Text("verification OTP Code", fontSize = 12.scaled()) },
                                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_otp_input"),
                                    placeholder = { Text("Code e.g. 123456", fontSize = 12.scaled()) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                // Countdown Timer & Resender Link Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (countdownSeconds > 0) {
                                        Text(
                                            text = "Resend code in ${countdownSeconds}s",
                                            fontSize = 11.scaled(),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        TextButton(
                                            onClick = {
                                                countdownSeconds = 30
                                                isTimerActive = true
                                                enteredOtp = ""
                                                authLogs = authLogs + "[System] Requesting OTP Code retransmission..."
                                                if (isSimulatedMode) {
                                                    authLogs = authLogs + "[Simulation] SMS delivered successfully! ID: r-sms-9189"
                                                } else {
                                                    // Trigger retry on firebase
                                                    val activity = context as? Activity
                                                    if (activity != null && phoneNumber.isNotBlank()) {
                                                        FirebaseAuthManager.sendVerificationCode(
                                                            activity = activity,
                                                            phoneNumber = phoneNumber,
                                                            onCodeSent = { id, tok ->
                                                                firebaseVerificationId = id
                                                                resendToken = tok
                                                                authLogs = authLogs + "[Firebase] Code resubmitted."
                                                            },
                                                            onVerificationCompleted = { cred ->
                                                                enteredOtp = cred.smsCode ?: ""
                                                            },
                                                            onVerificationFailed = { ex ->
                                                                errorMsg = "Verification failed: ${ex.localizedMessage}"
                                                            }
                                                        )
                                                    }
                                                }
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Resend Code via SMS", fontSize = 11.scaled(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            step = 1
                                            isTimerActive = false
                                            enteredOtp = ""
                                            errorMsg = null
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Change Phone", fontSize = 11.scaled(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }
                            3 -> {
                                Text(
                                    text = "SECURE YOUR PROFILE METADATA",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.scaled(),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Define your offline cryptographic passphrase inside Room database.",
                                    fontSize = 12.scaled(),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Display Nickname", fontSize = 12.scaled()) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_name_input"),
                                    placeholder = { Text("e.g. Laxmikant", fontSize = 12.scaled()) }
                                )

                                OutlinedTextField(
                                    value = passphrase,
                                    onValueChange = { passphrase = it },
                                    label = { Text("Crypto Database Passphrase", fontSize = 12.scaled()) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                    visualTransformation = if (isPassphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isPassphraseVisible = !isPassphraseVisible }) {
                                            Icon(
                                                imageVector = if (isPassphraseVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_pass_input"),
                                    placeholder = { Text("Encryption code", fontSize = 12.scaled()) }
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Keys reside purely locally in AES encrypted sandbox tables.",
                                        fontSize = 10.scaled(),
                                        lineHeight = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        if (errorMsg != null) {
                            Text(
                                text = errorMsg!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.scaled(),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Developer/Firebase Telemetry Console Log Panel
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📟 FIREBASE CLIENT CONSOLE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9100),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isSimulatedMode) "SIMULATED GATEWAY" else "REAL SDK CLIENT",
                                    fontSize = 8.sp,
                                    color = Color.Green,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Column {
                                    authLogs.forEach { log ->
                                        Text(
                                            text = log,
                                            fontSize = 8.sp,
                                            color = Color.LightGray,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Submissions Actions
            Button(
                onClick = {
                    errorMsg = null
                    when (step) {
                        1 -> {
                            val trimmedPhone = phoneNumber.trim()
                            if (trimmedPhone.length < 7) {
                                errorMsg = "Please enter a valid phone number."
                                authLogs = authLogs + "[Validation] Invalid phone registration format rejected."
                            } else {
                                isSendingOtp = true
                                if (isSimulatedMode) {
                                    // Simulated flow with delays
                                    authLogs = authLogs + "19:28:44 [LocalSimulator] Requesting recaptcha challenge..."
                                    scope.launch {
                                        kotlinx.coroutines.delay(800)
                                        authLogs = authLogs + "19:28:45 [LocalSimulator] Recaptcha bypass resolved. Dispatching code..."
                                        kotlinx.coroutines.delay(700)
                                        authLogs = authLogs + "19:28:46 [LocalSimulator] SMS dispatched to: $trimmedPhone"
                                        isSendingOtp = false
                                        step = 2
                                        countdownSeconds = 30
                                        isTimerActive = true
                                    }
                                } else {
                                    // Real Firebase setup
                                    val activity = context as? Activity
                                    if (activity != null) {
                                        authLogs = authLogs + "[Firebase] Connection check: requesting SDK connection..."
                                        FirebaseAuthManager.sendVerificationCode(
                                            activity = activity,
                                            phoneNumber = trimmedPhone,
                                            onCodeSent = { verificationId, token ->
                                                firebaseVerificationId = verificationId
                                                resendToken = token
                                                isSendingOtp = false
                                                step = 2
                                                countdownSeconds = 30
                                                isTimerActive = true
                                                authLogs = authLogs + "[Firebase] OnCodeSent verificationId: $verificationId"
                                            },
                                            onVerificationCompleted = { credential ->
                                                enteredOtp = credential.smsCode ?: ""
                                                isSendingOtp = false
                                                authLogs = authLogs + "[Firebase] Verification Auto-completed successfully."
                                            },
                                            onVerificationFailed = { ex ->
                                                isSendingOtp = false
                                                errorMsg = "Firebase transmission error: ${ex.localizedMessage}. Reverting to high-integrity simulated engine..."
                                                authLogs = authLogs + "[Firebase Error] ${ex.localizedMessage}"
                                                // Fallback so user is not blocked
                                                isSimulatedMode = true
                                            }
                                        )
                                    } else {
                                        isSendingOtp = false
                                        errorMsg = "Failed to bind Firebase trigger to standard Android Activity context."
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (isSimulatedMode) {
                                if (enteredOtp.trim() != "123456") {
                                    errorMsg = "Incorrect Verification OTP Code. Enter '123456' or tap notification to fill."
                                    authLogs = authLogs + "[Validation] Wrong code input."
                                } else {
                                    authLogs = authLogs + "[LocalSimulator] Auth validation verified successfully."
                                    step = 3
                                }
                            } else {
                                if (enteredOtp.trim().length < 6) {
                                    errorMsg = "Code must be 6 digits."
                                } else {
                                    authLogs = authLogs + "[Firebase] Submitting credential verification packet..."
                                    FirebaseAuthManager.verifyCredential(
                                        verificationId = firebaseVerificationId,
                                        otpCode = enteredOtp.trim(),
                                        onSuccess = {
                                            authLogs = authLogs + "[Firebase] Sign in successful. Moving to profile setup."
                                            step = 3
                                        },
                                        onFailure = { ex ->
                                            errorMsg = "Incorrect code: ${ex.localizedMessage}"
                                            authLogs = authLogs + "[Firebase Error] Authentication credentials rejected: ${ex.localizedMessage}"
                                        }
                                    )
                                }
                            }
                        }
                        3 -> {
                            if (name.isBlank() || passphrase.isBlank()) {
                                errorMsg = "Complete all identity attributes to initialize."
                            } else {
                                viewModel.registerUser(phoneNumber.trim(), name.trim(), passphrase)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_submit_button"),
                enabled = !isSendingOtp
            ) {
                Icon(
                    imageVector = if (step == 3) Icons.Default.LockOpen else Icons.Default.ChevronRight,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (step) {
                        1 -> "SEND SMS CODE"
                        2 -> "VERIFY OTP CODE"
                        else -> "INITIALIZE ENCRYPTED ENGINE"
                    },
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.scaled()
                )
            }
        }
    }
}

/**
 * DashboardScreen: Split into "Chats / Messages" and "Profile & Customize" sections.
 * Clean, flat, WhatsApp aesthetic.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: SecureTextViewModel, session: UserSession) {
    val contacts by viewModel.contacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showCiphertext by viewModel.showCiphertextGlobal.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Chats, 1: Profile & Customize

    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                    label = { Text("Chats", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Profile & Settings") },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        floatingActionButton = {
            if (activeTab == 0) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Small floating button for group creation
                    FloatingActionButton(
                        onClick = { showAddGroupDialog = true },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(46.dp).testTag("fab_add_group")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "New Group", modifier = Modifier.size(20.dp))
                    }
                    // Main primary action button
                    FloatingActionButton(
                        onClick = { showAddContactDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier
                            .testTag("fab_add_contact")
                            .padding(bottom = 6.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> {
                    // TAB 0: CHATS LIST
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header / Title Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Frendo Messages",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Channel: ${session.phoneNumber}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            // Global toggle button for decryption/ciphertext
                            Button(
                                onClick = { viewModel.toggleShowCiphertext() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (showCiphertext) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                            ) {
                                Text(
                                    text = if (showCiphertext) "CIPHER ON" else "DECRYPT ON",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (showCiphertext) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Sandbox environment security card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HealthAndSafety,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "E2EE AES Tunnel Fully Primed",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Keys derived on-the-fly dynamically.",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Search box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search chats or groups...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_search_input")
                        )

                        Text(
                            text = "ACTIVE CHANNELS (${contacts.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )

                        if (contacts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No private chats or groups yet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Create a new individual contact or secure group channel to begin encrypting.",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(contacts) { contact ->
                                    ContactItemRow(
                                        contact = contact,
                                        onSelect = { viewModel.selectContact(contact) },
                                        onDelete = { viewModel.deleteContact(contact) }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: PROFILE & CUSTOMIZATION & ACCESSIBILITY
                    ProfileCustomizeSection(viewModel, session)
                }
            }
        }
    }

    if (showAddContactDialog) {
        AddContactDialog(
            onDismiss = { showAddContactDialog = false },
            onConfirm = { name, number ->
                viewModel.createContact(name, number)
                showAddContactDialog = false
            }
        )
    }

    if (showAddGroupDialog) {
        AddGroupDialog(
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { name, members ->
                viewModel.createGroup(name, members)
                showAddGroupDialog = false
            }
        )
    }
}

/**
 * Profile & Customization Dashboard section: Includes Profile adjustments, font size multipliers,
 * bright wallpaper backgrounds, and easy Dark mode switches.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCustomizeSection(viewModel: SecureTextViewModel, session: UserSession) {
    var editName by remember { mutableStateOf(session.name) }
    var selectedColorHex by remember { mutableStateOf(session.avatarColorHex) }
    var selectedEmoji by remember { mutableStateOf(session.avatarEmoji) }

    val scope = rememberCoroutineScope()

    val colorsPalette = listOf(PaletteAzure, PaletteEmerald, PaletteAmber, PaletteRose, PaletteOrchid)
    val emojisPalette = listOf("👤", "💬", "🍀", "🦁", "🐼", "🦊", "🚀", "⚡", "🐳", "🧁")

    val wallpapers = listOf(
        WallpaperWhite to "Fresh White",
        WallpaperSand to "Soft Sand",
        WallpaperMint to "Pastel Mint",
        WallpaperSky to "Clear Sky",
        WallpaperPeach to "Warm Peach",
        WallpaperLilac to "Pale Lilac"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App settings header
        Text(
            text = "Profile & Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 10.dp)
        )

        // VISUAL PROFILE PANEL (AVATAR IS VISIBLE TO EVERYONE)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "PUBLIC CHAT IDENTIFIER",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Large Avatar using user's customized choices
                val activeBgColor = try {
                    Color(android.graphics.Color.parseColor(selectedColorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(activeBgColor)
                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedEmoji,
                        fontSize = 44.sp
                    )
                }

                Text(
                    text = "This avatar and nickname is shared globally inside tunnels.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Public Nickname") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_nickname_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Choose Background Color Circle Palettes
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Customize Avatar Background",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorsPalette.forEach { hex ->
                            val colorVal = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(colorVal)
                                    .border(
                                        width = if (selectedColorHex == hex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                }

                // Choose public emoji icon
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Customize Avatar Emoji",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        emojisPalette.take(5).forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedEmoji == emoji) MaterialTheme.colorScheme.secondaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { selectedEmoji = emoji }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        emojisPalette.drop(5).forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedEmoji == emoji) MaterialTheme.colorScheme.secondaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { selectedEmoji = emoji }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.updateUserProfile(editName.trim(), selectedColorHex, selectedEmoji)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_profile_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE PROFILE DETAILS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // DESIGN CUSTOMIZATION & ACCESSIBILITY OPTIONS (FONT SIZE, BRIGHT COLORS, DARK MODE)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ACCESSIBILITY & THEMING",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // 1. Accessibility Size Adjuster
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Text Font Size Scaling",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val textTagLabel = when (session.fontSizeMultiplier) {
                            0.85f -> "Small"
                            1.2f -> "Large"
                            1.4f -> "Extra Large"
                            else -> "Medium (Default)"
                        }
                        Text(
                            text = textTagLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        listOf(0.85f to "S", 1.0f to "M", 1.2f to "L", 1.4f to "XL").forEach { (multiplier, label) ->
                            Button(
                                onClick = {
                                    viewModel.updateThemeCustomizations(
                                        chatBgColorHex = session.chatBgColorHex,
                                        fontSizeMultiplier = multiplier,
                                        isDarkMode = session.isDarkMode
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (session.fontSizeMultiplier == multiplier) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (session.fontSizeMultiplier == multiplier) Color.White
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 2. ONLY Bright Wallpaper Colors Picker
                Column {
                    Text(
                        text = "Chat Bright Wallpaper",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        wallpapers.take(4).forEach { (hexCode, label) ->
                            val isSelected = session.chatBgColorHex == hexCode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(android.graphics.Color.parseColor(hexCode)))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.updateThemeCustomizations(
                                            chatBgColorHex = hexCode,
                                            fontSizeMultiplier = session.fontSizeMultiplier,
                                            isDarkMode = session.isDarkMode
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label.split(" ").last(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        wallpapers.drop(4).forEach { (hexCode, label) ->
                            val isSelected = session.chatBgColorHex == hexCode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(android.graphics.Color.parseColor(hexCode)))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.updateThemeCustomizations(
                                            chatBgColorHex = hexCode,
                                            fontSizeMultiplier = session.fontSizeMultiplier,
                                            isDarkMode = session.isDarkMode
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label.split(" ").last(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(2f)) // blank balance
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // 3. Dark Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Dark / Night Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Instantly toggle AMOLED-friendly visuals.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = session.isDarkMode,
                        onCheckedChange = { active ->
                            viewModel.updateThemeCustomizations(
                                chatBgColorHex = session.chatBgColorHex,
                                fontSizeMultiplier = session.fontSizeMultiplier,
                                isDarkMode = active
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // LOGOUT ACTION
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.logout() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Unregister Frendo Container",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Flush SQLite keys & memory data.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * ContactRow: Displays private chats and group conversations with customized visuals and last status.
 */
@Composable
fun ContactItemRow(
    contact: Contact,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Background Color Parser
            val avatarBgColor = try {
                Color(android.graphics.Color.parseColor(contact.avatarColorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarBgColor),
                contentAlignment = Alignment.Center
            ) {
                if (contact.isGroup) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    // Regular initials
                    val letter = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?"
                    Text(
                        text = letter,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = contact.name,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (contact.isGroup) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "GROUP",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (contact.isGroup) "Members: ${contact.groupMembersJson}" else contact.phoneNumber,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = contact.status,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_contact_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Remove Contact Node",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * AddContactDialog: Creates a direct individual E2EE chat connection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("E2EE Direct Channel Creation", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Secure Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && phoneNumber.isNotBlank()) onConfirm(name, phoneNumber) },
                enabled = name.isNotBlank() && phoneNumber.isNotBlank()
            ) {
                Text("ESTABLISH TUNNEL")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * AddGroupDialog: Creates a shared Group channel with custom participant mappings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var membersList by remember { mutableStateOf("") } // Comma separated list of member names

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("E2EE Group Container Connection", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Initiate a dynamic multi-receiver private keys workspace.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Core Devs") }
                )
                OutlinedTextField(
                    value = membersList,
                    onValueChange = { membersList = it },
                    label = { Text("Group Members (comma separated)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Alice Vance, Bob Miller, Sarah Jenkins") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (groupName.isNotBlank() && membersList.isNotBlank()) onConfirm(groupName, membersList) },
                enabled = groupName.isNotBlank() && membersList.isNotBlank()
            ) {
                Text("SPAWN GROUP WORKSPACE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

/**
 * ChatScreen: Supports dynamic Bright wallpaper selection, dynamic Font Multipliers,
 * and highlights sender avatar details. Fits standard WhatsApp minimalism.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: SecureTextViewModel, session: UserSession, contact: Contact) {
    val messages by viewModel.activeChatMessages.collectAsState()
    val showCiphertextGlobal by viewModel.showCiphertextGlobal.collectAsState()

    var inputMessageText by remember { mutableStateOf("") }
    var selectedInspectMessage by remember { mutableStateOf<Message?>(null) }

    val listState = rememberLazyListState()

    // Auto-scroll on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Capture User Custom Background Wallpaper
    val chatWallpaperBg = try {
        Color(android.graphics.Color.parseColor(session.chatBgColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.background
    }

    Scaffold(
        containerColor = chatWallpaperBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Contact Color Avatar
                        val headerAvatarBg = try {
                            Color(android.graphics.Color.parseColor(contact.avatarColorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(headerAvatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            if (contact.isGroup) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = contact.name,
                                fontSize = 14f.scaled(session),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(9.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (contact.isGroup) "GROUP SECURED ENCRYPTION" else "E2EE ACTIVE CHANNEL",
                                    fontSize = 8f.scaled(session),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.selectContact(null) },
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowCiphertext() }) {
                        Icon(
                            imageVector = if (showCiphertextGlobal) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Ciphertext View",
                            tint = if (showCiphertextGlobal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // E2EE tip bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Press any bubble to inspect live AES key derivation & packet payload arrays.",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 9f.scaled(session),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Message list (Scrolling region)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp)
            ) {
                items(messages) { message ->
                    ChatBubbleItem(
                        message = message,
                        showCiphertext = showCiphertextGlobal,
                        session = session,
                        senderPicColorHex = if (message.isUserSender) session.avatarColorHex else contact.avatarColorHex,
                        senderPicEmoji = if (message.isUserSender) session.avatarEmoji else null,
                        onInspectMessage = { selectedInspectMessage = message }
                    )
                }
            }

            // Input bar (Bottom fixed dock)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessageText,
                    onValueChange = { inputMessageText = it },
                    placeholder = { Text("Write encrypted text...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(22.dp),
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_text_input"),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (inputMessageText.isNotBlank()) {
                                    viewModel.sendSecureMessage(inputMessageText)
                                    inputMessageText = ""
                                }
                            },
                            enabled = inputMessageText.isNotBlank(),
                            modifier = Modifier.testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.Send,
                                contentDescription = "Send",
                                tint = if (inputMessageText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                )
            }
        }
    }

    if (selectedInspectMessage != null) {
        CryptographicInspectDialog(
            message = selectedInspectMessage!!,
            onDismiss = { selectedInspectMessage = null }
        )
    }
}

/**
 * ChatBubbleItem: Includes user/contact profile avatar choices and sender profile pictures.
 */
@Composable
fun ChatBubbleItem(
    message: Message,
    showCiphertext: Boolean,
    session: UserSession,
    senderPicColorHex: String,
    senderPicEmoji: String?,
    onInspectMessage: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeFormatted = formatter.format(Date(message.timestamp))

    val decryptedText = remember(message.cipherText, message.ivString, message.passphraseUsed) {
        EncryptionUtils.decrypt(message.cipherText, message.ivString, message.passphraseUsed)
    }

    val isUser = message.isUserSender
    val alignment = if (isUser) Alignment.End else Alignment.Start

    // Light-mode bright color scheme vs Dark mode colors
    val bubbleBgColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textBubbleColor = MaterialTheme.colorScheme.onSurfaceVariant

    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 2.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            // Display Sender's Active Profile Pic Circle (Visible to everyone!)
            val senderAvatarColor = try {
                Color(android.graphics.Color.parseColor(senderPicColorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .padding(end = 6.dp, bottom = 4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(senderAvatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (message.senderName.isNotEmpty()) message.senderName.firstOrNull()?.toString()?.uppercase() ?: "•" else "•",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleBgColor)
                .clickable { onInspectMessage() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Sender's Name in groups
            if (!isUser && message.senderName.isNotEmpty()) {
                Text(
                    text = message.senderName,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10f.scaled(session),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            if (showCiphertext) {
                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AES-CBC ciphertext",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 8f.scaled(session),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = message.cipherText,
                        color = textBubbleColor,
                        fontSize = 10f.scaled(session),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            } else {
                Text(
                    text = decryptedText,
                    color = textBubbleColor,
                    fontSize = 14f.scaled(session)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = timeFormatted,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 8f.scaled(session)
                )
                if (isUser) {
                    MessageDeliveryStatusIcon(status = message.deliveryStatus)
                }
            }
        }

        if (isUser) {
            // Display User's Custom Emoji Profile Pic on the right
            val userAvatarColor = try {
                Color(android.graphics.Color.parseColor(senderPicColorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .padding(start = 6.dp, bottom = 4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(userAvatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = senderPicEmoji ?: "👤",
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun MessageDeliveryStatusIcon(status: String) {
    val tintColor = MaterialTheme.colorScheme.primary
    when (status) {
        "SENDING" -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Sending",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(11.dp)
            )
        }
        "SENT" -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Sent",
                tint = tintColor.copy(alpha = 0.6f),
                modifier = Modifier.size(11.dp)
            )
        }
        "DELIVERED" -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = tintColor.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
        }
        "READ" -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read status",
                tint = Color(0xFF2196F3), // Classic WhatsApp read blue double ticks!
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun CryptographicInspectDialog(
    message: Message,
    onDismiss: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) }
    val timeFormatted = formatter.format(Date(message.timestamp))

    val decryptedText = remember(message.cipherText, message.ivString, message.passphraseUsed) {
        EncryptionUtils.decrypt(message.cipherText, message.ivString, message.passphraseUsed)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CIPHER WORKSPACE",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                MetricItem(label = "CIPHER ALGORITHM", value = "AES-CBC 128-bit PKCS5Padding with derived IV")
                MetricItem(label = "DYNAMIC IV ARRAY", value = message.ivString, monospace = true)
                MetricItem(label = "CIPHERTEXT HEX ENVELOPE", value = message.cipherText, monospace = true)
                MetricItem(label = "DERIVED KEY SEED", value = message.passphraseUsed, monospace = true)
                MetricItem(label = "DECRYPTED PLAIN TEXT", value = "\"$decryptedText\"", highlight = true)
                MetricItem(label = "TIMESTAMP SIGNATURE", value = timeFormatted)
                MetricItem(
                    label = "LINE DIRECTIVITY",
                    value = if (message.isUserSender) "OUTGOING PORT" else "INCOMING PORT"
                )
                MetricItem(label = "DELIVERY STATUS INDEX", value = message.deliveryStatus)

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text("SECURE RETURN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    monospace: Boolean = false,
    highlight: Boolean = false
) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = if (highlight) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(8.dp)
        )
    }
}

@Composable
fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return androidx.compose.foundation.rememberScrollState()
}
