package com.davidread.clothescatalog.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.davidread.clothescatalog.adapter.ProductCursorAdapter;
import com.davidread.clothescatalog.R;
import com.davidread.clothescatalog.database.ProductContract;
import com.davidread.clothescatalog.util.DummyConstants;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.Random;

/**
 * Provides a user interface for browsing a list of products queried from the product provider.
 */
public class InventoryActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Adapts a {@link Cursor} of data from the product provider for a {@link RecyclerView}.
     */
    private ProductCursorAdapter productCursorAdapter;

    /**
     * Root view of the layout for animating the add product button when a snackbar appears.
     */
    private CoordinatorLayout inventoryCoordinatorLayout;

    /**
     * Text shown in the UI when no data is available from the product provider.
     */
    private TextView emptyListPrimaryTextView;
    private TextView emptyListSecondaryTextView;

    /**
     * Callback invoked to initialize the activity. Initializes member variables, sets up the
     * {@link RecyclerView} and initializes a {@link CursorLoader} to query for product data.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        setTitle(R.string.inventory_action_bar_title);
        productCursorAdapter = new ProductCursorAdapter(this::onItemClick, this::onSaleButtonClick);
        inventoryCoordinatorLayout = findViewById(R.id.inventory_coordinator_layout);
        emptyListPrimaryTextView = findViewById(R.id.empty_list_primary_text_view);
        emptyListSecondaryTextView = findViewById(R.id.empty_list_secondary_text_view);
        FloatingActionButton addProductButton = findViewById(R.id.add_product_button);
        addProductButton.setOnClickListener(this::onAddProductButtonClick);
        TooltipCompat.setTooltipText(addProductButton, getString(R.string.add_product_button_tooltip));
        RecyclerView recyclerView = findViewById(R.id.product_recycler_view);
        recyclerView.setAdapter(productCursorAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
        );
        recyclerView.addItemDecoration(dividerItemDecoration);
        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    /**
     * Callback invoked to initialize the action bar. It inflates the action bar's layout.
     *
     * @param menu The options menu in which you place your items.
     * @return True to show the menu. False to hide the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    /**
     * Callback invoked when an action bar option is clicked. It specifies what actions to take when
     * either option is clicked.
     *
     * @param item The menu item that was selected.
     * @return False to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_dummy_product) {
            onAddDummyProductClick();
            return true;
        } else if (id == R.id.action_delete_all_products) {
            onDeleteAllProductsClick();
            return true;
        } else {
            // Superclass will handle all other clicks.
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Invoked when a loader is initially created. It returns a {@link CursorLoader} for fetching
     * data from the product provider.
     *
     * @param id   The ID of the loader to be created.
     * @param args Any arguments supplied by the caller.
     * @return A {@link CursorLoader} ready to fetch data.
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        String[] projection = {
                ProductContract.ProductEntry._ID,
                ProductContract.ProductEntry.COLUMN_NAME,
                ProductContract.ProductEntry.COLUMN_PRICE,
                ProductContract.ProductEntry.COLUMN_QUANTITY
        };
        return new CursorLoader(
                this,
                ProductContract.ProductEntry.CONTENT_URI,
                projection,
                null,
                null,
                null
        );
    }

    /**
     * Invoked whenever a previously created loader finishes its load. It passes the newly fetched
     * {@link Cursor} to {@link #productCursorAdapter} and sets the visibility of the empty list
     * text in the UI.
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        productCursorAdapter.setCursor(data);
        if (data == null || data.getCount() <= 0) {
            setEmptyListTextVisibility(View.VISIBLE);
        } else {
            setEmptyListTextVisibility(View.INVISIBLE);
        }
    }

    /**
     * Invoked when a previously created loader is being reset, thus invalidating its dataset. It
     * just resets {@link #productCursorAdapter} and shows the empty list text in the UI.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        productCursorAdapter.setCursor(null);
        setEmptyListTextVisibility(View.VISIBLE);
    }

    /**
     * Invoked when the add dummy product button in the action bar is clicked. It adds a product
     * with dummy data to the product provider. If the insertion operation fails, it shows an error
     * snackbar.
     */
    private void onAddDummyProductClick() {
        Uri insertUri = getContentResolver().insert(
                ProductContract.ProductEntry.CONTENT_URI,
                getRandomContentValues()
        );
        if (insertUri == null) {
            // Insertion failed.
            showSnackbar(R.string.add_product_failed_message);
        }
    }

    /**
     * Invoked when the delete all products button in the action bar is clicked. It deletes all
     * products from the product provider. If the deletion operation fails, it shows an error
     * snackbar.
     */
    private void onDeleteAllProductsClick() {
        int countRowsDeleted = getContentResolver().delete(
                ProductContract.ProductEntry.CONTENT_URI,
                null,
                null
        );
        if (countRowsDeleted == -1) {
            // Deletion failed.
            showSnackbar(R.string.delete_all_products_failed_message);
        }
    }

    /**
     * Invoked when a list item in the recycler view is clicked. It launches the
     * {@link DetailActivity} while passing the content URI that corresponds with the clicked
     * list item.
     *
     * @param id Id of the product corresponding with this list item.
     */
    private void onItemClick(long id) {
        Intent intent = new Intent(this, DetailActivity.class);
        Uri uri = ContentUris.withAppendedId(ProductContract.ProductEntry.CONTENT_URI, id);
        intent.setData(uri);
        startActivity(intent);
    }

    /**
     * Invoked when the sale button of a list item in the recycler view is clicked. It updates
     * the appropriate product in the product provider with a quantity decremented by one. If the
     * update operation fails, an error snackbar is shown.
     *
     * @param id       Id of the product corresponding with this list item.
     * @param quantity Quantity of the product corresponding with this list item.
     */
    private void onSaleButtonClick(long id, int quantity) {
        // Do not allow decrements below 0.
        if (quantity <= 0) {
            return;
        }

        // Perform update.
        Uri uri = ContentUris.withAppendedId(ProductContract.ProductEntry.CONTENT_URI, id);
        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_QUANTITY, quantity - 1);
        int countRowsUpdated = getContentResolver().update(uri, values, null, null);

        if (countRowsUpdated == -1) {
            // Update failed.
            showSnackbar(R.string.update_product_failed_message);
        }
    }

    /**
     * Invoked when the add product button is clicked. It launches the {@link DetailActivity}
     * without passing any content URI.
     */
    private void onAddProductButtonClick(View view) {
        Intent intent = new Intent(this, DetailActivity.class);
        startActivity(intent);
    }

    /**
     * Shows a snackbar in the UI with the given message.
     *
     * @param resId String resource id for the message.
     */
    private void showSnackbar(@StringRes int resId) {
        Snackbar.make(inventoryCoordinatorLayout, resId, BaseTransientBottomBar.LENGTH_SHORT)
                .show();
    }

    /**
     * Returns a {@link ContentValues} of dummy product data to insert into the product provider.
     *
     * @return A row of dummy product data.
     */
    @NonNull
    private ContentValues getRandomContentValues() {
        Random random = new Random(System.currentTimeMillis());
        ContentValues values = new ContentValues();
        values.put(
                ProductContract.ProductEntry.COLUMN_NAME,
                DummyConstants.DUMMY_NAMES[random.nextInt(DummyConstants.DUMMY_NAMES.length)]
        );
        values.put(ProductContract.ProductEntry.COLUMN_PRICE, random.nextInt(10000));
        values.put(ProductContract.ProductEntry.COLUMN_QUANTITY, random.nextInt(1000));
        values.put(
                ProductContract.ProductEntry.COLUMN_SUPPLIER,
                DummyConstants.DUMMY_SUPPLIERS[random.nextInt(DummyConstants.DUMMY_SUPPLIERS.length)]
        );
        values.put(
                ProductContract.ProductEntry.COLUMN_PICTURE,
                new byte[]{
                        (byte) (random.nextInt((127 - (-128)) + 1) + (-128)),
                        (byte) (random.nextInt((127 - (-128)) + 1) + (-128)),
                        (byte) (random.nextInt((127 - (-128)) + 1) + (-128)),
                        (byte) (random.nextInt((127 - (-128)) + 1) + (-128))
                }
        );
        return values;
    }

    /**
     * Sets the visibility of the empty list text in the UI. Ensure that
     * {@link #emptyListPrimaryTextView} and {@link #emptyListSecondaryTextView} are not
     * {@code null} before calling.
     *
     * @param visibility Either {@link View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     */
    private void setEmptyListTextVisibility(int visibility) {
        emptyListPrimaryTextView.setVisibility(visibility);
        emptyListSecondaryTextView.setVisibility(visibility);
    }
}