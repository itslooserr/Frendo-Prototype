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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import android.provider.ContactsContract
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import com.example.crypto.EncryptionUtils
import com.example.data.Contact
import com.example.data.Message
import com.example.data.UserSession
import com.example.data.LocalAccount
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

// Dynamic formatting helper for phone numbers
fun formatPhoneNumber(raw: String): String {
    val clean = raw.filter { it.isDigit() }
    if (clean.isEmpty()) return ""
    return if (clean.length > 10) {
        "+" + clean.substring(0, clean.length - 10) + " " + clean.substring(clean.length - 10, clean.length - 5) + " " + clean.substring(clean.length - 5)
    } else if (clean.length == 10) {
        "+91 " + clean.take(5) + " " + clean.drop(5)
    } else {
        raw
    }
}

// Contacts provider helper function
fun getDeviceContacts(context: android.content.Context): List<Contact> {
    val contactsList = mutableListOf<Contact>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
        return contactsList
    }
    try {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                if (nameIdx >= 0 && numIdx >= 0) {
                    val name = it.getString(nameIdx)
                    var num = it.getString(numIdx) ?: ""
                    num = num.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
                    if (num.isNotBlank() && name.isNotBlank()) {
                        val colors = listOf("#E57373", "#81C784", "#64B5F6", "#FFB74D", "#BA68C8", "#4DB6AC", "#D4E157")
                        contactsList.add(
                            Contact(
                                id = 0,
                                phoneNumber = num,
                                name = name,
                                status = "Hey there! I am using Frendo.",
                                avatarColorHex = colors.random()
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return contactsList.distinctBy { it.phoneNumber }
}

@Composable
fun ProfileAvatar(
    customPfpPath: String?,
    avatarColorHex: String,
    avatarEmoji: String,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    emojiSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    isGroup: Boolean = false
) {
    val avatarBgColor = try {
        Color(android.graphics.Color.parseColor(avatarColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val pfpFile = customPfpPath?.let { File(it) }
    val customBitmap = remember(customPfpPath) {
        if (pfpFile != null && pfpFile.exists()) {
            try {
                BitmapFactory.decodeFile(pfpFile.absolutePath)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarBgColor),
        contentAlignment = Alignment.Center
    ) {
        if (isGroup) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size / 2)
            )
        } else if (customBitmap != null) {
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = "Profile Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback to active emoji
            Text(
                text = if (avatarEmoji.isBlank()) "👤" else avatarEmoji,
                fontSize = emojiSize
            )
        }
    }
}

@Composable
fun BotSliderPuzzle(
    onPassed: () -> Unit
) {
    var sliderValue by remember { mutableStateOf(0f) }
    var hasPassed by remember { mutableStateOf(false) }
    val targetValue = 75f // target offset percentage
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🤖 HUMAN VERIFICATION PUZZLE",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Slide the locker handle to slot on the right to complete verification.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                // Target slot outline
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val targetX = size.width * (targetValue / 100f)
                    drawRoundRect(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        topLeft = androidx.compose.ui.geometry.Offset(targetX - 12.dp.toPx(), size.height / 2f - 12.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(24.dp.toPx(), 24.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val maxW = maxWidth
                        val currentOffset = maxW * (sliderValue / 100f)
                        
                        Box(
                            modifier = Modifier
                                .offset(x = currentOffset)
                                .align(Alignment.CenterStart)
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (hasPassed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasPassed) Icons.Default.Check else Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (!hasPassed) {
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        if (kotlin.math.abs(it - targetValue) < 4f) {
                            hasPassed = true
                            sliderValue = targetValue
                            onPassed()
                        }
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth().testTag("bot_verification_slider")
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                    Text(
                        text = "Puzzle Solved! Tap Continue below.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

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
    var countryPrefix by remember { mutableStateOf("+91") }
    var enteredPhoneCore by remember { mutableStateOf("") }
    
    // Derived formatted phone
    val phoneNumber = remember(countryPrefix, enteredPhoneCore) {
        countryPrefix + enteredPhoneCore.filter { it.isDigit() }
    }
    
    var enteredOtp by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("frendosecret") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isPassphraseVisible by remember { mutableStateOf(false) }

    // Multiple Offline Users Switcher state
    val localAccounts by viewModel.allLocalAccounts.collectAsState()
    var showRegistrationForm by remember { mutableStateOf(false) }
    var isBotVerified by remember { mutableStateOf(false) }

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

    // Prefix selections list
    val prefixList = listOf("+91 (IN)", "+1 (US/CA)", "+44 (UK)", "+61 (AU)", "+81 (JP)", "+49 (DE)")
    var prefixExpanded by remember { mutableStateOf(false) }

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
                                text = "🎨 ONBOARDING CONFIGURATION ENGINE",
                                fontSize = 10.scaled(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
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

                FrendoLogoHeader(modifier = Modifier.padding(vertical = 4.dp))

                // If saved accounts exist and they haven't explicitly chosen to register a new session, show profiles grid/switcher!
                if (localAccounts.isNotEmpty() && !showRegistrationForm) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "👥 REGISTERED ACCOUNTS ON THIS DEVICE",
                        fontSize = 12.scaled(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "Clean multi-session manager. Select any local profile to unlock instantly without password or SMS check:",
                        fontSize = 11.scaled(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        localAccounts.forEach { account ->
                            Card(
                                onClick = { viewModel.loginAsAccount(account) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileAvatar(
                                        customPfpPath = account.customPfpPath,
                                        avatarColorHex = account.avatarColorHex,
                                        avatarEmoji = account.avatarEmoji,
                                        size = 46.dp,
                                        emojiSize = 22.sp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = account.phoneNumber,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = account.bio,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteLocalAccount(account.phoneNumber) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Unregister profile",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { 
                                showRegistrationForm = true 
                                isBotVerified = false
                                step = 1
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PersonAddAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LOGIN WITH ANOTHER NUMBER", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                } else {
                    // Show standard Registration Steps (Form)
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
                                val clipManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                val fingerprints = remember { getAppFingerprints(context) }
                                val sha1 = fingerprints?.first ?: "Pending retrieval... (Compile android target)"
                                val sha256 = fingerprints?.second ?: "Pending retrieval... (Compile android target)"
                                
                                Column(
                                    modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "To enable real Firebase Authentication (automatic SMS OTP login), please link your Firebase project by executing these step-by-step instructions:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        thickness = 1.dp
                                    )
                                    
                                    Text(
                                        text = "1. REGISTER APPLICATION ID",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                clipManager.setText(androidx.compose.ui.text.AnnotatedString("com.aistudio.securetexting.fquwla"))
                                                Toast.makeText(context, "Package Name copied!", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Package Name:", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                                Text("com.aistudio.securetexting.fquwla", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            }
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    
                                    Text(
                                        text = "2. ADD SHA DIGEST FINGERPRINTS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                clipManager.setText(androidx.compose.ui.text.AnnotatedString(sha1))
                                                Toast.makeText(context, "SHA-1 Fingerprint copied!", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("SHA-1 Fingerprint (Copy to Firebase settings):", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(sha1, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                            }
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                clipManager.setText(androidx.compose.ui.text.AnnotatedString(sha256))
                                                Toast.makeText(context, "SHA-256 Fingerprint copied!", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("SHA-256 Fingerprint (Copy to Firebase settings):", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(sha256, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                            }
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Text(
                                        text = "3. ACTIVATE PHONE AUTH SMS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "In your Firebase Console: Go to Authentication -> Sign-In Method -> Click 'Add New Provider' -> Select 'Phone' -> Enable & Click Save.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = "4. REPLACE CONFIGURATION FILE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Download the 'google-services.json' file from the settings page for your Android app in your Firebase Console, then save/replace it inside '/app/' directory. Frendo compiles seamlessly using your production credentials!",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                                            "Authenticate with our local secure sandbox. Choose your Country Code and write your phone number below."
                                        } else {
                                            "Authenticate with real Firebase Phone SMS network. Code will be dispatched to your physical phone."
                                        },
                                        fontSize = 12.scaled(),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )

                                    // Country Prefix dropdown switcher
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                                            Button(
                                                onClick = { prefixExpanded = true },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.height(56.dp)
                                            ) {
                                                Text(text = countryPrefix, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.padding(start = 2.dp))
                                            }
                                            DropdownMenu(
                                                expanded = prefixExpanded,
                                                onDismissRequest = { prefixExpanded = false }
                                            ) {
                                                prefixList.forEach { value ->
                                                    DropdownMenuItem(
                                                        text = { Text(value, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                                                        onClick = {
                                                            countryPrefix = value.split(" ").first()
                                                            prefixExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = enteredPhoneCore,
                                            onValueChange = { enteredPhoneCore = it },
                                            label = { Text("Phone Number", fontSize = 12.scaled()) },
                                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("onboarding_phone_input"),
                                            placeholder = { Text("e.g. 9876543210", fontSize = 12.scaled()) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                    }

                                    // Live visual check of correct sanitizer format
                                    if (enteredPhoneCore.isNotBlank()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Formatted System Target: " + formatPhoneNumber(phoneNumber),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }

                                    // BOT CONTROL PUZZLE
                                    BotSliderPuzzle(onPassed = {
                                        isBotVerified = true
                                        errorMsg = null
                                    })

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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submissions Actions
                    Button(
                        onClick = {
                            errorMsg = null
                            when (step) {
                                1 -> {
                                    if (enteredPhoneCore.trim().length < 8) {
                                        errorMsg = "Please enter a valid phone number (minimum 8 digits)."
                                        authLogs = authLogs + "[Validation] Invalid format digits count."
                                        return@Button
                                    }
                                    if (!isBotVerified) {
                                        errorMsg = "Drag the puzzle slider to verify you are not a bot!"
                                        authLogs = authLogs + "[Bot Challenge] Puzzle is unsolved."
                                        return@Button
                                    }

                                    isSendingOtp = true
                                    val trimmedPhone = phoneNumber.trim()
                                    if (isSimulatedMode) {
                                        // Simulated flow with delays
                                        authLogs = authLogs + "19:28:44 [LocalSimulator] Recaptcha bypass resolved. Dispatching code..."
                                        scope.launch {
                                            kotlinx.coroutines.delay(1200)
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
                                1 -> "CONTINUE TO SMS OTP"
                                2 -> "VERIFY OTP CODE"
                                else -> "INITIALIZE ENCRYPTED ENGINE"
                            },
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.scaled()
                        )
                    }

                    if (localAccounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = { showRegistrationForm = false }
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back to Saved Profiles list", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * DashboardScreen: Split into "Chats / Messages" and "Profile & Customize" sections.
 * Clean, flat, WhatsApp aesthetic.
 */
data class UnifiedContact(
    val phoneNumber: String,
    val name: String,
    val isGroup: Boolean,
    val isAppUser: Boolean,
    val statusHex: String,
    val statusText: String,
    val dbContactRef: Contact?
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: SecureTextViewModel, session: UserSession) {
    val contacts by viewModel.contacts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showCiphertext by viewModel.showCiphertextGlobal.collectAsState()
    val localAccounts by viewModel.allLocalAccounts.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Chats, 1: Profile & Customize
    var serverCategory by remember { mutableStateOf("All") } // Discord-style: "All", "Direct", "Groups", "Bots"

    var showProfileSwitcher by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Frendo linked and synced with phone book successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "READ_CONTACTS permission denied. Using local memory storage only.", Toast.LENGTH_SHORT).show()
        }
    }

    val deviceContacts = remember(hasContactsPermission) {
        if (hasContactsPermission) {
            getDeviceContacts(context)
        } else {
            emptyList()
        }
    }

    val unifiedContacts = remember(contacts, deviceContacts, searchQuery) {
        val appUserPhones = contacts.map { it.phoneNumber.replace(" ", "").replace("-", "").replace("+", "") }.toSet()
        val mergedList = mutableListOf<UnifiedContact>()

        // 1. Add DB contacts
        contacts.forEach { dbContact ->
            mergedList.add(
                UnifiedContact(
                    phoneNumber = dbContact.phoneNumber,
                    name = dbContact.name,
                    isGroup = dbContact.isGroup,
                    isAppUser = true,
                    statusHex = dbContact.avatarColorHex,
                    statusText = dbContact.status,
                    dbContactRef = dbContact
                )
            )
        }

        // 2. Add Device Contacts
        deviceContacts.forEach { devContact ->
            val cleanDevNum = devContact.phoneNumber.replace(" ", "").replace("-", "").replace("+", "")
            if (cleanDevNum.isNotBlank()) {
                val alreadyMerged = mergedList.any { 
                    it.phoneNumber.replace(" ", "").replace("-", "").replace("+", "") == cleanDevNum 
                }
                if (!alreadyMerged) {
                    val isUser = appUserPhones.contains(cleanDevNum)
                    mergedList.add(
                        UnifiedContact(
                            phoneNumber = devContact.phoneNumber,
                            name = devContact.name,
                            isGroup = false,
                            isAppUser = isUser,
                            statusHex = devContact.avatarColorHex,
                            statusText = if (isUser) "Hey there! I am using Frendo." else "Offline • Invite to Frendo",
                            dbContactRef = if (isUser) contacts.firstOrNull { 
                                it.phoneNumber.replace(" ", "").replace("-", "").replace("+", "") == cleanDevNum 
                            } else null
                        )
                    )
                }
            }
        }

        val filtered = if (searchQuery.isNotBlank()) {
            mergedList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery, ignoreCase = true)
            }
        } else {
            mergedList
        }

        // A-Z sorting, with app users & groups at top, like WhatsApp!
        val appUsers = filtered.filter { it.isAppUser || it.isGroup }.sortedBy { it.name.lowercase() }
        val nonUsers = filtered.filter { !it.isAppUser && !it.isGroup }.sortedBy { it.name.lowercase() }

        appUsers + nonUsers
    }

    // Filter unifiedContacts according to our Discord-style category selection
    val categoryContacts = remember(unifiedContacts, serverCategory) {
        when (serverCategory) {
            "Direct" -> unifiedContacts.filter { !it.isGroup && !it.phoneNumber.startsWith("bot_") }
            "Groups" -> unifiedContacts.filter { it.isGroup }
            "Bots" -> unifiedContacts.filter { it.phoneNumber.startsWith("bot_") }
            else -> unifiedContacts
        }
    }

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
                    // Small floating button for group creation (WhatsApp style FAB)
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
                    // TAB 0: CHATS LIST (BLENDED CHAT FEED + DISCORD SIDEBAR)
                    Row(modifier = Modifier.fillMaxSize()) {
                        
                        // DISCORD-STYLE SIDEBAR RAIL:
                        Column(
                            modifier = Modifier
                                .width(72.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. ALL CATEGORY CIRCLE
                            val allSelected = serverCategory == "All"
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(if (allSelected) 12.dp else 24.dp))
                                    .background(if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { serverCategory = "All" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "All Channels",
                                    tint = if (allSelected) Color.White else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 2. DIRECT CHATS CIRCLE (Blurple)
                            val directSelected = serverCategory == "Direct"
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(if (directSelected) 12.dp else 24.dp))
                                    .background(if (directSelected) Color(0xFF5865F2) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { serverCategory = "Direct" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Direct Chats",
                                    tint = if (directSelected) Color.White else Color(0xFF5865F2),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 3. GROUP ROOMS CIRCLE (Teal/WhatsApp Green)
                            val groupsSelected = serverCategory == "Groups"
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(if (groupsSelected) 12.dp else 24.dp))
                                    .background(if (groupsSelected) Color(0xFF075E54) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { serverCategory = "Groups" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = "Secure Channels",
                                    tint = if (groupsSelected) Color.White else Color(0xFF075E54),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 4. BOTS CIRCLE (Gold)
                            val botsSelected = serverCategory == "Bots"
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(if (botsSelected) 12.dp else 24.dp))
                                    .background(if (botsSelected) Color(0xFFFEE75C) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { serverCategory = "Bots" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = "Security AI Bots",
                                    tint = if (botsSelected) Color.DarkGray else Color(0xFFFBE11E),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Visual hint indicator for current category
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = serverCategory.substring(0, minOf(3, serverCategory.length)).uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // MIDDLE CHATS FEED (WhatsApp + Telegram Style):
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            
                            // HEADER COMPONENT WITH TELEGRAM/WHATSAPP PROFILE DROPDOWN SWITCHER:
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clickable { showProfileSwitcher = true }, // Tap header to switch accounts!
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Circular Active Personal account icon with checkmark
                                    val actColor = try {
                                        Color(android.graphics.Color.parseColor(session.avatarColorHex))
                                    } catch (e: Exception) {
                                        MaterialTheme.colorScheme.primary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(actColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(session.avatarEmoji, fontSize = 20.sp)
                                    }
                                    
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = session.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Switch profile",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = "Line: ${session.phoneNumber}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                // Global Toggle button for Decryption live inspection
                                Button(
                                    onClick = { viewModel.toggleShowCiphertext() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showCiphertext) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                ) {
                                    Text(
                                        text = if (showCiphertext) "CIPHER" else "DECRYPT",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (showCiphertext) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // E2EE tip bar
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HealthAndSafety,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Encryption Tunnel Active",
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Direct peer keys are derived on-demand.",
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }

                            // Search Box
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text("Search chats...", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_search_input")
                            )

                            if (!hasContactsPermission) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Sync Phone Directory List",
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Match local friends instantly.",
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                                fontSize = 8.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            val activeTunnelsCount = categoryContacts.count { it.isAppUser || it.isGroup }
                            Text(
                                text = "CHANNELS (${activeTunnelsCount} active, ${categoryContacts.size - activeTunnelsCount} offline)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )

                            if (categoryContacts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Empty directory for: $serverCategory",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Add contacts or launch secure group chats to speak under AES tunnels.",
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(top = 2.dp)
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
                                    items(categoryContacts) { uContact ->
                                        val tempContact = uContact.dbContactRef ?: Contact(
                                            phoneNumber = uContact.phoneNumber,
                                            name = uContact.name,
                                            status = uContact.statusText,
                                            avatarColorHex = uContact.statusHex,
                                            isGroup = uContact.isGroup
                                        )
                                        ContactItemRow(
                                            contact = tempContact,
                                            onSelect = {
                                                if (uContact.dbContactRef != null) {
                                                    viewModel.selectContact(uContact.dbContactRef)
                                                } else {
                                                    viewModel.selectOrAddContact(uContact.name, uContact.phoneNumber)
                                                }
                                            },
                                            onDelete = if (uContact.dbContactRef != null && !tempContact.phoneNumber.startsWith("bot_")) {
                                                { viewModel.deleteContact(uContact.dbContactRef) }
                                            } else null
                                        )
                                    }
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

    // MULTI-ACCOUNT DROPDOWN SWITCHER DIALOG (WhatsApp & Telegram design):
    // Shows all pre-registered accounts, allows seamless tap-switching or adding new entries.
    if (showProfileSwitcher) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showProfileSwitcher = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Switch Account (Multi-Profile Link)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Tap to switch sessions instantly. Your messages and tunnels are kept secure.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(localAccounts) { acc ->
                                val isCurrent = acc.phoneNumber == session.phoneNumber
                                val borderCol = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent
                                val scaleBg = try {
                                    Color(android.graphics.Color.parseColor(acc.avatarColorHex))
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }
                                
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = BorderStroke(1.5.dp, borderCol),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.loginAsAccount(acc)
                                            showProfileSwitcher = false
                                            Toast.makeText(context, "Switched to ${acc.name} (${acc.phoneNumber})", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(scaleBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(acc.avatarEmoji, fontSize = 18.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(acc.phoneNumber, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Active",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showProfileSwitcher = false
                                viewModel.logout()
                            }
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Profile", fontWeight = FontWeight.Bold)
                        }
                        
                        TextButton(onClick = { showProfileSwitcher = false }) {
                            Text("Cancel")
                        }
                    }
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
    var editBio by remember { mutableStateOf(session.bio) }
    var selectedPfpPath by remember { mutableStateOf(session.customPfpPath) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val colorsPalette = listOf(PaletteAzure, PaletteEmerald, PaletteAmber, PaletteRose, PaletteOrchid)
    val emojisPalette = listOf("👤", "💬", "🍀", "🦁", "🐼", "🦊", "🚀", "⚡", "🐳", "🧁")

    // Launcher for photo gallery picker with size limitation of 3MB
    val pfpPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val sizeBytes = inputStream?.use { it.available() } ?: 0
                val sizeMB = sizeBytes / (1024f * 1024f)
                if (sizeMB > 3.0f) {
                    Toast.makeText(context, "Image exceeds 3MB limit! Please select a smaller photo.", Toast.LENGTH_LONG).show()
                } else {
                    // Copy to private sandboxed app folder so storage permissions don't expire
                    val file = java.io.File(context.filesDir, "pfp_${session.phoneNumber}.png")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    selectedPfpPath = file.absolutePath
                    Toast.makeText(context, "Profile picture selected successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Settings Header
        Text(
            text = "Profile & Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 10.dp)
        )

        // VISUAL PROFILE PANEL
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

                // Large Avatar with ProfileAvatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { pfpPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ProfileAvatar(
                        customPfpPath = selectedPfpPath,
                        avatarColorHex = selectedColorHex,
                        avatarEmoji = selectedEmoji,
                        size = 96.dp,
                        emojiSize = 44.sp
                    )
                    
                    // Small click overlay camera badge for aesthetic excellence
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .align(Alignment.BottomEnd)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change picture",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Text(
                    text = "Tap circle to upload your Profile Picture from Gallery (Under 3MB limit).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_nickname_input")
                )

                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text("Custom Bio (visible to others)") },
                    singleLine = true,
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_bio_input")
                )

                // Choose Background Color Circle Palettes
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Customize Backup Avatar Background",
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
                        text = "Customize Backup Avatar Emoji",
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
                        viewModel.updateUserProfile(
                            name = editName.trim(),
                            avatarColorHex = selectedColorHex,
                            avatarEmoji = selectedEmoji,
                            customPfpPath = selectedPfpPath,
                            bio = editBio.trim()
                        )
                        Toast.makeText(context, "Profile details saved globally!", Toast.LENGTH_SHORT).show()
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

        // ACCESSIBILITY & THEMING (No Wallpaper customization, keep only dynamic font size scaling & Night mode switches!)
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
                    text = "ACCESSIBILITY INTEGRATION",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

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

                // Night mode switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Night Mode Canvas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AMOLED eye-friendly dark colors",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = session.isDarkMode,
                        onCheckedChange = {
                            viewModel.updateThemeCustomizations(
                                chatBgColorHex = session.chatBgColorHex,
                                fontSizeMultiplier = session.fontSizeMultiplier,
                                isDarkMode = it
                            )
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Logout session
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LOGOUT THIS PROFILE SESSION", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                }
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
    onDelete: (() -> Unit)? = null
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

            if (onDelete != null) {
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
                        senderPicColorHex = if (message.senderPhone == session.phoneNumber) session.avatarColorHex else contact.avatarColorHex,
                        senderPicEmoji = if (message.senderPhone == session.phoneNumber) session.avatarEmoji else null,
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
            session = session,
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

    val isUser = message.senderPhone == session.phoneNumber
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
    session: UserSession,
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
                    value = if (message.senderPhone == session.phoneNumber) "OUTGOING PORT" else "INCOMING PORT"
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

fun getAppFingerprints(context: android.content.Context): Pair<String, String>? {
    try {
        val packageName = context.packageName
        val packageManager = context.packageManager
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val packageInfo = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = packageManager.getPackageInfo(packageName, @Suppress("DEPRECATION") android.content.pm.PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        
        if (signatures != null && signatures.isNotEmpty()) {
            val signatureBytes = signatures[0].toByteArray()
            val mdSha1 = java.security.MessageDigest.getInstance("SHA-1")
            val mdSha256 = java.security.MessageDigest.getInstance("SHA-256")
            
            val sha1Digest = mdSha1.digest(signatureBytes)
            val sha256Digest = mdSha256.digest(signatureBytes)
            
            val sha1 = sha1Digest.joinToString(":") { String.format("%02X", it) }
            val sha256 = sha256Digest.joinToString(":") { String.format("%02X", it) }
            return Pair(sha1, sha256)
        }
    } catch (e: Exception) {
        android.util.Log.e("FrendoSignature", "Error getting fingerprints", e)
    }
    return null
}
