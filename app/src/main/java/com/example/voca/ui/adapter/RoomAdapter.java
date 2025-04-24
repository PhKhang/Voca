package com.example.voca.ui.adapter;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.voca.ui.room.CreateRoomActivity;
import com.example.voca.ui.room.KaraokeRoom;
import com.example.voca.R;
import com.example.voca.dto.RoomDTO;

import java.util.ArrayList;
import java.util.List;

public class RoomAdapter extends BaseAdapter {
    private List<RoomDTO> rooms;
    public RoomAdapter(CreateRoomActivity createRoomActivity, List<RoomDTO> objects) {
        this.rooms = new ArrayList<>();
        this.rooms.addAll(objects);
    }

    @Override
    public int getCount() {
        return rooms.size();
    }

    @Override
    public Object getItem(int i) {
        return rooms.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = View.inflate(viewGroup.getContext(), R.layout.item_room_layout, null);
        }
        RoomDTO room = rooms.get(i);

        if (room != null) {
            // Set up the view with room data
            TextView roomAmount = view.findViewById(R.id.amount);
            TextView roomName = view.findViewById(R.id.nameOfRoom);

            roomAmount.setText(String.valueOf(room.getMembers().size()));
            roomName.setText(room.getName());

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Handle room click
                    Intent intent = new Intent(view.getContext(), KaraokeRoom.class);
                    intent.putExtra("roomId", room.getCode());
                    view.getContext().startActivity(intent);
                }
            });
        }
        return view;
    }
}
