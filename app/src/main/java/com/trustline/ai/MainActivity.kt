package com.trustline.ai
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.util.Log
import com.trustline.ai.network.UploadRepository
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.LaunchedEffect
import java.util.Locale
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
class MainActivity : ComponentActivity() {

    private lateinit var audioRecorder: AudioRecorder
    private var recordingJob: Job? = null
    private val microphonePermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                audioRecorder.startRecording()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioRecorder = AudioRecorder(this)

        setContent {
            val trustLineViewModel: TrustLineViewModel = viewModel()

            TrustLineApp(
                viewModel = trustLineViewModel,
                onStartRecording = {

                    if (
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {

                        audioRecorder.startRecording()

                        recordingJob = lifecycleScope.launch {

                            while (true) {

                                delay(8000)

                                val audioFile = audioRecorder.stopRecording()

                                if (audioFile != null) {

                                    audioRecorder.startRecording()
                                    Log.d(
                                        "TrustLine",
                                        "Audio file size = ${audioFile.length()} bytes"
                                    )
                                    Log.d(
                                        "TrustLine",
                                        "Uploading audio chunk: ${audioFile.name}"
                                    )

                                    lifecycleScope.launch {

                                        try {

                                            val result =
                                                UploadRepository.uploadAudio(audioFile)

                                            if (result != null) {

                                                trustLineViewModel.updateResult(
                                                    score = result.trust_score,
                                                    pred = result.prediction,
                                                    conf = result.confidence,
                                                    text = result.transcript,
                                                    newRiskOverride = result.risk_override,
                                                    newRuleScore = result.rule_score,
                                                    newAiScore = result.ai_score,
                                                    newVoiceScore = result.voice_score,

                                                    newRiskReasons = result.risk_reasons ?: emptyList(),
                                                    newVoicePrediction = result.voice_prediction,
                                                    newVoiceLabel = result.voice_label,
                                                    newVoiceConfidence = result.voice_confidence,
                                                    newVoiceAnalysis = result.voice_analysis
                                                        ?: "Voice authenticity could not be determined."
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "Chunk prediction = ${result.prediction}"
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "Chunk confidence = ${result.confidence}"
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "Chunk trust score = ${result.trust_score}"
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "Chunk transcript = ${result.transcript}"
                                                )

// ---------------------------------
// Explainable TrustLine information
// ---------------------------------

                                                Log.d(
                                                    "TrustLine",
                                                    "Rule score = ${result.rule_score}"
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "AI prediction = ${result.ai_prediction}"
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "AI score = ${result.ai_score}"
                                                )
                                                Log.d("TrustLine", "Risk override = ${result.risk_override}")
                                                Log.d(
                                                    "TrustLine",
                                                    "Voice prediction = ${result.voice_prediction}"
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "Voice label = ${result.voice_label}"
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "Voice confidence = ${result.voice_confidence}"
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "Voice score = ${result.voice_score}"
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "Risk reasons = ${result.risk_reasons}"
                                                )

                                                Log.d(
                                                    "TrustLine",
                                                    "Voice analysis = ${result.voice_analysis}"
                                                )

                                            } else {

                                                Log.e(
                                                    "TrustLine",
                                                    "Chunk upload failed"
                                                )
                                            }

                                        } catch (e: Exception) {

                                            Log.e(
                                                "TrustLine",
                                                "Chunk upload exception",
                                                e
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    } else {

                        microphonePermissionLauncher.launch(
                            Manifest.permission.RECORD_AUDIO
                        )
                    }
                },
                onStopRecording = {

                    recordingJob?.cancel()
                    recordingJob = null

                    audioRecorder.stopRecording()

                    Log.d(
                        "TrustLine",
                        "Monitoring stopped"
                    )
                }
            )
        }
    }
}

@Composable
fun TrustLineApp(
    viewModel: TrustLineViewModel,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
)  {

    var isRecording by remember {
        mutableStateOf(false)
    }

    var isMuted by remember {
        mutableStateOf(false)
    }

    var showVerifyDialog by remember {
        mutableStateOf(false)
    }
    var showSessionSummary by remember {
        mutableStateOf(false)
    }
    var showScamAlert by remember {
        mutableStateOf(false)
    }
    var showTrustedContactDialog by remember {
        mutableStateOf(false)
    }
    var showTrustedContactSetup by remember {
        mutableStateOf(false)
    }
    val context = LocalContext.current
    val sharedPreferences = remember {
        context.getSharedPreferences(
            "TrustLinePreferences",
            android.content.Context.MODE_PRIVATE
        )
    }

    var trustedPhoneNumber by remember {
        mutableStateOf(
            sharedPreferences.getString(
                "trusted_phone_number",
                ""
            ) ?: ""
        )
    }
    var scamAlertShown by remember {
        mutableStateOf(false)
    }

    val scrollState = rememberScrollState()



    val trustScore = viewModel.trustScore
    val sessionRiskScore = viewModel.sessionRiskScore
    val prediction = viewModel.prediction
    val accessibilityMode = viewModel.accessibilityMode
    val titleSize = if (accessibilityMode) 36.sp else 30.sp
    val normalTextSize = if (accessibilityMode) 20.sp else 16.sp
    val buttonTextSize = if (accessibilityMode) 20.sp else 14.sp
    val buttonHeight = if (accessibilityMode) 64.dp else 48.dp
    val confidence = viewModel.confidence
    val hasResult = viewModel.hasResult
    val transcript = viewModel.transcript
    val ruleScore = viewModel.ruleScore
    val aiScore = viewModel.aiScore
    val voiceScore = viewModel.voiceScore
    val dialogTitleSize = if (accessibilityMode) 26.sp else 20.sp
    val dialogTextSize = if (accessibilityMode) 20.sp else 14.sp
    val riskReasons = viewModel.riskReasons
    val sessionRiskReasons = viewModel.sessionRiskReasons
    val voicePrediction = viewModel.voicePrediction
    val voiceLabel = viewModel.voiceLabel
    val voiceConfidence = viewModel.voiceConfidence
    val voiceAnalysis = viewModel.voiceAnalysis
    LaunchedEffect(
        trustScore,
        viewModel.riskOverride,
        prediction
    ) {

        if (
            !scamAlertShown &&
            (
                    viewModel.riskOverride ||
                            (trustScore > 0 && trustScore < 40)
                    )
        ) {
            showScamAlert = true
            scamAlertShown = true
        }
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        )  {

            Text(
                text = "TrustLine AI",
                fontSize = titleSize,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Real-time call protection"
            )

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),

                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = when {
                            sessionRiskScore >= 80 -> "NORMAL CONVERSATION"
                            sessionRiskScore >= 60 -> "SUSPICIOUS CONVERSATION"
                            sessionRiskScore >= 40 -> "HIGH-RISK CONVERSATION"
                            else -> "SCAM PHONE CALL"
                        },
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (hasResult) sessionRiskScore.toString() else "—",
                        fontSize = if (accessibilityMode) 72.sp else 56.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "TRUST SCORE",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (!hasResult) {
                            "WAITING FOR ANALYSIS"
                        } else if (accessibilityMode) {
                            when {
                                trustScore >= 80 ->
                                    "✓ THIS CALL APPEARS SAFE"

                                trustScore >= 60 ->
                                    "⚠️ BE CAREFUL. DO NOT SHARE PERSONAL INFORMATION."

                                trustScore >= 40 ->
                                    "🚨 WARNING: THIS CALL MAY BE DANGEROUS."

                                else ->
                                    "🚨 WARNING: THIS CALL MAY BE A SCAM. DO NOT SEND MONEY."
                            }
                        } else {
                            when {
                                trustScore >= 80 -> "NORMAL CONVERSATION"
                                trustScore >= 60 -> "SUSPICIOUS CONVERSATION"
                                trustScore >= 40 -> "HIGH-RISK CONVERSATION"
                                else -> "SCAM PHONE CALL"
                            }
                        },
                        fontSize = if (accessibilityMode) 24.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (viewModel.riskOverride) {

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "⚠ CRITICAL SECURITY RISK OVERRIDE ACTIVE",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )
            Spacer(
                modifier = Modifier.height(15.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "VOICE AUTHENTICITY",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = when (voiceLabel.lowercase()) {
                            "fake" -> "🔴 Possible AI-generated voice"
                            "real" -> "🟢 Likely human voice"
                            else -> "⚪ Voice authenticity unknown"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "Confidence: %.2f%%".format(voiceConfidence)
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = voiceAnalysis
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "LIVE TRANSCRIPT",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = if (isRecording)
                            "Listening..."
                        else
                            transcript
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(15.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(18.dp)
                ) {

                    Text(
                        text = "WHY THIS SCORE?",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Final prediction: $prediction",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "AI confidence: %.2f%%".format(confidence)
                    )
                    if (sessionRiskReasons.isNotEmpty()) {

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = "Risk factors:",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        sessionRiskReasons.forEach { reason ->

                            Text(
                                text = "• $reason"
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )
                        }
                    }
                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Text(
                        text = "RISK BREAKDOWN",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Rule engine: $ruleScore / 100"
                    )

                    Text(
                        text = "AI classifier: %.2f / 100".format(aiScore)
                    )

                    Text(
                        text = "Voice authenticity: %.2f / 100".format(voiceScore)
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Text(
                        text = "REASONS",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    if (riskReasons.isEmpty()) {

                        Text(
                            text = "No suspicious indicators detected."
                        )

                    } else {

                        riskReasons.forEach { reason ->

                            val displayReason = when {
                                reason.startsWith("Suspicious keyword detected: otp", ignoreCase = true) ->
                                    "OTP request detected"

                                reason.startsWith("Suspicious keyword detected: account number", ignoreCase = true) ->
                                    "Account number requested"

                                reason.startsWith("Suspicious keyword detected: immediately", ignoreCase = true) ->
                                    "Urgent language detected"

                                reason.startsWith("AI classified", ignoreCase = true) ->
                                    "AI classified this conversation as suspicious"

                                reason.startsWith("Possible AI-generated", ignoreCase = true) ->
                                    "Possible cloned or AI-generated voice detected"

                                reason.startsWith("Voice appears human", ignoreCase = true) ->
                                    "Voice appears human, but this does not guarantee safety"

                                else ->
                                    reason
                            }

                            Text(
                                text = "⚠ $displayReason",
                                modifier = Modifier.padding(
                                    vertical = 3.dp
                                )
                            )
                        }
                    }
                }
            }
            Spacer(
                modifier = Modifier.height(20.dp)
            )
            Button(
                onClick = {
                    viewModel.toggleAccessibilityMode()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
            ) {
                Text(
                    text = if (accessibilityMode) {
                        "ACCESSIBILITY MODE: ON"
                    } else {
                        "ACCESSIBILITY MODE: OFF"
                    },
                    fontSize = buttonTextSize
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {

                        viewModel.resetSession()
                        scamAlertShown = false
                        onStartRecording()

                        isRecording = true
                        isMuted = false
                    },
                    enabled = !isRecording,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                ) {
                    Text(
                        text = "START",
                        fontSize = buttonTextSize
                    )
                }

                Button(
                    onClick = {
                        onStopRecording()

                        isRecording = false
                        isMuted = false

                        showSessionSummary = true
                    },
                    enabled = isRecording,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                ) {
                    Text(
                        text = "STOP",
                        fontSize = buttonTextSize
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )
            Button(
                onClick = {
                    showVerifyDialog = true
                },

                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Verify Caller",
                    fontSize = dialogTitleSize
                )
            }
            Spacer(
                modifier = Modifier.height(12.dp)
            )
            Button(
                onClick = {
                    showTrustedContactDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight)
            ) {
                Text(
                    text = "CONTACT TRUSTED PERSON",
                    fontSize = buttonTextSize
                )
            }
            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = {
                    if (isMuted) {
                        onStartRecording()
                        isMuted = false
                    } else {
                        onStopRecording()
                        isMuted = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isMuted) {
                        "🔊 RESUME MONITORING"
                    } else {
                        "🔇 PAUSE MONITORING"
                    }
                )
            }
        }
    }

    if (showScamAlert) {

        AlertDialog(

            onDismissRequest = {
                showScamAlert = false
            },

            title = {
                Text(
                    text = "🚨 SCAM ALERT",
                    fontWeight = FontWeight.Bold,
                    fontSize = dialogTitleSize
                )
            },

            text = {
                Column {

                    Text(
                        text = "This conversation appears to be a scam or high-risk conversation.",
                        fontSize = dialogTextSize
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Trust Score: $trustScore",
                        fontSize = dialogTextSize
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Do not share OTPs, passwords, PINs, or bank details.",
                        fontSize = dialogTextSize
                    )
                }
            },

            confirmButton = {

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = {
                            showScamAlert = false
                            showVerifyDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                    ) {
                        Text(
                            text = "VERIFY CALLER",
                            fontSize = buttonTextSize
                        )
                    }

                    Button(
                        onClick = {
                            showScamAlert = false

                            if (trustedPhoneNumber.isBlank()) {
                                showTrustedContactSetup = true
                            } else {
                                showTrustedContactDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                    ) {
                        Text(
                            text = "CONTACT PERSON",
                            fontSize = buttonTextSize
                        )
                    }

                    TextButton(
                        onClick = {
                            showScamAlert = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                    ) {
                        Text(
                            text = "DISMISS",
                            fontSize = buttonTextSize
                        )
                    }
                }
            },
            dismissButton = { }
        )
    }
    if (showTrustedContactDialog) {

        AlertDialog(
            onDismissRequest = {
                showTrustedContactDialog = false
            },

            title = {
                Text(
                    text = "Contact Trusted Person",
                    fontSize = dialogTitleSize
                )
            },

            text = {
                Text(
                    text = "Would you like to contact a trusted family member or friend for help?",
                    fontSize = dialogTextSize
                )
            },

            confirmButton = {
                Button(
                    onClick = {
                        showTrustedContactDialog = false

                        if (trustedPhoneNumber.isBlank()) {
                            showTrustedContactSetup = true
                        } else {
                            val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:$trustedPhoneNumber")
                            )

                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.height(buttonHeight)
                ) {
                    Text(
                        text = "CONTACT",
                        fontSize = buttonTextSize
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showTrustedContactDialog = false
                    },
                    modifier = Modifier.height(buttonHeight)
                ) {
                    Text(
                        text = "CANCEL",
                        fontSize = buttonTextSize
                    )
                }
            }
        )
    }
    if (showTrustedContactSetup) {
        AlertDialog(
            onDismissRequest = {
                showTrustedContactSetup = false
            },

            title = {
                Text(
                    text = "Set Trusted Contact",
                    fontSize = dialogTitleSize
                )
            },

            text = {
                Column {

                    Text(
                        text = "Enter the phone number of a family member or trusted person.",
                        fontSize = dialogTextSize
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = trustedPhoneNumber,
                        onValueChange = {
                            trustedPhoneNumber = it
                        },
                        label = {
                            Text("Phone Number")
                        },
                        singleLine = true
                    )
                }
            },

            confirmButton = {
                Button(
                    onClick = {
                        if (trustedPhoneNumber.isNotBlank()) {

                            sharedPreferences.edit()
                                .putString(
                                    "trusted_phone_number",
                                    trustedPhoneNumber
                                )
                                .apply()

                            showTrustedContactSetup = false
                        }
                    },
                    modifier = Modifier.height(buttonHeight)

                ) {
                    Text(
                        text = "SAVE",
                        fontSize = buttonTextSize
                    )
                }

            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showTrustedContactSetup = false
                    },
                    modifier = Modifier.height(buttonHeight)
                ) {
                    Text(
                        text = "CANCEL",
                        fontSize = buttonTextSize
                    )
                }
            }
        )
    }
    if (showVerifyDialog) {

        AlertDialog(
            onDismissRequest = {
                showVerifyDialog = false
            },

            title = {
                Text(
                    text = "Verify Caller",
                    fontSize = dialogTitleSize
                )
            },

            text = {
                Column {

                    Text(
                        text = "Do not trust the caller's identity based only on the voice.",
                        fontSize = dialogTextSize
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Recommended actions:",
                        fontSize = dialogTextSize
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "• End the call if you feel pressured.",
                        fontSize = dialogTextSize
                    )
                    Text(
                        text = "• Call the organization using an official number.",
                        fontSize = dialogTextSize
                    )

                    Text(
                        text = "• Never share OTPs, passwords, or PINs.",
                        fontSize = dialogTextSize
                    )

                    Text(
                        text = "• Verify unexpected payment requests independently.",
                        fontSize = dialogTextSize
                    )
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showVerifyDialog = false
                    },
                    modifier = Modifier.height(buttonHeight)
                ) {
                    Text(
                        text = "GOT IT",
                        fontSize = buttonTextSize

                    )

                }
            }
        )
    }
    if (showSessionSummary) {

        AlertDialog(

            onDismissRequest = {
                showSessionSummary = false
            },

            title = {
                Text(
                    text = "CALL SAFETY SUMMARY",
                    fontSize = dialogTitleSize,
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    )
                ) {
                    Text(
                        text = "Final Trust Score: $sessionRiskScore",
                        fontSize = dialogTextSize
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    val riskLevel = when {
                        sessionRiskScore < 40 -> "HIGH RISK"
                        sessionRiskScore < 60 -> "SUSPICIOUS"
                        sessionRiskScore < 80 -> "CAUTION"
                        else -> "LOW RISK"
                    }

                    Text(
                        text = "Risk Level: $riskLevel",
                        fontSize = dialogTextSize,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    if (sessionRiskReasons.isNotEmpty()) {

                        Text(
                            text = "Detected Concerns:",
                            fontSize = dialogTextSize,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )

                        sessionRiskReasons.forEach { reason ->

                            Text(
                                text = "• $reason",
                                fontSize = dialogTextSize
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    val recommendation = when {
                        sessionRiskScore < 40 ->
                            "Do not send money or share personal information. Contact a trusted person if you are unsure."

                        sessionRiskScore < 60 ->
                            "Be careful and independently verify the caller before taking any action."

                        sessionRiskScore < 80 ->
                            "Proceed with caution and avoid sharing sensitive information."

                        else ->
                            "No major risks were detected. Continue to stay cautious."
                    }

                    Text(
                        text = "Recommendation:",
                        fontSize = dialogTextSize,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = recommendation,
                        fontSize = dialogTextSize
                    )
                }
            },

            confirmButton = {

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    if (accessibilityMode && sessionRiskScore < 40) {

                        Button(
                            onClick = {
                                showSessionSummary = false

                                if (trustedPhoneNumber.isBlank()) {
                                    showTrustedContactSetup = true
                                } else {
                                    showTrustedContactDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(buttonHeight)
                        ) {
                            Text(
                                text = "CONTACT TRUSTED PERSON",
                                fontSize = buttonTextSize
                            )
                        }
                    }

                    Button(
                        onClick = {
                            showSessionSummary = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(buttonHeight)
                    ) {
                        Text(
                            text = "DONE",
                            fontSize = buttonTextSize
                        )
                    }
                }
            }
        )
    }
}