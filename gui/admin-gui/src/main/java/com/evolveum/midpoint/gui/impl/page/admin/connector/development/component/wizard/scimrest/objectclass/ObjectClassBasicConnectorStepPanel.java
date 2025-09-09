/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.connector.development.ConnectorDevelopmentDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormDefaultContainerablePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerValuePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.EnumerationValueTypeDefinitionType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
@PanelInstance(identifier = "cdw-object-class-basic",
        applicableForType = ConnectorDevelopmentType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "PageConnectorDevelopment.wizard.step.objectClassBasic", icon = "fa fa-wrench"),
        containerPath = "empty")
public class ObjectClassBasicConnectorStepPanel
        extends AbstractValueFormResourceWizardStepPanel<ConnDevObjectClassInfoType, ConnectorDevelopmentDetailsModel> {

    public static final String PANEL_TYPE = "cdw-object-class-basic";

    public ObjectClassBasicConnectorStepPanel(
            WizardPanelHelper<? extends Containerable, ConnectorDevelopmentDetailsModel> helper,
            IModel<PrismContainerValueWrapper<ConnDevObjectClassInfoType>> newValueModel) {
        super(helper, newValueModel);
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.objectType.objectClassBasic");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.objectClassBasic.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.objectType.objectClassBasic.subText");
    }

    @Override
    protected ItemMandatoryHandler getMandatoryHandler() {
        return this::checkMandatory;
    }

    protected boolean checkMandatory(ItemWrapper itemWrapper) {
        if (itemWrapper.getItemName().equals(ConnDevObjectClassInfoType.F_NAME)) {
            return true;
        }
        return itemWrapper.isMandatory();
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(ConnDevObjectClassInfoType.F_NAME)
                    || wrapper.getItemName().equals(ConnDevObjectClassInfoType.F_DESCRIPTION)) {
                return ItemVisibility.AUTO;
            }
            return ItemVisibility.HIDDEN;
        };
    }

    @Override
    public String getStepId() {
        if (StringUtils.isNotEmpty(getValueModel().getObject().getRealValue().getName())) {
            return getPanelType() + "-" + StringUtils.normalizeSpace(getValueModel().getObject().getRealValue().getName());
        }
        return getPanelType();
    }

    @Override
    protected void initLayout() {
        getTextLabel().add(AttributeAppender.replace("class", "mb-3 h4 w-100"));
        getSubtextLabel().add(AttributeAppender.replace("class", "text-secondary pb-3 lh-2 border-bottom mb-3 w-100"));
        getButtonContainer().add(AttributeAppender.replace("class", "d-flex gap-3 justify-content-between mt-3 w-100"));
        getFeedback().add(AttributeAppender.replace("class", "col-12 feedbackContainer"));

        WebMarkupContainer parent = new WebMarkupContainer(ID_PARENT);
        parent.setOutputMarkupId(true);
        parent.add(AttributeAppender.replace("class", "col-12"));
        add(parent);

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler())
                .mandatoryHandler(getMandatoryHandler())
                .headerVisibility(false).build();
        settings.setConfig(getContainerConfiguration());

        VerticalFormPrismContainerValuePanel panel
                = new VerticalFormPrismContainerValuePanel(ID_VALUE, getValueModel(), settings){

            @Override
            protected void onInitialize() {
                super.onInitialize();
                ((VerticalFormDefaultContainerablePanel)getValuePanel()).getFormContainer().add(AttributeAppender.remove("class"));
                get(ID_VALUE_FORM).add(AttributeAppender.remove("class"));
            }

            @Override
            protected IModel<String> getLabelModel() {
                return ObjectClassBasicConnectorStepPanel.this.createLabelModel();
            }

            @Override
            protected boolean isShowEmptyButtonVisible() {
                return false;
            }

            @Override
            protected String getIcon() {
                return ObjectClassBasicConnectorStepPanel.this.getIcon();
            }

        };
        parent.add(panel);
    }

    @Override
    public String appendCssToWizard() {
        return "col-10";
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    @Override
    protected IModel<String> getNextLabelModel() {
        return null;
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }
}
