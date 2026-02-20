package com.example.mydormitory;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class documentsAdapter extends RecyclerView.Adapter<documentsAdapter.DocumentsViewHolder> {

    private List<documents> documentsList;

    private List<documents> documentsListFiltered; // Отфильтрованный список
    private Context context;
    private boolean hasDocumentsWriteRole;

    public documentsAdapter(List<documents> documentsList, Context context, boolean hasDocumentsWriteRole) {
        this.documentsList = documentsList;
        this.documentsListFiltered = new ArrayList<>(documentsList);
        this.context = context;
        this.hasDocumentsWriteRole = hasDocumentsWriteRole;
    }

    public interface OnDocumentClickListener {
        void onDeleteClick(documents document, int position);
    }

    private OnDocumentClickListener listener;

    public void setOnDocumentClickListener(OnDocumentClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DocumentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.documents_item, parent, false);
        return new DocumentsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentsViewHolder holder, int position) {
        documents document = documentsListFiltered.get(position);

        holder.documentsBody.setText(document.getBody());
        holder.documentsDate.setText(document.getDate());
        holder.filesContainerForDocuments.removeAllViews();

        if (document.getDocumentsPath() != null) {
            for (String filePath : document.getDocumentsPath()) {
                utils.addFileToContainer(context, holder.filesContainerForDocuments, filePath);
            }
        }

        if (hasDocumentsWriteRole) {
            holder.btnDeleteFromDocument.setVisibility(View.VISIBLE);
            holder.btnDeleteFromDocument.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(document, pos);
                    }
                }
            });
        }
        else {
            holder.btnDeleteFromDocument.setVisibility(View.GONE);
        }
    }

    public void updateData(List<documents> documentsList) {
        this.documentsList = documentsList;
        this.documentsListFiltered = new ArrayList<>(documentsList);
        notifyDataSetChanged();
    }

    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<documents> filtered = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    // Если строка поиска пустая, показываем все новости
                    filtered.addAll(documentsList);
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (documents d : documentsList) {
                        // Ищем по заголовку, содержанию, автору и дате
                        if (d.getBody().toLowerCase().contains(pattern) ||
                                d.getDate().toLowerCase().contains(pattern)) {
                            filtered.add(d);
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
                documentsListFiltered.clear();
                documentsListFiltered.addAll((List<documents>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public int getItemCount() {
        return documentsListFiltered.size();
    }

    public static class DocumentsViewHolder extends RecyclerView.ViewHolder {
        TextView documentsBody, documentsDate;
        LinearLayout filesContainerForDocuments;
        ImageButton btnDeleteFromDocument;

        public DocumentsViewHolder(@NonNull View itemView) {
            super(itemView);
            documentsBody = itemView.findViewById(R.id.documentsBody);
            documentsDate = itemView.findViewById(R.id.documentsDate);
            filesContainerForDocuments = itemView.findViewById(R.id.filesContainerForDocuments);
            btnDeleteFromDocument = itemView.findViewById(R.id.btnDeleteFromDocument);
        }
    }
}