package com.example.personal_finance_manager.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.models.Transaction;

import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactions;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Transaction transaction);
    }

    public TransactionAdapter(List<Transaction> transactions, OnItemClickListener listener) {
        this.transactions = transactions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        
        holder.tvCategory.setText(transaction.getCategoryName() != null ? transaction.getCategoryName() : "Khác");
        holder.tvDescription.setText(transaction.getDescription() != null ? transaction.getDescription() : "");
        holder.tvDate.setText(transaction.getDate() != null ? transaction.getDate() : "");
        
        String prefix = "INCOME".equals(transaction.getType()) ? "+" : "-";
        int color = "INCOME".equals(transaction.getType()) ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
        
        holder.tvAmount.setText(String.format(Locale.getDefault(), "%s %,.0f đ", prefix, transaction.getAmount()));
        holder.tvAmount.setTextColor(color);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(transaction));
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDescription, tvDate, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
