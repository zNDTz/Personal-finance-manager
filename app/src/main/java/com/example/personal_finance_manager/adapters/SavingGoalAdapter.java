package com.example.personal_finance_manager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.personal_finance_manager.R;
import com.example.personal_finance_manager.models.SavingGoal;
import java.util.List;
import java.util.Locale;

public class SavingGoalAdapter extends RecyclerView.Adapter<SavingGoalAdapter.ViewHolder> {

    private List<SavingGoal> goals;
    private OnGoalClickListener listener;

    public interface OnGoalClickListener {
        void onAddMoneyClick(SavingGoal goal);
        void onEditClick(SavingGoal goal);
    }

    public SavingGoalAdapter(List<SavingGoal> goals, OnGoalClickListener listener) {
        this.goals = goals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saving_goal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavingGoal goal = goals.get(position);
        holder.tvGoalName.setText(goal.getName());
        holder.tvDeadline.setText(String.format("Đến: %s", goal.getDeadline()));
        
        String progressText = String.format(Locale.getDefault(), "%,.0f / %,.0f đ", goal.getCurrentAmount(), goal.getTargetAmount());
        holder.tvProgressText.setText(progressText);

        int progress = (int) ((goal.getCurrentAmount() / goal.getTargetAmount()) * 100);
        holder.pbGoalProgress.setProgress(Math.min(progress, 100));

        holder.btnAddMoney.setOnClickListener(v -> listener.onAddMoneyClick(goal));
        holder.ivEditGoal.setOnClickListener(v -> listener.onEditClick(goal));
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGoalName, tvDeadline, tvProgressText;
        ProgressBar pbGoalProgress;
        Button btnAddMoney;
        ImageView ivEditGoal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGoalName = itemView.findViewById(R.id.tvGoalName);
            tvDeadline = itemView.findViewById(R.id.tvDeadline);
            tvProgressText = itemView.findViewById(R.id.tvProgressText);
            pbGoalProgress = itemView.findViewById(R.id.pbGoalProgress);
            btnAddMoney = itemView.findViewById(R.id.btnAddMoney);
            ivEditGoal = itemView.findViewById(R.id.ivEditGoal);
        }
    }
}
