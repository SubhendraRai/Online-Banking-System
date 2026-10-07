package com.bank.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a global system configuration setting.
 * <p>
 * Corresponds to the {@code system_settings} table in the database.
 * Holds key-value configuration pairs governing limits, thresholds,
 * interest rates, and system operational modes.
 * </p>
 */
public class SystemSetting {

    private String key;
    private String value;
    private String description;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    /**
     * Default constructor.
     */
    public SystemSetting() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Parameterized constructor for setting initialization.
     *
     * @param key unique setting key identifier
     * @param value configuration value string
     * @param description human-readable explanation of setting
     */
    public SystemSetting(String key, String value, String description) {
        this(key, value, description, null, LocalDateTime.now());
    }

    /**
     * Full constructor initializing all system setting attributes.
     *
     * @param key unique setting key identifier
     * @param value configuration value string
     * @param description human-readable explanation of setting
     * @param updatedBy administrator user identifier who last modified this setting
     * @param updatedAt timestamp of last modification
     */
    public SystemSetting(String key, String value, String description, Long updatedBy, LocalDateTime updatedAt) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemSetting that = (SystemSetting) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "SystemSetting{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", description='" + description + '\'' +
                ", updatedBy=" + updatedBy +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
