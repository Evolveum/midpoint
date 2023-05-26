/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author lazyman
 */
@JaxbType(type = ShadowKindType.class)
public enum RShadowKind implements SchemaEnum<ShadowKindType> {

    ACCOUNT(ShadowKindType.ACCOUNT),
    ENTITLEMENT(ShadowKindType.ENTITLEMENT),
    GENERIC(ShadowKindType.GENERIC),
    UNKNOWN(ShadowKindType.UNKNOWN);

    private ShadowKindType kind;

    RShadowKind(ShadowKindType kind) {
        this.kind = kind;
        RUtil.register(this);
    }

    @Override
    public ShadowKindType getSchemaValue() {
        return kind;
    }
}
