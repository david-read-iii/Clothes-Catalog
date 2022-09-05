package com.davidread.clothescatalog.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.rule.provider.ProviderTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * This class tests the correctness of {@link ProductProvider}.
 */
public class ProductProviderTest {

    /**
     * To access the functions of {@link ProductProvider}.
     */
    private ContentResolver contentResolver;

    /**
     * To mock {@link #contentResolver}.
     */
    @Rule
    public ProviderTestRule providerTestRule =
            new ProviderTestRule.Builder(ProductProvider.class, ProductContract.CONTENT_AUTHORITY)
                    .build();

    /**
     * Callback invoked before each test. It initializes {@link #contentResolver}.
     */
    @Before
    public void setUp() {
        contentResolver = providerTestRule.getResolver();
    }

    /**
     * Verify that when a valid {@link ContentValues} is passed into
     * {@link ProductProvider#insert(Uri, ContentValues)}, it returns not {@code null}.
     */
    @Test
    public void insert_ValidValues_ReturnsNotNull() {

        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_NAME, "Red T-Shirt");
        values.put(ProductContract.ProductEntry.COLUMN_PRICE, 1000);
        values.put(ProductContract.ProductEntry.COLUMN_QUANTITY, 10);
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER, "Garment District");
        values.put(ProductContract.ProductEntry.COLUMN_PICTURE, new byte[]{0, 1, 2, 3});

        Uri insertUri = contentResolver.insert(ProductContract.ProductEntry.CONTENT_URI, values);

        assertNotNull(insertUri);
    }

    /**
     * Verify that when an invalid {@link ContentValues} is passed into
     * {@link ProductProvider#insert(Uri, ContentValues)}, it returns {@code null}.
     */
    @Test
    public void insert_InvalidValues_ReturnsNull() {

        // Verify that a row with invalid amount of attributes cannot be inserted.
        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_NAME, "Blue T-Shirt");

        Uri insertUri = contentResolver.insert(ProductContract.ProductEntry.CONTENT_URI, values);

        assertNull(insertUri);

        // Verify that a row with an invalid price cannot be inserted.
        ContentValues values1 = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_NAME, "Yellow T-Shirt");
        values.put(ProductContract.ProductEntry.COLUMN_PRICE, -23);
        values.put(ProductContract.ProductEntry.COLUMN_QUANTITY, 55);
        values.put(ProductContract.ProductEntry.COLUMN_SUPPLIER, "Bed Bath and Beyond");
        values.put(ProductContract.ProductEntry.COLUMN_PICTURE, new byte[]{4, 5, 6, 7});

        Uri insertUri1 = contentResolver.insert(ProductContract.ProductEntry.CONTENT_URI, values1);

        assertNull(insertUri1);
    }

    /**
     * Verify that when {@link ProductProvider#query(Uri, String[], String, String[], String)}
     * queries all rows, it returns not {@code null}.
     */
    @Test
    public void query_AllRows_ReturnsNotNull() {

        Cursor cursor = contentResolver.query(
                ProductContract.ProductEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        assertNotNull(cursor);
    }

    /**
     * Verify that when {@link ProductProvider#update(Uri, ContentValues, String, String[])} updates
     * all rows with valid {@link ContentValues}, it returns a non-error int.
     */
    @Test
    public void update_ValidValues_ReturnsNotError() {

        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_PICTURE, new byte[]{8, 9, 10, 11});

        int countRowsUpdated = contentResolver.update(
                ProductContract.ProductEntry.CONTENT_URI,
                values,
                null,
                null
        );

        assertNotEquals(-1, countRowsUpdated);
    }

    /**
     * Verify that when {@link ProductProvider#update(Uri, ContentValues, String, String[])} updates
     * all rows with an invalid {@link ContentValues}, it returns an error int.
     */
    @Test
    public void update_InvalidValues_ReturnsError() {

        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_PRICE, -616);

        int countRowsUpdated = contentResolver.update(
                ProductContract.ProductEntry.CONTENT_URI,
                values,
                null,
                null
        );

        assertEquals(-1, countRowsUpdated);
    }

    /**
     * Verifies that when {@link ProductProvider#delete(Uri, String, String[])} deletes all rows, it
     * returns a non-error int.
     */
    @Test
    public void delete_AllRows_ReturnNotError() {

        int countRowsDeleted = contentResolver.delete(
                ProductContract.ProductEntry.CONTENT_URI,
                null,
                null
        );

        assertNotEquals(-1, countRowsDeleted);
    }
}