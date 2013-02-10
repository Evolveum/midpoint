/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
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
    private boolean enabled;
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
        if (s1 == s2) {
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
