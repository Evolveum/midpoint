/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.*;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.Collection;

/**
 * @author lskublik
 */
public abstract class AbstractValueFormResourceWizardStepPanel<C extends Containerable, ODM extends ObjectDetailsModels>
        extends AbstractWizardStepPanel<ODM> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractValueFormResourceWizardStepPanel.class);
    private static final String ID_VALUE = "value";
    private final IModel<PrismContainerValueWrapper<C>> newValueModel;
    private final IModel<? extends PrismContainerValueWrapper<?>> parentModelForAllSteps;

    public AbstractValueFormResourceWizardStepPanel(
            ODM model,
            IModel<PrismContainerValueWrapper<C>> newValueModel) {
        this(model, newValueModel, newValueModel);
    }

    public <P extends Containerable> AbstractValueFormResourceWizardStepPanel(
            ODM model,
            IModel<PrismContainerValueWrapper<C>> newValueModel,
            IModel<PrismContainerValueWrapper<P>> parentModelForAllSteps) {
        super(model);
        this.newValueModel = newValueModel;
        this.parentModelForAllSteps = parentModelForAllSteps;
        if (newValueModel != null) {
            newValueModel.getObject().setExpanded(true);
            newValueModel.getObject().setShowEmpty(true);
        }
    }

    protected <Con extends Containerable, T extends Containerable> IModel<PrismContainerValueWrapper<Con>> createNewValueModel(
            IModel<PrismContainerValueWrapper<T>> parentValue, ItemName itemName) {
        return new LoadableDetachableModel<>() {

            @Override
            protected PrismContainerValueWrapper<Con> load() {
                PrismContainerWrapperModel<T, Con> model
                        = PrismContainerWrapperModel.fromContainerValueWrapper(
                        parentValue,
                        itemName);
                if (model.getObject().getValues().isEmpty()) {
                    try {
                        PrismContainerValue<Con> newItem = model.getObject().getItem().createNewValue();
                        PrismContainerValueWrapper<Con> newItemWrapper = WebPrismUtil.createNewValueWrapper(
                                model.getObject(), newItem, getPageBase());
                        model.getObject().getValues().add(newItemWrapper);
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't create new value for limitation container", e);
                        return null;
                    }
                }
                PrismContainerValueWrapper<Con> newItemWrapper = model.getObject().getValues().get(0);
                newItemWrapper.setExpanded(true);
                newItemWrapper.setShowEmpty(true);
                return newItemWrapper;
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(getVisibilityHandler())
                .mandatoryHandler(getMandatoryHandler()).build();
        settings.setConfig(getContainerConfiguration());
        VerticalFormPrismContainerValuePanel panel
                = new VerticalFormPrismContainerValuePanel(ID_VALUE, getValueModel(), settings){

            @Override
            protected WebMarkupContainer createHeaderPanel() {
                return super.createHeaderPanel();
            }

            @Override
            protected LoadableDetachableModel<String> getLabelModel() {
                return AbstractValueFormResourceWizardStepPanel.this.createLabelModel();
            }

            @Override
            protected String getIcon() {
                return AbstractValueFormResourceWizardStepPanel.this.getIcon();
            }
        };
        add(panel);
    }

    protected LoadableDetachableModel<String> createLabelModel() {
        return (LoadableDetachableModel<String>) getTitle();
    }

    protected ItemMandatoryHandler getMandatoryHandler() {
        return null;
    }

    protected ContainerPanelConfigurationType getContainerConfiguration() {
        return WebComponentUtil.getContainerConfiguration(
                (GuiObjectDetailsPageType) this.getDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                getPanelType());
    }

    protected abstract String getPanelType();

    @Override
    public String getStepId() {
        return getPanelType();
    }

    protected ItemVisibilityHandler getVisibilityHandler() {
        return null;
    }

    protected String getIcon() {
        return "fa fa-circle";
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        target.add(getFeedback());
        getValuePanel().visitChildren(
                VerticalFormPrismPropertyValuePanel.class,
                (component, objectIVisit) -> ((VerticalFormPrismPropertyValuePanel) component).updateFeedbackPanel(target));
        getValuePanel().visitChildren(
                VerticalFormPrismReferenceValuePanel.class,
                (component, objectIVisit) -> ((VerticalFormPrismReferenceValuePanel) component).updateFeedbackPanel(target));
    }

    @Override
    protected boolean isExitButtonVisible() {
        return true;
    }

    private VerticalFormPrismContainerValuePanel getValuePanel() {
        return (VerticalFormPrismContainerValuePanel) get(ID_VALUE);
    }

    protected IModel<PrismContainerValueWrapper<C>> getValueModel() {
        return newValueModel;
    }

    protected void refresh(AjaxRequestTarget target) {
        target.add(get(ID_VALUE));
    }

    @Override
    protected void onExitPreProcessing(AjaxRequestTarget target) {
        if (parentModelForAllSteps != null) {
            try {
                Collection<?> deltas = parentModelForAllSteps.getObject().getDeltas();
                if (!deltas.isEmpty()) {
                    WebComponentUtil.showToastForRecordedButUnsavedChanges(target, parentModelForAllSteps.getObject());
                }
            } catch (SchemaException e) {
                LOGGER.error("Couldn't collect deltas from " + parentModelForAllSteps.getObject(), e);
            }
        }
    }
}
