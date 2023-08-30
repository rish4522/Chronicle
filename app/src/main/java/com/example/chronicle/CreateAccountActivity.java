package com.example.chronicle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class CreateAccountActivity extends AppCompatActivity {

    static final int PICK_IMAGE_REQUEST = 1;
    EditText emailEditText, passwordEditText, confirmPasswordEditText;
    Button createAccountBtn, selectImageButton;
    CheckBox checkBox;
    TextView loginBtnTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        emailEditText = findViewById(R.id.email_editText);
        passwordEditText = findViewById(R.id.password_editText);
        confirmPasswordEditText = findViewById(R.id.confirm_password_editText);
        checkBox = findViewById(R.id.termscheckBox);
        createAccountBtn = findViewById(R.id.create_account_btn);
        loginBtnTextView = findViewById(R.id.create_account_textView_btn);

        createAccountBtn.setOnClickListener(v -> createAccount());
        loginBtnTextView.setOnClickListener(v -> finish());

    }

    void createAccount(){
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        boolean isvalidated = validateData(email,password,confirmPassword);
        if (!isvalidated){
            return;
        }

        createAccountInFirebase(email,password);
    }

    void createAccountInFirebase(String email, String password){
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(CreateAccountActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                    // account created
                    utility.showToast(CreateAccountActivity.this, "Account Created Successfully, Please check your email for verification");
                    Objects.requireNonNull(firebaseAuth.getCurrentUser()).sendEmailVerification();

                    Note defaultNote = new Note();
                    utility.getCollectionReferenceForNotes().document(Objects.requireNonNull(firebaseAuth.getUid())).collection("my_notes").document().set(defaultNote);
                    firebaseAuth.signOut();
                    finish();
                }
                else {
                    // failure
                    Toast.makeText(CreateAccountActivity.this, Objects.requireNonNull(task.getException()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    boolean validateData(String email, String password, String confirmPassword){
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            emailEditText.setError("Email is Invalid");
        }
        if (password.length()<6){
            passwordEditText.setError("Password is too Small");
            return false;
        }
        if (!password.equals(confirmPassword)){
            confirmPasswordEditText.setError("Password not matched");
            return false;
        }
        return true;
    }
}