/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RFocusActivation;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

public class FocusActivationMapper implements Mapper<ActivationType, RFocusActivation> {

    @Override
    public RFocusActivation map(ActivationType input, MapperContext context) {
        try {
            RFocusActivation activation = new RFocusActivation();
            RFocusActivation.fromJaxb(input, activation);

            return activation;
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate activation to entity", ex);
        }
    }
}
