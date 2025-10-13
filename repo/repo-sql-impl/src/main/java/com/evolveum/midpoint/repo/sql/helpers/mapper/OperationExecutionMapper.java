/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.ROperationExecution;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class OperationExecutionMapper extends ContainerMapper<OperationExecutionType, ROperationExecution> {

    @Override
    public ROperationExecution map(OperationExecutionType input, MapperContext context) {
        ROperationExecution execution = new ROperationExecution();

        RObject owner = (RObject) context.getOwner();

        try {
            ROperationExecution.fromJaxb(input, execution, owner, context.getRepositoryContext());
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate trigger to entity", ex);
        }

        return execution;
    }
}
