package com.example.voca.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voca.R;

import java.util.List;

public class FunctionAdapter extends RecyclerView.Adapter<FunctionAdapter.FunctionViewHolder> {
    private List<FunctionItem> functionList;
    private OnFunctionClickListener listener;

    public interface OnFunctionClickListener {
        void onFunctionClick(FunctionItem functionItem);
    }

    public FunctionAdapter(List<FunctionItem> functionList, OnFunctionClickListener listener) {
        this.functionList = functionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FunctionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_function_card, parent, false);
        return new FunctionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FunctionViewHolder holder, int position) {
        FunctionItem functionItem = functionList.get(position);
        holder.functionName.setText(functionItem.getFunctionName());
        holder.backgroundImage.setImageResource(functionItem.getBackgroundImageResId());
        holder.icon.setImageResource(functionItem.getIcon());
        holder.functionCard.setOnClickListener(v -> listener.onFunctionClick(functionItem));
    }

    @Override
    public int getItemCount() {
        return functionList.size();
    }

    static class FunctionViewHolder extends RecyclerView.ViewHolder {
        ImageView backgroundImage;
        ImageView icon;
        TextView functionName;
        CardView functionCard;


        public FunctionViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            backgroundImage = itemView.findViewById(R.id.backgroundImage);
            functionName = itemView.findViewById(R.id.functionName);
            functionCard = itemView.findViewById(R.id.functionCard);
        }
    }
}
