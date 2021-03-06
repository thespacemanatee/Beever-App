package com.example.beever.feature;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.beever.R;
import com.example.beever.database.GroupEntry;
import com.example.beever.database.UserEntry;
import com.example.beever.navigation.NavigationDrawer;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import br.com.simplepass.loadingbutton.customViews.CircularProgressButton;

public class ChatInfoFragment extends Fragment {

    private CircularProgressButton addUsersBtn, deleteGroup;
    private final ArrayList<String> grpMemberNames = new ArrayList<>();
    private final ArrayList<String> grpMemberImg = new ArrayList<>();
    private final ArrayList<String> grpMemberIds = new ArrayList<>();
    private GroupMemberAdapter adapter;
    private String groupId;
    private String groupName;
    private GroupEntry groupEntry;
    List<Object> members;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Inflate the layout for this fragment
        ((NavigationDrawer) getActivity()).getSupportActionBar();
        View rootView = inflater.inflate(R.layout.fragment_chat_info, container, false);

        //Receive arguments from ChatFragment
        Bundle bundle = this.getArguments();
        String selectedGrpImg = bundle.getString("groupImage");
        groupName = bundle.getString("groupName");
        groupId = bundle.getString("groupId");

        addUsersBtn = rootView.findViewById(R.id.addUsersBtn2);
        deleteGroup = rootView.findViewById(R.id.delete_group);

        //Get chat_info_img in fragment_chat_info.xml and setImageResource
        ShapeableImageView chatImg = rootView.findViewById(R.id.chat_info_img);
        Glide.with(getContext()).load(selectedGrpImg).centerCrop().into(chatImg);

        //Set the group members names
        ListView layout = rootView.findViewById(R.id.chat_info_group_members);
        layout.setScrollContainer(false);
        adapter = new GroupMemberAdapter(getActivity());
        layout.setAdapter(adapter);

        GroupEntry.GetGroupEntry getGroupEntry = new GroupEntry.GetGroupEntry(groupId, 5000) {
            @Override
            public void onPostExecute() {
                groupEntry = getResult();
                members = groupEntry.getMember_list();
                populateRecyclerView();
            }
        };
        getGroupEntry.start();


        addUsersBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putString("groupName", groupName);
                bundle.putString("groupId", groupId);
                bundle.putBoolean("groupExists", true);
                addUsersBtn.startAnimation();
                AddUsersFragment addUsersFragment = new AddUsersFragment();
                addUsersFragment.setArguments(bundle);
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction().addToBackStack("chatInfo");
                transaction.replace(R.id.fragment_container, addUsersFragment).commit();
            }
        });

        deleteGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DeleteGroupDialogFragment deleteGroupDialogFragment = new DeleteGroupDialogFragment(groupId, members);
                deleteGroupDialogFragment.show(getFragmentManager(), "chatInfo");
            }
        });

        return rootView;

    }

    public void populateRecyclerView() {
        grpMemberNames.clear();
        grpMemberImg.clear();
        grpMemberIds.clear();

        for (Object member: members) {
            UserEntry.GetUserEntry userGetter = new UserEntry.GetUserEntry((String) member, 5000) {
                @Override
                public void onPostExecute() {
                    grpMemberIds.add((String) member);
                    grpMemberNames.add(getResult().getName());
                    if (getResult().getDisplay_picture() == null) {
                        grpMemberImg.add("null");
                    } else {
                        grpMemberImg.add(getResult().getDisplay_picture());
                    }
                    adapter.notifyDataSetChanged();
                }
            };
            userGetter.start();
        }
    }

    class GroupMemberAdapter extends BaseAdapter {

        Context context;
        LayoutInflater inflater;
        GroupMemberAdapter(Context c) {
            context = c;
            inflater = LayoutInflater.from(c);
        }

        @Override
        public int getCount() { return grpMemberNames.size(); }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {

            //ViewHolder for smoother scrolling
            GrpMemberViewHolder viewHolder;

            if (view == null) {
                //If view (View to populate GridView cells) not loaded before,
                //create new ViewHolder to hold view
                viewHolder = new GrpMemberViewHolder();

                //Inflate the layout for GridView cells (created as a Fragment)
                view = inflater.inflate(R.layout.group_member_item, null);

                //Get ImageButton and TextView to populate
                viewHolder.memberImg = view.findViewById(R.id.grp_member_img);
                viewHolder.member = view.findViewById(R.id.grp_member);

                //Tag to reference
                view.setTag(viewHolder);

            } else {
                //If view loaded before, get view's tag and cast to ViewHolder
                viewHolder = (GrpMemberViewHolder)view.getTag();
            }

            //Set variables to allow multiple access of same image and text
            String selectedMemberImg = grpMemberImg.get(i);
            String selectedMemberName = grpMemberNames.get(i);
            String selectedMemberId = grpMemberIds.get(i);

            //setImageResource for ImageButton and setText for TextView
            if (selectedMemberImg.equals("null")) {
                Glide.with(context).load(R.drawable.pink_circle).centerCrop().into(viewHolder.memberImg);
            } else {
                Glide.with(context).load(selectedMemberImg).centerCrop().into(viewHolder.memberImg);
            }
            viewHolder.member.setText(selectedMemberName);

            viewHolder.memberImg.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    DeleteUserDialogFragment deleteUserDialogFragment = new DeleteUserDialogFragment(selectedMemberId, groupId, adapter,
                            grpMemberIds, grpMemberImg, grpMemberNames,
                            selectedMemberImg, selectedMemberName);
                    deleteUserDialogFragment.show(getFragmentManager(), "chatInfo");
                    return true;
                }
            });

            return view;
        }

        //To reduce reloading of same layout
        class GrpMemberViewHolder {
            ShapeableImageView memberImg;
            TextView member;
        }

    }

    public static class DeleteUserDialogFragment extends DialogFragment {

        private static final String TAG = "DIALOG";
        private final String selectedMemberId;
        private String selectedMemberImg;
        private final String selectedMemberName;
        private final String groupId;
        private final ChatInfoFragment.GroupMemberAdapter adapter;
        private final ArrayList<String> grpMemberId;
        private final ArrayList<String> grpMemberImg;
        private final ArrayList<String> grpMemberName;

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

    public static class DeleteGroupDialogFragment extends DialogFragment {

        private static final String TAG = "DIALOG";
        private final String groupId;
        private final List<Object> members;
        private final FirebaseFirestore fStore = FirebaseFirestore.getInstance();
        private int counter = 0;
        private Fragment fragment = new Fragment();
        private FragmentManager fm;

        public DeleteGroupDialogFragment(String groupId, List<Object> members) {
            this.groupId = groupId;
            this.members = members;
        }

        @NotNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_groups, null);

            if (getActivity().getSupportFragmentManager().getBackStackEntryAt(getActivity()
                    .getSupportFragmentManager().getBackStackEntryCount() - 1).getName() != null) {

                if (getActivity().getSupportFragmentManager().getBackStackEntryAt(getActivity()
                        .getSupportFragmentManager().getBackStackEntryCount() - 1).getName().equals("dashboard")) {

                    fragment = new DashboardFragment();

                } else if (getActivity().getSupportFragmentManager().getBackStackEntryAt(getActivity()
                        .getSupportFragmentManager().getBackStackEntryCount() - 1).getName().equals("groups")) {

                    fragment = new GroupsFragment();

                } else {

                    fragment = new GroupsFragment();
                }

            } else {

                fragment = new GroupsFragment();
            }

            fm = getActivity().getSupportFragmentManager();
            FragmentTransaction transaction = fm.beginTransaction();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Delete group?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            for (Object member: members) {

                                int full = members.size();
                                UserEntry.UpdateUserEntry deleteGroupId = new UserEntry.UpdateUserEntry((String) member,
                                        UserEntry.UpdateUserEntry.FieldChange.GROUPS_REMOVE, groupId, 5000) {
                                    @Override
                                    public void onPostExecute() {
                                        Log.d("DELETE GROUP ID", "onPostExecute: " + "SUCCESS");

                                        counter++;

                                        if (counter == full) {
                                            fStore.collection("groups").document(groupId).delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d("DELETE GROUP", "DocumentSnapshot successfully deleted!");
                                                            fm.popBackStack();
                                                            transaction.replace(R.id.fragment_container, fragment).commit();
                                                            Utils utils = new Utils(v.getContext());
                                                            utils.fadeIn();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.w("DELETE GROUP", "Error deleting document", e);
                                                        }
                                                    });
                                        }
                                    }
                                };
                                deleteGroupId.start();
                            }
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
}