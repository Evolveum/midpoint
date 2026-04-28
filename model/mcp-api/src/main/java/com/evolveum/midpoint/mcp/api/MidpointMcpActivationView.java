/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

/**
 * JSON-friendly snapshot of midPoint {@code ActivationType} for MCP explain/search responses.
 * All fields are optional; {@code null} means unknown or not set in the source object.
 */
public class MidpointMcpActivationView {

    private String administrativeStatus;
    private String effectiveStatus;
    private String validityStatus;
    private String validFrom;
    private String validTo;
    private String lockoutStatus;
    private String disableReason;
    private String enableTimestamp;
    private String disableTimestamp;
    private String archiveTimestamp;
    private String validityChangeTimestamp;
    private String lockoutExpirationTimestamp;

    public String getAdministrativeStatus() {
        return administrativeStatus;
    }

    public void setAdministrativeStatus(String administrativeStatus) {
        this.administrativeStatus = administrativeStatus;
    }

    public String getEffectiveStatus() {
        return effectiveStatus;
    }

    public void setEffectiveStatus(String effectiveStatus) {
        this.effectiveStatus = effectiveStatus;
    }

    public String getValidityStatus() {
        return validityStatus;
    }

    public void setValidityStatus(String validityStatus) {
        this.validityStatus = validityStatus;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(String validFrom) {
        this.validFrom = validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public void setValidTo(String validTo) {
        this.validTo = validTo;
    }

    public String getLockoutStatus() {
        return lockoutStatus;
    }

    public void setLockoutStatus(String lockoutStatus) {
        this.lockoutStatus = lockoutStatus;
    }

    public String getDisableReason() {
        return disableReason;
    }

    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
    }

    public String getEnableTimestamp() {
        return enableTimestamp;
    }

    public void setEnableTimestamp(String enableTimestamp) {
        this.enableTimestamp = enableTimestamp;
    }

    public String getDisableTimestamp() {
        return disableTimestamp;
    }

    public void setDisableTimestamp(String disableTimestamp) {
        this.disableTimestamp = disableTimestamp;
    }

    public String getArchiveTimestamp() {
        return archiveTimestamp;
    }

    public void setArchiveTimestamp(String archiveTimestamp) {
        this.archiveTimestamp = archiveTimestamp;
    }

    public String getValidityChangeTimestamp() {
        return validityChangeTimestamp;
    }

    public void setValidityChangeTimestamp(String validityChangeTimestamp) {
        this.validityChangeTimestamp = validityChangeTimestamp;
    }

    public String getLockoutExpirationTimestamp() {
        return lockoutExpirationTimestamp;
    }

    public void setLockoutExpirationTimestamp(String lockoutExpirationTimestamp) {
        this.lockoutExpirationTimestamp = lockoutExpirationTimestamp;
    }
}
