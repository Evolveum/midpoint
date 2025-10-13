/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.RTrigger;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TriggerMapper extends ContainerMapper<TriggerType, RTrigger> {

    @Override
    public RTrigger map(TriggerType input, MapperContext context) {
        RTrigger trigger = new RTrigger();

        RObject owner = (RObject) context.getOwner();

        try {
            RTrigger.fromJaxb(input, trigger, owner, context.getRepositoryContext());
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate trigger to entity", ex);
        }

        return trigger;
    }
}
