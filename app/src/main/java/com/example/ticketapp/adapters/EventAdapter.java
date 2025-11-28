package com.example.ticketapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.activities.AddEditEventActivity;
import com.example.ticketapp.models.Event;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EVENT = 1;

    private final Context context;
    private final List<Object> items;
    private final boolean isAdmin;

    private final OnEventClickListener onEventClick;
    private final OnEditClickListener onEditClick;
    private final OnDeleteClickListener onDeleteClick;

    public interface OnEventClickListener { void onEventClick(Event e); }
    public interface OnEditClickListener { void onEditClick(Event e); }
    public interface OnDeleteClickListener { void onDeleteClick(Event e); }

    public EventAdapter(Context context, List<Object> items,
                        OnEventClickListener onEventClick,
                        OnEditClickListener onEditClick,
                        OnDeleteClickListener onDeleteClick,
                        boolean isAdmin) {
        this.context = context;
        this.items = items;
        this.onEventClick = onEventClick;
        this.onEditClick = onEditClick;
        this.onDeleteClick = onDeleteClick;
        this.isAdmin = isAdmin;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_EVENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_event_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_event, parent, false);
            return new EventViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else if (holder instanceof EventViewHolder) {
            ((EventViewHolder) holder).bind((Event) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    // ------------------- ViewHolders -------------------

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tvHeader);
        }

        void bind(String header) {
            tvHeader.setText(header != null ? header : "");
        }
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvEdit, tvDelete;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEventTitle);
            tvDate = itemView.findViewById(R.id.tvEventDate);
            tvEdit = itemView.findViewById(R.id.tvEdit);
            tvDelete = itemView.findViewById(R.id.tvDelete);

            // Click: Open Event
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && onEventClick != null)
                    onEventClick.onEventClick((Event) items.get(pos));
            });

            // Click: Edit Event
            tvEdit.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && onEditClick != null)
                    onEditClick.onEditClick((Event) items.get(pos));
            });

            // Click: Delete Event
            tvDelete.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && onDeleteClick != null)
                    onDeleteClick.onDeleteClick((Event) items.get(pos));
            });
        }

        void bind(Event e) {
            if (e == null) return;

            tvTitle.setText(e.getTitle() != null ? e.getTitle() : "-");
            tvDate.setText(e.getDate() != null ? e.getDate() : "-");

            // Admin buttons
            tvEdit.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            tvDelete.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }
    }
}
