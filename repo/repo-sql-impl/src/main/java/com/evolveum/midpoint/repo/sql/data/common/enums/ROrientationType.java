/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrientationType;

@JaxbType(type = OrientationType.class)
public enum ROrientationType implements SchemaEnum<OrientationType> {
    LANDSCAPE(OrientationType.LANDSCAPE),
    PORTRAIT(OrientationType.PORTRAIT);

    private final OrientationType type;

    ROrientationType(OrientationType type) {
        this.type = type;
    }

    @Override
    public OrientationType getSchemaValue() {
        return type;
    }
}
