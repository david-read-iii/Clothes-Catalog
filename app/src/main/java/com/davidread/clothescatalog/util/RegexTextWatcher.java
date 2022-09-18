package com.davidread.clothescatalog.util;

import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.PatternSyntaxException;

/**
 * A concrete implementation of {@link TextWatcher} that may be attached to a
 * {@link com.google.android.material.textfield.TextInputEditText} to validate its contents. This is
 * done by matching it to some regular expression. If not matched, then an error message will be
 * shown on the field.
 */
public class RegexTextWatcher implements TextWatcher {

    /**
     * Regular expression pattern to match.
     */
    private final String pattern;

    /**
     * Error message to show when there is no match.
     */
    private final String errorMessage;

    /**
     * {@link TextInputLayout} to set the error on.
     */
    private final TextInputLayout textInputLayout;

    /**
     * Constructs a new {@link RegexTextWatcher}.
     *
     * @param pattern         Regular expression pattern to match.
     * @param errorMessage    Error message to show when there is no match
     * @param textInputLayout {@link TextInputLayout} to set the error on.
     */
    public RegexTextWatcher(@NonNull String pattern, @NonNull String errorMessage, @NonNull TextInputLayout textInputLayout) {
        this.pattern = pattern;
        this.errorMessage = errorMessage;
        this.textInputLayout = textInputLayout;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /**
     * Invoked each time the contents of
     * {@link com.google.android.material.textfield.TextInputEditText} changes. It validates the
     * contents against the regular expression {@link #pattern} and sets an error on
     * {@link #textInputLayout} if there is no match. Otherwise, it removes the error on it.
     *
     * @param s      {@link CharSequence} contained within the
     *               {@link com.google.android.material.textfield.TextInputEditText}.
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        boolean match;
        try {
            match = s.toString().matches(pattern);
        } catch (PatternSyntaxException e) {
            throw new IllegalStateException("pattern == " + pattern + " is not a valid regular expression");
        }
        if (match) {
            // Matches regular expression.
            textInputLayout.setError(null);
            textInputLayout.setErrorEnabled(false);
        } else {
            // Does not match regular expression.
            textInputLayout.setError(errorMessage);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
