package com.example.voca.ui.room;

import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.example.voca.R;
import com.example.voca.dao.SongDAO;
import com.example.voca.dto.SongDTO;
import com.example.voca.ui.PostAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link QueueFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class QueueFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public interface OnGetAllSongInQueue {
        void onGetAllSongInQueue();
    }

    private OnGetAllSongInQueue onGetAllSongInQueue;

    public void setOnGetAllSongInQueue(OnGetAllSongInQueue onGetAllSongInQueue) {
        this.onGetAllSongInQueue = onGetAllSongInQueue;
    }

    SongDAO songDAO;
    List<SongDTO> songList;

    private Button ask;
    private FrameLayout background;
    private CardView card;
    private ListView songs;

    public QueueFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment QueueFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static QueueFragment newInstance(String param1, String param2) {
        QueueFragment fragment = new QueueFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_queue, container, false);

        background = view.findViewById(R.id.background);
        background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requireActivity().onBackPressed();
            }
        });

        card = view.findViewById(R.id.card);
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });

//        ask = view.findViewById(R.id.ask);
//        ask.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                onGetAllSongInQueue.onGetAllSongInQueue();
//                requireActivity().onBackPressed();
//            }
//        });

        songs = view.findViewById(R.id.songs);

        songDAO = new SongDAO();
        songDAO.getSongs(new Callback<List<SongDTO>>() {
            @Override
            public void onResponse(Call<List<SongDTO>> call, Response<List<SongDTO>> response) {
                if (response.isSuccessful()) {
                    songs.setAdapter(new SingleSongAdapter(getContext(), response.body()));
                } else {
                    Log.e("TAG", "Lỗi khi lấy danh sách bài hát: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<SongDTO>> call, Throwable t) {
                Log.e("TAG", "Lỗi khi lấy danh sách bài hát: " + t.getMessage());
            }
        });

        return view;
    }
}