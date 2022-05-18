/*
 * Copyright (C) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.wizard.Wizard;
import com.evolveum.midpoint.gui.api.component.wizard.WizardBorder;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.self.component.PersonOfInterestPanel;
import com.evolveum.midpoint.web.page.self.component.RoleCatalogPanel;

/**
 * @author Viliam Repan (lazyman)
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self/requestAccess")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_URL,
                        label = "PageRequestAccess.auth.requestAccess.label",
                        description = "PageRequestAccess.auth.requestAccess.description") })
public class PageRequestAccess extends PageSelf {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageRequestAccess.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_WIZARD = "wizard";
    private static final String ID_PERSON_OF_INTEREST = "personOfInterest";
    private static final String ID_ROLE_CATALOG = "roleCatalog";

    private IModel<Wizard> wizard;

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Wizard w = new Wizard();
        w.getStepLabels().add(() -> "Person of interest");
        w.getStepLabels().add(() -> "Role catalog");
        w.getStepLabels().add(() -> "Shopping cart");

        wizard = Model.of(w);

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        WizardBorder wizard = new WizardBorder(ID_WIZARD, this.wizard);
        wizard.setOutputMarkupId(true);
        mainForm.add(wizard);

        PersonOfInterestPanel personOfInterest = new PersonOfInterestPanel(ID_PERSON_OF_INTEREST) {

            @Override
            protected void onNextPerformed(AjaxRequestTarget target) {
                Wizard w = PageRequestAccess.this.wizard.getObject();
                w.nextStep();

                target.add(wizard);
            }
        };
        personOfInterest.add(wizard.createWizardStepVisibleBehaviour(0));
        wizard.add(personOfInterest);

        RoleCatalogPanel roleCatalog = new RoleCatalogPanel(ID_ROLE_CATALOG) {

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                Wizard w = PageRequestAccess.this.wizard.getObject();
                w.previousStep();

                target.add(wizard);
            }
        };
        roleCatalog.add(wizard.createWizardStepVisibleBehaviour(1));
        wizard.add(roleCatalog);
    }
}
