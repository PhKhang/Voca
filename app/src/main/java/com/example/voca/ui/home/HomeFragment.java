package com.example.voca.ui.home;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voca.R;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;
import com.example.voca.databinding.FragmentHomeBinding;
import com.example.voca.ui.adapter.PostHomeAdapter;
import com.example.voca.ui.adapter.SingAdapter;
import com.example.voca.ui.room.CreateRoomActivity;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements FunctionAdapter.OnFunctionClickListener {
    private FragmentHomeBinding binding;
    private RecyclerView recyclerViewFunctions;
    private FunctionAdapter adapter;
    private List<FunctionItem> functionList;
    private NavController navController;
    private List<SongDTO> songList;
    private List<PostDTO> postList;
    private SingAdapter singAdapter;
    private PostHomeAdapter postAdapter;
    private ListView listViewSing;
    private ListView listViewPost;
    private Context context;
    private ProgressDialog progressDialog;
    private HomeViewModel homeViewModel;
    private static final String LOADING_MESSAGE = "Đang tải dữ liệu...";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        FirebaseApp.initializeApp(requireContext());
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        context = getContext();

        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);

        recyclerViewFunctions = binding.recyclerViewFunctions;
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerViewFunctions.setLayoutManager(layoutManager);

        functionList = new ArrayList<>();
        functionList.add(new FunctionItem("Hát solo", R.drawable.ic_karaoke_24dp, R.drawable.support_bar_background, R.id.action_homeFragment_to_singFragment));
        functionList.add(new FunctionItem("Hát chung", R.drawable.ic_room_karaoke_24dp, R.drawable.support_bar_background_2, CreateRoomActivity.class));

        adapter = new FunctionAdapter(functionList, this);
        recyclerViewFunctions.setAdapter(adapter);

        songList = new ArrayList<>();
        postList = new ArrayList<>();

        listViewSing = binding.listViewSings;
        listViewPost = binding.listViewPosts;

        singAdapter = new SingAdapter(context, postList, songList);
        postAdapter = new PostHomeAdapter(context, postList);
        listViewSing.setAdapter(singAdapter);
        listViewPost.setAdapter(postAdapter);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        setupObservers();
        homeViewModel.fetchData();

        listViewPost.setOnItemClickListener((parent, view, position, id) -> {
            PostDTO selectedPost = postList.get(position);
            String priorityPostId = selectedPost.get_id();

            Bundle args = new Bundle();
            args.putString("priorityPostId", priorityPostId);

            NavController navController = Navigation.findNavController(view);
            navController.navigate(
                    R.id.action_homeFragment_to_dashboardFragment,
                    args
            );
        });

        View root = binding.getRoot();
        return root;
    }

    private void setupObservers() {
        showLoadingDialog();

        homeViewModel.getSongsLiveData().observe(getViewLifecycleOwner(), songs -> {
            songList = songs != null ? new ArrayList<>(songs) : new ArrayList<>();
            singAdapter.updateData(postList, songList);
            singAdapter.notifyDataSetChanged();
            checkDataLoaded();
        });

        homeViewModel.getPostsLiveData().observe(getViewLifecycleOwner(), posts -> {
            postList = posts != null ? new ArrayList<>(posts) : new ArrayList<>();
            singAdapter.updateData(postList, songList);
            postAdapter.updateData(postList);
            singAdapter.notifyDataSetChanged();
            postAdapter.notifyDataSetChanged();
            checkDataLoaded();
        });

        homeViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            progressDialog.dismiss();
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
        });
    }

    private void checkDataLoaded() {
        // Dismiss progress dialog only when both songs and posts are loaded
        if (!songList.isEmpty() && !postList.isEmpty()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onFunctionClick(FunctionItem functionItem) {
        if (functionItem.getType() == FunctionItem.TYPE_FRAGMENT) {
            if (functionItem.getDestinationId() == R.id.action_homeFragment_to_singFragment) {
                navController.navigate(
                        R.id.action_homeFragment_to_singFragment,
                        null
                );
            } else {
                navController.navigate(functionItem.getDestinationId());
            }
        } else if (functionItem.getType() == FunctionItem.TYPE_ACTIVITY) {
            Intent intent = new Intent(getContext(), functionItem.getActivityClass());
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(LOADING_MESSAGE);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }
}