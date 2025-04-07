package com.example.voca.ui.management;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.voca.R;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.UserDTO;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class UsersManagementActivity extends AppCompatActivity {
    private static final String LOADING_MESSAGE = "Đang tải người dùng...";
    private ListView userListView;
    private List<UserDTO> users;
    private List<PostDTO> posts;
    private UserAdapter userAdapter;
    private UserBUS userBUS;
    private PostBUS postBUS;
    private SearchView searchView;
    private ProgressDialog progressDialog;

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
        loadUsers();
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
        userBUS = new UserBUS();
        postBUS = new PostBUS();
        userAdapter = new UserAdapter(this, users, posts);
        userListView.setAdapter(userAdapter);
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
                searchUsersByUsername(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    userAdapter.updateData(users);
                }
                return true;
            }
        });
    }

    private void setupRootViewClickListener() {
        findViewById(R.id.root_layout).setOnClickListener(v -> {
            searchView.clearFocus();
            searchView.setQuery("", false);
            userAdapter.updateData(users);
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
        intent.putExtra("username", user.getUsername());
        intent.putExtra("email", user.getEmail());
        intent.putExtra("avatar", user.getAvatar());
        intent.putExtra("created_at", user.getCreated_at());
        intent.putExtra("updated_at", user.getUpdated_at());
        startActivity(intent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        searchView.setQuery("", false);
        findViewById(R.id.root_layout).requestFocus();
    }

    private void loadUsers() {
        showLoadingDialog();
        userBUS.fetchUsers(new UserBUS.OnUsersFetchedListener() {
            @Override
            public void onUsersFetched(List<UserDTO> fetchedUsers) {
                users = fetchedUsers;
                userAdapter.updateData(users);
                loadPosts();
                progressDialog.dismiss();
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                showToast("Error fetching users: " + error);
            }
        });
    }

    private void loadPosts() {
        postBUS.fetchPosts(new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> fetchedPosts) {
                posts = fetchedPosts;
                userAdapter.updateDataPost(posts);
            }

            @Override
            public void onError(String error) {
                showToast("Error fetching posts: " + error);
            }
        });
    }

    private void searchUsersByUsername(String query) {
//        userBUS.searchUsersByUsername(query, new UserBUS.OnUsersFetchedListener() {
//            @Override
//            public void onUsersFetched(List<UserDTO> fetchedUsers) {
//                userAdapter.updateData(fetchedUsers);
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                showToast(errorMessage);
//            }
//        });
    }

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(LOADING_MESSAGE);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}