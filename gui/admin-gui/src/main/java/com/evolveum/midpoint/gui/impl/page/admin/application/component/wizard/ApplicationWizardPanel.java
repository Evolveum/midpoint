/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.application.component.wizard;

import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.application.component.wizard.basic.BasicApplicationWizardPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ApplicationType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;

/**
 * @author lskublik
 */
public class ApplicationWizardPanel extends AbstractWizardPanel<ApplicationType, AbstractRoleDetailsModel<ApplicationType>> {

    public ApplicationWizardPanel(String id, WizardPanelHelper<ApplicationType, AbstractRoleDetailsModel<ApplicationType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createBasicWizard()));
    }

    private BasicApplicationWizardPanel createBasicWizard() {
        BasicApplicationWizardPanel basicWizard = new BasicApplicationWizardPanel(
                getIdOfChoicePanel(), getHelper()) {

            @Override
            protected void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
                ApplicationWizardPanel.this.onFinishBasicWizardPerformed(target);
            }
        };
        basicWizard.setOutputMarkupId(true);
        return basicWizard;
    }
}
