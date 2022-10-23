package com.davidread.clothescatalog2.view;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType;
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly;
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

import com.davidread.clothescatalog2.BuildConfig;
import com.davidread.clothescatalog2.R;
import com.davidread.clothescatalog2.database.ProductContract;
import com.davidread.clothescatalog2.database.ProductProviderUtils;
import com.davidread.clothescatalog2.util.RegexTextWatcher;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Provides a user interface for viewing and editing a particular product. It is for a new product
 * if {@link #selectedProductUri} is {@code null}. Otherwise, it is for an existing product.
 */
public class DetailActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Tag to use for logs in this class.
     */
    private static final String TAG = DetailActivity.class.getSimpleName();

    /**
     * Regular expressions that each text field should be matched with to be valid.
     */
    private static final String NAME_PATTERN = "^.{1,250}$";
    private static final String PRICE_PATTERN = "^\\d{1,7}(|[.]\\d{1,2})$";
    private static final String QUANTITY_PATTERN = "^\\d{1,9}$";
    private static final String SUPPLIER_PATTERN = "^.{1,250}$";
    private static final String SUPPLIER_PHONE_NUMBER_PATTERN = Patterns.PHONE.toString();
    private static final String SUPPLIER_EMAIL_PATTERN = Patterns.EMAIL_ADDRESS.toString();

    /**
     * Format of a phone number uri for a phone intent.
     */
    private static final String PHONE_NUMBER_URI_FORMAT = "tel:%1$s";

    /**
     * Format of an email address uri for an email intent.
     */
    private static final String EMAIL_URI_FORMAT = "mailto:";

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
     * Whether the loader has already done an initial load of data. Keeping track of this stops
     * subsequent loads from occurring when this activity is navigated to and from.
     */
    private boolean isLoadDone;

    /**
     * Contains background colors to apply onto a sample image for
     * {@link #showSampleImageInPhotoImageView(int)}.
     */
    private int[] sampleImageBackgroundColors;

    /**
     * Unique id corresponding with this product.
     */
    private int id;

    /**
     * A path to the file containing the image corresponding with this product. If {@code null},
     * then this product has no picture.
     */
    private String picturePath;

    /**
     * Launches an activity to the camera to capture an image for the product.
     */
    private ActivityResultLauncher<Uri> takePictureActivityResultLauncher;

    /**
     * File path to the image captured by the camera in the activity started by
     * {@link #takePictureActivityResultLauncher}.
     */
    private String takePicturePath;

    /**
     * Launches an activity to pick an image for the product.
     */
    private ActivityResultLauncher<PickVisualMediaRequest> pickVisualMediaActivityResultLauncher;

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
    private TextInputEditText supplierPhoneNumberTextInputEditText;
    private TextInputEditText supplierEmailTextInputEditText;

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

        isLoadDone = false;

        sampleImageBackgroundColors = getResources().getIntArray(R.array.sample_image_backgrounds);

        takePictureActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                this::onTakePictureActivityResult
        );

        pickVisualMediaActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                this::onPickVisualMediaActivityResult
        );

        detailCoordinatorLayout = findViewById(R.id.detail_coordinator_layout);

        photoImageView = findViewById(R.id.photo_image_view);
        photoImageView.setScaleType(ImageView.ScaleType.CENTER);

        nameTextInputEditText = findViewById(R.id.name_text_input_edit_text);
        priceTextInputEditText = findViewById(R.id.price_text_input_edit_text);
        quantityTextInputEditText = findViewById(R.id.quantity_text_input_edit_text);
        supplierTextInputEditText = findViewById(R.id.supplier_text_input_edit_text);
        supplierPhoneNumberTextInputEditText = findViewById(
                R.id.supplier_phone_number_text_input_edit_text
        );
        supplierEmailTextInputEditText = findViewById(R.id.supplier_email_text_input_edit_text);

        TextInputLayout nameTextInputLayout = findViewById(R.id.name_text_input_layout);
        nameTextInputEditText.addTextChangedListener(new RegexTextWatcher(
                NAME_PATTERN,
                getString(R.string.text_invalid_error_message),
                nameTextInputLayout
        ));
        TextInputLayout priceTextInputLayout = findViewById(R.id.price_text_input_layout);
        priceTextInputEditText.addTextChangedListener(new RegexTextWatcher(
                PRICE_PATTERN,
                getString(R.string.price_invalid_error_message),
                priceTextInputLayout
        ));
        TextInputLayout quantityTextInputLayout = findViewById(R.id.quantity_text_input_layout);
        quantityTextInputEditText.addTextChangedListener(new RegexTextWatcher(
                QUANTITY_PATTERN,
                getString(R.string.quantity_invalid_error_message),
                quantityTextInputLayout
        ));
        TextInputLayout supplierTextInputLayout = findViewById(R.id.supplier_text_input_layout);
        supplierTextInputEditText.addTextChangedListener(new RegexTextWatcher(
                SUPPLIER_PATTERN,
                getString(R.string.text_invalid_error_message),
                supplierTextInputLayout
        ));
        TextInputLayout supplierPhoneNumberTextInputLayout = findViewById(
                R.id.supplier_phone_number_text_input_layout
        );
        supplierPhoneNumberTextInputEditText.addTextChangedListener(new RegexTextWatcher(
                SUPPLIER_PHONE_NUMBER_PATTERN,
                getString(R.string.phone_number_invalid_error_message),
                supplierPhoneNumberTextInputLayout
        ));
        TextInputLayout supplierEmailTextInputLayout = findViewById(
                R.id.supplier_email_text_input_layout
        );
        supplierEmailTextInputEditText.addTextChangedListener(new RegexTextWatcher(
                SUPPLIER_EMAIL_PATTERN,
                getString(R.string.email_invalid_error_message),
                supplierEmailTextInputLayout
        ));

        Button changePhotoButton = findViewById(R.id.change_photo_button);
        changePhotoButton.setOnClickListener((view) -> onChangePhotoButtonClick());
        Button decrementQuantityButton = findViewById(R.id.decrement_quantity_button);
        decrementQuantityButton.setOnClickListener((view) -> onDecrementQuantityButtonClick());
        TooltipCompat.setTooltipText(decrementQuantityButton, getString(R.string.decrement_quantity_button_tooltip));
        Button incrementQuantityButton = findViewById(R.id.increment_quantity_button);
        incrementQuantityButton.setOnClickListener((view) -> onIncrementQuantityButtonClick());
        TooltipCompat.setTooltipText(incrementQuantityButton, getString(R.string.increment_quantity_button_tooltip));
        Button callSupplierButton = findViewById(R.id.call_supplier_button);
        callSupplierButton.setOnClickListener((view) -> onCallSupplierButtonClick());
        Button emailSupplierButton = findViewById(R.id.email_supplier_button);
        emailSupplierButton.setOnClickListener((view) -> onEmailSupplierButtonClick());

        FloatingActionButton saveProductButton = findViewById(R.id.save_product_button);
        saveProductButton.setOnClickListener((view) -> onSaveProductButtonClick());
        TooltipCompat.setTooltipText(saveProductButton, getString(R.string.save_product_button_tooltip));

        if (selectedProductUri == null) {
            // Put UI in add product mode.
            setTitle(R.string.add_product_title);
            showSampleImageInPhotoImageView(id);
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
                ProductContract.ProductEntry.COLUMN_SUPPLIER_PHONE_NUMBER,
                ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL,
                ProductContract.ProductEntry.COLUMN_PICTURE_PATH
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
        if (isLoadDone || !data.moveToFirst()) {
            // Cursor has already been loaded or Cursor is empty.
            return;
        }

        int idColumnIndex = data.getColumnIndex(ProductContract.ProductEntry._ID);
        int nameColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_NAME);
        int quantityColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_QUANTITY);
        int supplierColumnIndex = data.getColumnIndex(ProductContract.ProductEntry.COLUMN_SUPPLIER);
        int supplierPhoneNumberColumnIndex = data.getColumnIndex(
                ProductContract.ProductEntry.COLUMN_SUPPLIER_PHONE_NUMBER
        );
        int supplierEmailColumnIndex = data.getColumnIndex(
                ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL
        );
        int picturePathColumnIndex = data.getColumnIndex(
                ProductContract.ProductEntry.COLUMN_PICTURE_PATH
        );

        id = data.getInt(idColumnIndex);
        String name = data.getString(nameColumnIndex);
        String price = ProductProviderUtils.getDecimalFormatPrice(data);
        String quantity = data.getString(quantityColumnIndex);
        String supplier = data.getString(supplierColumnIndex);
        String supplierPhoneNumber = data.getString(supplierPhoneNumberColumnIndex);
        String supplierEmail = data.getString(supplierEmailColumnIndex);
        picturePath = data.getString(picturePathColumnIndex);

        nameTextInputEditText.setText(name);
        priceTextInputEditText.setText(price);
        quantityTextInputEditText.setText(quantity);
        supplierTextInputEditText.setText(supplier);
        supplierPhoneNumberTextInputEditText.setText(supplierPhoneNumber);
        supplierEmailTextInputEditText.setText(supplierEmail);
        if (picturePath == null) {
            // Show sample image.
            showSampleImageInPhotoImageView(id);
        } else {
            // Show stored image.
            showImageInPhotoImageView(picturePath);
        }

        isLoadDone = true;
    }

    /**
     * Invoked when a previously created loader is being reset, thus invalidating its dataset. It
     * populates the image view with a sample image and resets the text fields.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        nameTextInputEditText.setText("");
        priceTextInputEditText.setText("");
        quantityTextInputEditText.setText("");
        supplierTextInputEditText.setText("");
        supplierPhoneNumberTextInputEditText.setText("");
        supplierEmailTextInputEditText.setText("");
        picturePath = null;
        showSampleImageInPhotoImageView(id);
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
     * camera to take a photo. It puts the photo file in {@link #takePicturePath} and invokes
     * {@link #onTakePictureActivityResult(boolean)} when done.
     */
    private void onTakeNewPhotoButtonClick() {
        File file = createNewInternalFile();
        takePicturePath = file.getAbsolutePath();
        Uri takePictureUri = FileProvider.getUriForFile(
                this,
                FILE_PROVIDER_AUTHORITY,
                file
        );
        takePictureActivityResultLauncher.launch(takePictureUri);
    }

    /**
     * Invoked when the remove photo button is clicked. It clears the stored photo for this product
     * and shows the sample image in the UI.
     */
    private void onRemovePhotoButtonClick() {
        picturePath = null;
        showSampleImageInPhotoImageView(id);
    }

    /**
     * Invoked when the select new photo button is clicked. It launches an intent to the device's
     * gallery to pick a photo. It returns a URI to the selected photo in
     * {@link #onPickVisualMediaActivityResult(Uri)} when done.
     */
    private void onSelectNewPhotoButtonClick() {
        VisualMediaType mediaType = (VisualMediaType) ImageOnly.INSTANCE;
        PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                .setMediaType(mediaType)
                .build();
        pickVisualMediaActivityResultLauncher.launch(request);
    }

    /**
     * Invoked when the decrement button is clicked. It decrements the quantity of the value in
     * {@link #quantityTextInputEditText} by 1 without letting the quantity fall below 0.
     */
    private void onDecrementQuantityButtonClick() {
        Integer quantity = extractValueFromEditText(
                quantityTextInputEditText,
                null,
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
     * {@link #quantityTextInputEditText} by 1 without letting the quantity fall above 999,999,999.
     */
    private void onIncrementQuantityButtonClick() {
        Integer quantity = extractValueFromEditText(
                quantityTextInputEditText,
                null,
                Integer.class
        );
        if (quantity == null || quantity >= 999999999) {
            // No quantity was in the text field or quantity cannot be incremented any further.
            return;
        }
        quantity++;
        String quantityString = Integer.toString(quantity);
        quantityTextInputEditText.setText(quantityString);
    }

    /**
     * Invoked when the call supplier button is clicked. It starts an implicit intent to the
     * device's phone app to call the supplier phone number associated with this product.
     */
    private void onCallSupplierButtonClick() {
        String supplierPhoneNumber = extractValueFromEditText(
                supplierPhoneNumberTextInputEditText,
                SUPPLIER_PHONE_NUMBER_PATTERN,
                String.class
        );
        if (supplierPhoneNumber == null) {
            // Phone number could not be extracted or is not a valid phone number.
            showSnackbar(R.string.check_form_message);
            return;
        }
        String uriString = String.format(PHONE_NUMBER_URI_FORMAT, supplierPhoneNumber);
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse(uriString));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showSnackbar(R.string.no_phone_app_message);
            Log.e(TAG, e.toString());
        }
    }

    /**
     * Invoked when the email supplier button is clicked. It starts an implicit intent to the
     * device's email app to draft an email to the supplier email associated with this product.
     */
    private void onEmailSupplierButtonClick() {
        String name = extractValueFromEditText(
                nameTextInputEditText,
                NAME_PATTERN,
                String.class
        );
        Integer quantity = extractValueFromEditText(
                quantityTextInputEditText,
                QUANTITY_PATTERN,
                Integer.class
        );
        String supplierEmail = extractValueFromEditText(
                supplierEmailTextInputEditText,
                SUPPLIER_EMAIL_PATTERN,
                String.class
        );
        if (name == null || quantity == null || supplierEmail == null) {
            // A value could not be extracted or the value did not match its regular expression.
            showSnackbar(R.string.check_form_message);
            return;
        }
        String subject = getString(R.string.email_supplier_subject, name);
        String message = getString(R.string.email_supplier_message, name, quantity);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setData(Uri.parse(EMAIL_URI_FORMAT));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{supplierEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showSnackbar(R.string.no_email_app_message);
            Log.e(TAG, e.toString());
        }
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
        Double price = extractValueFromEditText(
                priceTextInputEditText,
                PRICE_PATTERN,
                Double.class
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
        String supplierPhoneNumber = extractValueFromEditText(
                supplierPhoneNumberTextInputEditText,
                SUPPLIER_PHONE_NUMBER_PATTERN,
                String.class
        );
        String supplierEmail = extractValueFromEditText(
                supplierEmailTextInputEditText,
                SUPPLIER_EMAIL_PATTERN,
                String.class
        );

        if (name == null
                || price == null
                || quantity == null
                || supplier == null
                || supplierPhoneNumber == null
                || supplierEmail == null
        ) {
            // A value could not be extracted or the value did not match its regular expression.
            showSnackbar(R.string.check_form_message);
            return;
        }

        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_NAME, name);
        ProductProviderUtils.putPrice(values, price);
        values.put(ProductContract.ProductEntry.COLUMN_QUANTITY, quantity);
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER, supplier);
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER_PHONE_NUMBER, supplierPhoneNumber);
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER_EMAIL, supplierEmail);
        values.put(ProductContract.ProductEntry.COLUMN_PICTURE_PATH, picturePath);

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
     *
     * @param isSuccess Whether a picture was successfully snapped.
     */
    private void onTakePictureActivityResult(boolean isSuccess) {
        if (!isSuccess) {
            return;
        }
        picturePath = takePicturePath;
        showImageInPhotoImageView(picturePath);
    }

    /**
     * Invoked when the activity started by {@link #pickVisualMediaActivityResultLauncher} finishes
     * and control returns to this activity. If the previous activity successfully picked a picture,
     * it populates the image view with the picture.
     *
     * @param uri URI of the picked picture.
     */
    private void onPickVisualMediaActivityResult(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        File file = copyImageUriToNewInternalFile(uri);
        picturePath = file.getAbsolutePath();
        showImageInPhotoImageView(picturePath);
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
     * @param <T>         Class type to convert the extracted value to. Accepts only {@link String},
     *                    {@link Integer}, and {@link Double} so far.
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
        } else if (returnClass == Double.class) {
            try {
                // Return value as Double.
                double textDouble = Double.parseDouble(textString);
                return returnClass.cast(textDouble);
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
    private File createNewInternalFile() {
        String timestamp = new SimpleDateFormat(FILE_TIMESTAMP_PATTERN).format(new Date());
        String fileName = String.format(FILE_NAME, timestamp);
        File fileDir = getFilesDir();
        return new File(fileDir, fileName);
    }

    /**
     * Copies the data at a given URI onto a new file in this app's private directory.
     *
     * @param uri URI to copy from.
     * @return A new {@link File} instance.
     */
    @NonNull
    private File copyImageUriToNewInternalFile(@NonNull Uri uri) {
        File file = createNewInternalFile();
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
        return file;
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
     * @param filePath Path to the file containing the image to display.
     */
    private void showImageInPhotoImageView(@NonNull String filePath) {
        photoImageView.setColorFilter(null);
        photoImageView.setBackgroundColor(getColor(android.R.color.transparent));

        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        photoImageView.setImageBitmap(bitmap);
    }
}
