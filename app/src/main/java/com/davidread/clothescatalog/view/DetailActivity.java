package com.davidread.clothescatalog.view;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.davidread.clothescatalog.BuildConfig;
import com.davidread.clothescatalog.R;
import com.davidread.clothescatalog.database.ProductContract;
import com.davidread.clothescatalog.util.RegexTextWatcher;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

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
     * Ids for identifying which dialog item is clicked in {@link #onChangePhotoButtonClick()}.
     */
    private static final int TAKE_NEW_PHOTO_DIALOG_ITEM_ID = 0;
    private static final int REMOVE_PHOTO_DIALOG_ITEM_ID = 1;
    private static final int SELECT_NEW_PHOTO_DIALOG_ITEM_ID = 2;

    /**
     * Authority for this app's file provider.
     */
    private static final String FILE_PROVIDER_AUTHORITY =
            BuildConfig.APPLICATION_ID + ".fileprovider";

    /**
     * Used for building file names.
     */
    private static final String FILE_NAME = "IMG_%1$s_.jpg";
    private static final String FILE_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss";

    /**
     * Content URI corresponds with the product being shown. If {@code null}, then a new product is
     * being added.
     */
    private Uri selectedProductUri;

    /**
     * Contains background colors to apply onto a sample image for
     * {@link #showSampleImageInPhotoImageView(int)}.
     */
    private int[] sampleImageBackgroundColors;

    /**
     * Contains the {@code byte[]} representation of the image corresponding with this product. If
     * {@code null}, then this product has no picture.
     */
    private byte[] picture;

    /**
     * Launches an activity to the camera to capture an image for the product.
     */
    private ActivityResultLauncher<Uri> takePictureActivityResultLauncher;

    /**
     * File containing the image captured by the camera in the activity started by
     * {@link #takePictureActivityResultLauncher}.
     */
    private File takePictureFile;

    /**
     * Root view of the layout for animating the save product button when a snackbar appears.
     */
    private CoordinatorLayout detailCoordinatorLayout;

    /**
     * Image view to display an image representing the product.
     */
    private ImageView photoImageView;

    /**
     * Text fields displaying the value of each product property in the layout.
     */
    private TextInputEditText nameTextInputEditText;
    private TextInputEditText priceTextInputEditText;
    private TextInputEditText quantityTextInputEditText;
    private TextInputEditText supplierTextInputEditText;

    /**
     * Callback invoked to initialize the activity. Initializes member variables, initializes the
     * layout views, and puts the activity in either add product or update product mode.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        selectedProductUri = intent.getData();

        sampleImageBackgroundColors = getResources().getIntArray(R.array.sample_image_backgrounds);

        takePictureActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                this::onTakePictureActivityResult
        );

        detailCoordinatorLayout = findViewById(R.id.detail_coordinator_layout);

        photoImageView = findViewById(R.id.photo_image_view);
        photoImageView.setScaleType(ImageView.ScaleType.CENTER);

        nameTextInputEditText = findViewById(R.id.name_text_input_edit_text);
        priceTextInputEditText = findViewById(R.id.price_text_input_edit_text);
        quantityTextInputEditText = findViewById(R.id.quantity_text_input_edit_text);
        supplierTextInputEditText = findViewById(R.id.supplier_text_input_edit_text);

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

        Button changePhotoButton = findViewById(R.id.change_photo_button);
        changePhotoButton.setOnClickListener((view) -> onChangePhotoButtonClick());
        Button decrementQuantityButton = findViewById(R.id.decrement_quantity_button);
        decrementQuantityButton.setOnClickListener((view) -> onDecrementQuantityButtonClick());
        TooltipCompat.setTooltipText(decrementQuantityButton, getString(R.string.decrement_quantity_button_tooltip));
        Button incrementQuantityButton = findViewById(R.id.increment_quantity_button);
        incrementQuantityButton.setOnClickListener((view) -> onIncrementQuantityButtonClick());
        TooltipCompat.setTooltipText(incrementQuantityButton, getString(R.string.increment_quantity_button_tooltip));

        FloatingActionButton saveProductButton = findViewById(R.id.save_product_button);
        saveProductButton.setOnClickListener((view) -> onSaveProductButtonClick());
        TooltipCompat.setTooltipText(saveProductButton, getString(R.string.save_product_button_tooltip));

        if (selectedProductUri == null) {
            // Put UI in add product mode.
            setTitle(R.string.add_product_title);
            showSampleImageInPhotoImageView(0);
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
     * Invoked whenever a previously created loader finishes its load. It populates the image view
     * and text fields with properties of the fetched product.
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

        int idColumnIndex = data.getColumnIndex(ProductContract.ProductEntry._ID);
        int nameColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_NAME);
        int priceColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRICE);
        int quantityColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_QUANTITY);
        int supplierColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_SUPPLIER);
        int pictureColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_PICTURE);

        int id = data.getInt(idColumnIndex);
        String name = data.getString(nameColumnIndex);
        String price = data.getString(priceColumnIndex);
        String quantity = data.getString(quantityColumnIndex);
        String supplier = data.getString(supplierColumnIndex);
        picture = data.getBlob(pictureColumnIndex);

        nameTextInputEditText.setText(name);
        priceTextInputEditText.setText(price);
        quantityTextInputEditText.setText(quantity);
        supplierTextInputEditText.setText(supplier);
        if (picture == null) {
            // Show sample image.
            showSampleImageInPhotoImageView(id);
        } else {
            // Show stored image.
            showImageInPhotoImageView(picture);
        }
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
        picture = null;
        showSampleImageInPhotoImageView(0);
    }

    /**
     * Invoked when the delete product button in the action bar is clicked. It shows a delete
     * product confirmation dialog.
     */
    private void onDeleteProductButtonClick() {
        DialogInterface.OnClickListener onPositiveButtonClickListener = (dialogInterface, which) ->
                onDeleteProductConfirmationDialogDeleteButtonClick();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.delete_product_confirmation_dialog_message)
                .setPositiveButton(
                        R.string.generic_delete_dialog_button_label,
                        onPositiveButtonClickListener
                )
                .setNegativeButton(R.string.generic_cancel_dialog_button_label, null)
                .create();
        dialog.show();
    }

    /**
     * Invoked when the delete button of the delete product confirmation dialog is clicked. It
     * deletes the product corresponding with this activity. If the deletion operation fails, it
     * shows an error snackbar.
     */
    private void onDeleteProductConfirmationDialogDeleteButtonClick() {
        int countRowsDeleted = getContentResolver().delete(selectedProductUri, null, null);
        if (countRowsDeleted == -1) {
            // Deletion failed.
            showSnackbar(R.string.delete_product_failed_message);
            return;
        }
        finish();
    }

    /**
     * Invoked when the change photo button is clicked. It shows a dialog that presents change photo
     * options.
     */
    private void onChangePhotoButtonClick() {
        DialogInterface.OnClickListener onItemClickListener = (dialogInterface, which) -> {
            switch (which) {
                case TAKE_NEW_PHOTO_DIALOG_ITEM_ID:
                    onTakeNewPhotoButtonClick();
                    break;
                case REMOVE_PHOTO_DIALOG_ITEM_ID:
                    onRemovePhotoButtonClick();
                    break;
                case SELECT_NEW_PHOTO_DIALOG_ITEM_ID:
                    onSelectNewPhotoButtonClick();
                    break;
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setItems(R.array.change_photo_dialog_item_labels, onItemClickListener)
                .setNegativeButton(R.string.generic_cancel_dialog_button_label, null)
                .create();
        dialog.show();
    }

    /**
     * Invoked when the take new photo button is clicked. It launches an intent to the device's
     * camera to take a photo. It puts the photo file in {@link #takePictureFile} and invokes
     * {@link #onTakePictureActivityResult(boolean)} when done.
     */
    private void onTakeNewPhotoButtonClick() {
        takePictureFile = createFile();
        Uri takePictureUri = FileProvider.getUriForFile(
                this,
                FILE_PROVIDER_AUTHORITY,
                takePictureFile
        );
        takePictureActivityResultLauncher.launch(takePictureUri);
    }

    /**
     * Invoked when the remove photo button is clicked. It does nothing for now.
     */
    private void onRemovePhotoButtonClick() {
        // TODO: Put sample photo in the ImageView.
        Snackbar.make(
                detailCoordinatorLayout,
                "Put sample photo in the ImageView",
                BaseTransientBottomBar.LENGTH_SHORT
        ).show();
    }

    /**
     * Invoked when the select new photo button is clicked. It does nothing for now.
     */
    private void onSelectNewPhotoButtonClick() {
        // TODO: Launch intent to select a new photo with some Gallery API and put it in the ImageView.
        Snackbar.make(
                detailCoordinatorLayout,
                "Launch intent to select a new photo with some Gallery API and put it in the ImageView",
                BaseTransientBottomBar.LENGTH_SHORT
        ).show();
    }

    /**
     * Invoked when the decrement button is clicked. It decrements the quantity of the value in
     * {@link #quantityTextInputEditText} by 1 without letting the quantity fall below 0.
     */
    private void onDecrementQuantityButtonClick() {
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
    private void onIncrementQuantityButtonClick() {
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
     * fields. If an invalidation if found, a snackbar error is shown and execution stops. If no
     * invalidation is found, it then either adds a product or updates a product, depending on this
     * activity's mode.
     */
    private void onSaveProductButtonClick() {

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

        if (name == null || price == null || quantity == null || supplier == null) {
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
     * Invoked when the activity started by {@link #takePictureActivityResultLauncher} finishes and
     * control returns to this activity. If the previous activity successfully snapped a picture,
     * it populates the image view with the picture.
     */
    private void onTakePictureActivityResult(boolean isSuccess) {
        if (isSuccess) {
            String takePictureFilePath = takePictureFile.getAbsolutePath();
            Bitmap takePictureBitmap = BitmapFactory.decodeFile(takePictureFilePath);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            takePictureBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            picture = outputStream.toByteArray();
            showImageInPhotoImageView(picture);
        } else {
            showSnackbar(R.string.take_picture_activity_failed_message);
        }
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
        } else {
            // Unsupported return class.
            return null;
        }
    }

    /**
     * Creates a new file in this app's private directory and returns an instance of it.
     *
     * @return A new {@link File} instance.
     */
    @SuppressLint("SimpleDateFormat")
    @NonNull
    private File createFile() {
        String timestamp = new SimpleDateFormat(FILE_TIMESTAMP_PATTERN).format(new Date());
        String fileName = String.format(FILE_NAME, timestamp);
        File fileDir = getFilesDir();
        return new File(fileDir, fileName);
    }

    /**
     * Displays a sample image resource in the given image view.
     *
     * @param id Used as a seed for picking a background color. Recommend to use this product's id
     *           to keep the background color consistent.
     */
    private void showSampleImageInPhotoImageView(int id) {
        photoImageView.setColorFilter(getColor(R.color.white));
        int backgroundColorIndex = id % sampleImageBackgroundColors.length;
        photoImageView.setBackgroundColor(sampleImageBackgroundColors[backgroundColorIndex]);

        photoImageView.setImageResource(R.drawable.ic_sample_image);
    }

    /**
     * Displays an image resource in the given image view.
     *
     * @param array {@code byte[]} representation of the image to display.
     */
    private void showImageInPhotoImageView(@NonNull byte[] array) {
        photoImageView.setColorFilter(null);
        photoImageView.setBackgroundColor(getColor(android.R.color.transparent));

        Bitmap bitmap = BitmapFactory.decodeByteArray(array, 0, array.length);
        photoImageView.setImageBitmap(bitmap);
    }
}
