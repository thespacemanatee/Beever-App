
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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.beever.R;
import com.example.beever.database.GroupEntry;
import com.example.beever.database.UserEntry;
import com.example.beever.navigation.NavigationDrawer;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroupsFragment extends Fragment {

    private final FirebaseAuth fAuth = FirebaseAuth.getInstance();
    private final String userId = fAuth.getUid();
    private static final String GROUP_ENTRIES = "groupEntries";
    private static final String GROUP_IDS = "groupIds";
    private static final String USER_ENTRY = "userEntry";
    private UserEntry userEntry;
    private ArrayList<GroupEntry> groupEntries = new ArrayList<>();
    private ArrayList<String> groupIds = new ArrayList<>();

    //Initialise global ArrayLists for storing information of Groups a User is in
    private final ArrayList<String> grpImages = new ArrayList<>();
    private final ArrayList<String> grpNames = new ArrayList<>();
    private final ArrayList<String> grpIds = new ArrayList<>();

    //For the Add Groups Button
    private final String addGrpBtnImg = Integer.toString(R.drawable.plus);
    private final String addGrpBtnText = "Create group";
    private ImageView imageView;
    private TextView textView;
    private GridAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Inflate the layout for this fragment
        ((NavigationDrawer)getActivity()).getSupportActionBar().setTitle("Groups");

        View rootView = inflater.inflate(R.layout.fragment_groups, container, false);
        imageView = rootView.findViewById(R.id.no_group_image);
        textView = rootView.findViewById(R.id.no_group_text);

        //Populate GridView in fragment_groups.xml with Groups
        GridView layout = rootView.findViewById(R.id.groupButtons);
        adapter = new GridAdapter(getActivity());
        layout.setAdapter(adapter);

        Bundle bundle = this.getArguments();

        if (bundle != null) {
            userEntry = bundle.getParcelable(USER_ENTRY);
            groupEntries = bundle.getParcelableArrayList(GROUP_ENTRIES);
            Log.d("GROUP ENTRIES", groupEntries.toString());
            groupIds = bundle.getStringArrayList(GROUP_IDS);
            Log.d("GROUP IDS", groupIds.toString());
            populateRecyclerView();

        } else {
            UserEntry.GetUserEntry getUserEntry = new UserEntry.GetUserEntry(userId, 5000) {
                @Override
                public void onPostExecute() {
                    userEntry = getResult();

                    for (Object o: userEntry.getGroups()) {
                        int full = getResult().getGroups().size();
                        GroupEntry.GetGroupEntry getGroupEntry = new GroupEntry.GetGroupEntry((String) o, 5000) {
                            @Override
                            public void onPostExecute() {
                                if (isSuccessful()) {
                                    groupEntries.add(getResult());
                                    groupIds.add(getGroupId());
                                    if (groupEntries.size() == full) {
                                        populateRecyclerView();
                                    }
                                }
                            }
                        };
                        getGroupEntry.start();

                    }
                }
            };
            getUserEntry.start();
        }

        return rootView;
    }

    public void populateRecyclerView() {
        grpNames.clear();
        grpImages.clear();
        grpIds.clear();

        //Append addGrpBtnImg and addGrpBtnText to beginning of each ArrayList
        grpNames.add(addGrpBtnText);
        grpImages.add(addGrpBtnImg);
        grpIds.add(null);

        for (int i = 0; i < groupIds.size(); i++) {
            grpIds.add(groupIds.get(i));
            grpNames.add(groupEntries.get(i).getName());
            grpImages.add(groupEntries.get(i).getDisplay_picture());
        }
        if (grpIds.size() > 0) {
            imageView.setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    //Load Group Information from FireStore before going to next Fragment
    public void getGroupMemberInfo(String groupID, String groupImg, String groupName) {

        //Create ArrayList to store grpMemberIDs, HashMaps to store grpMemberNames and grpMemberImgs
        ArrayList<String> grpMemberIDs = new ArrayList<>();
        HashMap<String, String> grpMemberNames = new HashMap<>();
        HashMap<String, String> grpMemberImgs = new HashMap<>();
        Bundle bundle = new Bundle();

        //Get grpMemberIds, grpMemberNames, grpMemberImgs from FireStore
        GroupEntry.GetGroupEntry grpGetter = new GroupEntry.GetGroupEntry(groupID, 100000) {
            @Override
            public void onPostExecute() {
                if (isSuccessful()) {
                    for (Object o: getResult().getMember_list()) {
                        Log.d("MEMBER ID", (String)o);
                        int full = getResult().getMember_list().size();
                        grpMemberIDs.add((String)o);
                        UserEntry.GetUserEntry userGetter = new UserEntry.GetUserEntry((String)o, 100000) {
                            @Override
                            public void onPostExecute() {
                                if (isSuccessful()) {
                                    grpMemberImgs.put((String)o, getResult().getDisplay_picture());
                                    grpMemberNames.put((String)o, getResult().getName());

                                    if (grpMemberNames.size() == full) {
                                        //Add everything to bundle
                                        bundle.putStringArrayList("grpMemberIDs", grpMemberIDs);
                                        bundle.putSerializable("grpMemberImgs", grpMemberImgs);
                                        bundle.putSerializable("grpMemberNames", grpMemberNames);
                                        bundle.putString("groupImage", groupImg);
                                        bundle.putString("groupName", groupName);
                                        bundle.putString("groupId", groupID);

                                        //Fade Out Nav Bar
                                        Utils utils = new Utils(getContext());
                                        utils.fadeOut();

                                        //Go to IndivChatFragment
                                        IndivGroupFragment indivGroupFragment = new IndivGroupFragment();
                                        indivGroupFragment.setArguments(bundle);
                                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                                        transaction.add(R.id.fragment_container, indivGroupFragment, "openChat").addToBackStack("groups").commit();
                                    }
                                }
                            }
                        };
                        userGetter.start();
                    }
                }
            }
        };
        grpGetter.start();

    }

    //Class to populate GridView
    class GridAdapter extends BaseAdapter {

        Context context;
        LayoutInflater inflater;
        GridAdapter(Context c) {
            context = c;
            inflater = LayoutInflater.from(c);
        }

        @Override
        public int getCount() {
            return grpNames.size();
        }

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
            ViewHolder viewHolder;

            if (view == null) {
                //If view (View to populate GridView cells) not loaded before,
                //create new ViewHolder to hold view
                viewHolder = new ViewHolder();

                //Inflate the layout for GridView cells (created as a Fragment)
                view = inflater.inflate(R.layout.group_grid_item, null);

                //Get ImageButton and TextView to populate
                viewHolder.gridImg = view.findViewById(R.id.grid_item_img);
                viewHolder.gridTxt = view.findViewById(R.id.grid_item_text);

                //Tag to reference
                view.setTag(viewHolder);

            } else {
                //If view loaded before, get view's tag and cast to ViewHolder
                viewHolder = (ViewHolder)view.getTag();
            }

            //Set variables to allow multiple access of same image and text
            String selectedGrpImg = grpImages.get(i);
            String selectedGrpName = grpNames.get(i);
            String selectedGrpId = grpIds.get(i);

            //setText for TextView
            viewHolder.gridTxt.setText(selectedGrpName);

            //Set onClick
            if (selectedGrpImg.equals(addGrpBtnImg) && selectedGrpName.equals(addGrpBtnText)) {
                //If gridImg is addGrpBtnImg and gridImgText is addGrpBtnText, the Add Group Button is created.

                //Set Add Group image for ShapeableImageView
                Glide.with(context).load(Integer.parseInt(selectedGrpImg)).into(viewHolder.gridImg);

                //Go to CreateGroupFragment
                viewHolder.gridImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Fade Out Nav Bar
                        Utils utils = new Utils(getContext());
                        utils.fadeOut();

                        //Go to CreateGroupFragment
                        CreateGroupFragment fragment = new CreateGroupFragment();
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        transaction.add(R.id.fragment_container, fragment).addToBackStack(null).commit();
                    }
                });
            } else {
                //If gridImg is not addGrpBtnImg and gridImgText is not addGrpBtnText,

                viewHolder.gridImg.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        AddToDashboardDialogFragment dialog = new AddToDashboardDialogFragment(selectedGrpId, userId);
                        assert getFragmentManager() != null;
                        dialog.show(getFragmentManager(), "");
                        return true;
                    }
                });

                //Set image for ShapeableImageView
                if (selectedGrpImg ==  null) {
                    Glide.with(context).load(R.drawable.pink_circle).centerCrop().into(viewHolder.gridImg);
                } else {
                    Glide.with(context).load(selectedGrpImg).centerCrop().into(viewHolder.gridImg);
                }

                //Go to IndivChatFragment
                viewHolder.gridImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Bundle arguments to send to ChatFragment
                        getGroupMemberInfo(selectedGrpId, selectedGrpImg, selectedGrpName);
                    }
                });
            }

            return view;
        }

        //To reduce reloading of same layout
        class ViewHolder {
            ShapeableImageView gridImg;
            TextView gridTxt;
        }
    }
    public static class AddToDashboardDialogFragment extends DialogFragment {

        private static final String TAG = "DIALOG";
        private final String selectedGrpId;
        private final String userID;

        public AddToDashboardDialogFragment(String selectedGrpId, String userID) {
            this.selectedGrpId = selectedGrpId;
            this.userID = userID;
        }

        @NotNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Add to favourites?")
                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Log.d(TAG, "onClick: " + userID);
                            UserEntry.GetUserEntry addToDashboard = new UserEntry.GetUserEntry(userID, 5000) {
                                @Override
                                public void onPostExecute() {
                                    List<Object> groups = new ArrayList<>();
                                    groups.add(selectedGrpId);
                                    UserEntry userEntry = getResult();
                                    List<Object> current = userEntry.getDashboard_grps();
                                    for (int i = 0; i < current.size(); i++) {
                                        if (current.get(i) != null) {
                                            if (current.get(i).equals(selectedGrpId)) {
                                                break;
                                            }
                                        } else {
                                            userEntry.assignDashboardGrp(i, selectedGrpId);
                                            Log.d(TAG, "onPostExecute: " + groups.toString());
                                            UserEntry.SetUserEntry setUserEntry = new UserEntry.SetUserEntry(userEntry, userID, 5000) {
                                                @Override
                                                public void onPostExecute() {
                                                    Log.d(TAG, "onPostExecute: " + "success!");
                                                }
                                            };
                                            setUserEntry.start();
                                            break;
                                        }
                                    }

                                }
                            };
                            addToDashboard.start();
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