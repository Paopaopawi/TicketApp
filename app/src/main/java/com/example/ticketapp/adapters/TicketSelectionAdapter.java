package com.example.ticketapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ticketapp.R;
import com.example.ticketapp.models.TicketFormData;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class TicketSelectionAdapter extends RecyclerView.Adapter<TicketSelectionAdapter.TicketViewHolder> {

    private final List<TicketFormData> tickets;
    private final Map<String, Integer> selectedQuantities;
    private final Runnable onQuantityChanged;

    public TicketSelectionAdapter(List<TicketFormData> tickets, Map<String, Integer> selectedQuantities, Runnable onQuantityChanged) {
        this.tickets = tickets;
        this.selectedQuantities = selectedQuantities;
        this.onQuantityChanged = onQuantityChanged;
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket_selection, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        TicketFormData t = tickets.get(position);
        holder.tvName.setText(t.getName());
        holder.tvPrice.setText("₱" + t.getPrice() + " / ticket");
        holder.tvAvailable.setText("Available: " + t.getQuantity());

        Integer currentSelected = selectedQuantities.getOrDefault(t.getName(), 0);
        Integer[] options = IntStream.rangeClosed(0, Math.min(4, t.getQuantity())).boxed().toArray(Integer[]::new);
        ArrayAdapter<Integer> spinnerAdapter = new ArrayAdapter<>(holder.itemView.getContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.spinnerQuantity.setAdapter(spinnerAdapter);
        holder.spinnerQuantity.setSelection(currentSelected);

        holder.spinnerQuantity.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                selectedQuantities.put(t.getName(), options[pos]);
                onQuantityChanged.run();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvAvailable;
        Spinner spinnerQuantity;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTicketName);
            tvPrice = itemView.findViewById(R.id.tvTicketPrice);
            tvAvailable = itemView.findViewById(R.id.tvTicketAvailable);
            spinnerQuantity = itemView.findViewById(R.id.spinnerQuantity);
        }
    }
}
