/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;

/**
 * @author lazyman
 */
@JaxbType(type = ActivationStatusType.class)
public enum RActivationStatus implements SchemaEnum<ActivationStatusType> {

    ENABLED(ActivationStatusType.ENABLED),

    DISABLED(ActivationStatusType.DISABLED),

    ARCHIVED(ActivationStatusType.ARCHIVED);

    private ActivationStatusType status;

    RActivationStatus(ActivationStatusType status) {
        this.status = status;
    }

    @Override
    public ActivationStatusType getSchemaValue() {
        return status;
    }
}
