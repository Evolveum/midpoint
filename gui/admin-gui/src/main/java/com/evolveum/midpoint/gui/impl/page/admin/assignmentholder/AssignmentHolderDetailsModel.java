/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

public class AssignmentHolderDetailsModel<AH extends AssignmentHolderType> extends ObjectDetailsModels<AH> {


    public AssignmentHolderDetailsModel(LoadableModel<PrismObject<AH>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);
    }

    @Override
    protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {
        GuiObjectDetailsPageType defaultPageConfig = super.loadDetailsPageConfiguration();

        AH modelObject = getPrismObject().asObjectable();
        List<ObjectReferenceType> archetypes = modelObject.getArchetypeRef();

        OperationResult result = new OperationResult("mergeArchetypeConfig");
        return getAdminGuiConfigurationMergeManager().mergeObjectDetailsPageConfiguration(defaultPageConfig, archetypes, result);
    }
}
