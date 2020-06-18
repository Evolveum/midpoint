/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
