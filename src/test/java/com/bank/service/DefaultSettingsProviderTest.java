package com.bank.service;

import com.bank.dao.SettingsDao;
import com.bank.model.SystemSetting;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSettingsProviderTest {

    private SettingsDao fakeDao;
    private DefaultSettingsProvider provider;

    @BeforeEach
    void setUp() {
        fakeDao = new SettingsDao() {
            @Override
            public Optional<SystemSetting> findById(String key) {
                return switch (key) {
                    case "savings.min_balance" -> Optional.of(new SystemSetting(key, "500.00", "Min bal"));
                    case "session.timeout_minutes" -> Optional.of(new SystemSetting(key, "15", "Timeout"));
                    case "fraud.velocity_count" -> Optional.of(new SystemSetting(key, "5000000000", "Long val"));
                    case "maintenance.mode" -> Optional.of(new SystemSetting(key, "true", "Mode"));
                    case "invalid.decimal" -> Optional.of(new SystemSetting(key, "not_a_number", "Invalid"));
                    default -> Optional.empty();
                };
            }
        };

        provider = new DefaultSettingsProvider(fakeDao);
    }

    @Test
    @DisplayName("Typed getters parse configured values accurately")
    void testTypedGettersSuccess() {
        assertEquals("500.00", provider.getString("savings.min_balance", "100.00"));
        assertEquals(new BigDecimal("500.00"), provider.getDecimal("savings.min_balance", BigDecimal.ZERO));
        assertEquals(15, provider.getInt("session.timeout_minutes", 30));
        assertEquals(5000000000L, provider.getLong("fraud.velocity_count", 1L));
        assertTrue(provider.getBoolean("maintenance.mode", false));
    }

    @Test
    @DisplayName("Typed getters return fallback default when key is missing or unparseable")
    void testFallbackDefaults() {
        assertEquals("default_val", provider.getString("missing.key", "default_val"));
        assertEquals(new BigDecimal("250.00"), provider.getDecimal("missing.key", new BigDecimal("250.00")));
        assertEquals(new BigDecimal("999.00"), provider.getDecimal("invalid.decimal", new BigDecimal("999.00")));
        assertEquals(42, provider.getInt("missing.key", 42));
        assertEquals(100L, provider.getLong("missing.key", 100L));
        assertFalse(provider.getBoolean("missing.key", false));
    }
}
