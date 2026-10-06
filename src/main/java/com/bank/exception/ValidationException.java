package com.bank.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checked exception thrown when user input or entity fields fail validation rules.
 * <p>
 * Supports both single error descriptions and structured field-to-error mappings
 * for displaying precise validation feedback on web forms.
 * </p>
 */
public class ValidationException extends BankingException {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> fieldErrors;

    /**
     * Constructs a {@code ValidationException} with a single descriptive message.
     *
     * @param message description of the validation failure
     */
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.fieldErrors = Collections.emptyMap();
    }

    /**
     * Constructs a {@code ValidationException} associated with a specific field name.
     *
     * @param field the form or entity property name that failed validation
     * @param error the validation failure reason
     */
    public ValidationException(String field, String error) {
        super(ErrorCode.VALIDATION_FAILED, String.format("Validation failed for '%s': %s", field, error));
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(field, error);
        this.fieldErrors = Collections.unmodifiableMap(errors);
    }

    /**
     * Constructs a {@code ValidationException} with a collection of field errors.
     *
     * @param fieldErrors map of field names to their specific error messages
     */
    public ValidationException(Map<String, String> fieldErrors) {
        super(ErrorCode.VALIDATION_FAILED, "Input validation failed for submitted data.");
        this.fieldErrors = fieldErrors != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors))
                : Collections.emptyMap();
    }

    /**
     * Returns an unmodifiable map of field names to error messages.
     *
     * @return map of validation errors, or an empty map if none were supplied
     */
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
