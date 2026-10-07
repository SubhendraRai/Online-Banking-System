package com.bank.service;

import com.bank.dao.SettingsDao;
import com.bank.model.SystemSetting;
import com.bank.util.Money;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transitional implementation of {@link SettingsProvider} that queries configuration
 * parameters on demand directly from {@link SettingsDao}.
 * <p>
 * Ensures fault tolerance: if the database or key is unreachable, gracefully falls
 * back to the programmatic default values specified by callers.
 * </p>
 */
public class DefaultSettingsProvider implements SettingsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSettingsProvider.class);

    private final SettingsDao settingsDao;

    /**
     * Default constructor creating an unmanaged provider with standard {@link SettingsDao}.
     */
    public DefaultSettingsProvider() {
        this(new SettingsDao());
    }

    /**
     * Parameterized constructor allowing injection of custom or fake settings DAO.
     *
     * @param settingsDao DAO instance for configuration lookup
     */
    public DefaultSettingsProvider(SettingsDao settingsDao) {
        this.settingsDao = Objects.requireNonNull(settingsDao, "settingsDao cannot be null");
    }

    @Override
    public String getString(String key, String defaultValue) {
        if (key == null) {
            return defaultValue;
        }
        try {
            Optional<SystemSetting> setting = settingsDao.findById(key);
            if (setting.isPresent()) {
                String val = setting.get().getValue();
                if (val != null && !val.isBlank()) {
                    return val.trim();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed retrieving setting key '{}', using default '{}': {}", key, defaultValue, e.getMessage());
        }
        return defaultValue;
    }

    @Override
    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String val = getString(key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Money.of(new BigDecimal(val));
        } catch (Exception e) {
            LOGGER.warn("Invalid decimal format for setting key '{}' (value='{}'), using default '{}'", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String val = getString(key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            LOGGER.warn("Invalid integer format for setting key '{}' (value='{}'), using default '{}'", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public long getLong(String key, long defaultValue) {
        String val = getString(key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            LOGGER.warn("Invalid long format for setting key '{}' (value='{}'), using default '{}'", key, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = getString(key, null);
        if (val == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val);
    }
}
