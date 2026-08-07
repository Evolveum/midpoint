/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard;

import java.util.List;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevObjectClassInfoType;

/**
 * Connector-type-specific decisions used by the connector development wizard (REST, SCIM, SQL),
 * so step panels delegate here instead of branching on the connector's integration type
 * themselves. Resolved via {@link ConnectorDevelopmentWizardUtil#wizardStrategyFor(ConnectorDevelopmentDetailsModel)}.
 * Each {@code *ObjectClassSteps} method returns the full children step list for the matching
 * router panel, mirroring {@code ConnectorDevelopmentBackend}'s per-type abstract methods on the
 * model layer.
 */
public interface ConnectorWizardStrategy {

    List<WizardStep> connectionSteps(WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper);

    List<WizardStep> initObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel);

    List<WizardStep> searchAllObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel);

    List<WizardStep> searchByIdObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel);

    List<WizardStep> searchFilterObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel);

    List<WizardStep> createObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel);

    List<WizardStep> updateObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel);

    List<WizardStep> deleteObjectClassSteps(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> objectClassModel);

    /** Config property holding the connection base URL for this connector type. */
    ItemName connectionUrlFieldName();
}
