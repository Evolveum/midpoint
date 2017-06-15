/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.home.dto;

import com.evolveum.midpoint.web.component.util.Selectable;

/**
 * @author lazyman
 */
public class PasswordAccountDto extends Selectable implements Comparable<PasswordAccountDto> {

    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_RESOURCE_NAME = "resourceName";
    public static final String F_ENABLED = "enabled";

    private String oid;
    private String displayName;
    private String resourceName;
    private String cssClass = "";
    private boolean enabled;
    private boolean passwordOutbound;
    private boolean passwordCapabilityEnabled;
    /**
     * true if this DTO represents default midpoint account;
     */
    private boolean midpoint;

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
    public int compareTo(PasswordAccountDto o) {
        if (o == null) {
            return 1;
        }

        if (isMidpoint() != o.isMidpoint()) {
            return isMidpoint() ? -1 : 1;
        }

        int value = compareString(getResourceName(), o.getResourceName());
        if (value != 0) {
            return value;
        }

        return compareString(getDisplayName(), o.getDisplayName());
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
