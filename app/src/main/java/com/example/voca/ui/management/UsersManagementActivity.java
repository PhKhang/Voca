package com.example.voca.ui.management;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.voca.R;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.ui.adapter.UserAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class UsersManagementActivity extends AppCompatActivity {
    private static final String LOADING_MESSAGE = "Đang tải người dùng...";
    private ListView userListView;
    private List<UserDTO> users;
    private List<PostDTO> posts;
    private UserAdapter userAdapter;
    private SearchView searchView;
    private ProgressDialog progressDialog;
    private UsersManagementViewModel viewModel;
    private Handler searchHandler;
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.activity_users);

        initializeViews();
        initializeData();
        setupListeners();
        setupViewModel();
        setClickOnNavigationButton();
    }

    private void setClickOnNavigationButton() {
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void initializeViews() {
        userListView = findViewById(R.id.listViewUsers);
        searchView = findViewById(R.id.searchView);
    }

    private void initializeData() {
        users = new ArrayList<>();
        posts = new ArrayList<>();
        userAdapter = new UserAdapter(this, users, posts);
        userListView.setAdapter(userAdapter);
        searchHandler = new Handler(Looper.getMainLooper());
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(UsersManagementViewModel.class);
        viewModel.getUsersLiveData().observe(this, fetchedUsers -> {
            users = fetchedUsers != null ? new ArrayList<>(fetchedUsers) : new ArrayList<>();
            userAdapter.updateData(users);
            checkDataLoaded();
        });
        viewModel.getPostsLiveData().observe(this, fetchedPosts -> {
            posts = fetchedPosts != null ? new ArrayList<>(fetchedPosts) : new ArrayList<>();
            userAdapter.updateDataPost(posts);
            checkDataLoaded();
        });
        viewModel.getErrorLiveData().observe(this, error -> {
            progressDialog.dismiss();
            showToast(error);
        });
        showLoadingDialog();
        viewModel.fetchData();
    }

    private void setupListeners() {
        setupSearchViewListener();
        setupRootViewClickListener();
        setupUserItemClickListener();
    }

    private void setupSearchViewListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.searchUsers(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> viewModel.searchUsers(newText);
                searchHandler.postDelayed(searchRunnable, 300); // 300ms debounce
                return true;
            }
        });
    }

    private void setupRootViewClickListener() {
        findViewById(R.id.root_layout).setOnClickListener(v -> {
            searchView.clearFocus();
            searchView.setQuery("", false);
            viewModel.searchUsers("");
        });
    }

    private void setupUserItemClickListener() {
        userListView.setOnItemClickListener((parent, view, position, id) -> {
            UserDTO selectedUser = userAdapter.getItem(position);
            if (selectedUser.getRole() != null && selectedUser.getRole().equals("admin")) {
                Toast.makeText(this, "Không thể xem hồ sơ của Admin", Toast.LENGTH_SHORT).show();
            } else {
                navigateToUserDetails(selectedUser);
            }
        });
    }

    private void navigateToUserDetails(UserDTO user) {
        Intent intent = new Intent(this, UserDetailsActivity.class);
        intent.putExtra("user_id", user.get_id());
        startActivity(intent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        viewModel.fetchData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        searchView.setQuery("", false);
        findViewById(R.id.root_layout).requestFocus();
    }

    private void checkDataLoaded() {
        if (!users.isEmpty() && !posts.isEmpty()) {
            progressDialog.dismiss();
        }
    }

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(LOADING_MESSAGE);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}