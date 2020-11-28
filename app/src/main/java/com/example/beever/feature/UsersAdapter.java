package com.example.beever.feature;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beever.R;
import com.example.beever.admin.UserHelperClass;

import java.util.ArrayList;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    private ArrayList<UserHelperClass> users;

    public static class ViewHolder extends RecyclerView.ViewHolder{
        private TextView textViewName;
        private TextView textViewEmail;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.added_name);
            textViewEmail = itemView.findViewById(R.id.added_email);
        }

        public TextView getTextViewName() {
            return textViewName;
        }

        public TextView getTextViewEmail() {
            return textViewEmail;
        }
    }

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet String[] containing the data to populate views to be used
     * by RecyclerView.
     */
    public UsersAdapter(ArrayList<UserHelperClass> array) {
        this.users = array;
        for (UserHelperClass user: array) {
            Log.i("HELLLLO MY NAMES NINO", user.getName());
            Log.i("HELLLLO MY EMAILS NINO", user.getEmail());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_users, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getTextViewName().setText(users.get(position).getName());
        holder.getTextViewEmail().setText(users.get(position).getEmail());
    }

    @Override
    public int getItemCount() {
        return users.size();
    }
}