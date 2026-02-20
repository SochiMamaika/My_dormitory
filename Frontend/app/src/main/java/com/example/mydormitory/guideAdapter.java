package com.example.mydormitory;

import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class guideAdapter extends RecyclerView.Adapter<guideAdapter.GuideViewHolder> {

    private List<guide> guideList;

    private List<guide> guideListFiltered;
    private boolean hasGuidesWriteRole;

    public guideAdapter(List<guide> guideList, boolean hasGuidesWriteRole) {
        this.guideList = guideList;
        this.guideListFiltered = new ArrayList<>(guideList);
        this.hasGuidesWriteRole = hasGuidesWriteRole;
    }

    public interface OnGuideClickListener {
        void onDeleteClick(guide guideItem, int position);
    }

    private OnGuideClickListener listener;

    public void setOnGuideClickListener(OnGuideClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public GuideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.guide_item, parent, false);
        return new GuideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GuideViewHolder holder, int position) {
        guide guide = guideListFiltered.get(position);

        holder.guideHeader.setText(guide.getHeader());
        holder.guideBody.setText(guide.getBody());
        holder.guideDate.setText(guide.getDate());
        holder.filesContainerForGuides.removeAllViews();

        if (guide.getTutorPath() != null && !guide.getTutorPath().isEmpty()) {
            for (int i = 0; i < guide.getTutorPath().size(); i++) {
                addImageToContainer(holder.filesContainerForGuides, guide.getTutorPath().get(i));
            }
        }

        if (hasGuidesWriteRole) {
            holder.btnDeleteFromGuide.setVisibility(View.VISIBLE);
            holder.btnDeleteFromGuide.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(guide, pos);
                    }
                }
            });
        }
        else {
            holder.btnDeleteFromGuide.setVisibility(View.GONE);
        }
    }

    public void updateData(List<guide> guideList) {
        this.guideList = guideList;
        this.guideListFiltered = new ArrayList<>(guideList);
        notifyDataSetChanged();
    }

    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<guide> filtered = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    // Если строка поиска пустая, показываем все новости
                    filtered.addAll(guideList);
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (guide g : guideList) {
                        // Ищем по заголовку, содержанию, автору и дате
                        if (g.getHeader().toLowerCase().contains(pattern) ||
                                g.getBody().toLowerCase().contains(pattern) ||
                                g.getDate().toLowerCase().contains(pattern)) {
                            filtered.add(g);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                guideListFiltered.clear();
                guideListFiltered.addAll((List<guide>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public int getItemCount() {
        return guideListFiltered.size();
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

    public static class GuideViewHolder extends RecyclerView.ViewHolder {
        TextView guideHeader, guideBody, guideDate;
        LinearLayout filesContainerForGuides;
        ImageButton btnDeleteFromGuide;

        public GuideViewHolder(@NonNull View itemView) {
            super(itemView);
            guideHeader = itemView.findViewById(R.id.guideHeader);
            guideBody = itemView.findViewById(R.id.guideBody);
            guideDate = itemView.findViewById(R.id.guideDate);
            filesContainerForGuides = itemView.findViewById(R.id.filesContainerForGuides);
            btnDeleteFromGuide = itemView.findViewById(R.id.btnDeleteFromGuide);
        }
    }
}