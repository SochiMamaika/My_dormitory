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

public class newsAdapter extends RecyclerView.Adapter<newsAdapter.NewsViewHolder> {

    private List<news> newsList; // Оригинальный список
    private List<news> newsListFiltered; // Отфильтрованный список
    private boolean hasNewsWriteRole;

    public newsAdapter(List<news> newsList, boolean hasNewsWriteRole) {
        this.newsList = newsList;
        this.newsListFiltered = new ArrayList<>(newsList); // Инициализируем отфильтрованный список
        this.hasNewsWriteRole = hasNewsWriteRole;
    }

    public interface OnNewsClickListener {
        void onDeleteClick(news newsItem, int position);
    }

    private OnNewsClickListener listener;

    public void setOnNewsClickListener(OnNewsClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.news_item, parent, false);
        return new NewsViewHolder(view, hasNewsWriteRole);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        news news = newsListFiltered.get(position); // Используем отфильтрованный список

        holder.newsHeader.setText(news.getHeader());
        holder.newsBody.setText(news.getBody());
        holder.newsDateStartAndEnd.setText(
                "Начало: " + news.getDateStart() + "\n\nКонец: " + news.getDateEnd()
        );
        holder.newsAuthor.setText(news.getAuthor());
        holder.newsDate.setText(news.getDate());
        holder.filesContainerForNews.removeAllViews();

        if (news.getNewsPath() != null && !news.getNewsPath().isEmpty()) {
            for (String path : news.getNewsPath()) {
                addImageToContainer(holder.filesContainerForNews, path);
            }
        }

        if (hasNewsWriteRole && listener != null) {
            holder.deleteButton.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(newsListFiltered.get(pos), pos); // Используем отфильтрованный список
                }
            });
        } else {
            holder.deleteButton.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return newsListFiltered.size(); // Возвращаем размер отфильтрованного списка
    }

    // Метод для обновления данных
    public void updateData(List<news> newNewsList) {
        this.newsList = newNewsList;
        this.newsListFiltered = new ArrayList<>(newNewsList);
        notifyDataSetChanged();
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

    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<news> filtered = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    // Если строка поиска пустая, показываем все новости
                    filtered.addAll(newsList);
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (news n : newsList) {
                        // Ищем по заголовку, содержанию, автору и дате
                        if (n.getHeader().toLowerCase().contains(pattern) ||
                                n.getBody().toLowerCase().contains(pattern) ||
                                n.getAuthor().toLowerCase().contains(pattern) ||
                                n.getDate().contains(pattern)) {
                            filtered.add(n);
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
                newsListFiltered.clear();
                newsListFiltered.addAll((List<news>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView newsHeader, newsBody, newsDateStartAndEnd, newsAuthor, newsDate;
        LinearLayout filesContainerForNews;
        ImageButton deleteButton;

        public NewsViewHolder(@NonNull View itemView, boolean hasNewsWriteRole) {
            super(itemView);

            newsHeader = itemView.findViewById(R.id.typeRepairman);
            newsBody = itemView.findViewById(R.id.repairmanBody);
            newsDateStartAndEnd = itemView.findViewById(R.id.newsDateStartAndEnd);
            newsAuthor = itemView.findViewById(R.id.repairmanRoom);
            filesContainerForNews = itemView.findViewById(R.id.filesContainerForRepairman);
            newsDate = itemView.findViewById(R.id.newsDate);
            deleteButton = itemView.findViewById(R.id.btnDeleteFromNews);
            deleteButton.setVisibility(hasNewsWriteRole ? View.VISIBLE : View.GONE);
        }
    }
}