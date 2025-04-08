package com.example.voca.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voca.R;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.UserDTO;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private EditText emailInput, passwordInput;
    private Button loginBtn;
    private FirebaseAuth mAuth; // Firebase Authentication
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100;

    UserBUS userBUS = new UserBUS();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.login);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("376964926034-26q0v64muogqhda7e51ket6qgpt9soos.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.btnGoogleSignIn).setOnClickListener(v -> signInWithGoogle());

        loginBtn = findViewById(R.id.btnLogin);
        emailInput = findViewById(R.id.etEmail);
        passwordInput = findViewById(R.id.etPassword);

        mAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        Button btnGoToRegister = findViewById(R.id.btnSignup);
        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        TextView forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        forgotPasswordTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPassActivity.class);
            startActivity(intent);
        });

    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            updateCurrentUser(user.getUid());
                            goToMainActivity(user.getUid());
                        }
                    } else {
                        Log.e("LoginError", "Đăng nhập thất bại", task.getException());
                        Toast.makeText(LoginActivity.this, "Tài khoản hoặc mật khẩu không đúng!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("Google Sign-In", "Đăng nhập thất bại", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkIfUserExists(user);
                            updateCurrentUser(mAuth.getUid());
                        }
                    } else {
                        Toast.makeText(this, "Xác thực thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIfUserExists(FirebaseUser user) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        String uid = user.getUid();

        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Người dùng chưa có trong database -> đăng nhập lần đầu -> yêu cầu nhập thêm dữ liệu để lưu trên databasse
                    showUsernamePrompt(user, usersRef);
                } else {
                    // Người dùng đã có trong database -> Chuyển đến MainActivity
                    goToMainActivity(uid);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Firebase", "Lỗi kiểm tra người dùng", error.toException());
            }
        });
    }

    private void showUsernamePrompt(FirebaseUser user, DatabaseReference usersRef) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Lần đầu đăng nhập. Vui lòng nhập tên tài khoản");

        // Tạo ô nhập username
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String username = input.getText().toString().trim();
            if (!username.isEmpty()) {
                // Lưu vào Firebase Realtime Database
                RegisterActivity.User newUser = new RegisterActivity.User(username, user.getEmail());

                // Lưu dữ liệu người dùng lên MongoDB
                RegisterActivity.createUser(user.getEmail(), username, user.getUid());

                usersRef.child(user.getUid()).setValue(newUser)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                goToMainActivity(user.getUid());
                            } else {
                                Toast.makeText(this, "Lưu thất bại", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Tên không được để trống", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    public void updateCurrentUser(String firebaseUID){
        userBUS.fetchUserByFirebaseUID(firebaseUID, new UserBUS.OnUserFetchedByFirebaseUIDListener() {
            @Override
            public void onUserFetched(List<UserDTO> user) {
                UserDTO currentUser = user.get(0);
//                Log.d("fetch user data success", user.toString());

                // Lưu userId để dùng trong quá trình sử dụng ứng dụng
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("userId", currentUser.get_id());
                editor.apply();
            }

            @Override
            public void onError(String error) {
                Log.d("fetch user data fail", error);
            }
        });
    }
    private void goToMainActivity(String userFirebaseUID) {
        // Kiêm tra vai trò của người dùng và chuyển hướng đến màn hình chính tương ứng
        userBUS.fetchUserByFirebaseUID(userFirebaseUID, new UserBUS.OnUserFetchedByFirebaseUIDListener() {
            @Override
            public void onUserFetched(List<UserDTO> user) {
                UserDTO currentUser = user.get(0);
                Intent intent;
                if (currentUser.getRole().equals("user")){
                    intent = new Intent(LoginActivity.this, MainActivity.class);
                } else {
                    intent = new Intent(LoginActivity.this, AdminActivity.class);
                    intent.putExtra("username", currentUser.getUsername());
                    startActivity(intent);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {

            }
        });
    }
}
