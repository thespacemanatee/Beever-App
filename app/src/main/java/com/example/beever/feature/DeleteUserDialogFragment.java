package com.example.beever.feature;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.DialogFragment;

import com.example.beever.database.GroupEntry;
import com.example.beever.database.UserEntry;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DeleteUserDialogFragment extends DialogFragment {

    private static final String TAG = "DIALOG";
    private String selectedMemberId, selectedMemberImg, selectedMemberName, groupId;
    private ChatInfoFragment.GroupMemberAdapter adapter;
    private ArrayList<String> grpMemberId, grpMemberImg, grpMemberName;

    public DeleteUserDialogFragment(String selectedMemberId, String groupId, ChatInfoFragment.GroupMemberAdapter adapter,
                                    ArrayList<String> grpMemberId, ArrayList<String> grpMemberImg, ArrayList<String> grpMemberName,
                                    String selectedMemberImg, String selectedMemberName) {
        this.selectedMemberId = selectedMemberId;
        this.groupId = groupId;
        this.selectedMemberImg = selectedMemberImg;
        this.selectedMemberImg = selectedMemberImg;
        this.selectedMemberName =selectedMemberName;
        this.grpMemberId = grpMemberId;
        this.grpMemberImg = grpMemberImg;
        this.grpMemberName = grpMemberName;
        this.adapter = adapter;
    }

    @NotNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Delete user?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        UserEntry.UpdateUserEntry deleteGroupId = new UserEntry.UpdateUserEntry(selectedMemberId,
                                UserEntry.UpdateUserEntry.FieldChange.GROUPS_REMOVE, groupId, 5000) {
                            @Override
                            public void onPostExecute() {
                                Log.d("DELETE GROUP ID", "onPostExecute: " + "SUCCESS");
                                GroupEntry.UpdateGroupEntry deleteUser = new GroupEntry.UpdateGroupEntry(groupId,
                                        GroupEntry.UpdateGroupEntry.FieldChange.MEMBER_LIST_REMOVE, selectedMemberId, 5000) {
                                    @Override
                                    public void onPostExecute() {
                                        Log.d("DELETE GROUP USER", "onPostExecute: " + "SUCCESS");
                                        grpMemberId.remove(selectedMemberId);
                                        grpMemberImg.remove(selectedMemberImg);
                                        grpMemberName.remove(selectedMemberName);
                                        adapter.notifyDataSetChanged();

                                    }
                                };
                                deleteUser.start();
                            }
                        };
                        deleteGroupId.start();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}