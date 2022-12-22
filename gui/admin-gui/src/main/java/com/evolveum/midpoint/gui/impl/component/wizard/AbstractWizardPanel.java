/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * @author lskublik
 */
public abstract class AbstractWizardPanel<C extends Containerable, AHD extends AssignmentHolderDetailsModel> extends BasePanel {

    private static final String ID_FRAGMENT = "fragment";
    private static final String ID_CHOICE_FRAGMENT = "choiceFragment";
    private static final String ID_CHOICE_PANEL = "choicePanel";
    private static final String ID_WIZARD_FRAGMENT = "wizardFragment";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_WIZARD = "wizard";

    private final WizardPanelHelper<C, AHD> helper;

    public AbstractWizardPanel(
            String id,
            WizardPanelHelper<C, AHD> helper) {
        super(id);
        this.helper = helper;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
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
        helper.onExitPerformed(target);
    }

    public AHD getAssignmentHolderModel() {
        return helper.getDetailsModel();
    }

    public IModel<PrismContainerValueWrapper<C>> getValueModel() {
        return helper.getValueModel();
    }

    protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
        return helper.onSaveResourcePerformed(target);
    }

    protected boolean isSavedAfterWizard() {
        return helper.isSavedAfterWizard();
    }

    public WizardPanelHelper<C, AHD> getHelper() {
        return helper;
    }
}
