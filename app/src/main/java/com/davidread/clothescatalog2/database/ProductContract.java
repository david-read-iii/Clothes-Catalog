package com.davidread.clothescatalog2.database;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * A class that defines constants to help work with content URIs, column names, and other features
 * of the content provider.
 */
public final class ProductContract {

    /**
     * Unique identifier for the content provider.
     */
    public static final String CONTENT_AUTHORITY = "com.davidread.clothescatalog";

    /**
     * Base content URI to refer to data in the content provider.
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Path to append to {@link #BASE_CONTENT_URI} to refer to the products table.
     */
    public static final String PATH_PRODUCTS = "products";

    private ProductContract() {
        // Private constructor prevents accidental instantiation of this class.
    }

    /**
     * A class that defines constants to help work with data in the products table.
     */
    public static class ProductEntry implements BaseColumns {

        /**
         * Content URI to refer to data in the products table.
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_PRODUCTS);

        /**
         * MIME type of a single piece of data in the products table.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /**
         * MIME type of a list of data in the products table.
         */
        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_PRODUCTS;

        /**
         * Table name of the products table.
         */
        public static final String TABLE_NAME = "products";

        // Column names of the products table.
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_PRICE = "price";
        public static final String COLUMN_QUANTITY = "quantity";
        public static final String COLUMN_SUPPLIER = "supplier";
        public static final String COLUMN_SUPPLIER_PHONE_NUMBER = "supplier_phone_number";
        public static final String COLUMN_SUPPLIER_EMAIL = "supplier_email";
        public static final String COLUMN_PICTURE_PATH = "picture_path";
    }
}