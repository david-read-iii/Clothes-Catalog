package com.davidread.clothescatalog.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A class that defines a {@link ContentProvider} for products data. Data is provided to
 * applications through implementing the {@link android.content.ContentResolver} interface.
 */
public class ProductProvider extends ContentProvider {

    /**
     * URI matcher code for a content URI that refers to all products.
     */
    private static final int URI_CODE_ALL_PRODUCTS = 100;

    /**
     * URI matcher code for a content URI that refers to a single product.
     */
    private static final int URI_CODE_SINGLE_PRODUCT = 101;

    /**
     * Matches a content URI to a URI matcher code.
     */
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initialization of {@link #uriMatcher}.
    static {
        uriMatcher.addURI(
                ProductContract.CONTENT_AUTHORITY,
                ProductContract.PATH_PRODUCTS, URI_CODE_ALL_PRODUCTS
        );
        uriMatcher.addURI(
                ProductContract.CONTENT_AUTHORITY,
                ProductContract.PATH_PRODUCTS + "/#", URI_CODE_SINGLE_PRODUCT
        );
    }

    /**
     * Gets SQLite database references.
     */
    private ProductDbHelper productDbHelper;

    /**
     * Callback invoked on this content provider's startup. It initializes {@link #productDbHelper}.
     *
     * @return True if the provider was successfully loaded, false otherwise.
     */
    @Override
    public boolean onCreate() {
        productDbHelper = new ProductDbHelper(getContext());
        return true;
    }

    /**
     * Returns the MIME type of the data stored in this content provider that the given content URI
     * refers to.
     *
     * @param uri The content URI to query.
     * @return The MIME type of the data. Returns {@code null} if the content URI does not refer to
     * data stored in this content provider or if it just corresponds to data with no MIME type.
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int match = uriMatcher.match(uri);
        switch (match) {
            case URI_CODE_ALL_PRODUCTS:
                return ProductContract.ProductEntry.CONTENT_LIST_TYPE;
            case URI_CODE_SINGLE_PRODUCT:
                return ProductContract.ProductEntry.CONTENT_ITEM_TYPE;
            default:
                return null;
        }
    }

    /**
     * Inserts a new product into this content provider. Registered observers will be notified of
     * the insertion.
     *
     * @param uri    Content URI of the insertion request.
     * @param values A set of column name/value pairs to add to the database.
     * @return The content URI for the newly inserted item. Is {@code null} if the insert request
     * fails.
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @NonNull ContentValues values) {

        // Return null if ContentValues are invalid.
        if (values.size() != 5 || !hasValidContentValues(values)) {
            return null;
        }

        // Perform insert operation.
        SQLiteDatabase db = productDbHelper.getWritableDatabase();
        long insertId;
        int match = uriMatcher.match(uri);
        switch (match) {
            case URI_CODE_ALL_PRODUCTS:
                insertId = db.insert(
                        ProductContract.ProductEntry.TABLE_NAME,
                        null,
                        values
                );
                break;
            default:
                insertId = -1;
        }

        // Return null if the insertion operation failed.
        if (insertId == -1) {
            return null;
        }

        // Notify listeners of insertion.
        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, insertId);
    }

    /**
     * Query products from this content provider. The return {@link Cursor} is registered to listen
     * for changes in the content provider.
     *
     * @param uri           Content URI of the query request.
     * @param projection    List of columns to put into the {@link Cursor}. If {@code null} then all
     *                      columns are included.
     * @param selection     A selection criteria to apply when filtering rows. If {@code null} then
     *                      all rows are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values
     *                      from selectionArgs, in order that they appear in the selection. The
     *                      values will be bound as {@link String}s.
     * @param sortOrder     How the rows in the cursor should be sorted. If {@code null} then the
     *                      content provider default sort order is used.
     * @return A {@link Cursor} containing product data according to the query request. If
     * {@code null} then the query request failed.
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        // Perform query operation.
        Cursor cursor;
        SQLiteDatabase db = productDbHelper.getReadableDatabase();
        int match = uriMatcher.match(uri);
        switch (match) {
            case URI_CODE_ALL_PRODUCTS:
                cursor = db.query(
                        ProductContract.ProductEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case URI_CODE_SINGLE_PRODUCT:
                selection = ProductContract.ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(
                        ProductContract.ProductEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                cursor = null;
        }

        // Setup listener that will keep this Cursor and this content provider's data in sync.
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    /**
     * Update products in this content provider. Registered observers will be notified of the
     * update.
     *
     * @param uri           Content URI of the update request.
     * @param values        A set of column name/value pairs to update in the database.
     * @param selection     A selection criteria to apply when filtering rows. If {@code null} then
     *                      all rows are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values
     *                      from selectionArgs, in order that they appear in the selection. The
     *                      values will be bound as {@link String}s.
     * @return The number of rows updated. Is {@code -1} if the update request failed.
     */
    @Override
    public int update(@NonNull Uri uri, @NonNull ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {

        // Return 0 if ContentValues is empty.
        if (values.size() == 0) {
            return 0;
        }

        // Return -1 if ContentValues are invalid.
        if (!hasValidContentValues(values)) {
            return -1;
        }

        // Perform update operation.
        int countRowsUpdated;
        SQLiteDatabase db = productDbHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        switch (match) {
            case URI_CODE_ALL_PRODUCTS:
                countRowsUpdated = db.update(
                        ProductContract.ProductEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            case URI_CODE_SINGLE_PRODUCT:
                selection = ProductContract.ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                countRowsUpdated = db.update(
                        ProductContract.ProductEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs
                );
                break;
            default:
                countRowsUpdated = -1;
        }

        // Notify listeners of update.
        if (countRowsUpdated > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return countRowsUpdated;
    }

    /**
     * Delete products from this content provider. Registered observers will be notified of this
     * deletion.
     *
     * @param uri           Content URI of the delete request.
     * @param selection     A selection criteria to apply when filtering rows. If {@code null} then
     *                      all rows are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values
     *                      from selectionArgs, in order that they appear in the selection. The
     *                      values will be bound as {@link String}s.
     * @return The number of rows deleted. Is {@code -1} if the delete request failed.
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {

        // Perform the delete operation.
        int countRowsDeleted;
        SQLiteDatabase db = productDbHelper.getWritableDatabase();
        int match = uriMatcher.match(uri);
        switch (match) {
            case URI_CODE_ALL_PRODUCTS:
                countRowsDeleted = db.delete(
                        ProductContract.ProductEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            case URI_CODE_SINGLE_PRODUCT:
                selection = ProductContract.ProductEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                countRowsDeleted = db.delete(
                        ProductContract.ProductEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            default:
                countRowsDeleted = -1;
        }

        // Notify listeners of delete.
        if (countRowsDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return countRowsDeleted;
    }

    /**
     * Returns whether a {@link ContentValues} has valid data that may be stored in this content
     * provider.
     *
     * @param values {@link ContentValues} to query.
     * @return True if the {@link ContentValues} are valid.
     */
    private boolean hasValidContentValues(@NonNull ContentValues values) {

        // Name column must be a nonempty String.
        if (values.containsKey(ProductContract.ProductEntry.COLUMN_NAME)) {
            Object name = values.get(ProductContract.ProductEntry.COLUMN_NAME);
            if (!(name instanceof String)
                    || ((String) name).isEmpty()) {
                return false;
            }
        }

        // Price column must be a non-negative Integer.
        if (values.containsKey(ProductContract.ProductEntry.COLUMN_PRICE)) {
            Object price = values.get(ProductContract.ProductEntry.COLUMN_PRICE);
            if (!(price instanceof Integer)
                    || ((Integer) price) < 0) {
                return false;
            }
        }

        // Quantity column must be a non-negative Integer.
        if (values.containsKey(ProductContract.ProductEntry.COLUMN_QUANTITY)) {
            Object quantity = values.get(ProductContract.ProductEntry.COLUMN_QUANTITY);
            if (!(quantity instanceof Integer)
                    || ((Integer) quantity) < 0) {
                return false;
            }
        }

        // Supplier column must be a nonempty String.
        if (values.containsKey(ProductContract.ProductEntry.COLUMN_SUPPLIER)) {
            Object supplier = values.get(ProductContract.ProductEntry.COLUMN_SUPPLIER);
            if (!(supplier instanceof String)
                    || ((String) supplier).isEmpty()) {
                return false;
            }
        }

        // Picture column must be either null or a byte array.
        if (values.containsKey(ProductContract.ProductEntry.COLUMN_PICTURE)) {
            Object picture = values.get(ProductContract.ProductEntry.COLUMN_PICTURE);
            if (picture != null && !(picture instanceof byte[])) {
                return false;
            }
        }

        return true;
    }
}
