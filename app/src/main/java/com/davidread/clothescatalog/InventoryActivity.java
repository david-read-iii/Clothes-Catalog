package com.davidread.clothescatalog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.davidread.clothescatalog.database.ProductContract;

import java.util.Arrays;

/**
 * Provides a user interface for browsing a list of products queried from the product provider.
 */
public class InventoryActivity extends AppCompatActivity {

    /**
     * Contains the string representation of the product provider.
     */
    private TextView databaseInfoTextView;

    /**
     * Callback invoked to initialize the activity. Initializes member variables and makes an
     * initial query for product provider data.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        databaseInfoTextView = findViewById(R.id.database_info_text_view);
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
        if (id == R.id.action_insert_dummy) {
            Toast.makeText(this, R.string.action_insert_dummy_label, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_delete_all) {
            Toast.makeText(this, R.string.action_delete_all_label, Toast.LENGTH_SHORT).show();
            return true;
        }
        // Superclass will handle all other clicks.
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Queries the product provider for all its data and writes its string representation to
     * {@link #databaseInfoTextView}. If the query operation fails, an error toast is shown.
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

        // Write header content.
        databaseInfoTextView.setText("");
        String count = String.format("Count=%s\n", cursor.getCount());
        databaseInfoTextView.append(count);
        String columns = String.format("Columns=%s\n", Arrays.toString(cursor.getColumnNames()));
        databaseInfoTextView.append(columns);
        databaseInfoTextView.append("\n");

        // Write rows content.
        while (cursor.moveToNext()) {
            String row = String.format(
                    "%s, %s, %s, %s, %s, %s\n",
                    cursor.getString(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    Arrays.toString(cursor.getBlob(5))
            );
            databaseInfoTextView.append(row);
        }

        cursor.close();
    }
}