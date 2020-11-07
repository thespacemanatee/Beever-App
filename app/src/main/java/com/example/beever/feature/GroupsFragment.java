package com.example.beever.feature;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.beever.R;
import com.example.beever.navigation.NavigationDrawer;

public class GroupsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        ((NavigationDrawer)getActivity()).getSupportActionBar().setTitle("Groups");

        return inflater.inflate(R.layout.fragment_groups, container, false);
    }
}