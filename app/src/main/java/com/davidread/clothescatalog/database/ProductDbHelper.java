package com.davidread.clothescatalog.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A helper class for the SQLite database that stores the products table. It helps with database
 * creation and version management.
 */
public class ProductDbHelper extends SQLiteOpenHelper {

    /**
     * File name for the database.
     */
    private static final String DB_NAME = "products.db";

    /**
     * Version for the database schema. Is constant since the schema will not be upgraded.
     */
    private static final int DB_VERSION = 1;


    /**
     * Constructs a new {@link ProductDbHelper}.
     *
     * @param context For the superclass.
     */
    public ProductDbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Callback invoked when the database is created for the first time. It initializes the database
     * by creating a new products table.
     *
     * @param db The database being created.
     */
    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        final String SQL_CREATE_PRODUCTS_TABLE = "CREATE TABLE " + ProductContract.ProductEntry.TABLE_NAME + " ("
                + ProductContract.ProductEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ProductContract.ProductEntry.COLUMN_NAME + " TEXT NOT NULL, "
                + ProductContract.ProductEntry.COLUMN_PRICE + " INTEGER NOT NULL DEFAULT 0, "
                + ProductContract.ProductEntry.COLUMN_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                + ProductContract.ProductEntry.COLUMN_SUPPLIER + " TEXT NOT NULL, "
                + ProductContract.ProductEntry.COLUMN_PICTURE + " BLOB);";
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE);
    }

    /**
     * Callback invoked when the database schema is upgraded. It does nothing, since the schema will
     * not be upgraded.
     *
     * @param db         The database being upgraded.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
