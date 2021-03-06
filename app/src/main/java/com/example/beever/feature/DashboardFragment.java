package com.example.beever.feature;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beever.R;
import com.example.beever.database.EventEntry;
import com.example.beever.database.GroupEntry;
import com.example.beever.database.UserEntry;
import com.example.beever.navigation.NavigationDrawer;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private final FirebaseAuth fAuth = FirebaseAuth.getInstance();
    private final String userID = fAuth.getUid();
    private final int currentTime = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    private SharedPreferences mSharedPref;
    private final ArrayList<String> dbGrpImgs = new ArrayList<>();
    private final ArrayList<String> dbGrpNames = new ArrayList<>();
    private final ArrayList<String> dbGrpIds = new ArrayList<>();
    private final ArrayList<EventEntry> dbEvents = new ArrayList<>();

    private static final String GROUP_ENTRIES = "groupEntries";
    private static final String GROUP_IDS = "groupIds";
    private static final String USER_ENTRY = "userEntry";
    private static final String RELEVANT_EVENTS = "relevantEvents";
    private static final String DASH_GROUP_ENTRIES = "dashGroupEntries";
    private static final String DASH_GROUP_IDS = "dashGroupIds";

    private UserEntry userEntry;
    private ArrayList<GroupEntry> groupEntries = new ArrayList<>();
    private ArrayList<GroupEntry> dashGroupEntries = new ArrayList<>();
    private ArrayList<String> groupIds = new ArrayList<>();
    private ArrayList<String> dashGroupIds = new ArrayList<>();
    private ArrayList<EventEntry> events = new ArrayList<>();

    TextView greeting, name, noUpcomingText, noFavouriteText;
    DashboardGroupsAdapter grpAdapter;
    DashboardEventsAdapter eventsAdapter;
    ImageView noUpcomingImage, noFavouriteImage;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //Inflate layout for this fragment
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.dashboard_fragment, container, false);

        ((NavigationDrawer)getActivity()).getSupportActionBar().setTitle("Dashboard");
        mSharedPref = this.getActivity().getSharedPreferences("SharedPref", Context.MODE_PRIVATE);
        greeting = root.findViewById(R.id.greeting);
        name = root.findViewById(R.id.name);
        noUpcomingImage = root.findViewById(R.id.no_event_image);
        noUpcomingText = root.findViewById(R.id.no_event_text);
        noFavouriteImage = root.findViewById(R.id.no_fav_groups_image);
        noFavouriteText = root.findViewById(R.id.no_fav_groups_text);

        //Populate RecyclerView in dashboard_fragment.xml with 3 Upcoming Events
        RecyclerView eventLayout = root.findViewById(R.id.dashboard_events);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        eventLayout.setLayoutManager(layoutManager);
        eventsAdapter = new DashboardEventsAdapter(getContext());
        eventLayout.setAdapter(eventsAdapter);

        //Populate GridView in dashboard_fragment.xml with Groups
        GridView grpLayout = root.findViewById(R.id.dashboard_groups);
        grpAdapter = new DashboardGroupsAdapter(getActivity());
        grpLayout.setAdapter(grpAdapter);

        Bundle bundle = this.getArguments();

        if (bundle != null) {
            userEntry = bundle.getParcelable(USER_ENTRY);
            groupEntries = bundle.getParcelableArrayList(GROUP_ENTRIES);
            groupIds = bundle.getStringArrayList(GROUP_IDS);
            events = bundle.getParcelableArrayList(RELEVANT_EVENTS);
            dashGroupEntries = bundle.getParcelableArrayList(DASH_GROUP_ENTRIES);
            dashGroupIds = bundle.getStringArrayList(DASH_GROUP_IDS);
            Log.d("WTF", "onCreateView: " + dashGroupEntries.toString());

            populateRecyclerView();

        } else {
            UserEntry.GetUserEntry getUserEntry = new UserEntry.GetUserEntry(userID, 5000) {
                @Override
                public void onPostExecute() {
                    userEntry = getResult();

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

                    UserEntry.GetUserRelevantEvents getUserRelevantEvents = new UserEntry.GetUserRelevantEvents(userEntry, 5000, true, false) {
                        @Override
                        public void onPostExecute() {
                            events = getResult();
                            populateRecyclerView();
                        }
                    };
                    getUserRelevantEvents.start();
                }
            };
            getUserEntry.start();
        }

        //Save User's Name
        name.setText(mSharedPref.getString("registeredName", "Beever") + ".");

        //Greeting
        if (currentTime < 12) {
            greeting.setText(R.string.greetings_morning);
        } else if (currentTime < 18) {
            greeting.setText(R.string.greetings_afternoon);
        } else {
            greeting.setText(R.string.greetings_evening);
        }

        return root;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void populateRecyclerView() {
        dbGrpIds.clear();
        dbGrpImgs.clear();
        dbGrpNames.clear();
        dbEvents.clear();

        events.sort(new DashboardEventComparator());
        ArrayList<EventEntry> upcomingEvents = new ArrayList<>();

        for (int i = 0; i < events.size(); i++) {
            if (!(events.get(i).getStart_time().toDate().getTime() < new Date().getTime())) {
                upcomingEvents.add(events.get(i));
            }
        }

        for (int i = 0; i < 3; i++) {
            if (i < upcomingEvents.size()) {
                dbEvents.add(upcomingEvents.get(i));
            }
        }

        if (dbEvents.size() > 0) {
            noUpcomingImage.setVisibility(View.GONE);
            noUpcomingText.setVisibility(View.GONE);
        }
        eventsAdapter.notifyDataSetChanged();
        Log.d("EVENTS", dbEvents.toString());

        for (int i = 0; i < dashGroupIds.size(); i++) {
            Log.d("GROUP ENTRY", "success");
            if (dashGroupEntries.get(i) != null) {
                dbGrpIds.add(dashGroupIds.get(i));
                dbGrpNames.add(dashGroupEntries.get(i).getName());
                noFavouriteImage.setVisibility(View.GONE);
                noFavouriteText.setVisibility(View.GONE);
                if (dashGroupEntries.get(i).getDisplay_picture() == null) {
                    dbGrpImgs.add("null");
                } else {
                    dbGrpImgs.add(dashGroupEntries.get(i).getDisplay_picture());
                }
            }
        }
        grpAdapter.notifyDataSetChanged();
    }

    public void getGroupMemberInfo(String groupID, String groupImg, String groupName, Bundle bundle) {

        //Create ArrayList to store grpMemberIDs, HashMaps to store grpMemberNames and grpMemberImgs
        ArrayList<String> grpMemberIDs = new ArrayList<>();
        HashMap<String, String> grpMemberNames = new HashMap<>();
        HashMap<String, String> grpMemberImgs = new HashMap<>();

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
                                        transaction.add(R.id.fragment_container, indivGroupFragment, "openChat")
                                                .addToBackStack("dashboard")
                                                .commit();
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


    class DashboardGroupsAdapter extends BaseAdapter {

        Context context;
        LayoutInflater inflater;
        DashboardGroupsAdapter(Context c) {
            context = c;
            inflater = LayoutInflater.from(c);
        }

        @Override
        public int getCount() {
            return dbGrpImgs.size();
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
            DashboardGroupViewHolder viewHolder;

            if (view == null) {
                //If view (View to populate GridView cells) not loaded before,
                //create new ViewHolder to hold view
                viewHolder = new DashboardGroupViewHolder();

                //Inflate the layout for GridView cells (created as a Fragment)
                view = inflater.inflate(R.layout.group_grid_item, null);

                //Get ImageButton and TextView to populate
                viewHolder.gridImg = view.findViewById(R.id.grid_item_img);
                viewHolder.gridTxt = view.findViewById(R.id.grid_item_text);

                //Tag to reference
                view.setTag(viewHolder);

            } else {
                //If view loaded before, get view's tag and cast to ViewHolder
                viewHolder = (DashboardGroupViewHolder)view.getTag();
            }

            //Set variables to allow multiple access of same image and text
            String selectedGrpImg = dbGrpImgs.get(i);
            String selectedGrpName = dbGrpNames.get(i);
            String selectedGrpId = dbGrpIds.get(i);

            //setImageResource for ImageButton and setText for TextView
            if (selectedGrpImg.equals("null")) {
                Glide.with(context).load(R.drawable.pink_circle).centerCrop().into(viewHolder.gridImg);
            } else {
                Glide.with(context).load(selectedGrpImg).centerCrop().into(viewHolder.gridImg);
            }
            viewHolder.gridTxt.setText(selectedGrpName);

            //Set onClick
            viewHolder.gridImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Bundle arguments to send to ChatFragment
                    Bundle bundle = new Bundle();
                    getGroupMemberInfo(selectedGrpId, selectedGrpImg, selectedGrpName, bundle);
                }
            });
            return view;
        }

        //To reduce reloading of same layout
        class DashboardGroupViewHolder {
            ShapeableImageView gridImg;
            TextView gridTxt;
        }
    }

    class DashboardEventsAdapter extends RecyclerView.Adapter<DashboardEventsAdapter.ViewHolder> {

        int moreThanDay;
        SimpleDateFormat sfDate = new SimpleDateFormat("dd MMM", Locale.getDefault());
        SimpleDateFormat sfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

        Context context;
        LayoutInflater inflater;
        DashboardEventsAdapter(Context c) {
            context = c;
            inflater = LayoutInflater.from(c);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            TextView eventName;
            LinearLayout eventTiming;
            LinearLayout eventTiming_More;
            TextView startDate;
            TextView startTime;
            TextView endDate;
            TextView endTime;

            public ViewHolder(View view) {
                super(view);

                eventName = view.findViewById(R.id.event_title);
                eventTiming = view.findViewById(R.id.event);
                eventTiming_More = view.findViewById(R.id.event_more);
                if (moreThanDay == 1)  {
                    startDate = view.findViewById(R.id.start_date_more);
                    startTime = view.findViewById(R.id.start_time_more);
                    endDate = view.findViewById(R.id.end_date_more);
                    endTime = view.findViewById(R.id.end_time_more);
                } else {
                    startDate = view.findViewById(R.id.start_date);
                    startTime = view.findViewById(R.id.start_time);
                    endTime = view.findViewById(R.id.end_time);
                }
            }

            public TextView getEventName() {
                return eventName;
            }

            public LinearLayout getEventTiming() {
                return eventTiming;
            }

            public LinearLayout getEventTiming_More() {
                return eventTiming_More;
            }

            public TextView getStartDate() {
                return startDate;
            }

            public TextView getEndDate() {
                return endDate;
            }

            public TextView getStartTime() {
                return startTime;
            }

            public TextView getEndTime() {
                return endTime;
            }
        }

        @Override
        public int getItemViewType(int i) {
            String start = sfDate.format(dbEvents.get(i).getStart_time().toDate());
            String end = sfDate.format(dbEvents.get(i).getEnd_time().toDate());
            if (start.equals(end)) {
                moreThanDay = 0;
            } else {
                moreThanDay = 1;
            }
            return moreThanDay;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
            View view = inflater.inflate(R.layout.calendar_card, viewGroup, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            String eventName = dbEvents.get(i).getName();
            String startTime = sfTime.format(dbEvents.get(i).getStart_time().toDate());
            String startDate = sfDate.format(dbEvents.get(i).getStart_time().toDate());
            String endTime = sfTime.format(dbEvents.get(i).getEnd_time().toDate());
            String endDate = sfDate.format(dbEvents.get(i).getEnd_time().toDate());

            Log.d("CHECK ADAPTER", "info includes - " +eventName+", " +startTime+", " +startDate+", " +endTime+", " +endDate);

            if (moreThanDay == 1) {
                viewHolder.getEndDate().setText(endDate);
                viewHolder.getEventTiming().setVisibility(View.GONE);
                viewHolder.getEventTiming_More().setVisibility(View.VISIBLE);
            } else {
                viewHolder.getEventTiming().setVisibility(View.VISIBLE);
                viewHolder.getEventTiming_More().setVisibility(View.GONE);
            }
            viewHolder.getEventName().setText(eventName);
            viewHolder.getStartDate().setText(startDate);
            viewHolder.getStartTime().setText(startTime);
            viewHolder.getEndTime().setText(endTime);

        }

        @Override
        public int getItemCount() {
            return dbEvents.size();
        }

    }
}