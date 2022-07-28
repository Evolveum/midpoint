/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.self.dto;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 * Created by honchar.
 */
public enum AssignmentViewType {

    ROLE_CATALOG_VIEW(SchemaConstants.OBJECT_COLLECTION_ROLE_CATALOG_URI, AbstractRoleType.class),
    ROLE_TYPE(SchemaConstants.OBJECT_COLLECTION_ALL_ROLES_URI, RoleType.class),
    ORG_TYPE(SchemaConstants.OBJECT_COLLECTION_ALL_ORGS_URI, OrgType.class),
    SERVICE_TYPE(SchemaConstants.OBJECT_COLLECTION_ALL_SERVICES_URI, ServiceType.class),
    USER_TYPE(SchemaConstants.OBJECT_COLLECTION_USER_ASSIGNMENTS_URI, AbstractRoleType.class);

    private String uri;
    private Class type;

    AssignmentViewType(String uri, Class type) {
        this.uri = uri;
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public Class getType() {
        return type;
    }

    public static AssignmentViewType getViewByUri(String uri) {
        if (uri == null) {
            return null;
        }

        for (AssignmentViewType a : values()) {
            if (uri.equals(a.getUri())) {
                return a;
            }
        }

        return null;
    }
}
