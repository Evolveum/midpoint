/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormContainerHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormDefaultContainerablePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyValuePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismReferenceValuePanel;
import com.evolveum.midpoint.prism.Containerable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public abstract class AbstractValueFormResourceWizardStepPanel<C extends Containerable> extends AbstractResourceWizardStepPanel {

    private static final String ID_HEADER = "header";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";

    private static final String ID_VALUE = "value";
    private final IModel<PrismContainerValueWrapper<C>> newValueModel;

    public AbstractValueFormResourceWizardStepPanel(
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<C>> newValueModel) {
        super(model);
        this.newValueModel = newValueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.setOutputMarkupId(true);
        add(header);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getIcon()));
        header.add(icon);

        header.add(new Label(ID_TITLE, getModel()));

//        VerticalFormContainerHeaderPanel header = new VerticalFormContainerHeaderPanel(ID_HEADER, getTitle()) {
//            @Override
//            protected String getIcon() {
//                return AbstractValueFormResourceWizardStepPanel.this.getIcon();
//            }
//        };
//        add(header);

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler()).build();
        VerticalFormDefaultContainerablePanel<C> panel
                = new VerticalFormDefaultContainerablePanel<C>(ID_VALUE, newValueModel, settings);
        add(panel);
    }

    protected ItemVisibilityHandler getVisibilityHandler() {
        return null;
    }

    protected String getIcon() {
        return "fa fa-circle";
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        getValuePanel().visitChildren(VerticalFormPrismPropertyValuePanel.class, (component, objectIVisit) -> {
            ((VerticalFormPrismPropertyValuePanel) component).updateFeedbackPanel(target);
        });
        getValuePanel().visitChildren(VerticalFormPrismReferenceValuePanel.class, (component, objectIVisit) -> {
            ((VerticalFormPrismReferenceValuePanel) component).updateFeedbackPanel(target);
        });
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    private VerticalFormDefaultContainerablePanel getValuePanel() {
        return (VerticalFormDefaultContainerablePanel) get(ID_VALUE);
    }

    protected IModel<PrismContainerValueWrapper<C>> getValueModel() {
        return newValueModel;
    }
}
