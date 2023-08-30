package com.example.chronicle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    EditText emailEditText, passwordEditText;
    Button loginBtn;
    TextView createAccountBtnTextView, forgotPasswordTextView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.email_editText);
        passwordEditText = findViewById(R.id.password_editText);
        loginBtn = findViewById(R.id.login_btn);
        createAccountBtnTextView = findViewById(R.id.create_account_textView_btn);
        forgotPasswordTextView = findViewById(R.id.forgot_password_textView_btn);
        forgotPasswordTextView.setOnClickListener((v) -> resetPassword());

        loginBtn.setOnClickListener((v) -> loginUser());
        createAccountBtnTextView.setOnClickListener((v) -> startActivity(new Intent(LoginActivity.this, CreateAccountActivity.class)));
    }

    void resetPassword() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is Required");
            return;
        }

        FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                utility.showToast(LoginActivity.this, "Password reset email sent.");
            }
            else {
                utility.showToast(LoginActivity.this, "Failed to send reset email. Retry");
            }
        });
    }

    boolean validateData(String email, String password){
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailEditText.setError("Email is Invalid");
        }
        if (password.length()<6){
            passwordEditText.setError("Password is too Small");
            return false;
        }
        return true;
    }

    void loginUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        boolean isvalidated = validateData(email,password);
        if (!isvalidated){
            return;
        }

        loginAccountInFirebase(email,password);
    }

    void loginAccountInFirebase(String email, String password){
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    //login successful
                    if (Objects.requireNonNull(firebaseAuth.getCurrentUser()).isEmailVerified()){
                        //go to main activity
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    }
                    else {
                        utility.showToast(LoginActivity.this, "Email not Verified");
                    }
                }
                else {
                    // login unsuccessful
                    utility.showToast(LoginActivity.this, Objects.requireNonNull(task.getException()).getLocalizedMessage());
                }
            }
        });
    }
}