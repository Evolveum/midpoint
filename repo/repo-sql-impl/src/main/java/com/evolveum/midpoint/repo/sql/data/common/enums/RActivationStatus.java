/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
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
        RUtil.register(this);
    }

    @Override
    public ActivationStatusType getSchemaValue() {
        return status;
    }
}
