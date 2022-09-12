package com.davidread.clothescatalog.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.davidread.clothescatalog.R;

/**
 * Provides a user interface for viewing and editing a particular product. It is for a new product
 * if {@link #selectedProductUri} is {@code null}. Otherwise, it is for an existing product.
 */
public class DetailActivity extends AppCompatActivity {

    /**
     * Content URI corresponds with the product being shown. If {@code null}, then a new product is
     * being added.
     */
    private Uri selectedProductUri;

    /**
     * Callback invoked to initialize the activity. Initializes member variables and initializes the
     * simple layout for now.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        selectedProductUri = intent.getData();

        if (selectedProductUri == null) {
            setTitle(R.string.add_product_title);
        } else {
            setTitle(R.string.update_product_title);
        }

        TextView textView = findViewById(R.id.detail_text_view);
        textView.setText("Launched DetailActivity with selectedProductUri=" + selectedProductUri);
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
        if (id == android.R.id.home) {
            // Mimic back press behavior for its back animation.
            onBackPressed();
            return true;
        } else {
            // Superclass will handle all other clicks.
            return super.onOptionsItemSelected(item);
        }
    }
}
