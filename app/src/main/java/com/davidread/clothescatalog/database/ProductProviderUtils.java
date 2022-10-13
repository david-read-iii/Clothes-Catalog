package com.davidread.clothescatalog.database;

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public final class ProductProviderUtils {

    private ProductProviderUtils() {
        // Private constructor prevents accidental instantiation of this class.
    }

    /**
     * Puts a price from the UI into the given content values to store in the product provider.
     *
     * @param values Content values to put the price in.
     * @param price  Price directly from the UI.
     */
    public static void putPrice(@NonNull ContentValues values, double price) {
        int intPrice = (int) (price * 100);
        values.put(ProductContract.ProductEntry.COLUMN_PRICE, intPrice);
    }

    /**
     * Returns a price string in decimal format to show in the UI from a cursor from the product
     * provider. Returns {@code null} if no price is stored in the cursor.
     *
     * @param cursor Cursor already pointing at some row.
     * @return Price string in decimal format or {@code null}.
     */
    @Nullable
    public static String getDecimalFormatPrice(@NonNull Cursor cursor) {
        double doublePrice = getDoublePrice(cursor);
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        return doublePrice != -1
                ? decimalFormat.format(doublePrice)
                : null;
    }

    /**
     * Returns a price string in currency format to show in the UI from a cursor from the product
     * provider. Returns {@code null} if no price is stored in the cursor.
     *
     * @param cursor Cursor already pointing at some row.
     * @return Price string in currency format or {@code null}.
     */
    @Nullable
    public static String getCurrencyFormatPrice(@NonNull Cursor cursor) {
        double doublePrice = getDoublePrice(cursor);
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.US);
        return doublePrice != -1
                ? numberFormat.format(doublePrice)
                : null;
    }

    /**
     * Returns a price double from a cursor from the product provider. Returns {@code -1} if no
     * price is stored in the cursor.
     *
     * @param cursor Cursor already pointing at some row.
     * @return Price double or {@code -1}.
     */
    private static double getDoublePrice(@NonNull Cursor cursor) {
        int priceColumnIndex = cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRICE);
        if (priceColumnIndex == -1) {
            return -1;
        }
        int intPrice = cursor.getInt(priceColumnIndex);
        return (double) intPrice * 0.01;
    }
}