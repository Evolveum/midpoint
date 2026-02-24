/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.services;

import java.io.Serial;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * @author Viliam Repan
 */
public abstract class PageApplicationServices extends PageServices {

    @Serial private static final long serialVersionUID = 1L;

    @Override
    protected SerializableSupplier<ObjectQuery> createObjectQuerySupplier() {
        return () ->
                PrismContext.get().queryFor(ServiceType.class)
                        .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_APPLICATIONS.value())
                        .build();
    }
}
