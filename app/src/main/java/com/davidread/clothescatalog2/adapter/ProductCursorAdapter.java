package com.davidread.clothescatalog2.adapter;

import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.davidread.clothescatalog2.R;
import com.davidread.clothescatalog2.database.ProductContract;
import com.davidread.clothescatalog2.database.ProductProviderUtils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Adapts a {@link Cursor} of product provider data for a {@link RecyclerView}.
 */
public class ProductCursorAdapter extends RecyclerView.Adapter<ProductCursorAdapter.ProductViewHolder> {

    /**
     * Listener that specifies what to do when a list item is clicked.
     */
    private final Consumer<Long> onItemClickListener;

    /**
     * Listener that specifies what to do when the sale button in a list item is clicked.
     */
    private final BiConsumer<Long, Integer> onSaleButtonClickListener;

    /**
     * {@link Cursor} to be adapted.
     */
    private Cursor cursor;

    /**
     * Id column index for retrieving ids from {@link #cursor}.
     */
    private int idColumnIndex;

    /**
     * Constructs a new adapter with listeners for handling clicks.
     *
     * @param onItemClickListener       Listener that specifies what to do when a list item is
     *                                  clicked.
     * @param onSaleButtonClickListener Listener that specifies what to do when the sale button in a
     *                                  list item is clicked.
     */
    public ProductCursorAdapter(
            @NonNull Consumer<Long> onItemClickListener,
            @NonNull BiConsumer<Long, Integer> onSaleButtonClickListener
    ) {
        this.onItemClickListener = onItemClickListener;
        this.onSaleButtonClickListener = onSaleButtonClickListener;
        setHasStableIds(true);
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
        int quantityColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_QUANTITY);
        String name = cursor.getString(nameColumnIndex);
        String price = ProductProviderUtils.getCurrencyFormatPrice(cursor);
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
     * Returns the id of the item at the given position.
     *
     * @param position Position of the item in the adapter.
     * @return The id of the item. {@link RecyclerView#NO_ID} if no id exists for the given
     * position.
     */
    @Override
    public long getItemId(int position) {
        if (cursor == null || position < 0 || position >= getItemCount()) {
            return RecyclerView.NO_ID;
        } else {
            cursor.moveToPosition(position);
            return cursor.getLong(idColumnIndex);
        }
    }

    /**
     * Set a new {@link Cursor} to adapt.
     */
    public void setCursor(@Nullable Cursor newCursor) {
        if (cursor != null && newCursor != cursor) {
            cursor.close();
        }
        cursor = newCursor;
        if (newCursor != null) {
            idColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry._ID);
        }
        notifyDataSetChanged();
    }

    /**
     * Returns the {@link Cursor} being adapted pointing at the given position.
     *
     * @param position Position of the item in the adapter.
     * @return {@link Cursor} moved to the given position. {@code null} if no {@link Cursor} is
     * being adapted or if the given position is out of bounds.
     */
    @Nullable
    public Cursor getItem(int position) {
        boolean isMoveSuccessful = false;
        if (cursor != null) {
            isMoveSuccessful = cursor.moveToPosition(position);
        }
        if (isMoveSuccessful) {
            return cursor;
        } else {
            return null;
        }
    }

    /**
     * Describes a single product item view and metadata about its place within the
     * {@link RecyclerView}.
     */
    protected class ProductViewHolder extends RecyclerView.ViewHolder {

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
            nameTextView = itemView.findViewById(R.id.name_text_view);
            priceTextView = itemView.findViewById(R.id.price_text_view);
            quantityTextView = itemView.findViewById(R.id.quantity_text_view);

            itemView.setOnClickListener((view) -> onItemClickListener.accept(getItemId()));
            Button saleButton = itemView.findViewById(R.id.sale_button);
            saleButton.setOnClickListener(
                    (view) -> onSaleButtonClickListener.accept(getItemId(), getQuantity())
            );
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

        /**
         * Returns the quantity of the product that corresponds with this view holder.
         *
         * @return Quantity corresponding with this view holder.
         */
        private int getQuantity() {
            Cursor cursor = getItem(getAdapterPosition());
            assert cursor != null;
            int quantityColumnIndex = cursor.getColumnIndex(
                    ProductContract.ProductEntry.COLUMN_QUANTITY
            );
            return cursor.getInt(quantityColumnIndex);
        }
    }
}
