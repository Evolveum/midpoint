/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;

/**
 * @author lazyman
 */
@JaxbType(type = AuthorizationDecisionType.class)
public enum RAuthorizationDecision implements SchemaEnum<AuthorizationDecisionType> {

    ALLOW(AuthorizationDecisionType.ALLOW),

    DENY(AuthorizationDecisionType.DENY);

    private AuthorizationDecisionType decision;

    RAuthorizationDecision(AuthorizationDecisionType decision) {
        this.decision = decision;
        RUtil.register(this);
    }

    @Override
    public AuthorizationDecisionType getSchemaValue() {
        return decision;
    }
}
