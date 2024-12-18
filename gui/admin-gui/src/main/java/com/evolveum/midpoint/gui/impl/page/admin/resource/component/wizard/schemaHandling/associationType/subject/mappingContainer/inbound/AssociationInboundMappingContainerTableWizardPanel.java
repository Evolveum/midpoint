/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.inbound;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.AbstractAssociationMappingContainerTableWizardPanel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

@PanelType(name = "rw-association-mappings")
@PanelInstance(identifier = "rw-association-inbounds",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "MappingContainerWizardPanel.inboundTable", icon = "fa fa-arrow-right-to-bracket"))
public abstract class AssociationInboundMappingContainerTableWizardPanel extends AbstractAssociationMappingContainerTableWizardPanel {

    public AssociationInboundMappingContainerTableWizardPanel(String id, WizardPanelHelper<ShadowAssociationDefinitionType, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }

    protected ItemName getItemNameForMappingContainer(){
        return ShadowAssociationDefinitionType.F_INBOUND;
    }

    @Override
    protected String getAddButtonLabelKey() {
        return "AssociationInboundMappingContainerTableWizardPanel.addButtonLabel";
    }

    protected UserProfileStorage.TableId getTableId(){
        return UserProfileStorage.TableId.PANEL_ASSOCIATION_INBOUND;
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("AssociationInboundMappingContainerTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("AssociationInboundMappingContainerTableWizardPanel.subText");
    }

    @Override
    protected String getTitleIconClass() {
        return "fa fa-arrow-right-to-bracket";
    }

    @Override
    protected IModel<String> getExitLabel() {
        if(getHelper().getExitLabel() != null) {
            return getHelper().getExitLabel();
        }
        return super.getExitLabel();
    }

    @Override
    protected void postProcessNewMapping(PrismContainerValue<MappingType> newValue) throws SchemaException {
        newValue.asContainerable().beginExpression();
        ExpressionUtil.updateAssociationSynchronizationExpressionValue(
                newValue.asContainerable().getExpression(),
                new AssociationSynchronizationExpressionEvaluatorType());
    }
}
