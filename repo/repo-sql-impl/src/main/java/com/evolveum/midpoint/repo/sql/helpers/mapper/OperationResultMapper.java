/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.ROperationResult;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import javax.xml.namespace.QName;

/**
 * Created by Viliam Repan (lazyman).
 */
public class OperationResultMapper implements Mapper<OperationResultType, ROperationResult> {

    @Override
    public ROperationResult map(OperationResultType input, MapperContext context) {
        ROperationResult repo = (ROperationResult) context.getOwner();

        try {
            RUtil.copyResultFromJAXB(new QName(SchemaConstantsGenerated.NS_COMMON, "result"), input, repo);
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate operation result to entity", ex);
        }

        return repo;
    }
}

