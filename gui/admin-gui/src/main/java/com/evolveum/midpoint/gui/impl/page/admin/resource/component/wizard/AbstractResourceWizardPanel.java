/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * @author lskublik
 */
public abstract class AbstractResourceWizardPanel<C extends Containerable> extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractResourceWizardPanel.class);

    private static final String ID_FRAGMENT = "fragment";
    private static final String ID_CHOICE_FRAGMENT = "choiceFragment";
    private static final String ID_CHOICE_PANEL = "choicePanel";
    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_WIZARD = "wizard";

    private final ResourceDetailsModel resourceModel;

    public AbstractResourceWizardPanel(
            String id,
            ResourceDetailsModel model) {
        super(id);
        this.resourceModel = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected abstract void initLayout();

    protected Fragment createChoiceFragment(Component choicePanel) {
        Fragment fragment = new Fragment(ID_FRAGMENT, ID_CHOICE_FRAGMENT, AbstractResourceWizardPanel.this);
        fragment.setOutputMarkupId(true);
        choicePanel.setOutputMarkupId(true);
        fragment.add(choicePanel);
        return fragment;
    }

    protected String getIdOfChoicePanel() {
        return ID_CHOICE_PANEL;
    }

    protected void showChoiceFragment(AjaxRequestTarget target, Component choicePanel) {
        showFragment(target, createChoiceFragment(choicePanel));
    }

    private void showFragment(AjaxRequestTarget target, Fragment fragment) {
        AbstractResourceWizardPanel.this.replace(fragment);
        target.add(fragment);
    }

    protected Fragment createWizardFragment(Component wizardPanel) {
        Fragment fragment = new Fragment(ID_FRAGMENT, ID_WIZARD_FRAGMENT, AbstractResourceWizardPanel.this);
        fragment.setOutputMarkupId(true);
        Form mainForm = new Form(ID_MAIN_FORM);
        fragment.add(mainForm);
        wizardPanel.setOutputMarkupId(true);
        mainForm.add(wizardPanel);
        return fragment;
    }

    protected String getIdOfWizardPanel() {
        return ID_WIZARD;
    }

    protected void showWizardFragment(AjaxRequestTarget target, Component wizardPanel) {
        showFragment(target, createWizardFragment(wizardPanel));
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
    }

    protected IModel<PrismContainerValueWrapper<C>> createModelOfNewValue(ItemPath path) {
        return new IModel() {

            private PrismContainerValueWrapper<C> newItemWrapper;

            @Override
            public Object getObject() {
                if (newItemWrapper == null) {
                    try {
                        PrismContainerWrapper<C> container = findContainer(path);
                        PrismContainerValue<C> newItem = container.getItem().createNewValue();
                        newItemWrapper = WebPrismUtil.createNewValueWrapper(
                                container, newItem, getPageBase(), getWrapperContext(container));
                        container.getValues().add(newItemWrapper);
                    } catch (SchemaException e) {
                        LOGGER.error("Cannot find wrapper: {}", e.getMessage());
                    }
                }
                return newItemWrapper;
            }
        };
    }

    private WrapperContext getWrapperContext(PrismContainerWrapper<C> container) {
        WrapperContext context = getResourceModel().createWrapperContext();
        context.setObjectStatus(container.findObjectStatus());
        context.setShowEmpty(true);
        context.setCreateIfEmpty(true);
        return context;
    }

    protected PrismContainerWrapper<C> findContainer(ItemPath path) throws SchemaException {
        return getResourceModel().getObjectWrapper().findContainer(path);
    }

    public ResourceDetailsModel getResourceModel() {
        return resourceModel;
    }

    protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
        return null;
    }
}
