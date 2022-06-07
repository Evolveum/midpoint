/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PersonOfInterestPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.RequestAccess;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.RoleCatalogPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.ShoppingCartPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.self.PageSelf;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

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

    public static final String PARAM_STEP = "step";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_WIZARD = "wizard";

    public PageRequestAccess() {
    }

    public PageRequestAccess(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();

        PageParameters params = getPageParameters();
        if (params == null) {
            return;
        }

        StringValue step = params.get(PARAM_STEP);
        if (step == null) {
            return;
        }

        WizardPanel panel = getWizard();
        if (panel == null) {
            return;
        }

        WizardModel model = panel.getWizardModel();
        model.setActiveStepById(step.toString());
    }

    private WizardPanel getWizard() {
        return (WizardPanel) get(createComponentPath(ID_MAIN_FORM, ID_WIZARD));
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        WizardPanel wizard = new WizardPanel(ID_WIZARD, new WizardModel(createSteps()));
        wizard.setOutputMarkupId(true);
        mainForm.add(wizard);
    }

    private List<WizardStep> createSteps() {
        IModel<RequestAccess> model = () -> getSessionStorage().getRequestAccess();

        PersonOfInterestPanel personOfInterest = new PersonOfInterestPanel(model);
        RoleCatalogPanel roleCatalog = new RoleCatalogPanel(model);
        ShoppingCartPanel shoppingCart = new ShoppingCartPanel(model);

        return Arrays.asList(personOfInterest, roleCatalog, shoppingCart);
    }
}
