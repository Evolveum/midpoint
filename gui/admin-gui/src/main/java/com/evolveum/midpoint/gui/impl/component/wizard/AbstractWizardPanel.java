/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;

import java.util.Collection;

/**
 * @author lskublik
 */
public abstract class AbstractWizardPanel<C extends Containerable, AHD extends AssignmentHolderDetailsModel> extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractWizardPanel.class);

    private static final String ID_FRAGMENT = "fragment";
    private static final String ID_CHOICE_FRAGMENT = "choiceFragment";
    private static final String ID_CHOICE_PANEL = "choicePanel";
    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_WIZARD = "wizard";

    private final WizardPanelHelper<C, AHD> helper;
    private final boolean startWithChoiceTemplate;

    public AbstractWizardPanel(
            String id,
            WizardPanelHelper<C, AHD> helper) {
        super(id);
        this.helper = helper;
        startWithChoiceTemplate = initStartWithChoiceTemplate();
    }

    protected boolean initStartWithChoiceTemplate() {
        return nameOfObjectIsNotNull();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    /**
     * Define if will be showed introductory selection some options.
     */
    protected final boolean isStartWithChoiceTemplate() {
        return startWithChoiceTemplate;
    }

    protected abstract void initLayout();

    protected Fragment createChoiceFragment(Component choicePanel) {
        Fragment fragment = new Fragment(ID_FRAGMENT, ID_CHOICE_FRAGMENT, AbstractWizardPanel.this);
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
        AbstractWizardPanel.this.replace(fragment);
        target.add(fragment);
    }

    protected Fragment createWizardFragment(Component wizardPanel) {
        Fragment fragment = new Fragment(ID_FRAGMENT, ID_WIZARD_FRAGMENT, AbstractWizardPanel.this);
        fragment.setOutputMarkupId(true);
        MidpointForm mainForm = new MidpointForm(ID_MAIN_FORM);
        mainForm.setMultiPart(true);
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
        helper.onExitPerformed(target);
    }

    public AHD getAssignmentHolderModel() {
        return helper.getDetailsModel();
    }

    public IModel<PrismContainerValueWrapper<C>> getValueModel() {
        return helper.getValueModel();
    }

    protected OperationResult onSavePerformed(AjaxRequestTarget target) {
        return helper.onSaveObjectPerformed(target);
    }

    public WizardPanelHelper<C, AHD> getHelper() {
        return helper;
    }

    private boolean nameOfObjectIsNotNull() {
        if (getHelper().getValueModel() == null) {
            return true;
        }

        PrismContainerValueWrapper<C> value = getHelper().getValueModel().getObject();
        if (value == null) {
            return true;
        }

        C bean = value.getRealValue();
        if (!(bean instanceof ObjectType)) {
            return true;
        }

        if (((ObjectType) bean).getName() == null) {
            return true;
        }

        return false;
    }

    protected final void showUnsavedChangesToast(AjaxRequestTarget target){
        if (getValueModel() != null) {
            try {
                Collection<?> deltas = getValueModel().getObject().getDeltas();
                if (!deltas.isEmpty()) {
                    WebComponentUtil.showToastForRecordedButUnsavedChanges(target, getValueModel().getObject());
                }
            } catch (SchemaException e) {
                LOGGER.error("Couldn't collect deltas from " + getValueModel().getObject(), e);
            }
        }
    }

    protected void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = getHelper().onSaveObjectPerformed(target);
        if (!result.isError()) {
            exitToPreview(target);
        }
    }

    protected void exitToPreview(AjaxRequestTarget target) {
        getHelper().onExitPerformed(target);
    }

}
