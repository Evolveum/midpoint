/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.util.Selectable;

/**
 * @author lazyman
 */
public class PasswordAccountDto extends Selectable<PasswordAccountDto>
        implements Comparable<PasswordAccountDto> {

    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_RESOURCE_NAME = "resourceName";
    public static final String F_ENABLED = "enabled";

    private final String oid;
    private final String displayName;
    private final String resourceName;
    private final boolean enabled;

    private String cssClass = "";
    private boolean passwordOutbound;
    private boolean passwordCapabilityEnabled;
    /**
     * true if this DTO represents default midpoint account;
     */
    private final boolean midpoint;

    public PasswordAccountDto(String oid, String displayName, String resourceName, boolean enabled) {
        this(oid, displayName, resourceName, enabled, false);
    }

    public PasswordAccountDto(String oid, String displayName, String resourceName, boolean enabled, boolean midpoint) {
        this.displayName = displayName;
        this.resourceName = resourceName;
        this.enabled = enabled;
        this.oid = oid;
        this.midpoint = midpoint;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getOid() {
        return oid;
    }

    public boolean isMidpoint() {
        return midpoint;
    }

    public boolean isPasswordOutbound() {
        return passwordOutbound;
    }

    public void setPasswordOutbound(boolean passwordOutbound) {
        this.passwordOutbound = passwordOutbound;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public boolean isPasswordCapabilityEnabled() {
        return passwordCapabilityEnabled;
    }

    public void setPasswordCapabilityEnabled(boolean passwordCapabilityEnabled) {
        this.passwordCapabilityEnabled = passwordCapabilityEnabled;
    }

    @Override
    public int compareTo(@NotNull PasswordAccountDto that) {
        // TODO by contract it is never null, remove in 2021 if you still see this
//        if (that == null) {
//            return 1;
//        }

        if (isMidpoint() != that.isMidpoint()) {
            return isMidpoint() ? -1 : 1;
        }

        int value = compareString(getResourceName(), that.getResourceName());
        if (value != 0) {
            return value;
        }

        return compareString(getDisplayName(), that.getDisplayName());
    }

    private int compareString(String s1, String s2) {
        if (s1 != null && s1.equals(s2)) {
            return 0;
        }

        if (s1 == null) {
            return -1;
        }

        if (s2 == null) {
            return 1;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
    }
}
