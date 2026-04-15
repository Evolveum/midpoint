/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.services;

import java.io.Serial;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

public abstract class PageApplicationService extends PageService {

    @Serial private static final long serialVersionUID = 1L;

    public PageApplicationService() {
        super(createEmptyService());
    }

    private static PrismObject<ServiceType> createEmptyService() {
        ServiceType service = new ServiceType();
        service.beginAssignment()
                .targetRef(SystemObjectsType.ARCHETYPE_APPLICATION.value(), ArchetypeType.COMPLEX_TYPE);
        service.archetypeRef(SystemObjectsType.ARCHETYPE_APPLICATION.value(), ArchetypeType.COMPLEX_TYPE);

        return service.asPrismObject();
    }
}
