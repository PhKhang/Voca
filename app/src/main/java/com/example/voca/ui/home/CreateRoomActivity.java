package com.example.voca.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.voca.KaraokeRoom;
import com.example.voca.R;
import com.example.voca.bus.UserBUS;
import com.example.voca.dao.RoomDAO;
import com.example.voca.dto.RoomDTO;
import com.example.voca.dto.RoomMessageDTO;
import com.example.voca.dto.SongDTO;
import com.example.voca.dto.UserDTO;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateRoomActivity extends AppCompatActivity {

    Button taoPhong;
    private UserDTO me;
    EditText roomName;
    EditText roomDesc;
    Switch roomPublic;

    GridView rooms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_room);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String userId = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("userId", null);
        Toast.makeText(this, "User ID: " + userId, Toast.LENGTH_SHORT).show();

        roomName = findViewById(R.id.roomName);
        roomDesc = findViewById(R.id.roomDesc);
        roomPublic = findViewById(R.id.isPublic);

        rooms = findViewById(R.id.rooms);
//        rooms.setAdapter(new RoomAdapter(this, new ArrayList<>()));

        taoPhong = findViewById(R.id.taoPhong);
        taoPhong.setOnClickListener(v -> {
            // Handle the button click here
            // For example, you can start a new activity or show a message
//            Intent intent = new Intent(CreateRoomActivity.this, KaraokeRoom.class);
            String currentTime;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentTime = LocalDateTime.now().toString();
            }
            else {
                currentTime = "2025-01-01T12:00:00"; // Fallback for older versions
            }
            UserBUS userBUS = new UserBUS();
            userBUS.fetchUserById(userId, new UserBUS.OnUserFetchedListener() {
                @Override
                public void onUserFetched(UserDTO user) {
                    Toast.makeText(getApplicationContext(), "User fetched: " + user.getUsername(), Toast.LENGTH_LONG).show();
                    me = user;
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getApplicationContext(), "Failed to fetch: " + error, Toast.LENGTH_LONG).show();
                }
            });

            RoomDTO roomDTO = new RoomDTO(
                    null,
                    roomName.getText().toString(),
                    null,
                    !roomPublic.isChecked(),
                    roomDesc.getText().toString(),
                    currentTime,
                    currentTime,
                    me,
                    null,
                    null,
                    null,
                    "0",
                    null
            );

            RoomDAO roomDAO = new RoomDAO();
            roomDAO.createRoom(roomDTO, new Callback<RoomDTO>() {
                @Override
                public void onResponse(Call<RoomDTO> call, Response<RoomDTO> response) {
                    if (response.isSuccessful()){
                        Toast.makeText(getApplicationContext(), "Room created successfully", Toast.LENGTH_LONG).show();
                        Log.e("Room created successfully", response.body().toString());

                        Toast.makeText(CreateRoomActivity.this, "Room code: " + roomDTO.getCode(), Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(CreateRoomActivity.this, KaraokeRoom.class);
                        intent.putExtra("roomId", roomDTO.get_id());
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Failed to create room", Toast.LENGTH_LONG).show();
                        Log.e("Room creation failed", response.errorBody().toString());
                    }
                }

                @Override
                public void onFailure(Call<RoomDTO> call, Throwable t) {
                    Toast.makeText(getApplicationContext(), "API Failed to create room: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("API Failed to create room", t.getMessage());
                }
            });
            Toast.makeText(this, "Room code: " + roomDTO.getCode(), Toast.LENGTH_SHORT).show();
            //            intent.putExtra("room", roomDAO);
//            startActivity(intent);
//            finish();
        });
    }

    private List<RoomDTO> myRooms;

    @Override
    protected void onStart() {
        super.onStart();

        myRooms = new ArrayList<>();
        RoomDAO roomDAO = new RoomDAO();
        UserBUS userBUS = new UserBUS();
        String userId = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("userId", null);
        userBUS.fetchUserById(userId, new UserBUS.OnUserFetchedListener() {
            @Override
            public void onUserFetched(UserDTO user) {
                Toast.makeText(getApplicationContext(), "User fetched: " + user.getUsername(), Toast.LENGTH_LONG).show();
                me = user;
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getApplicationContext(), "Failed to fetch: " + error, Toast.LENGTH_LONG).show();
            }
        });

        roomDAO.getRoomByUserId(userId, new Callback<List<RoomDTO>>() {
            @Override
            public void onResponse(Call<List<RoomDTO>> call, Response<List<RoomDTO>> response) {
                if (response.isSuccessful()) {
                    myRooms = response.body();
                    Log.e("My rooms", myRooms.size() + "");
                    rooms.setAdapter(new RoomAdapter(CreateRoomActivity.this, myRooms));
                } else {
                    Log.e("Error", "Failed to fetch rooms: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<RoomDTO>> call, Throwable t) {
                Log.e("Error", "API call failed: " + t.getMessage());
            }
        });
    }
}