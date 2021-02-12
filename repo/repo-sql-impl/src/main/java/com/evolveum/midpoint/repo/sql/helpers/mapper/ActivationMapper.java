/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ActivationMapper implements Mapper<ActivationType, RActivation> {

    @Override
    public RActivation map(ActivationType input, MapperContext context) {
        try {
            RActivation ractivation = new RActivation();
            RActivation.fromJaxb(input, ractivation);

            return ractivation;
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate activation to entity", ex);
        }
    }
}
