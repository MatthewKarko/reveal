package com.example.revealapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;

import java.util.Map;
import java.util.HashMap;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText nameEditText;
    private EditText usernameEditText;
    private Button continueButton;
    private CheckBox termsCheckBox;
    private TextView loginHyperlink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialise Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // Reference to input fields
        nameEditText = findViewById(R.id.nameTextBox);
        usernameEditText = findViewById(R.id.usernameTextBox);
        emailEditText = findViewById(R.id.emailTextBox);
        passwordEditText = findViewById(R.id.passwordTextBox);
        continueButton = findViewById(R.id.continueButton);
        termsCheckBox = findViewById(R.id.checkBox);
        loginHyperlink = findViewById(R.id.loginHyperlink);

        continueButton.setOnClickListener(v -> {
            if (termsCheckBox.isChecked()) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String name = nameEditText.getText().toString().trim();
                String username = usernameEditText.getText().toString().trim();
                if (!email.isEmpty() && !password.isEmpty() && !name.isEmpty() && !username.isEmpty()) {
                    createAccount(email, password);
                }
                else {
                    updateUI(null);
                }

            } else {
                Toast.makeText(RegisterActivity.this, "Please accept the terms and conditions.", Toast.LENGTH_SHORT).show();
            }
        });

        loginHyperlink.setOnClickListener(v -> changeToLogin());
    }

    private void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("RegisterActivity", "Register successful");
                            FirebaseUser user = mAuth.getCurrentUser();

                            UserProfileChangeRequest updateProfile = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(usernameEditText.getText().toString().trim())
                                    .build();

                            user.updateProfile(updateProfile)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d("RegisterActivity", "Username added");
                                                changeToLogin();
                                            }
                                            else {
                                                Log.d("RegisterActivity", "Failed to add username", task.getException());
                                            }
                                        }
                                    });
                            updateUI(user);
//
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("RegisterActivity", "Register failed", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", nameEditText.getText().toString().trim());
        userMap.put("username", usernameEditText.getText().toString().trim());
        userMap.put("email", user.getEmail());

        db.collection("Users").document(user.getUid()).set(userMap)
                .addOnSuccessListener(new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {
                        Log.d("RegisterActivity", "DocumentSnapshot added with ID: " + user.getUid());
                        mAuth.signOut(); // sign out to prevent auto login
                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("RegisterActivity", "Error adding document", e);
                        mAuth.signOut(); // sign out to prevent auto login
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
            saveUserToFirestore(user);
            // redirect to home user page
        } else {
            Toast.makeText(RegisterActivity.this, "Failed to register user.", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
    }
}