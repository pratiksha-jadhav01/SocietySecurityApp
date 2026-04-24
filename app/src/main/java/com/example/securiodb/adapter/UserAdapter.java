package com.example.securiodb.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.securiodb.R;
import com.example.securiodb.models.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onDeleteClick(User user);
    }

    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());
        holder.tvRole.setText(user.getRole().toUpperCase());

        if ("owner".equalsIgnoreCase(user.getRole())) {
            holder.tvFlat.setVisibility(View.VISIBLE);
            holder.tvFlat.setText("Flat: " + user.getFlatNumber());
            holder.tvRole.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_approved_text));
            holder.tvRole.setBackgroundResource(R.drawable.badge_approved);
        } else {
            holder.tvFlat.setVisibility(View.GONE);
            holder.tvRole.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
            holder.tvRole.setBackgroundResource(R.drawable.badge_pending);
        }

        // Load profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(user));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateList(List<User> newList) {
        this.userList = newList;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole, tvFlat;
        ImageView btnDelete;
        CircleImageView ivAvatar;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRole = itemView.findViewById(R.id.tvUserRole);
            tvFlat = itemView.findViewById(R.id.tvUserFlat);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }
    }
}
