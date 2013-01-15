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
public class PasswordAccountDto extends Selectable {

    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_RESOURCE_NAME = "resourceName";
    public static final String F_ENABLED = "enabled";

    private String oid;
    private String displayName;
    private String resourceName;
    private boolean enabled;

    public PasswordAccountDto(String oid, String displayName, String resourceName, boolean enabled) {
        this.displayName = displayName;
        this.resourceName = resourceName;
        this.enabled = enabled;
        this.oid = oid;
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
}
