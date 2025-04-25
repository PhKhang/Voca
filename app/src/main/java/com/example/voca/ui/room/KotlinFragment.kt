package com.example.voca.ui.room

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.voca.bus.SongBUS
import com.example.voca.dto.SongDTO
import com.google.android.exoplayer2.util.Log
import io.getstream.video.android.compose.permission.LaunchMicrophonePermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException

class KotlinFragment : Fragment() {

    private var songList = mutableStateListOf<SongDTO>()
    private var queueList = mutableStateListOf<SongDTO>()
    private var _call = mutableStateOf<Call?>(null)
    val call: Call? get() = _call.value
    lateinit var songBUS: SongBUS
    private val userToken = mutableStateOf<String>("")

    private var songUpdateCallback: SongUpdateCallback? = null

    fun setSongUpdateCallback(callback: SongUpdateCallback) {
        songUpdateCallback = callback
    }

    @SuppressLint("UnrememberedMutableState")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val sharedPref = activity?.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userId = sharedPref?.getString("userId", "defaultValue")

        val http = OkHttpClient()

        val mediaType = "application/json".toMediaTypeOrNull()
        val body =
            RequestBody.create(mediaType, "{\n  \"user_id\": \"$userId\"\n}")
        val request = Request.Builder()
//            .url("http://10.0.2.2:3000/rooms/" + "EBAC2A" + "/join")
            .url("https://voca-spda.onrender.com/rooms/" + "EBAC2A" + "/join")
            .post(body)
            .addHeader("content-type", "application/json")
            .build()

        http.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                // Handle failure
                Log.d("TAG", "Failed to join room: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                // Handle response
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody)
                    userToken.value = json.getString("user_token")
                    Log.d("TAG", "Response: $responseBody")
                } else {
                    Log.d("TAG", "Failed to join room: ${response.message}")
                }
            }
        })


        // Optional safety check

        val apiKey = "x6wubjfby45d"
        val callId = "EBAC2A"

//        val userId: String =
//            getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("userId", null)


//        val client = StreamVideoBuilder(
//            context = requireContext().applicationContext,
//            apiKey = apiKey,
//            geo = GEO.GlobalEdgeNetwork,
////            user = user,
//            token = userToken,
//        ).build()
//
//        call = client.call("audio_room", "")


        queueList.clear();
        songUpdateCallback?.queue?.let { queueList.addAll(it) };

        return ComposeView(requireContext()).apply {
            setContent {

                songBUS = SongBUS();
                songBUS.fetchSongs(object : SongBUS.OnSongsFetchedListener {
                    override fun onSongsFetched(songs: List<SongDTO>) {
                        songList.clear()
                        songList.addAll(songs)
                        Toast.makeText(
                            requireContext(),
                            "List: " + songList.size,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onError(error: String) {
                        Toast.makeText(
                            requireContext(),
                            "Error fetching songs: $error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })


                _call.value?.let {
                    LaunchMicrophonePermissions(
                        call = it,
                        onPermissionsResult = { granted ->
                            if (granted) {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    //                                val result = call.join()
                                    //                                result.onError {
                                    //                                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                                    //                                }
                                }
                            }
                        }
                    )
                }

                VideoTheme {
                    val connection =
                        _call.value?.state?.connection?.collectAsState() ?: mutableStateOf(null)

                    LazyColumn(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Top,
                        modifier = androidx.compose.ui.Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            Button(onClick = {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    StreamVideo.removeClient()

                                    val user = User(
                                        id = userId ?: "",
                                        name = "user",
                                        role = "admin",
                                    )

                                    val clientt = StreamVideoBuilder(
                                        context = requireContext().applicationContext,
                                        apiKey = apiKey,
                                        geo = GEO.GlobalEdgeNetwork,
                                        user = user,
                                        token = userToken.value,
                                    ).build()

                                    _call.value = clientt.call("audio_room", callId)
                                    val result = _call.value?.join()
                                    result?.onError {
                                        Toast.makeText(
                                            requireContext(),
                                            it.message,
                                            Toast.LENGTH_LONG
                                        )
                                            .show()
                                    }
                                }
                            }) {
                                Text("Join call")
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .height(480.dp)
                            ) {
                                Column {
                                    if (connection.value != RealtimeConnection.Connected) {
                                        Text("Tap the button to join voice call", fontSize = 30.sp)
                                    } else {
                                        _call.value?.let { AudioRoom(call = it) }
                                    }
                                }
                            }
                        }

                        item { Text("Danh sách chờ") }
                        item {
                            SongList(queueList,
                                onSingClick = { song ->
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        Log.d("TAG", "Song to sing: ${song.title}")
                                        songUpdateCallback?.singSong(song._id)
                                    }
                                },
                                showSing = true,
                                showRemove = true,
                                onRemoveClick = { song ->
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        Log.d("TAG", "Song to remove: ${song.title}")
                                        songUpdateCallback?.removeSong(song._id)
                                        queueList.clear()
                                        queueList.addAll(songUpdateCallback?.queue ?: emptyList())
                                    }
                                })
                        }

                        item { Text("Danh sách bài hát") }
                        item {
                            SongList(
                                songList,
                                onAddClick = { song ->
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        Log.d("TAG", "Song clicked: ${song.title}")
                                        songUpdateCallback?.addSong(song)
                                        queueList.clear()
                                        queueList.addAll(songUpdateCallback?.queue ?: emptyList())
                                    }
                                },
                                showAdd = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongList(
    songs: List<SongDTO>,
    onSingClick: (SongDTO) -> Unit = {},
    onAddClick: (SongDTO) -> Unit = {},
    onRemoveClick: (SongDTO) -> Unit = {},
    showRemove: Boolean = false,
    showAdd: Boolean = false,
    showSing: Boolean = false,
) {
    songs.forEach { song ->
        SongItem(
            imageUrl = song.thumbnail,
            title = song.title,
            micCount = song.recorded_people,
            onSingClick = { onSingClick(song) },
            onAddClick = { onAddClick(song) },
            onRemoveClick = { onRemoveClick(song) },
            showSing = showSing,
            showAdd = showAdd,
            showRemove = showRemove
        )
    }
}

@Composable
fun SongItem(
    imageUrl: String,
    title: String,
    micCount: Int,
    onSingClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    showSing: Boolean = false,
    showAdd: Boolean = false,
    showRemove: Boolean = false,
) {
    val emptyCallback: () -> Unit = {}
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(Color(0xFFFCEEF5), shape = RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Song thumbnail",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Top) {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Email, // Replace with custom if needed
                    contentDescription = "Mic Count",
                    tint = Color(0xFFAA00FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "$micCount", fontSize = 12.sp)
            }
        }

        if (showAdd) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = "Thêm", color = Color.White)
            }
        }

        if (showRemove) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRemoveClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = "Xóa", color = Color.White)
            }
        }

        if (showSing) {
            Button(
                onClick = onSingClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = "Hát", color = Color.White)
            }
        }
    }
}
