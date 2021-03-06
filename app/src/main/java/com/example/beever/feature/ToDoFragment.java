package com.example.beever.feature;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beever.R;
import com.example.beever.database.GroupEntry;
import com.example.beever.database.TodoEntry;
import com.example.beever.database.UserEntry;
import com.example.beever.navigation.NavigationDrawer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ToDoFragment extends Fragment implements AdapterView.OnItemSelectedListener{

    public static final String TAG = "ToDoFragment";
    public static final String SPINNER = "Spinner Set-Up Successfully";
    public static final String RECYCLERVIEW = "RecyclerView Set-Up Successfully";
    public static final String FAB = "FAB Set-Up Successfully";
    public static final String ADD_TO_DO = "ADD_TO_DO";

    private static final String GROUP_ENTRIES = "groupEntries";
    private static final String GROUP_IDS = "groupIds";
    private static final String USER_ENTRY = "userEntry";
    private UserEntry userEntry;
    private ArrayList<GroupEntry> groupEntries = new ArrayList<>();
    private ArrayList<String> groupIds = new ArrayList<>();

    // making main components class variables
    protected RecyclerView toDoRecyclerView;
    protected RecyclerView.LayoutManager layoutManager;
    protected ToDoAdapter toDoAdapter;
    protected Spinner toDoSpinner;
    protected ArrayAdapter<String> arrayAdapter;
    protected FloatingActionButton toDoAddButton;
    protected ExpandableListView toDoArchivedListView;
    protected ExpandableListAdapter toDoArchivedAdapter;

    // for connecting with firestore
    private final FirebaseAuth fAuth = FirebaseAuth.getInstance();
    private String userID;
    private String groupID;
    private ToDoHelper helper;

    // to store data retrieved locally for display
    protected List<String> ARCHIVED = new ArrayList<>();
    protected List<TodoEntry> archivedList = new ArrayList<>();
    protected HashMap<String, List<TodoEntry>> expandableListDetail = new HashMap<>();
    protected ArrayList<TodoEntry> toDoList = new ArrayList<>();
    protected List<String> projectList = new ArrayList<>();
    protected List<String> groupsList = new ArrayList<>();
    protected Map<String, Object> map = new HashMap<>();
    protected int scrollPosition = 0;

    ImageView noTodoImage;
    TextView noTodoText;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstanceState) {

        Objects.requireNonNull(((NavigationDrawer) requireActivity()).getSupportActionBar()).setTitle("To-Do");

        // getting userid from firestore
        FirebaseUser fUser = fAuth.getCurrentUser();
        userID = fUser.getUid();

        View rootView = layoutInflater.inflate(R.layout.fragment_to_do, viewGroup, false);
        rootView.setTag(TAG);

        noTodoImage = rootView.findViewById(R.id.no_todo_image);
        noTodoText = rootView.findViewById(R.id.no_todo_text);

        //Fade in Nav Bar
        View bottom_menu = getActivity().findViewById(R.id.bottom_menu);
        if (bottom_menu.getVisibility() == View.GONE) {
            Utils utils = new Utils(getContext());
            utils.fadeIn();
        }

        /*
         * SPINNER COMPONENT: Showcases what groups/ projects the user is in
         * Allows the user to select a group/ project and display the to-do list for that group
         */
        // setting Spinner to select Project
        toDoSpinner = rootView.findViewById(R.id.toDoSpinner);
        toDoSpinner.setOnItemSelectedListener(this);
        // ArrayAdapter for Spinner, pass in projectList
        arrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_item, projectList);
        arrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        toDoSpinner.setAdapter(arrayAdapter);
        Log.d(TAG, SPINNER);

        /*
         * RECYCLER VIEW COMPONENT: Showcases the to-do list
         */
        // set Linear Layout for RecyclerView To Do List
        toDoRecyclerView = rootView.findViewById(R.id.toDoRecyclerView);
        layoutManager = new LinearLayoutManager(getActivity());
        if (toDoRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) toDoRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        }
        toDoRecyclerView.setLayoutManager(layoutManager);
        toDoRecyclerView.setItemAnimator(new DefaultItemAnimator());
        toDoRecyclerView.scrollToPosition(scrollPosition);

        /*
         * EXPANDABLE VIEW COMPONENT: Showcases the completed to-dos when clicked
         */
        // set Archived to dos for Expandable View
        toDoArchivedListView = rootView.findViewById(R.id.toDoArchivedListView);
        ARCHIVED = new ArrayList<>();
        ARCHIVED.add("Completed");

        Bundle bundle = this.getArguments();

        if (bundle != null) {
            userEntry = bundle.getParcelable(USER_ENTRY);
            groupEntries = bundle.getParcelableArrayList(GROUP_ENTRIES);
            groupIds = bundle.getStringArrayList(GROUP_IDS);
            populateProjectList();
            if (!projectList.isEmpty()) {
                groupID = projectList.get(0);
            }

        } else {
            UserEntry.GetUserEntry getUserEntry = new UserEntry.GetUserEntry(userID, 5000) {
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
                                        populateProjectList();
                                        if (!projectList.isEmpty()) {
                                            groupID = projectList.get(0);
                                        }
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

        // set on group expand listener for to do archived
        toDoArchivedListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                GroupEntry.GetGroupEntry groupEntry = new GroupEntry.GetGroupEntry(groupID, 5000) {
                    @Override
                    public void onPostExecute() {
                        archivedList = getResult().retrieveGroupTodos(false, true);
                        toDoArchivedAdapter.notifyDataSetChanged();
                    }
                };

                groupEntry.start();
            }
        });

        // set on child click listener for expandable list
        toDoArchivedListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                try {
                    helper.showDeleteAlertDialog(getContext(), (TodoEntry) toDoArchivedAdapter.getChild(groupPosition, childPosition), false, false);
                    return true;
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Can't Click", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        // set To Do Form for FloatingActionButton
        toDoAddButton = rootView.findViewById(R.id.toDoAddButton);
        toDoAddButton.setOnClickListener(v -> {
            if (groupID != null) {
                ToDoDialogFragment toDoDialogFragment = new ToDoDialogFragment(groupID, helper, toDoRecyclerView);
                assert getFragmentManager() != null;
                toDoDialogFragment.show(getFragmentManager(), ADD_TO_DO);
            } else {
                Toast.makeText(getContext(), "No Group Available", Toast.LENGTH_LONG).show();
            }
        });
        Log.d(TAG, FAB);

        return rootView;
    }


    /**
     * populateProjectList : retrieves the projects the user is in from firestore
     * using the userID
     */
    private void populateProjectList() {

        groupsList.clear();
        projectList.clear();

        List<Object> groups = userEntry.getGroups();
        for (Object group : groups) {
            try {
                Log.d("PROJECT LIST", (String) group);
                // for projectList: add the group name only (without the userID in front)
                projectList.add(((String) group).substring(28));
                // for groupsList: add the full groupID
                groupsList.add((String) group);
            } catch (IndexOutOfBoundsException e) {
                projectList.add((String) group);
                groupsList.add((String) group);
            }
        }
        // update the data displayed in spinner component
        arrayAdapter.notifyDataSetChanged();
    }


    /**
     * onItemSelected : Listens to spinner and detects when an item is selected
     * On selected item, populate toDoList and archivedList accordingly
     *
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // retrieve chosen groupID from groupsList
        groupID = groupsList.get(position);
        Log.d("GROUP ID", groupID);

        int groupIdx = groupIds.indexOf(groupID);

        try {
            // get current group todos from GroupEntry and sort according to time
            toDoList = groupEntries.get(groupIdx).retrieveGroupTodos(true, false);
            Log.d("TODO LIST", String.valueOf(toDoList));
            toDoList.sort(new ToDoComparator());

            // get past group todos from GroupEntry and sort according to time as well
            archivedList = groupEntries.get(groupIdx).retrieveGroupTodos(false, true);
            Log.d("ARCHIVED LIST", String.valueOf(archivedList));
            archivedList.sort(new ToDoComparator());

            // set option into hashmap for expandable list and set toDoArchivedAdapter
            expandableListDetail.put("Completed", archivedList);
            toDoArchivedAdapter = new ExpandableListAdapter(getContext(), ARCHIVED, expandableListDetail);
            toDoArchivedListView.setAdapter(toDoArchivedAdapter);

            // set toDoAdapter for RecyclerView in onPostExecute so that groupID is not null
            toDoAdapter = new ToDoAdapter(toDoList, getContext(), getFragmentManager(), expandableListDetail, toDoArchivedAdapter,
                    groupID, noTodoImage, noTodoText);
            toDoRecyclerView.setAdapter(toDoAdapter);
            Log.d(TAG, RECYCLERVIEW);

            // instantiate a helper class here to help with adding/ removing todos later
            helper = new ToDoHelper(getContext(), getFragmentManager(), toDoList, toDoAdapter, expandableListDetail, toDoArchivedAdapter,
                    groupID, noTodoImage, noTodoText);

            // if there are no todos, display a visual cue to user
            if (toDoList.size() > 0) {
                noTodoImage.setVisibility(View.GONE);
                noTodoText.setVisibility(View.GONE);
            }

            if (toDoList.size() == 0) {
                noTodoImage.setVisibility(View.VISIBLE);
                noTodoText.setVisibility(View.VISIBLE);
            }

            // notify changes to the adapters for them to refresh the view
            toDoAdapter.notifyDataSetChanged();
            toDoArchivedAdapter.notifyDataSetChanged();

        } catch (NullPointerException e) { // if toDoList and archivedList is null

            if (toDoList == null) {

                Toast.makeText(parent.getContext(), "To Do List Not Found", Toast.LENGTH_LONG).show();
                toDoList = new ArrayList<>();
                toDoAdapter = new ToDoAdapter(toDoList, getContext(), getFragmentManager(), expandableListDetail, toDoArchivedAdapter,
                        groupID, noTodoImage, noTodoText);
                helper = new ToDoHelper(getContext(), getFragmentManager(), toDoList, toDoAdapter, expandableListDetail, toDoArchivedAdapter,
                        groupID, noTodoImage, noTodoText);
                toDoRecyclerView.setAdapter(toDoAdapter);
                toDoAdapter.notifyDataSetChanged();

            } else if (archivedList == null) {

                Toast.makeText(parent.getContext(), "Completed List Not Found", Toast.LENGTH_LONG).show();
                expandableListDetail = new HashMap<>();
                helper = new ToDoHelper(getContext(), getFragmentManager(), toDoList, toDoAdapter, expandableListDetail, toDoArchivedAdapter,
                        groupID, noTodoImage, noTodoText);
                toDoArchivedAdapter = new ExpandableListAdapter(getContext(), ARCHIVED, expandableListDetail);
                toDoArchivedListView.setAdapter(toDoArchivedAdapter);
                toDoArchivedAdapter.notifyDataSetChanged();
            }

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * ANDROID FRAGMENT LIFECYCLE TEST FOR DEBUG
     */
    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "ON START");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "ON PAUSE");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "ON STOP");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "ON DESTROYVIEW");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ON DESTROY");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "ON DETACH");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "ON RESUME");
    }
}