package com.huimantaoxiang.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;

public class RegisterActivity extends AppCompatActivity {

	private EditText etUsername;
	private EditText etPassword;
	private EditText etConfirmPassword;
	private Button btnRegister;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		etUsername = findViewById(R.id.et_register_username);
		etPassword = findViewById(R.id.et_register_password);
		etConfirmPassword = findViewById(R.id.et_register_confirm_password);
		btnRegister = findViewById(R.id.btn_register);
		View btnBack = findViewById(R.id.iv_back);

		btnRegister.setOnClickListener(v -> handleRegister());
		btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
	}

	private void handleRegister() {
		String username = etUsername.getText().toString().trim();
		String password = etPassword.getText().toString().trim();
		String confirmPassword = etConfirmPassword.getText().toString().trim();

		if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
			Toast.makeText(this, R.string.register_input_required, Toast.LENGTH_SHORT).show();
			return;
		}

		if (password.length() < 6) {
			Toast.makeText(this, R.string.register_password_short, Toast.LENGTH_SHORT).show();
			return;
		}

		if (!password.equals(confirmPassword)) {
			Toast.makeText(this, R.string.register_password_not_match, Toast.LENGTH_SHORT).show();
			return;
		}

		btnRegister.setEnabled(false);

		BmobAppUser user = new BmobAppUser();
		user.setUsername(username);
		user.setPassword(password);
		try {
			user.signUp(new SaveListener<BmobAppUser>() {
				@Override
				public void done(BmobAppUser signedUser, BmobException e) {
					btnRegister.setEnabled(true);
					if (e == null) {
						Toast.makeText(RegisterActivity.this, R.string.register_success, Toast.LENGTH_SHORT).show();
						finish();
					} else {
						Toast.makeText(RegisterActivity.this, BmobErrorMessageUtil.toFriendlyMessage(e), Toast.LENGTH_SHORT).show();
					}
				}
			});
		} catch (Throwable t) {
			btnRegister.setEnabled(true);
			Toast.makeText(this, BmobErrorMessageUtil.toFriendlyMessage(t), Toast.LENGTH_SHORT).show();
		}
	}
}

