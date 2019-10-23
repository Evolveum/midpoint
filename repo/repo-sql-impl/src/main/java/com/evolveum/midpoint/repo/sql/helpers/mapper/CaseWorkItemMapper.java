/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.repo.sql.data.common.RCase;
import com.evolveum.midpoint.repo.sql.data.common.container.RCaseWorkItem;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CaseWorkItemMapper extends ContainerMapper<CaseWorkItemType, RCaseWorkItem> {

    @Override
    public RCaseWorkItem map(CaseWorkItemType input, MapperContext context) {
        RCase owner = (RCase) context.getOwner();

        RCaseWorkItem item;
        try {
            item = RCaseWorkItem.toRepo(owner, input, context.getRepositoryContext());
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate CaseWorkItemType to entity", ex);
        }

        return item;
    }
}
