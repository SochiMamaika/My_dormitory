package com.example.mydormitory;

import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class avitostanAdapter extends RecyclerView.Adapter<avitostanAdapter.AvitostanViewHolder> {

    private List<avitostan> avitostanList;
    private boolean hasAvitostanWriteRole;

    public avitostanAdapter(List<avitostan> avitostanList, boolean hasAvitostanWriteRole) {
        this.avitostanList = avitostanList;
        this.hasAvitostanWriteRole = hasAvitostanWriteRole;
    }

    public interface OnAvitostanClickListener {
        void onDeleteClick(avitostan avitostanItem, int position);
    }

    private OnAvitostanClickListener listener;

    public void setOnAvitostanClickListener(OnAvitostanClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvitostanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.avitostan_item, parent, false);
        return new AvitostanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvitostanViewHolder holder, int position) {
        avitostan avitostan = avitostanList.get(position);

        holder.avitostanType.setText(avitostan.getType());
        holder.avitostanBody.setText(avitostan.getBody());
        holder.avitostanRoom.setText("Комната: " + avitostan.getRoom());
        holder.avitoDate.setText(avitostan.getDate());
        holder.filesContainerForAvitostan.removeAllViews();

        if (avitostan.getAvitoPath() != null && !avitostan.getAvitoPath().isEmpty()) {
            for (String path : avitostan.getAvitoPath()) {
                addImageToContainer(holder.filesContainerForAvitostan, path);
            }
        }

        if (hasAvitostanWriteRole) {
            holder.btnDeleteFromAvitostan.setVisibility(View.VISIBLE);
            holder.btnDeleteFromAvitostan.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(avitostan, pos);
                    }
                }
            });
        }
        else
        {
            holder.btnDeleteFromAvitostan.setVisibility(View.GONE);
        }
    }

    private void addImageToContainer(LinearLayout container, String imagePath) {
        ImageView imageView = new ImageView(container.getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300, 300);
        params.setMargins(0, 0, 16, 15);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundColor(0xFFEEEEEE);

        loadImage(imageView, imagePath);

        imageView.setOnClickListener(v -> {
            Intent intent = new Intent(container.getContext(), fullScreenImageActivity.class);
            intent.putExtra("image_path", imagePath);
            container.getContext().startActivity(intent);
        });
        container.addView(imageView);
    }

    private void loadImage(ImageView imageView, String imagePath) {
        new Thread(() -> {
            try {
                Bitmap bitmap = utils.downloadImageFromServer(imagePath);
                if (bitmap != null) {
                    imageView.post(() -> imageView.setImageBitmap(bitmap));
                }
            } catch (Exception e) {
                Log.e("IMAGE_DEBUG", "Load failed: " + imagePath);
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return avitostanList.size();
    }

    public static class AvitostanViewHolder extends RecyclerView.ViewHolder {
        TextView avitostanType, avitostanBody, avitoDate, avitostanRoom;
        LinearLayout filesContainerForAvitostan;
        ImageButton btnDeleteFromAvitostan;

        public AvitostanViewHolder(@NonNull View itemView) {
            super(itemView);
            avitostanType = itemView.findViewById(R.id.avitostanType);
            avitostanBody = itemView.findViewById(R.id.avitostanBody);
            avitoDate = itemView.findViewById(R.id.avitoDate);
            avitostanRoom = itemView.findViewById(R.id.avitostanRoom);
            filesContainerForAvitostan = itemView.findViewById(R.id.filesContainerForAvitostan);
            btnDeleteFromAvitostan = itemView.findViewById(R.id.btnDeleteFromAvitostan);
        }
    }
}