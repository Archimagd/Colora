package alex.kaghktsyan.colora;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ColorSwatchAdapter extends RecyclerView.Adapter<ColorSwatchAdapter.ViewHolder> {

    private final List<Integer> colors;
    private final OnColorClickListener listener;
    private int selectedPosition = -1;

    public interface OnColorClickListener {
        void onColorClick(int color);
    }

    public ColorSwatchAdapter(List<Integer> colors, OnColorClickListener listener) {
        this.colors = colors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color_swatch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int color = colors.get(position);
        holder.colorView.setBackgroundColor(color);
        
        // Highlight selected color
        if (position == selectedPosition) {
            holder.cardView.setStrokeWidth(6);
            holder.cardView.setStrokeColor(Color.parseColor("#6200EE")); // Purple main
        } else {
            holder.cardView.setStrokeWidth(2);
            holder.cardView.setStrokeColor(Color.parseColor("#F5F5F5")); // Light grey
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
            listener.onColorClick(color);
        });
    }

    public void setSelectedColor(int color) {
        int index = colors.indexOf(color);
        if (index != -1) {
            int oldPos = selectedPosition;
            selectedPosition = index;
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
        }
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View colorView;
        MaterialCardView cardView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.colorView);
            cardView = (MaterialCardView) itemView.findViewById(R.id.colorCard);
        }
    }
}
