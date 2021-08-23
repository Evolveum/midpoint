/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.List;

public abstract class PageAssignmentHolder<AH extends AssignmentHolderType> extends AbstractPageObject<AH> {

    public PageAssignmentHolder(PageParameters pageParameters) {
        super(pageParameters);
    }

    @Override
    protected GuiObjectDetailsPageType loadPageConfiguration(PrismObject<AH> prismObject) {
        AH modelObject = prismObject.asObjectable();
        List<ObjectReferenceType> archetypes = modelObject.getArchetypeRef();

        GuiObjectDetailsPageType defaultPageConfig = getCompiledGuiProfile().findObjectDetailsConfiguration(getType());

        OperationResult result = new OperationResult("mergeArchetypeConfig");
        return getAdminGuiConfigurationMergeManager().mergeObjectDetailsPageConfiguration(defaultPageConfig, archetypes, result);
    }

}
