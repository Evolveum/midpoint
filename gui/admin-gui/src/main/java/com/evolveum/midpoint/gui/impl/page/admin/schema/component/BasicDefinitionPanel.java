/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractValueFormResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormDefaultContainerablePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerValuePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.DefinitionType;

/**
 * @author lskublik
 */
public abstract class BasicDefinitionPanel<C extends Containerable>
        extends AbstractValueFormResourceWizardStepPanel<C, AssignmentHolderDetailsModel<SchemaType>> {


    public BasicDefinitionPanel(
            AssignmentHolderDetailsModel<SchemaType> model,
            IModel<PrismContainerValueWrapper<C>> newValueModel) {
        super(model, newValueModel, null);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        // To compensate big margin set on BasicWizardStepPanel root div
        add(AttributeAppender.append("class", "mt-n4"));
    }

    @Override
    public VisibleEnableBehaviour getBackBehaviour() {
        return new VisibleBehaviour(() -> false);
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new VisibleBehaviour(() -> false);
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isSubmitVisible() {
        return false;
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return createStringResource("OnePanelPopupPanel.button.done");
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        onExitPerformed(target);
    }

    @Override
    protected ItemVisibilityHandler getVisibilityHandler() {
        return wrapper -> {
            if (wrapper.getItemName().equals(DefinitionType.F_LIFECYCLE_STATE)) {
                return ItemVisibility.HIDDEN;
            }
            return ItemVisibility.AUTO;
        };
    }

    @Override
    protected void initLayout() {

        getValueModel().getObject().setExpanded(true);

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
                Component parent = get(
                        createComponentPath(
                                ID_VALUE_FORM,
                                ID_VALUE_CONTAINER,
                                ID_INPUT,
                                VerticalFormDefaultContainerablePanel.ID_PROPERTIES_LABEL,
                                VerticalFormDefaultContainerablePanel.ID_FORM_CONTAINER));
                if (parent != null) {
                    parent.add(AttributeAppender.replace("class", "p-0 mb-0"));
                }
                get(ID_VALUE_FORM).add(AttributeAppender.remove("class"));
            }

            @Override
            protected boolean isShowEmptyButtonVisible() {
                return false;
            }
        };
        parent.add(panel);
    }

    @Override
    public String appendCssToWizard() {
        return "mx-auto col-12";
    }
}
