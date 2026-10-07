package com.bank.service.fakes;

import com.bank.service.SettingsProvider;
import com.bank.util.Money;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory test double for {@link SettingsProvider}.
 */
public class FakeSettingsProvider implements SettingsProvider {

    private final Map<String, String> settings = new ConcurrentHashMap<>();

    public void put(String key, String value) {
        if (key != null && value != null) {
            settings.put(key, value);
        }
    }

    @Override
    public String getString(String key, String defaultValue) {
        return settings.getOrDefault(key, defaultValue);
    }

    @Override
    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String val = settings.get(key);
        if (val == null) return defaultValue;
        try {
            return Money.of(new BigDecimal(val));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String val = settings.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public long getLong(String key, long defaultValue) {
        String val = settings.get(key);
        if (val == null) return defaultValue;
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String val = settings.get(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val);
    }

    public void clear() {
        settings.clear();
    }
}
