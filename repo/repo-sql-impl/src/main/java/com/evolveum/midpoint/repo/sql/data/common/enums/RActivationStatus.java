package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;

/**
 * @author lazyman
 */
@JaxbType(type = ActivationStatusType.class)
public enum RActivationStatus implements SchemaEnum<ActivationStatusType> {

    ENABLED(ActivationStatusType.ENABLED),

    DISABLED(ActivationStatusType.DISABLED),

    ARCHIVED(ActivationStatusType.ARCHIVED),

    ABSENT(ActivationStatusType.ABSENT);

    private ActivationStatusType status;

    private RActivationStatus(ActivationStatusType status) {
        this.status = status;
    }

    @Override
    public ActivationStatusType getSchemaValue() {
        return status;
    }
}
