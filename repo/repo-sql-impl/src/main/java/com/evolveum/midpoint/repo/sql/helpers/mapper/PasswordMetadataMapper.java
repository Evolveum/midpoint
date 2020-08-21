/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.Metadata;
import com.evolveum.midpoint.repo.sql.data.common.RFocus;
import com.evolveum.midpoint.repo.sql.data.factory.MetadataFactory;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;

/**
 * @author lskublik
 */
public class PasswordMetadataMapper implements Mapper<MetadataType, RFocus> {

    @Override
    public RFocus map(MetadataType input, MapperContext context) {
        RFocus focus = (RFocus) context.getOwner();
        if (input == null) {
            focus.setPasswordCreateTimestamp(null);
            focus.setModifyTimestamp(null);
        } else {
            focus.setPasswordCreateTimestamp(input.getCreateTimestamp());
            focus.setModifyTimestamp(input.getModifyTimestamp());
        }
        return focus;
    }
}
