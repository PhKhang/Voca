package com.example.voca.ui.room

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.voca.ui.room.ui.theme.VocaTheme
import io.getstream.android.video.generated.models.MemberRequest
import io.getstream.video.android.compose.permission.LaunchMicrophonePermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.avatar.UserAvatar
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleMicrophoneAction
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.CreateCallOptions
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class JetpackActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val apiKey = "x6wubjfby45d"
        val userToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiam9obiIsInZhbGlkaXR5X2luX3NlY29uZHMiOjI1OTIwMDAsInJvbGUiOiJhZG1pbiIsImlhdCI6MTc0NDk1Mjg3MCwiZXhwIjoxNzQ3NTQ0ODcwfQ.PWBaa1XqWiicvtHRDOg3UNArFY9onCAzExpQAyhPKXo"
        val userId = "john"
        val callId = "audio_room_fe9b67b7-a42d-4acf-a123-11ed6baa92b1"

        // Create a user
        val user = User(
            id = userId, // any string
            name = "Tutorial", // name and image are used in the UI
            role = "admin",
        )

        // Initialize StreamVideo. For a production app we recommend adding the client to your Application class or di module
        val client = StreamVideoBuilder(
            context = applicationContext,
            apiKey = apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = user,
            token = userToken,
        ).build()

        // Create a call with type as `audio_room` and id as callId
        val call = client.call("audio_room", callId)

        enableEdgeToEdge()
        setContent {
            // Request microphone permission
            LaunchMicrophonePermissions(
                call = call,
                onPermissionsResult = { granted ->
                    if (granted) {
                        // Mic permissions are granted, so we can join the call.
                        lifecycleScope.launch {
                            val result = call.join(
//                                create = true, createOptions = CreateCallOptions(
//                                    members = listOf(
//                                        MemberRequest(
//                                            userId = userId,
//                                            role = "host",
//                                            custom = emptyMap()
//                                        )
//                                    ),
//                                    custom = mapOf(
//                                        "title" to "Compose Trends",
//                                        "description" to "Talk about how easy compose makes it to reuse and combine UI"
//                                    )
//                                )
                            )
                            result.onError {
                                Toast.makeText(applicationContext, it.message, Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
                }
            )

            // Define the UI
            VideoTheme {
                val connection by call.state.connection.collectAsState()
                val activeSpeakers by call.state.activeSpeakers.collectAsState()
                val audioLevel = activeSpeakers.firstOrNull()?.audioLevel?.collectAsState()

                val color1 = Color.White.copy(alpha = 0.2f + (audioLevel?.value ?: 0f) * 0.8f)
                val color2 = Color.White.copy(alpha = 0.2f + (audioLevel?.value ?: 0f) * 0.8f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(color1, color2)))
                        .fillMaxSize()
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {

                    if (connection != RealtimeConnection.Connected) {
                        Text("Loading", fontSize = 30.sp)
                    } else {
                        AudioRoom(call = call)
                    }
                }
            }

        }

    }
}

@Composable
public fun AudioRoom(
    call: Call,
) {
    val custom by call.state.custom.collectAsState()
    val title = custom["title"] as? String
    val description = custom["description"] as? String
    val participants by call.state.participants.collectAsState()
    val activeSpeakers by call.state.activeSpeakers.collectAsState()
    val activeSpeaker = activeSpeakers.firstOrNull()

    val backstage by call.state.backstage.collectAsState()
    val isMicrophoneEnabled by call.microphone.isEnabled.collectAsState()

    Description(title, description, participants)

    activeSpeaker?.let {
        Text("${it.userNameOrId} is speaking")
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(0.dp, 32.dp, 0.dp, 0.dp)
    ) {
        Participants(
            participants = participants
        )
        Controls(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), call = call,
            isMicrophoneEnabled = isMicrophoneEnabled,
            backstage = backstage,
            enableMicrophone = { call.microphone.setEnabled(it) }
        )
    }
}

@Composable
public fun Description(
    title: String?,
    description: String?,
    participants: List<ParticipantState>
) {
    Column {
//        Text("$title", fontSize = 30.sp)
//        Text("$description", fontSize = 20.sp, modifier = Modifier.padding(16.dp))
        Text("${participants.size} participants", fontSize = 20.sp)
    }
}

@Composable
public fun Participants(
    modifier: Modifier = Modifier,
    participants: List<ParticipantState>
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(minSize = 128.dp),
    ) {
        items(items = participants, key = { it.sessionId }) { participant ->
            ParticipantAvatar(participant)
        }
    }
}

@Composable
public fun ParticipantAvatar(
    participant: ParticipantState,
    modifier: Modifier = Modifier
) {
    val image by participant.image.collectAsState()
    val nameOrId by participant.userNameOrId.collectAsState()
    val isSpeaking by participant.speaking.collectAsState()
    val audioEnabled by participant.audioEnabled.collectAsState()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(modifier = Modifier.size(56.dp)) {
            UserAvatar(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(VideoTheme.dimens.componentPaddingFixed),
                userImage = image,
                userName = nameOrId,
            )

            if (isSpeaking) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(BorderStroke(2.dp, Color.Gray), CircleShape)
                )
            } else if (!audioEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black)
                            .size(16.dp)
                    ) {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            painter = painterResource(id = io.getstream.video.android.ui.common.R.drawable.stream_video_ic_mic_off),
                            tint = Color.White,
                            contentDescription = null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = nameOrId,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = participant.roles.value.firstOrNull() ?: "",
            fontSize = 11.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
public fun Controls(
    modifier: Modifier = Modifier,
    call: Call,
    backstage: Boolean = false,
    isMicrophoneEnabled: Boolean = false,
    enableMicrophone: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ToggleMicrophoneAction(
            modifier = Modifier.size(52.dp),
            isMicrophoneEnabled = isMicrophoneEnabled,
            onCallAction = { enableMicrophone(it.isEnabled) }
        )

//        Button(
//            onClick = {
//                scope.launch {
//                    if (backstage) call.goLive() else call.stopLive()
//                }
//            }
//        ) {
//            Text(text = if (backstage) "Go Live" else "End")
//        }
        Button(
            onClick = {
                scope.launch {
                    call.leave()
                }
            }
        ) {
            Text(text = "Leave")
        }
    }
}

// ----------------------

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VocaTheme {
        Greeting("Android")
    }
}