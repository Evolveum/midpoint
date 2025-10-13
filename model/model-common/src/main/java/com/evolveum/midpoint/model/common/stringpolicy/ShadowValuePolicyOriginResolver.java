/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.stringpolicy;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
public class ShadowValuePolicyOriginResolver extends AbstractValuePolicyOriginResolver<ShadowType> {

    public ShadowValuePolicyOriginResolver(PrismObject<ShadowType> object, ObjectResolver objectResolver) {
        super(object, objectResolver);
    }

    @Override
    public ObjectQuery getOwnerQuery() {
        return PrismContext.get()
                .queryFor(UserType.class)
                .item(UserType.F_LINK_REF).ref(getObject().getOid())
                .build();
    }
}
