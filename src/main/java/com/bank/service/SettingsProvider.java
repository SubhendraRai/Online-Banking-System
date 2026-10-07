package com.bank.service;

import java.math.BigDecimal;

/**
 * Strategy contract providing strongly-typed access to global system configuration parameters.
 * <p>
 * Decouples services from direct database DAO queries and provides fallback defaults
 * when specific configuration entries are missing or uninitialized.
 * </p>
 */
public interface SettingsProvider {

    /**
     * Retrieves a configuration setting as a string.
     *
     * @param key configuration parameter key
     * @param defaultValue fallback string if key is absent or blank
     * @return configured or fallback string value
     */
    String getString(String key, String defaultValue);

    /**
     * Retrieves a configuration setting as a monetary {@link BigDecimal}.
     *
     * @param key configuration parameter key
     * @param defaultValue fallback value if key is absent or invalid
     * @return configured or fallback monetary value
     */
    BigDecimal getDecimal(String key, BigDecimal defaultValue);

    /**
     * Retrieves a configuration setting as an integer.
     *
     * @param key configuration parameter key
     * @param defaultValue fallback value if key is absent or invalid
     * @return configured or fallback int value
     */
    int getInt(String key, int defaultValue);

    /**
     * Retrieves a configuration setting as a long integer.
     *
     * @param key configuration parameter key
     * @param defaultValue fallback value if key is absent or invalid
     * @return configured or fallback long value
     */
    long getLong(String key, long defaultValue);

    /**
     * Retrieves a configuration setting as a boolean.
     *
     * @param key configuration parameter key
     * @param defaultValue fallback value if key is absent or invalid
     * @return configured or fallback boolean value
     */
    boolean getBoolean(String key, boolean defaultValue);
}
