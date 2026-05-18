package com.example.personal_finance_manager.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.models.Category;
import com.example.personal_finance_manager.models.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Category> categories;
    private List<Transaction> transactions = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Category category);
    }

    public CategoryAdapter(List<Category> categories, OnItemClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvCategoryName.setText(category.getName());
        boolean isIncome = "INCOME".equals(category.getType());
        holder.tvCategoryType.setText(isIncome ? "Thu nhập" : "Chi tiêu");
        holder.tvCategoryType.setTextColor(isIncome ? 0xFF2E7D32 : 0xFFC62828);

        if (category.getBudget() > 0) {
            holder.layoutBudget.setVisibility(View.VISIBLE);
            double currentAmount = calculateTotalAmount(category.getName(), category.getType());
            int progress = (int) ((currentAmount / category.getBudget()) * 100);
            holder.pbBudget.setProgress(Math.min(progress, 100));
            
            int color;
            if (isIncome) {
                color = 0xFF2E7D32; // Luôn màu xanh lá cho Thu nhập
                holder.tvBudgetText.setText(String.format(Locale.getDefault(), 
                    "Đã thu: %,.0f / %,.0f đ (%d%%)", currentAmount, category.getBudget(), progress));
                holder.tvBudgetText.setTextColor(0xFF757575);
            } else {
                // Logic màu sắc cho Chi tiêu
                if (progress >= 90) {
                    color = 0xFFC62828; // Đỏ (>= 90%)
                } else if (progress >= 60) {
                    color = 0xFFFBC02D; // Vàng (60% - 90%)
                } else {
                    color = 0xFF2E7D32; // Xanh lá (< 60%)
                }
                
                holder.tvBudgetText.setText(String.format(Locale.getDefault(), 
                    "Đã chi: %,.0f / %,.0f đ (%d%%)", currentAmount, category.getBudget(), progress));
                
                if (progress >= 90) {
                    holder.tvBudgetText.setTextColor(0xFFC62828);
                } else {
                    holder.tvBudgetText.setTextColor(0xFF757575);
                }
            }
            
            holder.pbBudget.setProgressTintList(ColorStateList.valueOf(color));
        } else {
            holder.layoutBudget.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> listener.onItemClick(category));
    }

    private double calculateTotalAmount(String categoryName, String type) {
        double total = 0;
        for (Transaction t : transactions) {
            if (type.equals(t.getType()) && categoryName.equals(t.getCategoryName())) {
                total += t.getAmount();
            }
        }
        return total;
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvCategoryType, tvBudgetText;
        ProgressBar pbBudget;
        View layoutBudget;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvCategoryType = itemView.findViewById(R.id.tvCategoryType);
            tvBudgetText = itemView.findViewById(R.id.tvBudgetText);
            pbBudget = itemView.findViewById(R.id.pbBudget);
            layoutBudget = itemView.findViewById(R.id.layoutBudget);
        }
    }
}
