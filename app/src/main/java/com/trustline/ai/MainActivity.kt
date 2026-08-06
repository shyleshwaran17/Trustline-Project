package com.trustline.ai
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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

class MainActivity : ComponentActivity() {

    private lateinit var audioRecorder: AudioRecorder

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

                    } else {

                        microphonePermissionLauncher.launch(
                            Manifest.permission.RECORD_AUDIO
                        )
                    }
                },

                onStopRecording = {

                    val audioFile = audioRecorder.stopRecording()

                    if (audioFile != null) {

                        lifecycleScope.launch {

                            try {

                                Log.d("TrustLine", "Uploading audio...")

                                val result = UploadRepository.uploadAudio(audioFile)

                                if (result != null) {

                                    trustLineViewModel.updateResult(
                                        score = result.trust_score,
                                        pred = result.prediction,
                                        conf = result.confidence,
                                        text = result.transcript
                                    )

                                    Log.d("TrustLine", "Trust Score = ${result.trust_score}")
                                    Log.d("TrustLine", "Prediction = ${result.prediction}")
                                    Log.d("TrustLine", "Confidence = ${result.confidence}")
                                    Log.d("TrustLine", "Transcript = ${result.transcript}")

                                } else {

                                    Log.e("TrustLine", "Upload failed")

                                }

                            } catch (e: Exception) {

                                Log.e("TrustLine", "Upload Exception", e)

                            }
                        }
                    }
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
    val trustScore = viewModel.trustScore

    val prediction = viewModel.prediction

    val confidence = viewModel.confidence

    val transcript = viewModel.transcript

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),

            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "TrustLine AI",
                fontSize = 30.sp,
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
                        .padding(25.dp),

                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "TRUST SCORE",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$trustScore / 100",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isRecording)
                            "MONITORING"
                        else
                            "NOT MONITORING"
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

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
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Prediction: $prediction",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Confidence: %.2f%%".format(confidence)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = {

                        onStartRecording()

                        isRecording = true
                    },

                    enabled = !isRecording,

                    modifier = Modifier.weight(1f)
                ) {

                    Text("START")
                }

                Button(
                    onClick = {

                        onStopRecording()

                        isRecording = false
                    },

                    enabled = isRecording,

                    modifier = Modifier.weight(1f)
                ) {

                    Text("STOP")
                }
            }
        }
    }
}