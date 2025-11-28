package com.example.ticketapp.activities;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.ticketapp.R;
import com.example.ticketapp.models.TicketFormData;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.Locale;

public class TicketForm extends LinearLayout {

    private TextInputLayout tilName, tilPrice, tilType, tilQuantity;
    private TextInputEditText etName, etPrice, etType, etQuantity;

    private boolean editing;

    public TicketForm(Context context) {
        super(context);
        init(context, null);
    }

    public TicketForm(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, null);
    }

    public TicketForm(Context context, @Nullable TicketFormData data) {
        super(context);
        init(context, data);
    }

    private void init(Context context, @Nullable TicketFormData data){
        inflate(context, R.layout.ticket_form_layout, this);

        tilName = findViewById(R.id.tilTicketName);
        tilPrice = findViewById(R.id.tilTicketPrice);
        tilType = findViewById(R.id.tilTicketType);
        tilQuantity = findViewById(R.id.tilTicketQuantity);

        etName = findViewById(R.id.etTicketName);
        etPrice = findViewById(R.id.etTicketPrice);
        etType = findViewById(R.id.etTicketType);
        etQuantity = findViewById(R.id.etTicketQuantity);

        if(data != null){
            etName.setText(data.getName());
            etType.setText(data.getType());
            etPrice.setText(String.valueOf(data.getPrice()));
            etQuantity.setText(String.valueOf(data.getAvailableQuantity()));
        }

        // Price formatting
        etPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if(editing) return;
                editing = true;
                String str = s.toString().replaceAll("[₱,]", "");
                if(!str.isEmpty()){
                    try {
                        long value = Long.parseLong(str);
                        String formatted = "₱"+ NumberFormat.getNumberInstance(Locale.getDefault()).format(value);
                        etPrice.setText(formatted);
                        etPrice.setSelection(formatted.length());
                    } catch (NumberFormatException ignored){}
                }
                editing = false;
            }
        });
    }

    public TicketFormData getData() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String type = etType.getText() != null ? etType.getText().toString().trim() : "";
        String priceStr = etPrice.getText() != null ? etPrice.getText().toString().replaceAll("[₱,]", "") : "";
        String quantityStr = etQuantity.getText() != null ? etQuantity.getText().toString().trim() : "";

        boolean valid = true;
        if(name.isEmpty()){ tilName.setError("Name required"); valid=false;} else tilName.setError(null);
        if(type.isEmpty()){ tilType.setError("Type required"); valid=false;} else tilType.setError(null);
        if(priceStr.isEmpty()){ tilPrice.setError("Price required"); valid=false;} else tilPrice.setError(null);
        if(quantityStr.isEmpty()){ tilQuantity.setError("Quantity required"); valid=false;} else tilQuantity.setError(null);
        if(!valid) return null;

        try {
            int price = Integer.parseInt(priceStr);
            int quantity = Integer.parseInt(quantityStr);

            TicketFormData t = new TicketFormData();
            t.setName(name);
            t.setType(type);
            t.setPrice(price);
            t.setAvailableQuantity(quantity);
            t.setOnHoldQuantity(0);

            return t;
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid price or quantity", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
