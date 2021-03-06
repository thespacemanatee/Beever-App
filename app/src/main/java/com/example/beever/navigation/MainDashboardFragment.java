package com.example.beever.navigation;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.beever.R;
import com.example.beever.database.EventEntry;
import com.example.beever.database.GroupEntry;
import com.example.beever.database.UserEntry;
import com.example.beever.feature.CalendarFragment;
import com.example.beever.feature.DashboardFragment;
import com.example.beever.feature.GroupsFragment;
import com.example.beever.feature.ToDoFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;

import java.util.ArrayList;

public class MainDashboardFragment extends Fragment {

    private static final String GROUP_ENTRIES = "groupEntries";
    private static final String GROUP_IDS = "groupIds";
    private static final String RELEVANT_EVENTS = "relevantEvents";
    private static final String DASH_GROUP_ENTRIES = "dashGroupEntries";
    private static final String DASH_GROUP_IDS = "dashGroupIds";
    private static final String USER_ENTRY = "userEntry";
    private UserEntry userEntry;
    private final ArrayList<GroupEntry> groupEntries = new ArrayList<>();
    private final ArrayList<GroupEntry> dashGroupEntries = new ArrayList<>();
    private final ArrayList<String> groupIds = new ArrayList<>();
    private final ArrayList<String> dashGroupIds = new ArrayList<>();
    private ArrayList<EventEntry> events = new ArrayList<>();
    private final FirebaseAuth fAuth = FirebaseAuth.getInstance();
    private String userId;
    ChipNavigationBar chipNavigationBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.main_dashboard_fragment, container, false);

        ((NavigationDrawer)getActivity()).getSupportActionBar().setTitle("Dashboard");

        chipNavigationBar = root.findViewById(R.id.bottom_menu);
        chipNavigationBar.setItemSelected(R.id.bottom_menu_dashboard,true);

        getFragmentManager().beginTransaction().replace(R.id.fragment_container, new DashboardFragment()).commit();

        userId = fAuth.getCurrentUser().getUid();

        UserEntry.GetUserEntry getUserEntry = new UserEntry.GetUserEntry(userId, 5000) {
            @Override
            public void onPostExecute() {
                Log.d("USER LISTENERS", "getUserEntry: " + "I'M CALLED");

                userEntry = getResult();
                getGroupEntries(userEntry);
                getUserRelevantEvents(userEntry);
                getDashboardGroups(userEntry);
                bottomMenu();
            }
        };
        getUserEntry.start();

        UserEntry.UserEntryListener userEntryListener = new UserEntry.UserEntryListener(userId, 5000) {
            @Override
            public void onPreListening() {

            }

            @Override
            public void onListenerUpdate() {
//                Log.d("USER LISTENERS", "userEntryListener: " + "I'M CALLED");
                userEntry = getResult();
                getGroupEntries(userEntry);
                getUserRelevantEvents(userEntry);
                getDashboardGroups(userEntry);
            }

            @Override
            public void onSetupFailure() {

            }
        };
        userEntryListener.start();

        return root;
    }

    private void getDashboardGroups(UserEntry userEntry) {
        dashGroupEntries.clear();
        dashGroupIds.clear();
        for (Object o: userEntry.getDashboard_grps()) {
            if (o != null) {
                GroupEntry.GetGroupEntry getGroupEntry = new GroupEntry.GetGroupEntry((String) o, 5000) {
                    @Override
                    public void onPostExecute() {
                        dashGroupEntries.add(getResult());
                        dashGroupIds.add(getGroupId());
                    }
                };
                getGroupEntry.start();
            }
        }
    }

    private void getUserRelevantEvents(UserEntry userEntry) {
        events.clear();
        UserEntry.GetUserRelevantEvents getUserRelevantEvents = new UserEntry.GetUserRelevantEvents(userEntry, 5000, true, false) {
            @Override
            public void onPostExecute() {
                events = getResult();
            }
        };
        getUserRelevantEvents.start();
    }

    private void getGroupEntries(UserEntry userEntry) {
        Log.d("GROUP LISTENERS", "getGroupEntries(): " + "I'M CALLED");

        groupEntries.clear();
        groupIds.clear();
        for (Object o: userEntry.getGroups()) {

            int full = userEntry.getGroups().size();
            GroupEntry.GetGroupEntry getGroupEntry = new GroupEntry.GetGroupEntry((String) o, 5000) {
                @Override
                public void onPostExecute() {
                    groupEntries.add(getResult());
                    groupIds.add(getGroupId());
                    if (groupEntries.size() == full) {
                        Log.d("GROUP LISTENERS", "Calling getGroupEntryListener()");
                        getGroupEntryListeners();
                    }
                }
            };
            getGroupEntry.start();

        }
    }

    private void getGroupEntryListeners() {

        Log.d("GROUP LISTENERS", "getGroupEntryListeners(): " + "I'M CALLED");
        for (String groupId: groupIds) {

            Log.d("GROUP LISTENERS", "startListening: " + groupId);
            GroupEntry.GroupEntryListener groupEntryListener = new GroupEntry.GroupEntryListener(groupId, 5000) {
                @Override
                public void onPreListening() {

                }

                @Override
                public void onListenerUpdate() {
                    groupEntries.clear();
                    groupIds.clear();
                    getUserRelevantEvents(userEntry);
                    for (Object o: userEntry.getGroups()) {

                        GroupEntry.GetGroupEntry getGroupEntry = new GroupEntry.GetGroupEntry((String) o, 5000) {
                            @Override
                            public void onPostExecute() {
                                Log.d("GROUP LISTENERS", "getGroupEntryListeners() onPostExecute: " + "I'M CALLED");
                                groupEntries.add(getResult());
                                groupIds.add(getGroupId());
                            }
                        };
                        getGroupEntry.start();

                    }
                }

                @Override
                public void onSetupFailure() {

                }
            };
            groupEntryListener.start();

        }
    }

    private void bottomMenu() {

        chipNavigationBar.setOnItemSelectedListener(i -> {
            Fragment fragment = null;
            Bundle bundle = new Bundle();
            bundle.putParcelable(USER_ENTRY, userEntry);
            bundle.putParcelableArrayList(GROUP_ENTRIES, groupEntries);
            bundle.putStringArrayList(GROUP_IDS, groupIds);
            bundle.putParcelableArrayList(RELEVANT_EVENTS, events);
            bundle.putParcelableArrayList(DASH_GROUP_ENTRIES, dashGroupEntries);
            bundle.putStringArrayList(DASH_GROUP_IDS, dashGroupIds);
            Log.d("GROUP LISTENERS", "bottomMenu(): " + groupIds);

            switch (i) {

                case R.id.bottom_menu_dashboard:
                    fragment = new DashboardFragment();
                    fragment.setArguments(bundle);
                    break;

                case R.id.bottom_menu_calendar:
                    fragment = new CalendarFragment();
                    fragment.setArguments(bundle);
                    break;

                case R.id.bottom_menu_groups:
                    fragment = new GroupsFragment();
                    fragment.setArguments(bundle);
                    break;

                case R.id.bottom_menu_todo:
                    fragment = new ToDoFragment();
                    fragment.setArguments(bundle);
                    break;
            }

            getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();

        });
    }
}
