package com.davidread.clothescatalog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.davidread.clothescatalog.database.ProductContract;

import java.util.Random;

/**
 * Provides a user interface for browsing a list of products queried from the product provider.
 */
public class InventoryActivity extends AppCompatActivity {

    /**
     * List representation of the product provider.
     */
    private ProductCursorAdapter productCursorAdapter;

    /**
     * Callback invoked to initialize the activity. Initializes member variables and makes an
     * initial query for product provider data.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        productCursorAdapter = new ProductCursorAdapter();
        RecyclerView recyclerView = findViewById(R.id.product_recycler_view);
        recyclerView.setAdapter(productCursorAdapter);
        queryRows();
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
        // Insert a dummy row.
        if (id == R.id.action_insert_dummy) {
            insertRow();
            queryRows();
            return true;
        }
        // Delete all rows.
        else if (id == R.id.action_delete_all) {
            deleteAllRows();
            queryRows();
            return true;
        }
        // Superclass will handle all other clicks.
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Inserts a row of dummy data into the product provider. If the insertion operation fails, an
     * error toast is shown.
     */
    private void insertRow() {
        // Perform insertion.
        Uri insertUri = getContentResolver().insert(
                ProductContract.ProductEntry.CONTENT_URI,
                getRandomContentValues()
        );
        if (insertUri == null) {
            // Insertion failed.
            Toast.makeText(this, R.string.insert_failed_message, Toast.LENGTH_SHORT).show();
        }
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
     * Queries the product provider for all its data and passes it to {@link #productCursorAdapter}
     * to adapt for the recycler view. If the query operation fails, an error toast is shown.
     */
    private void queryRows() {
        // Perform query.
        Cursor cursor = getContentResolver().query(
                ProductContract.ProductEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        if (cursor == null) {
            // Query failed.
            Toast.makeText(this, R.string.query_failed_message, Toast.LENGTH_SHORT).show();
            return;
        }

        // Pass data to adapter.
        productCursorAdapter.setCursor(cursor);
    }

    /**
     * Deletes all data from the product provider. If the deletion operation fails, it shows an
     * error toast is shown.
     */
    private void deleteAllRows() {
        // Perform deletion.
        int countRowsDeleted = getContentResolver().delete(
                ProductContract.ProductEntry.CONTENT_URI,
                null,
                null
        );
        if (countRowsDeleted == -1) {
            // Deletion failed.
            Toast.makeText(this, R.string.delete_failed_message, Toast.LENGTH_SHORT).show();
        }
    }
}