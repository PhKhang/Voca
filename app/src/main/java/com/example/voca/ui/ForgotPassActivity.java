package com.example.voca.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voca.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassActivity extends AppCompatActivity {
    private EditText emailEditText;
    private Button resetPasswordButton, returnLoginButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        setContentView(R.layout.forgot_password);

        emailEditText = findViewById(R.id.emailEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        mAuth = FirebaseAuth.getInstance();

        resetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(ForgotPassActivity.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Gửi yêu cầu đặt lại mật khẩu
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(ForgotPassActivity.this, "Kiểm tra email để đặt lại mật khẩu", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(ForgotPassActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        findViewById(R.id.returnToLoginButton).setOnClickListener(v -> {
            startActivity(new Intent(ForgotPassActivity.this, LoginActivity.class));
        });
    }
}
