/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.Metadata;
import com.evolveum.midpoint.repo.sql.data.factory.MetadataFactory;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MetadataMapper implements Mapper<MetadataType, Metadata> {

    @Override
    public Metadata map(MetadataType input, MapperContext context) {
        Metadata metadata = (Metadata) context.getOwner();
        try {
            MetadataFactory.fromJaxb(input, metadata, context.getRelationRegistry());
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate metadata to entity");
        }

        return metadata;
    }
}
