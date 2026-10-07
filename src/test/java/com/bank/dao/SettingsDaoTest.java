package com.bank.dao;

import com.bank.model.SystemSetting;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsDaoTest extends BaseDaoIntegrationTest {

    private SettingsDao settingsDao;

    @BeforeEach
    void setUp() {
        settingsDao = new SettingsDao();
    }

    @Test
    @DisplayName("findById retrieves seeded system setting accurately")
    void testFindById() {
        Optional<SystemSetting> setting = settingsDao.findById("transfer.daily_limit");
        assertTrue(setting.isPresent());
        assertEquals("100000.00", setting.get().getValue());
        assertEquals("Daily transfer limit", setting.get().getDescription());
    }

    @Test
    @DisplayName("getAll loads dictionary of all system configuration keys and values")
    void testGetAll() {
        Map<String, String> map = settingsDao.getAll();
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("50000.00", map.get("transfer.per_txn_limit"));
        assertEquals("100000.00", map.get("transfer.daily_limit"));
        assertEquals("500.00", map.get("savings.min_balance"));
    }

    @Test
    @DisplayName("save and update modify setting values and persist administrator audit reference")
    void testSaveAndUpdate() {
        // Save new setting
        SystemSetting newSetting = new SystemSetting("maintenance.mode", "false", "System operational state", 1L, null);
        settingsDao.save(newSetting);

        Optional<SystemSetting> fetched = settingsDao.findById("maintenance.mode");
        assertTrue(fetched.isPresent());
        assertEquals("false", fetched.get().getValue());

        // Update value with administrator auditing
        boolean updated = settingsDao.update("maintenance.mode", "true", 1L);
        assertTrue(updated);

        SystemSetting refreshed = settingsDao.findById("maintenance.mode").orElseThrow();
        assertEquals("true", refreshed.getValue());
        assertEquals(1L, refreshed.getUpdatedBy());
    }

    @Test
    @DisplayName("delete removes an existing configuration setting")
    void testDelete() {
        SystemSetting temp = new SystemSetting("temp.key", "tempVal", "Temporary key");
        settingsDao.save(temp);
        assertTrue(settingsDao.findById("temp.key").isPresent());

        boolean deleted = settingsDao.delete("temp.key");
        assertTrue(deleted);
        assertFalse(settingsDao.findById("temp.key").isPresent());
    }

    @Test
    @DisplayName("findAll returns full list of system settings")
    void testFindAll() {
        List<SystemSetting> all = settingsDao.findAll();
        assertEquals(3, all.size());
    }
}
