package com.example.mydormitory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class userAdapter extends RecyclerView.Adapter<userAdapter.UserViewHolder> implements Filterable {

    private List<user> userList;
    private List<user> userListFiltered;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(user user);
    }

    public userAdapter(List<user> userList) {
        this.userList = userList;
        this.userListFiltered = new ArrayList<>(userList);
    }

    public void updateData(List<user> newUsers) {
        userList.clear();
        userList.addAll(newUsers);
        userListFiltered.clear();
        userListFiltered.addAll(newUsers);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        user currentUser = userListFiltered.get(position);

        // Устанавливаем данные
        holder.userName.setText(currentUser.getName());
        holder.userLastName.setText(currentUser.getLastName());
        holder.userSurname.setText(currentUser.getSurname());
        holder.userPhone.setText(currentUser.getPhoneNumber());

        // Клик по элементу
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentUser);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<user> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(userList);
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (user u : userList) {
                        // Ищем по имени, фамилии, отчеству, телефону
                        if (u.getName().toLowerCase().contains(pattern) ||
                                u.getLastName().toLowerCase().contains(pattern) ||
                                u.getSurname().toLowerCase().contains(pattern) ||
                                u.getPhoneNumber().contains(pattern)) {
                            filtered.add(u);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                userListFiltered.clear();
                userListFiltered.addAll((List<user>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userLastName, userSurname, userPhone;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userLastName = itemView.findViewById(R.id.userLastName);
            userSurname = itemView.findViewById(R.id.userSurname);
            userPhone = itemView.findViewById(R.id.userPhone);
        }
    }
}