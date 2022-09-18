package com.davidread.clothescatalog.view;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
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
        decrementQuantityButton.setOnClickListener(this::onDecrementQuantityButtonClick);
        TooltipCompat.setTooltipText(decrementQuantityButton, getString(R.string.decrement_quantity_button_tooltip));
        Button incrementQuantityButton = findViewById(R.id.increment_quantity_button);
        incrementQuantityButton.setOnClickListener(this::onIncrementQuantityButtonClick);
        TooltipCompat.setTooltipText(incrementQuantityButton, getString(R.string.increment_quantity_button_tooltip));

        FloatingActionButton saveProductButton = findViewById(R.id.save_product_button);
        saveProductButton.setOnClickListener(this::onSaveProductButtonClick);
        TooltipCompat.setTooltipText(saveProductButton, getString(R.string.save_product_button_tooltip));

        if (selectedProductUri == null) {
            // Put UI in add product mode.
            setTitle(R.string.add_product_title);
            pictureTextInputEditText.setText(getRandomPictureValue());
        } else {
            // Put UI in update product mode.
            setTitle(R.string.update_product_title);
            LoaderManager.getInstance(this).initLoader(0, null, this);
        }
    }

    /**
     * Callback invoked to initialize the action bar. It inflates the action bar's layout.
     *
     * @param menu The options menu in which you place your items.
     * @return True to show the menu. False to hide the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (selectedProductUri != null) {
            // Only show if UI is in update product mode.
            getMenuInflater().inflate(R.menu.menu_detail_update_product_mode, menu);
        }
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
        if (id == R.id.action_delete_product) {
            onDeleteProductButtonClick();
            return true;
        } else if (id == android.R.id.home) {
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
     * Invoked when the delete product button in the action bar is clicked. It shows a delete
     * product confirmation dialog.
     */
    private void onDeleteProductButtonClick() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.delete_confirmation_dialog_message)
                .setPositiveButton(
                        R.string.delete_confirmation_dialog_positive_label,
                        this::onDeleteProductConfirmationPositiveButtonClick
                )
                .setNegativeButton(R.string.delete_confirmation_dialog_negative_label, null)
                .create();
        dialog.show();
    }

    /**
     * Invoked when the positive button of the delete product confirmation dialog ic clicked. It
     * deletes the product corresponding with this activity. If the deletion operation fails, it
     * shows an error snackbar.
     */
    private void onDeleteProductConfirmationPositiveButtonClick(DialogInterface dialog, int which) {
        int countRowsDeleted = getContentResolver().delete(selectedProductUri, null, null);
        if (countRowsDeleted == -1) {
            // Deletion failed.
            showSnackbar(R.string.delete_product_failed_message);
            return;
        }
        finish();
    }

    /**
     * Invoked when the decrement button is clicked. It decrements the quantity of the value in
     * {@link #quantityTextInputEditText} by 1 without letting the quantity fall below 0.
     */
    private void onDecrementQuantityButtonClick(View view) {
        Integer quantity = extractValueFromEditText(
                quantityTextInputEditText,
                QUANTITY_PATTERN,
                Integer.class
        );
        if (quantity == null || quantity <= 0) {
            // No quantity was in the text field or the quantity cannot be decremented any further.
            return;
        }
        quantity--;
        String quantityString = Integer.toString(quantity);
        quantityTextInputEditText.setText(quantityString);
    }

    /**
     * Invoked when the increment button is clicked. It increments the quantity of the value in
     * {@link #quantityTextInputEditText} by 1.
     */
    private void onIncrementQuantityButtonClick(View view) {
        Integer quantity = extractValueFromEditText(
                quantityTextInputEditText,
                QUANTITY_PATTERN,
                Integer.class
        );
        if (quantity == null) {
            // No quantity was in the text field.
            return;
        }
        quantity++;
        String quantityString = Integer.toString(quantity);
        quantityTextInputEditText.setText(quantityString);
    }

    /**
     * Invoked when the save product button is clicked. First, it validates the contents of the text
     * fields. If an invalidation if found, a snackbar error is shown and execution stops. Then, it
     * either adds a product or updates a product, depending on this activity's mode.
     */
    private void onSaveProductButtonClick(View view) {

        String name = extractValueFromEditText(
                nameTextInputEditText,
                NAME_PATTERN,
                String.class
        );
        Integer price = extractValueFromEditText(
                priceTextInputEditText,
                PRICE_PATTERN,
                Integer.class
        );
        Integer quantity = extractValueFromEditText(
                quantityTextInputEditText,
                QUANTITY_PATTERN,
                Integer.class
        );
        String supplier = extractValueFromEditText(
                supplierTextInputEditText,
                SUPPLIER_PATTERN,
                String.class
        );
        byte[] picture = extractValueFromEditText(
                pictureTextInputEditText,
                null,
                byte[].class
        );

        if (name == null || price == null || quantity == null || supplier == null
                || picture == null) {
            // A value could not be extracted or the value did not match its regular expression.
            showSnackbar(R.string.check_form_message);
            return;
        }

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
     * Extracts the value from an edit text and returns it as some given class type. It will only
     * return the value if the value matches some given regular expression and if no conversion
     * errors occur. Otherwise, it will return {@code null}.
     *
     * @param editText    Edit text to extract from.
     * @param pattern     Regular expression to match the extracted string to. {@code null} if no
     *                    matching should be done.
     * @param returnClass Class type to convert the extracted value to. Accepts only {@link String}
     *                    and {@link Integer} so far.
     * @param <T>         Class type to convert the extracted value to. Accepts only {@link String}
     *                    and {@link Integer} so far.
     * @return The value from the edit text. {@code null} if regular expression matching fails or
     * if some conversion error occurs.
     */
    @Nullable
    private <T> T extractValueFromEditText(
            @NonNull TextInputEditText editText,
            @Nullable String pattern,
            @NonNull Class<T> returnClass
    ) {
        // Extract String from EditText.
        Editable textEditable = editText.getText();
        if (textEditable == null) {
            return null;
        }
        String textString = textEditable.toString();
        if (pattern != null && !textString.matches(pattern)) {
            return null;
        }
        if (returnClass == String.class) {
            // Return value as String.
            return returnClass.cast(textString);
        } else if (returnClass == Integer.class) {
            try {
                // Return value as Integer.
                int textInteger = Integer.parseInt(textString);
                return returnClass.cast(textInteger);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (returnClass == byte[].class) {
            try {
                // Return value as byte[].
                byte[] textByteArray = parseStringToBytes(textString);
                return returnClass.cast(textByteArray);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            // Unsupported return class.
            return null;
        }
    }

    /**
     * Returns the string representation of a dummy picture {@code byte[]} to display in
     * {@link #pictureTextInputEditText}.
     *
     * @return A dummy picture {@code byte[]} string representation.
     */
    @NonNull
    private String getRandomPictureValue() {
        Random random = new Random(System.currentTimeMillis());
        byte[] byteArray = new byte[]{
                (byte) (random.nextInt((127 - (-128)) + 1) + (-128)),
                (byte) (random.nextInt((127 - (-128)) + 1) + (-128)),
                (byte) (random.nextInt((127 - (-128)) + 1) + (-128)),
                (byte) (random.nextInt((127 - (-128)) + 1) + (-128))
        };
        return Arrays.toString(byteArray);
    }

    /**
     * Parses the string representation of a {@code byte[]} into a {@code byte[]}.
     *
     * @param string String representation of a {@code byte[]}.
     * @return {@code byte[]} parsed from the string.
     */
    @NonNull
    private byte[] parseStringToBytes(@NonNull String string) throws NumberFormatException {
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
