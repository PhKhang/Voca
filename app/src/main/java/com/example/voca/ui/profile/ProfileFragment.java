package com.example.voca.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.voca.R;
import com.example.voca.bus.PostBUS;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.PostDTO;
import com.example.voca.dto.UserDTO;
import com.example.voca.service.FileUploader;
import com.example.voca.ui.adapter.PostAdapter;
import com.example.voca.ui.auth.LoginActivity;
import com.example.voca.ui.dashboard.SharedPostViewModel;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProfileFragment extends Fragment {
    private UserDTO curUser;
    RecyclerView recyclerView;
    PostAdapter postAdapter;
    List<PostDTO> postList;
    ExoPlayer player;
    PostBUS postBUS = new PostBUS();
    UserBUS userBUS = new UserBUS();
    private SharedPostViewModel sharedPostViewModel;
    SharedPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        player = new ExoPlayer.Builder(requireContext()).build();

        recyclerView = view.findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        sharedPostViewModel = new ViewModelProvider(requireActivity()).get(SharedPostViewModel.class);
        sharedPostViewModel.fetchUserPosts(userId);
        sharedPostViewModel.getUserPostsLiveData().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                List<PostDTO> reversedPosts = new ArrayList<>(posts);
                Collections.reverse(reversedPosts);
                postAdapter.updateData(reversedPosts);
            }
        });

        postAdapter = new PostAdapter(new ArrayList<>(), requireContext(), player);
        recyclerView.setAdapter(postAdapter);

        userBUS.fetchUserById(userId, new UserBUS.OnUserFetchedListener() {
            @Override
            public void onUserFetched(UserDTO user) {
                TextView username = view.findViewById(R.id.txt_username);
                username.setText(user.getUsername());

                TextView email = view.findViewById(R.id.txt_email);
                email.setText(user.getEmail());

                ImageView avatar = view.findViewById(R.id.avatarImage);

                if (isAdded() && avatar != null) { // Check both Fragment and View
                    Glide.with(ProfileFragment.this)
                            .load(user.getAvatar())
                            .placeholder(R.drawable.ic_profile_2_24dp) // Ảnh mặc định nếu tải chậm
                            .error(R.drawable.ic_profile_2_24dp) // Ảnh nếu lỗi tải
                            .into(avatar);
                }

                curUser = user;
                loadUserPosts();
            }

            @Override
            public void onError(String error) {
                Log.d("UserProfileError", error);
            }
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            for (UserInfo userInfo : user.getProviderData()) {
                if (userInfo.getProviderId().equals(EmailAuthProvider.PROVIDER_ID)) {
                    // Hiện mật khẩu và nút chỉnh sửa nếu đăng ký bằng email và password
                    view.findViewById(R.id.password_section).setVisibility(View.VISIBLE);
                    view.findViewById(R.id.txt_password).setVisibility(View.VISIBLE);
                    break;
                }
            }
        }

        view.findViewById(R.id.edit_avatar).setOnClickListener(v1 -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 2);
        });

        view.findViewById(R.id.edit_passwordBtn).setOnClickListener(v1 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
            builder.setView(dialogView);

            EditText edtOldPassword = dialogView.findViewById(R.id.edtOldPassword);
            EditText edtNewPassword = dialogView.findViewById(R.id.edtNewPassword);
            EditText edtConfirmPassword = dialogView.findViewById(R.id.edtConfirmPassword);
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);

            AlertDialog dialog = builder.create();
            dialog.show();

            btnConfirm.setOnClickListener(v2 -> {
                String oldPass = edtOldPassword.getText().toString().trim();
                String newPass = edtNewPassword.getText().toString().trim();
                String confirmPass = edtConfirmPassword.getText().toString().trim();

                if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newPass.equals(confirmPass)) {
                    Toast.makeText(requireContext(), "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (user != null) {
                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);
                    user.reauthenticate(credential).addOnCompleteListener(authTask -> {
                        if (authTask.isSuccessful()) {
                            user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                                if (updateTask.isSuccessful()) {
                                    Toast.makeText(requireContext(), "Mật khẩu đã cập nhật thành công!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(requireContext(), "Lỗi cập nhật mật khẩu!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(requireContext(), "Mật khẩu cũ không đúng!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            btnCancel.setOnClickListener(v -> dialog.dismiss());
        });

        view.findViewById(R.id.edit_usernameBtn).setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_change_username, null);
            builder.setView(dialogView);

            EditText edtUsername = dialogView.findViewById(R.id.edtUsername);
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);

            // Đặt username hiện tại làm giá trị mặc định
            edtUsername.setText(curUser.getUsername());
            edtUsername.setSelection(curUser.getUsername().length()); // Đưa con trỏ đến cuối

            AlertDialog dialog = builder.create();
            dialog.show();

            btnConfirm.setOnClickListener(v1 -> {
                String newUsername = edtUsername.getText().toString().trim();

                if (newUsername.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập tên người dùng!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newUsername.equals(curUser.getUsername())) {
                    Toast.makeText(requireContext(), "Tên người dùng mới phải khác tên hiện tại!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newUsername.length() > 50) {
                    edtUsername.setError("Tên không quá 50 ký tự");
                    return;
                }

                // Cập nhật username lên MongoDB
                curUser.setUsername(newUsername);
                userBUS.updateUser(curUser.get_id(), curUser, new UserBUS.OnUserUpdatedListener() {
                    @Override
                    public void onUserUpdated(UserDTO user) {
                        Toast.makeText(getContext(), "Thay đổi tên thành công!", Toast.LENGTH_SHORT).show();
                        TextView username = view.findViewById(R.id.txt_username);
                        username.setText(newUsername);
                        loadUserPosts();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Thay đổi tên thất bại!", Toast.LENGTH_SHORT).show();
                    }
                });
            });

            btnCancel.setOnClickListener(v1 -> dialog.dismiss());
        });

        view.findViewById(R.id.signOutBtn).setOnClickListener(v -> {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
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

            // Xóa dữ liệu SharedPreferences
            SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void loadUserPosts() {
        postBUS.fetchPostsByUserId(curUser.get_id(), new PostBUS.OnPostsFetchedListener() {
            @Override
            public void onPostsFetched(List<PostDTO> posts) {
                if (postList == null) {
                    postList = new ArrayList<>();
                }
                postList.clear(); // Xóa danh sách cũ
                postList.addAll(posts); // Thêm danh sách mới vào danh sách cũ
                postAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Log.d("UserProfileError", error);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri imageUri = data.getData(); // Lấy URI của ảnh đã chọn
                showConfirmDialog(imageUri); // Hiển thị Dialog xác nhận
            }
        }
    }

    private void showConfirmDialog(Uri imageUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_confirm_avatar, null);
        builder.setView(dialogView);

        ImageView previewAvatar = dialogView.findViewById(R.id.preview_avatar);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        Glide.with(this).load(imageUri).circleCrop().into(previewAvatar);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Xác nhận -> Upload file lên server
        btnConfirm.setOnClickListener(v -> {
            new FileUploader().run(requireContext(), imageUri, new FileUploader.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String url) {
                    String currentAvatarUrl = curUser.getAvatar();
                    // Xóa avatar cũ
                    if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                        new FileUploader().deleteFileByURL(currentAvatarUrl);
                    }
                    curUser.setAvatar(url);

                    userBUS.updateUser(curUser.get_id(), curUser,new UserBUS.OnUserUpdatedListener() {
                        @Override
                        public void onUserUpdated(UserDTO user) {
                            loadUserPosts();
                            ImageView avatar = requireView().findViewById(R.id.avatarImage);

                            Glide.with(ProfileFragment.this)
                                    .load(user.getAvatar())
                                    .placeholder(R.drawable.default_account_avatar) // Ảnh mặc định nếu tải chậm
                                    .error(R.drawable.default_account_avatar) // Ảnh nếu lỗi tải
                                    .into(avatar);
                        }
                        @Override
                        public void onError(String error) {
                            Log.d("AvatarUpdateFailed", error);
                        }
                    });

                    dialog.dismiss();
                }

                @Override
                public void onFailure() {
                    Log.d("UploadAvatarFailed", "");
                }
            });

        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
    }
}