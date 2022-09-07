package com.davidread.clothescatalog;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.davidread.clothescatalog.database.ProductContract;

/**
 * Adapts a {@link Cursor} of product provider data for a {@link RecyclerView}.
 */
public class ProductCursorAdapter extends RecyclerView.Adapter<ProductCursorAdapter.ProductViewHolder> {

    /**
     * {@link Cursor} to be adapted.
     */
    private Cursor cursor;

    /**
     * Constructs a new adapter with a {@code null} {@link #cursor}.
     */
    public ProductCursorAdapter() {
    }

    /**
     * Invoked when the {@link RecyclerView} needs a new {@link ProductViewHolder}.
     *
     * @param parent   The {@link ViewGroup} into which the new {@link View} will be added after it
     *                 is bound to an adapter position.
     * @param viewType The view type of the new {@link View}. Not used.
     * @return A new {@link ProductViewHolder} that holds a {@link View} of the given view type.
     */
    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View itemView = layoutInflater.inflate(R.layout.list_item, parent, false);
        return new ProductViewHolder(itemView);
    }

    /**
     * Invoked when the {@link RecyclerView} needs to bind data onto a {@link ProductViewHolder} at
     * a specific adapter position.
     *
     * @param holder   The {@link ProductViewHolder} which should be updated to represent the
     *                 contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ProductCursorAdapter.ProductViewHolder holder, int position) {
        cursor.moveToPosition(position);
        int nameColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_QUANTITY);
        String name = cursor.getString(nameColumnIndex);
        String price = cursor.getString(priceColumnIndex);
        String quantity = cursor.getString(quantityColumnIndex);

        holder.getNameTextView().setText(name);
        holder.getPriceTextView().setText(price);
        holder.getQuantityTextView().setText(quantity);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        if (cursor == null) {
            return 0;
        } else {
            return cursor.getCount();
        }
    }

    /**
     * Set a new {@link Cursor} to adapt.
     */
    public void setCursor(@Nullable Cursor newCursor) {
        if (cursor != null) {
            cursor.close();
        }
        cursor = newCursor;
        notifyDataSetChanged();
    }

    /**
     * Describes a single product item view and metadata about its place within the
     * {@link RecyclerView}.
     */
    protected static class ProductViewHolder extends RecyclerView.ViewHolder {

        /**
         * Holds the name of the product.
         */
        private final TextView nameTextView;

        /**
         * Holds the price of the product.
         */
        private final TextView priceTextView;

        /**
         * Holds the quantity of the product.
         */
        private final TextView quantityTextView;

        /**
         * Constructs a new view holder for the given item view.
         *
         * @param itemView Item view to be held.
         */
        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            this.nameTextView = itemView.findViewById(R.id.name_text_view);
            this.priceTextView = itemView.findViewById(R.id.price_text_view);
            this.quantityTextView = itemView.findViewById(R.id.quantity_text_view);
        }

        public TextView getNameTextView() {
            return nameTextView;
        }

        public TextView getPriceTextView() {
            return priceTextView;
        }

        public TextView getQuantityTextView() {
            return quantityTextView;
        }
    }
}
