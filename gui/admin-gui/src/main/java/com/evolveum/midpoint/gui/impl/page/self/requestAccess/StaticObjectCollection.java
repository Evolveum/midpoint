/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public enum StaticObjectCollection {

    ALL_ROLES(SchemaConstants.OBJECT_COLLECTION_ALL_ROLES_QNAME, RoleType.class),

    ALL_SERVICES(SchemaConstants.OBJECT_COLLECTION_ALL_SERVICES_QNAME, ServiceType.class),

    ALL_ORGS(SchemaConstants.OBJECT_COLLECTION_ALL_ORGS_QNAME, OrgType.class),

    USER_ASSIGNMENTS(SchemaConstants.OBJECT_COLLECTION_USER_ASSIGNMENTS_QNAME, AbstractRoleType.class),

    ROLE_CATALOG_VIEW(SchemaConstants.OBJECT_COLLECTION_ROLE_CATALOG_QNAME, AbstractRoleType.class);

    private final QName uri;

    private final Class<? extends ObjectType> type;

    StaticObjectCollection(QName uri, Class<? extends ObjectType> type) {
        this.uri = uri;
        this.type = type;
    }

    public QName getUri() {
        return uri;
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public String getStringUri() {
        return QNameUtil.qNameToUri(uri);
    }

    public static StaticObjectCollection findCollection(String identifier) {
        if (identifier == null) {
            return null;
        }

        for (StaticObjectCollection collection : values()) {
            if (identifier.equals(collection.getUri().getLocalPart())) {
                return collection;
            }

            if (identifier.equals(collection.getStringUri())) {
                return collection;
            }
        }

        return null;
    }
}
