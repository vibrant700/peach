package com.huimantaoxiang.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

	private static final String AUTH_PREFS = "auth_prefs";
	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD = "password";

	private EditText etUsername;
	private EditText etPassword;
	private EditText etConfirmPassword;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		etUsername = findViewById(R.id.et_register_username);
		etPassword = findViewById(R.id.et_register_password);
		etConfirmPassword = findViewById(R.id.et_register_confirm_password);
		Button btnRegister = findViewById(R.id.btn_register);
		ImageView btnBack = findViewById(R.id.iv_back);

		btnRegister.setOnClickListener(v -> handleRegister());
		btnBack.setOnClickListener(v -> finish());
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

		SharedPreferences sp = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);
		sp.edit()
				.putString(KEY_USERNAME, username)
				.putString(KEY_PASSWORD, password)
				.apply();

		Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
		finish();
	}
}

