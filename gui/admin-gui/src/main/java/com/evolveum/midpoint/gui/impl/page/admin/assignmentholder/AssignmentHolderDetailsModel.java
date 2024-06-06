/*
 * Copyright (C) 2021-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder;

import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;

import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;

public class AssignmentHolderDetailsModel<AH extends AssignmentHolderType> extends ObjectDetailsModels<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentHolderDetailsModel.class);

    public AssignmentHolderDetailsModel(LoadableDetachableModel<PrismObject<AH>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);
    }

    @Override
    protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {
        GuiObjectDetailsPageType defaultPageConfig = super.loadDetailsPageConfiguration();

        return applyArchetypePolicy(defaultPageConfig);
    }

    protected GuiObjectDetailsPageType applyArchetypePolicy(GuiObjectDetailsPageType defaultPageConfig) {
        OperationResult result = new OperationResult("mergeArchetypeConfig");
        PrismObject<AH> assignmentHolder = getPrismObject();
        try {
            ArchetypePolicyType archetypePolicyType = getModelServiceLocator().getModelInteractionService().determineArchetypePolicy(assignmentHolder, result);
            return getAdminGuiConfigurationMergeManager().mergeObjectDetailsPageConfiguration(defaultPageConfig, archetypePolicyType, result);
        } catch (SchemaException | ConfigurationException e) {
            LOGGER.error("Cannot merge details page configuration from archetype policy, {}", e.getMessage(), e);
            return defaultPageConfig;
        }
    }

    public PageAssignmentHolderDetails<AH, AssignmentHolderDetailsModel<AH>> getPageAssignmentHolder() {
        return (PageAssignmentHolderDetails<AH, AssignmentHolderDetailsModel<AH>>) super.getPageBase();
    }
}
