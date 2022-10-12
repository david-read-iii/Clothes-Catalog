package com.davidread.clothescatalog.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputLayout;

/**
 * A concrete implementation of {@link TextWatcher} that may be attached to a
 * {@link com.google.android.material.textfield.TextInputEditText} to validate that contents
 * contains an email address. If not, then an error message will be shown on the field.
 */
public class EmailTextWatcher implements TextWatcher {

    /**
     * Error message to show when there is no match.
     */
    private final String errorMessage;

    /**
     * {@link TextInputLayout} to set the error on.
     */
    private final TextInputLayout textInputLayout;

    /**
     * Constructs a new {@link EmailTextWatcher}.
     *
     * @param errorMessage    Error message to show when there is no match
     * @param textInputLayout {@link TextInputLayout} to set the error on.
     */
    public EmailTextWatcher(@NonNull String errorMessage, @NonNull TextInputLayout textInputLayout) {
        this.errorMessage = errorMessage;
        this.textInputLayout = textInputLayout;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /**
     * Invoked each time the contents of
     * {@link com.google.android.material.textfield.TextInputEditText} changes. It validates the
     * contents and sets an error on {@link #textInputLayout} if there is no match. Otherwise, it
     * removes the error on it.
     *
     * @param s      {@link CharSequence} contained within the
     *               {@link com.google.android.material.textfield.TextInputEditText}.
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        boolean match = Patterns.EMAIL_ADDRESS.matcher(s).matches();
        if (match) {
            // Matches valid email address.
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
        } else {
            // Does not match valid email address.
            textInputLayout.setError(errorMessage);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
