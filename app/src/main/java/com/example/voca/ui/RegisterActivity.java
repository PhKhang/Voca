package com.example.voca.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.voca.R;
import com.example.voca.bus.UserBUS;
import com.example.voca.dto.UserDTO;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends Activity {
    private EditText etEmail, etPassword, etConfirmPassword, etUsername;
    private Button btnRegister, btnBackToLogin;
    private FirebaseAuth auth;

    static UserBUS userBUS = new UserBUS();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        FirebaseApp.initializeApp(this);

        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        btnBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(username)||TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
                        User user = new User(username, email);
                        userRef.setValue(user);

                        createUser(email, username, auth.getCurrentUser().getUid());

                        Toast.makeText(RegisterActivity.this, "Đăng kí thành công", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, LoginActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Đăng kí thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static void createUser(String email, String username, String firebaseUID){
        UserDTO newUser = new UserDTO(null, firebaseUID, username,
                email, "",
                "user", null, null, 0);

        userBUS.createUser(newUser, new UserBUS.OnUserCreatedListener() {
            @Override
            public void onUserCreated(UserDTO user) {
                Log.d("User create successfully", "Success");
            }

            @Override
            public void onError(String error) {
                Log.d("User create failed", error);
            }
        });
    }

    public static class User {
        public String name, email;
        public User() {}
        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
