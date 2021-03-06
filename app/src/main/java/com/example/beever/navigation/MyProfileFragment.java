package com.example.beever.navigation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.beever.R;
import com.example.beever.admin.MainActivity;
import com.example.beever.database.GroupEntry;
import com.example.beever.database.UserEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;
import de.hdodenhof.circleimageview.CircleImageView;

public class MyProfileFragment extends Fragment {

    //Create variables for each element
    private TextInputLayout name, email;
    private TextView usernameLabel, nameLabel, profileMeetings, profileDeadlines;
    private CircularProgressButton update;
    private CircleImageView profilePic;
    private SharedPreferences mSharedPref;

    //Global variables to hold user data inside this activity
    private static String _USERNAME,_NAME,_EMAIL;
    private final StorageReference storageReference = FirebaseStorage.getInstance().getReference();
    private final FirebaseAuth fAuth = FirebaseAuth.getInstance();
    private String userID;
    private final String TAG = "Logcat";
    private int counterDeadlines = 0;
    private int counter = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.my_profile_fragment, container, false);

        ((NavigationDrawer)getActivity()).getSupportActionBar().setTitle("Profile");

        mSharedPref = getActivity().getSharedPreferences("SharedPref", Context.MODE_PRIVATE);

        //Get user information from SharedPreferences
        _USERNAME = mSharedPref.getString("registeredUsername","");
        _NAME = mSharedPref.getString("registeredName","");
        _EMAIL = mSharedPref.getString("registeredEmail","");

        //Hooks
        usernameLabel = root.findViewById(R.id.username_label);
        nameLabel = root.findViewById(R.id.name_label);
        name = root.findViewById(R.id.name_field);
        email = root.findViewById(R.id.email_field);
        update = root.findViewById(R.id.update_button);
        profilePic = root.findViewById(R.id.profile_image);
        profileMeetings = root.findViewById(R.id.profile_meetings);
        profileDeadlines = root.findViewById(R.id.profile_deadlines);
        FloatingActionButton toOnBoarding = root.findViewById(R.id.to_onboarding);


        FirebaseUser fUser = fAuth.getCurrentUser();
        userID = fUser.getUid();


        if (fUser.getPhotoUrl() != null) {
            Glide.with(getActivity()).load(fUser.getPhotoUrl()).into(profilePic);
        }

        UserEntry.GetUserEntry getUserEntry = new UserEntry.GetUserEntry(userID, 5000) {
            @Override
            public void onPostExecute() {
            }
        };
        getUserEntry.start();

        showUserData();

        toOnBoarding.setOnClickListener(v -> {
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putBoolean("firstTime", true);
            editor.apply();

            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
            getActivity().finish();
        });


        update.setOnClickListener(v -> {

            update.startAnimation();

            //Call each method to check if user inputs are different from existing values and if not, set them to new values
            if (!isNameChanged()) {
                update.revertAnimation();
                Toast.makeText(getActivity(), "Please enter a different value", Toast.LENGTH_SHORT).show();
            }
        });

        profilePic.setOnClickListener(v -> {

            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 1000);
        });

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1000) {
            if (resultCode == Activity.RESULT_OK) {
                Uri imageUri = data.getData();

                update.startAnimation();
                uploadImage(imageUri);
            }
        }
    }

    private void uploadImage(Uri imageUri) {
        StorageReference fileReference = storageReference.child("users/" + userID + "/group_image.jpg");
        fileReference.putFile(imageUri).addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl().addOnSuccessListener(this::uploadToFirebase));
    }

    private void uploadToFirebase(Uri imageUri) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(_NAME)
                .setPhotoUri(imageUri)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated.");
                    }
                });

        UserEntry.UpdateUserEntry setProfileImage = new UserEntry.UpdateUserEntry(userID,
                UserEntry.UpdateUserEntry.FieldChange.DISPLAY_PICTURE, imageUri.toString(), 5000) {
            @Override
            public void onPostExecute() {
                Glide.with(getActivity()).load(imageUri).into(profilePic);
                Glide.with(getActivity()).load(imageUri).into((CircleImageView) getActivity().findViewById(R.id.profile_nav));
                Toast.makeText(getActivity(), "Updated profile image", Toast.LENGTH_SHORT).show();
                update.revertAnimation();
            }
        };
        setProfileImage.start();
    }

    private void showUserData() {

        usernameLabel.setText(_USERNAME);
        nameLabel.setText(_NAME);
        name.getEditText().setText(_NAME);
        email.getEditText().setText(_EMAIL);

        UserEntry.GetUserEntry getUserEntry = new UserEntry.GetUserEntry(userID, 5000) {
            @Override
            public void onPostExecute() {

                for (Object o: getResult().getGroups()) {
                    int full = getResult().getGroups().size();
                    GroupEntry.GetGroupEntry getGroupEntry = new GroupEntry.GetGroupEntry((String) o, 5000) {
                        @Override
                        public void onPostExecute() {
                            counter++;
                            if (getResult().getTodo_list().size() > 0) {
                                counterDeadlines += (getResult().retrieveGroupTodos(true, false).size());
                            }
                            if (counter == full) {
                                profileDeadlines.setText(String.valueOf(counterDeadlines));
                            }
                        }
                    };
                    getGroupEntry.start();
                }

                UserEntry.GetUserRelevantEvents getUserRelevantEvents = new UserEntry.GetUserRelevantEvents(getResult(), 5000, true, false) {
                    @Override
                    public void onPostExecute() {
                        int counterMeetings = getResult().size();
                        profileMeetings.setText(String.valueOf(counterMeetings));
                    }
                };
                getUserRelevantEvents.start();
            }
        };
        getUserEntry.start();
    }

    private boolean isNameChanged() {

        if (!_NAME.equals(name.getEditText().getText().toString())) {
            String newName = name.getEditText().getText().toString();
            if (newName.isEmpty()) {
                name.setError("Field cannot be empty");
                return false;
            } else {
                name.setError(null);
                name.setErrorEnabled(false);

                UserEntry.UpdateUserEntry setName = new UserEntry.UpdateUserEntry(userID,
                        UserEntry.UpdateUserEntry.FieldChange.NAME, newName, 5000) {
                    @Override
                    public void onPostExecute() {
                        _NAME = newName;
                        nameLabel.setText(_NAME);
                        SharedPreferences.Editor editor = mSharedPref.edit();
                        editor.putString("registeredName", newName);
                        editor.apply();
                        update.revertAnimation();
                        Toast.makeText(getActivity(), "Name changed successfully!", Toast.LENGTH_SHORT).show();
                    }
                };
                setName.start();
                return true;
            }

        } else {
            return false;
        }
    }
}
