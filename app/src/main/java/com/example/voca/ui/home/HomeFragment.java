package com.example.voca.ui.home;

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
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.SongBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.SongDTO;
import com.example.voca.databinding.FragmentHomeBinding;
import com.example.voca.ui.sing.SingAdapter;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private PostAdapter postAdapter;
    private SongBUS songBUS;
    private PostBUS postBUS;
    private ListView listViewSing;
    private ListView listViewPost;
    private Context context;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
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
        songBUS = new SongBUS();
        postBUS = new PostBUS();

        listViewSing = binding.listViewSings;
        listViewPost = binding.listViewPosts;

        fetchSongsAndPosts();

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
        /*
        binding.btnSignout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "signout", Toast.LENGTH_SHORT).show();
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(requireContext(), "Không có người dùng nào đăng nhập!", Toast.LENGTH_SHORT).show();
            }

            if (user != null) {
                for (UserInfo profile : user.getProviderData()) {
                    String providerId = profile.getProviderId();

                    if (providerId.equals("google.com")) {
                        GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();
                    }
                }
            }
            mAuth.signOut();

            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });
        */

        binding.openSongsManagementPage.setOnClickListener(v -> {
            String priorityPostId = "post123";
            Bundle bundle = new Bundle();
            bundle.putString("priorityPostId", priorityPostId);
            navController.navigate(R.id.action_homeFragment_to_dashboardFragment, bundle);
        });

        binding.openSongsManagementPage.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AdminActivity.class);
            startActivity(intent);
        });

        View root = binding.getRoot();
        return root;
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

    private void fetchSongsAndPosts() {
        songBUS.fetchSongs(new SongBUS.OnSongsFetchedListener() {
            @Override
            public void onSongsFetched(List<SongDTO> songs) {
                if (songs != null) {
                    Collections.sort(songs, new Comparator<SongDTO>() {
                        @Override
                        public int compare(SongDTO s1, SongDTO s2) {
                            return Integer.compare(s2.getRecorded_people(), s1.getRecorded_people());
                        }
                    });
                    if (songs.size() > 3) {
                        songList = new ArrayList<>(songs.subList(0, 3));
                    } else {
                        songList = new ArrayList<>(songs);
                    }
                } else {
                    songList = new ArrayList<>();
                }

                postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
                    @Override
                    public void onPostsFetched(List<PostDTO> posts) {
                        postList = posts;
                        if (context != null)
                            singAdapter = new SingAdapter(context, postList, songList);
                        listViewSing.setAdapter(singAdapter);

                        if (posts != null) {
                            Collections.sort(posts, new Comparator<PostDTO>() {
                                @Override
                                public int compare(PostDTO p1, PostDTO p2) {
                                    return Integer.compare(p2.getLikes(), p1.getLikes());
                                }
                            });
                            if (posts.size() > 3) {
                                postList = new ArrayList<>(posts.subList(0, 3));
                            } else {
                                postList = new ArrayList<>(posts);
                            }
                        } else {
                            postList = new ArrayList<>();
                        }

                        if (context != null)
                            postAdapter = new PostAdapter(context, postList);
                        listViewPost.setAdapter(postAdapter);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(requireContext(), "Error fetching posts: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error fetching songs: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}