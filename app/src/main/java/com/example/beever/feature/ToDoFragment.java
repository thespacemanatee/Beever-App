package com.example.beever.feature;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beever.R;
import com.example.beever.database.GroupEntry;
import com.example.beever.database.TodoEntry;
import com.example.beever.database.UserEntry;
import com.example.beever.navigation.NavigationDrawer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.Date;
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

    protected RecyclerView toDoRecyclerView;
    protected RecyclerView.LayoutManager layoutManager;
    protected ToDoAdapter toDoAdapter;
    protected Spinner toDoSpinner;
    protected ArrayAdapter<String> arrayAdapter;
    protected FloatingActionButton toDoAddButton;
    protected ExpandableListView toDoArchivedListView;
    protected ExpandableListAdapter toDoArchivedAdapter;
    private View rootView;

    private final FirebaseAuth fAuth = FirebaseAuth.getInstance();
    private String userID;
    private String groupID;

    protected List<String> ARCHIVED = new ArrayList<>();
    protected List<TodoEntry> archivedList = new ArrayList<>();
    protected HashMap<String, List<TodoEntry>> expandableListDetail = new HashMap<>();
    protected ArrayList<TodoEntry> toDoList = new ArrayList<>();
    protected List<String> projectList = new ArrayList<>();
    protected List<String> groupsList = new ArrayList<>();
    protected Map<String, Object> map = new HashMap<>();
    protected int scrollPosition = 0;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstanceState) {

        Objects.requireNonNull(((NavigationDrawer) Objects.requireNonNull(getActivity())).getSupportActionBar()).setTitle("To-Do");

        FirebaseUser fUser = fAuth.getCurrentUser();
        userID = fUser.getUid();

        rootView = layoutInflater.inflate(R.layout.fragment_to_do, viewGroup, false);
        rootView.setTag(TAG);

        // setting Spinner to select Project
        toDoSpinner = rootView.findViewById(R.id.toDoSpinner);
        toDoSpinner.setOnItemSelectedListener(this);
        // ArrayAdapter for Spinner
        arrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_item, projectList);
        arrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown);
        toDoSpinner.setAdapter(arrayAdapter);
        Log.d(TAG, SPINNER);

        // set Linear Layout for RecyclerView To Do List
        toDoRecyclerView = rootView.findViewById(R.id.toDoRecyclerView);
        layoutManager = new LinearLayoutManager(getActivity());
        if (toDoRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) toDoRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        }
        toDoRecyclerView.setLayoutManager(layoutManager);
        toDoRecyclerView.setItemAnimator(new DefaultItemAnimator());
        toDoRecyclerView.scrollToPosition(scrollPosition);

        // set Archived to dos for Expandable View
        toDoArchivedListView = rootView.findViewById(R.id.toDoArchivedListView);
        ARCHIVED = new ArrayList<>();
        ARCHIVED.add("Archived");

        // set To Do Form for FloatingActionButton
        toDoAddButton = rootView.findViewById(R.id.toDoAddButton);
        toDoAddButton.setOnClickListener(v -> {
            if (groupID != null) {
                ToDoDialogFragment toDoDialogFragment = new ToDoDialogFragment(groupID, R.layout.fragment_to_do_dialog, toDoAdapter);
                assert getFragmentManager() != null;
                toDoDialogFragment.show(getFragmentManager(), ADD_TO_DO);
            } else {
                Toast.makeText(getContext(), "No Group Available", Toast.LENGTH_LONG).show();
            }
        });
        Log.d(TAG, FAB);

        populateProjectList();
        if (!projectList.isEmpty()) {
            groupID = projectList.get(0);
        }

        return rootView;
    }

    private void populateProjectList() {
        UserEntry.GetUserEntry getUserEntry = new UserEntry.GetUserEntry(userID, 5000) {
            @Override
            public void onPostExecute() {
                if (isSuccessful()) {
                    List<Object> groups = getResult().getGroups();
                    for (Object group : groups) {
                        Log.d("PROJECT LIST", (String) group);
                        projectList.add(((String) group).substring(28));
                        groupsList.add((String) group);
                    }
                    arrayAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Groups Not Found", Toast.LENGTH_LONG).show();
                }
            }
        };

        getUserEntry.start();

        /*projectList = new ArrayList<>();
        projectList.add("50.001 1D");
        projectList.add("50.002 1D");
        projectList.add("50.004 2D");*/
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // TODO: get the to do list from firebase according to the project
        groupID = groupsList.get(position);
        Log.d("GROUP ID", groupID);

        toDoAdapter = new ToDoAdapter(toDoList, groupID, getContext());

        GroupEntry.GetGroupEntry getGroupEntry = new GroupEntry.GetGroupEntry(groupID, 5000 ) {
            @Override
            public void onPostExecute() {
                if (isSuccessful()) {
                    try {
                        toDoList = getResult().getGroupTodo(true, false);
                        Log.d("TODO LIST", String.valueOf(toDoList));
                        archivedList = getResult().getGroupTodo(false, true);
                        Log.d("ARCHIVED LIST", String.valueOf(archivedList));

                        // set toDoAdapter for RecyclerView in onPostExecute so that groupID is not null
                        toDoAdapter = new ToDoAdapter(toDoList, groupID, getContext());
                        toDoRecyclerView.setAdapter(toDoAdapter);
                        Log.d(TAG, RECYCLERVIEW);

                        // set expandable list for archived todos
                        expandableListDetail.put("Archived", archivedList);
                        toDoArchivedAdapter = new ExpandableListAdapter(getContext(), ARCHIVED, expandableListDetail);
                        toDoArchivedListView.setAdapter(toDoArchivedAdapter);
                        toDoArchivedListView.setOnGroupExpandListener(groupPosition -> Toast.makeText(getContext(), "Archive Expanded.", Toast.LENGTH_SHORT).show());

                        toDoAdapter.notifyDataSetChanged();
                        toDoArchivedAdapter.notifyDataSetChanged();
                    } catch (NullPointerException e) {
                        Toast.makeText(parent.getContext(), "To Do List Not Found", Toast.LENGTH_LONG).show();
                        int size = toDoList.size();
                        toDoList.clear();
                        toDoAdapter.notifyItemRangeRemoved(0, size);
                    }
                } else {
                    Toast.makeText(parent.getContext(), "Unsuccessful :(", Toast.LENGTH_LONG).show();
                }
            }
        };

        getGroupEntry.start();
        Snackbar.make(view, "Selected: " + parent.getItemAtPosition(position).toString(), Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /* private void initToDoList() {
        toDoList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            toDoList.add(new TodoEntry("Item Number " + i, "Details", "Xing Yi", new Timestamp(new Date()), "50.001 1D"));
        }
    }

    private void initArchivedList() {
        archivedList = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            archivedList.add(new TodoEntry("Archived Number " + i, "Details", "Xing Yi", new Timestamp(new Date()), "50.001 1D"));
        }
    }*/

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