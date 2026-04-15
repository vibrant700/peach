package com.huimantaoxiang.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_login);

        if (BmobUser.getCurrentUser(BmobAppUser.class) != null) {
            goToMain();
            return;
        }

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        TextView tvGoRegister = findViewById(R.id.tv_go_register);

        btnLogin.setOnClickListener(v -> handleLogin());
        tvGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.login_input_required, Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);

        BmobAppUser loginUser = new BmobAppUser();
        loginUser.setUsername(username);
        loginUser.setPassword(password);
        try {
            loginUser.login(new SaveListener<BmobAppUser>() {
                @Override
                public void done(BmobAppUser user, BmobException e) {
                    btnLogin.setEnabled(true);
                    if (e == null) {
                        Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                        goToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, BmobErrorMessageUtil.toFriendlyMessage(e), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Throwable t) {
            btnLogin.setEnabled(true);
            Toast.makeText(this, BmobErrorMessageUtil.toFriendlyMessage(t), Toast.LENGTH_SHORT).show();
        }
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}

