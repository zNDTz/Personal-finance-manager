package com.example.personal_finance_manager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.models.Wallet;

import java.util.List;
import java.util.Locale;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.ViewHolder> {

    private List<Wallet> wallets;
    private OnWalletClickListener listener;

    public interface OnWalletClickListener {
        void onEditClick(Wallet wallet);
        void onItemClick(Wallet wallet);
    }

    public WalletAdapter(List<Wallet> wallets, OnWalletClickListener listener) {
        this.wallets = wallets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Wallet wallet = wallets.get(position);
        holder.tvWalletName.setText(wallet.getName());
        holder.tvWalletBalance.setText(String.format(Locale.getDefault(), "%,.0f đ", wallet.getBalance()));

        holder.btnEditWallet.setOnClickListener(v -> listener.onEditClick(wallet));
        holder.itemView.setOnClickListener(v -> listener.onItemClick(wallet));
    }

    @Override
    public int getItemCount() {
        return wallets.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvWalletName, tvWalletBalance;
        ImageButton btnEditWallet;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWalletName = itemView.findViewById(R.id.tvWalletName);
            tvWalletBalance = itemView.findViewById(R.id.tvWalletBalance);
            btnEditWallet = itemView.findViewById(R.id.btnEditWallet);
        }
    }
}
