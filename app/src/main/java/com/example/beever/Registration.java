package com.example.beever;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Registration extends AppCompatActivity {

    Button callLogin, regButton;
    TextInputLayout regName, regUsername, regEmail, regPassword;

    FirebaseDatabase rootNode;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        //Hooks
        callLogin = findViewById(R.id.sign_in);
        regName = findViewById(R.id.reg_name);
        regUsername = findViewById(R.id.reg_username);
        regEmail = findViewById(R.id.reg_email);
        regPassword = findViewById(R.id.reg_password);
        regButton = findViewById(R.id.register);

        //Store data to firebase on click Submit button
        regButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser(v);
            }
        });

        //Return to sign in page
        callLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Registration.super.onBackPressed();
            }
        });
    }

    private Boolean validateName() {
        String s = regName.getEditText().getText().toString();
        if (s.isEmpty()) {
            regName.setError("Field cannot be empty");
            return false;
        } else {
            regName.setError(null);
            regName.setErrorEnabled(false);
            return true;
        }
    }

    private Boolean validateUserName() {
        String s = regUsername.getEditText().getText().toString();
        String noWhiteSpace = ".*\\s+.*";

        if (s.isEmpty()) {
            regUsername.setError("Field cannot be empty");
            return false;

        } else if (s.matches(noWhiteSpace)) {
            regUsername.setError("Spaces are not allowed in username");
            return false;

        } else if (s.length() >= 12) {
            regUsername.setError("Username cannot be more than 12 characters");
            return false;

        } else {
            regUsername.setError(null);
            regUsername.setErrorEnabled(false);
            return true;
        }
    }

    private Boolean validateEmail() {
        String s = regEmail.getEditText().getText().toString();
        String validEmail = "[a-zA-z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (s.isEmpty()) {
            regEmail.setError("Field cannot be empty");
            return false;
        } else if (!s.matches(validEmail)) {
            regEmail.setError("Invalid email address");
            return false;

        } else {
            regEmail.setError(null);
            regEmail.setErrorEnabled(false);
            return true;
        }
    }

    private Boolean validatePassword() {
        String s = regPassword.getEditText().getText().toString();
        String validPassword = "^" +
//                "(?=.*[0-9])" +          //at least 1 digit
//                "(?=.*[a-z])" +          //at least 1 lower case letter
//                "(?=.*[A-Z])" +          //at least 1 upper case letter
                "(?=.*[a-zA-Z])" +         //any letter
                "(?=.*[@#$%^&+=])" +       //at least 1 special character
                ".{4,}" +                  //at least 4 characters
                "$";
        if (s.isEmpty()) {
            regPassword.setError("Field cannot be empty");
            return false;

        } else if (s.contains(" ")) {
            regPassword.setError("Spaces not allowed in password");
            return false;

        } else if (!s.matches(validPassword)) {
            regPassword.setError("Password is too weak");
            return false;

        } else {
            regPassword.setError(null);
            regPassword.setErrorEnabled(false);
            return true;
        }
    }

    //Save user data to Firebase on register click
    public void registerUser(View view) {

        if (!validateName() || !validateUserName() || !validateEmail() || !validatePassword()) {
            return;
        }

        rootNode = FirebaseDatabase.getInstance();
        reference = rootNode.getReference("Users");

        //Get values from user inputs
        String name = regName.getEditText().getText().toString();
        String userName = regUsername.getEditText().getText().toString();
        String email = regEmail.getEditText().getText().toString();
        String password = regPassword.getEditText().getText().toString();

        UserHelperClass userHelperClass = new UserHelperClass(name,userName,email,password);

        reference.child(userName).setValue(userHelperClass);

    }
}