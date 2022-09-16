package com.davidread.clothescatalog.view;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.davidread.clothescatalog.R;
import com.davidread.clothescatalog.database.ProductContract;
import com.davidread.clothescatalog.util.RegexTextWatcher;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.Random;

/**
 * Provides a user interface for viewing and editing a particular product. It is for a new product
 * if {@link #selectedProductUri} is {@code null}. Otherwise, it is for an existing product.
 */
public class DetailActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Regular expressions that each text field should be matched with to be valid.
     */
    private static final String NAME_PATTERN = "^.{1,250}$";
    private static final String PRICE_PATTERN = "^\\d{1,9}$";
    private static final String QUANTITY_PATTERN = "^\\d{1,9}$";
    private static final String SUPPLIER_PATTERN = "^.{1,250}$";

    /**
     * Content URI corresponds with the product being shown. If {@code null}, then a new product is
     * being added.
     */
    private Uri selectedProductUri;

    /**
     * Root view of the layout for animating the save product button when a snackbar appears.
     */
    private CoordinatorLayout detailCoordinatorLayout;

    /**
     * Text fields displaying the value of each product property in the layout.
     */
    private TextInputEditText nameTextInputEditText;
    private TextInputEditText priceTextInputEditText;
    private TextInputEditText quantityTextInputEditText;
    private TextInputEditText supplierTextInputEditText;
    private TextInputEditText pictureTextInputEditText;

    /**
     * Callback invoked to initialize the activity. Initializes member variables, initializes the
     * text fields, and puts the activity in either add product or update product mode.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        selectedProductUri = intent.getData();

        detailCoordinatorLayout = findViewById(R.id.detail_coordinator_layout);
        nameTextInputEditText = findViewById(R.id.name_text_input_edit_text);
        priceTextInputEditText = findViewById(R.id.price_text_input_edit_text);
        quantityTextInputEditText = findViewById(R.id.quantity_text_input_edit_text);
        supplierTextInputEditText = findViewById(R.id.supplier_text_input_edit_text);
        pictureTextInputEditText = findViewById(R.id.picture_text_input_edit_text);

        TextInputLayout nameTextInputLayout = findViewById(R.id.name_text_input_layout);
        nameTextInputEditText.addTextChangedListener(new RegexTextWatcher(
                NAME_PATTERN,
                getString(R.string.text_invalid_error_message),
                nameTextInputLayout
        ));
        TextInputLayout priceTextInputLayout = findViewById(R.id.price_text_input_layout);
        priceTextInputEditText.addTextChangedListener(new RegexTextWatcher(
                PRICE_PATTERN,
                getString(R.string.number_invalid_error_message),
                priceTextInputLayout
        ));
        TextInputLayout quantityTextInputLayout = findViewById(R.id.quantity_text_input_layout);
        quantityTextInputEditText.addTextChangedListener(new RegexTextWatcher(
                QUANTITY_PATTERN,
                getString(R.string.number_invalid_error_message),
                quantityTextInputLayout
        ));
        TextInputLayout supplierTextInputLayout = findViewById(R.id.supplier_text_input_layout);
        supplierTextInputEditText.addTextChangedListener(new RegexTextWatcher(
                SUPPLIER_PATTERN,
                getString(R.string.text_invalid_error_message),
                supplierTextInputLayout
        ));
        pictureTextInputEditText.setEnabled(false);

        Button decrementQuantityButton = findViewById(R.id.decrement_quantity_button);
        TooltipCompat.setTooltipText(decrementQuantityButton, getString(R.string.decrement_quantity_button_tooltip));
        Button incrementQuantityButton = findViewById(R.id.increment_quantity_button);
        TooltipCompat.setTooltipText(incrementQuantityButton, getString(R.string.increment_quantity_button_tooltip));

        FloatingActionButton saveProductButton = findViewById(R.id.save_product_button);
        saveProductButton.setOnClickListener(this::onSaveProductButtonClick);
        TooltipCompat.setTooltipText(saveProductButton, getString(R.string.save_product_button_tooltip));

        if (selectedProductUri == null) {
            // Put UI in add product mode.
            setTitle(R.string.add_product_title);
            pictureTextInputEditText.setText(Arrays.toString(getRandomPictureValue()));
        } else {
            // Put UI in update product mode.
            setTitle(R.string.update_product_title);
            LoaderManager.getInstance(this).initLoader(0, null, this);
        }
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

    /**
     * Invoked when a loader is initially created. It returns a {@link CursorLoader} for fetching
     * the product with {@link #selectedProductUri} from the product provider.
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
                ProductContract.ProductEntry.COLUMN_QUANTITY,
                ProductContract.ProductEntry.COLUMN_SUPPLIER,
                ProductContract.ProductEntry.COLUMN_PICTURE
        };
        return new CursorLoader(
                this,
                selectedProductUri,
                projection,
                null,
                null,
                null
        );
    }

    /**
     * Invoked whenever a previously created loader finishes its load. It populates the text fields
     * with properties of the fetched product.
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        boolean hasFirstRow = data.moveToFirst();
        if (!hasFirstRow) {
            // Cursor is empty.
            return;
        }

        int nameColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_NAME);
        int priceColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRICE);
        int quantityColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_QUANTITY);
        int supplierColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_SUPPLIER);
        int pictureColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_PICTURE);

        String name = data.getString(nameColumnIndex);
        String price = data.getString(priceColumnIndex);
        String quantity = data.getString(quantityColumnIndex);
        String supplier = data.getString(supplierColumnIndex);
        byte[] picture = data.getBlob(pictureColumnIndex);
        String pictureString = Arrays.toString(picture);

        nameTextInputEditText.setText(name);
        priceTextInputEditText.setText(price);
        quantityTextInputEditText.setText(quantity);
        supplierTextInputEditText.setText(supplier);
        pictureTextInputEditText.setText(pictureString);
    }

    /**
     * Invoked when a previously created loader is being reset, thus invalidating its dataset. It
     * just resets the text fields.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        nameTextInputEditText.setText("");
        priceTextInputEditText.setText("");
        quantityTextInputEditText.setText("");
        supplierTextInputEditText.setText("");
        pictureTextInputEditText.setText("");
    }

    /**
     * Invoked when the save product button is clicked. First, it validates the contents of the text
     * fields. If an invalidation if found, a snackbar error is shown and execution stops. Then, it
     * either adds a product or updates a product, depending on this activity's mode.
     */
    private void onSaveProductButtonClick(View view) {

        Editable nameEditable = nameTextInputEditText.getText();
        Editable priceEditable = priceTextInputEditText.getText();
        Editable quantityEditable = quantityTextInputEditText.getText();
        Editable supplierEditable = supplierTextInputEditText.getText();
        Editable pictureEditable = pictureTextInputEditText.getText();

        if (nameEditable == null || priceEditable == null || quantityEditable == null
                || supplierEditable == null || pictureEditable == null) {
            // One or more Editable is null.
            showSnackbar(R.string.fill_form_message);
            return;
        }

        String name = nameEditable.toString();
        String priceString = priceEditable.toString();
        String quantityString = quantityEditable.toString();
        String supplier = supplierEditable.toString();
        String pictureString = pictureEditable.toString();

        if (name.isEmpty() || priceString.isEmpty() || quantityString.isEmpty()
                || supplier.isEmpty()) {
            // One or more text field is empty.
            showSnackbar(R.string.fill_form_message);
            return;
        }

        if (!name.matches(NAME_PATTERN) || !priceString.matches(PRICE_PATTERN)
                || !quantityString.matches(QUANTITY_PATTERN) || !supplier.matches(SUPPLIER_PATTERN)) {
            // One or more text field does not match their expected pattern.
            showSnackbar(R.string.check_form_for_errors_message);
            return;
        }

        int price = Integer.parseInt(priceString);
        int quantity = Integer.parseInt(quantityString);
        byte[] picture = parseStringToBytes(pictureString);

        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_NAME, name);
        values.put(ProductContract.ProductEntry.COLUMN_PRICE, price);
        values.put(ProductContract.ProductEntry.COLUMN_QUANTITY, quantity);
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER, supplier);
        values.put(ProductContract.ProductEntry.COLUMN_PICTURE, picture);

        if (selectedProductUri == null) {
            // Add a product.
            Uri insertUri = getContentResolver().insert(
                    ProductContract.ProductEntry.CONTENT_URI,
                    values
            );
            if (insertUri == null) {
                // Insert operation failed.
                showSnackbar(R.string.add_product_failed_message);
                return;
            }
        } else {
            // Update a product.
            int countRowsUpdated = getContentResolver().update(
                    selectedProductUri,
                    values,
                    null,
                    null
            );
            if (countRowsUpdated == -1) {
                // Update operation failed.
                showSnackbar(R.string.update_product_failed_message);
                return;
            }
        }
        finish();
    }

    /**
     * Shows a snackbar in the UI with the given message.
     *
     * @param resId String resource id for the message.
     */
    private void showSnackbar(@StringRes int resId) {
        Snackbar.make(detailCoordinatorLayout, resId, BaseTransientBottomBar.LENGTH_SHORT).show();
    }

    /**
     * Returns a dummy picture {@code byte[]} to insert into the product provider.
     *
     * @return A dummy picture {@code byte[]}.
     */
    private byte[] getRandomPictureValue() {
        Random random = new Random(System.currentTimeMillis());
        return new byte[]{
                (byte) (random.nextInt((127 - (-128)) + 1) + (-128)),
                (byte) (random.nextInt((127 - (-128)) + 1) + (-128)),
                (byte) (random.nextInt((127 - (-128)) + 1) + (-128)),
                (byte) (random.nextInt((127 - (-128)) + 1) + (-128))
        };
    }

    /**
     * Parses the string representation of a {@code byte[]} into a {@code byte[]}.
     *
     * @param string String representation of a {@code byte[]}.
     * @return {@code byte[]} parsed from the string.
     */
    private byte[] parseStringToBytes(String string) throws NumberFormatException {
        string = string.replace(" ", "");
        string = string.replace("[", "");
        string = string.replace("]", "");
        String[] byteStrings = string.split(",");

        byte[] bytes = new byte[byteStrings.length];
        for (int i = 0; i < byteStrings.length; i++) {
            bytes[i] = Byte.parseByte(byteStrings[i]);
        }
        return bytes;
    }
}
