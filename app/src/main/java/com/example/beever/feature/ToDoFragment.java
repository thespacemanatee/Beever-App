package com.example.beever.feature;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beever.R;
import com.example.beever.navigation.NavigationDrawer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ToDoFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    public static final String TAG = "ToDoFragment";
    public static final String SPINNER = "Spinner Set-Up Successfully";
    public static final String RECYCLERVIEW = "RecyclerView Set-Up Successfully";
    public static final String FAB = "FAB Set-Up Successfully";
    public static final String ADD_TO_DO = "ADD_TO_DO";

    protected RecyclerView toDoRecyclerView;
    protected RecyclerView.LayoutManager layoutManager;
    protected ToDoAdapter toDoAdapter;
    protected Spinner toDoSpinner;
    protected FloatingActionButton toDoAddButton;
    protected ExpandableListView toDoArchivedListView;
    protected ExpandableListAdapter toDoArchivedAdapter;

    private FirebaseAuth fAuth = FirebaseAuth.getInstance();
    private FirebaseFirestore fStore = FirebaseFirestore.getInstance();

    protected static List<String> ARCHIVED = new ArrayList<>();
    protected static List<String> archivedList = new ArrayList<>();
    protected static HashMap<String, List<String>> expandableListDetail = new HashMap<>();
    protected static ArrayList<String> toDoList = new ArrayList<>();
    protected static ArrayList<String> projectList = new ArrayList<>();
    protected int scrollPosition = 0;

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle savedInstanceState) {

        FirebaseUser fUser = fAuth.getCurrentUser();

        View rootView = layoutInflater.inflate(R.layout.fragment_to_do, viewGroup, false);
        rootView.setTag(TAG);

        ((NavigationDrawer)getActivity()).getSupportActionBar().setTitle("To-Do");

        // setting Spinner to select Project
        toDoSpinner = rootView.findViewById(R.id.toDoSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.spinner_item, projectList);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown);
        toDoSpinner.setAdapter(adapter);
        toDoSpinner.setOnItemSelectedListener(this);
        Log.d(TAG, SPINNER);

        // set Linear Layout for RecyclerView To Do List
        toDoRecyclerView = rootView.findViewById(R.id.toDoRecyclerView);
        layoutManager = new LinearLayoutManager(getActivity());
        if (toDoRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) toDoRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        }
        toDoRecyclerView.setLayoutManager(layoutManager);
        toDoRecyclerView.scrollToPosition(scrollPosition);

        toDoAdapter = new ToDoAdapter(getActivity());
        // set toDoAdapter for RecyclerView
        toDoRecyclerView.setAdapter(toDoAdapter);
        Log.d(TAG, RECYCLERVIEW);

        // set Archived to dos for Expandable View
        toDoArchivedListView = rootView.findViewById(R.id.toDoArchivedListView);
        toDoArchivedAdapter = new ExpandableListAdapter(this.getContext(), ARCHIVED, expandableListDetail);
        toDoArchivedListView.setAdapter(toDoArchivedAdapter);
        toDoArchivedListView.setOnGroupExpandListener(groupPosition -> Toast.makeText(getContext(), "Archived Expanded.", Toast.LENGTH_SHORT).show());

        // set To Do Form for FloatingActionButton
        toDoAddButton = rootView.findViewById(R.id.toDoAddButton);
        toDoAddButton.setOnClickListener(v -> {
            ToDoDialogFragment toDoDialogFragment = new ToDoDialogFragment();
            toDoDialogFragment.show(getFragmentManager(), ADD_TO_DO);
        });
        Log.d(TAG, FAB);

        return rootView;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // TODO: get the to do list from firebase according to the project
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void addNewToDo() {

    }

}