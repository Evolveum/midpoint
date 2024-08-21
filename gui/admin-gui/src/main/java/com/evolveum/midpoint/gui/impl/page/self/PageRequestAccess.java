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
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.*;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.self.PageSelf;

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

    private WizardModel model;

    public PageRequestAccess() {
        this(new PageParameters());
    }

    public PageRequestAccess(PageParameters parameters) {
        this(parameters,null);
    }

    public PageRequestAccess(PageParameters parameters, WizardModel model) {
        super(parameters);

        this.model = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        if (model == null) {
            model = new WizardModel(createSteps());
        }
        WizardPanel wizard = new WizardPanel(ID_WIZARD, model);
        wizard.setOutputMarkupId(true);
        mainForm.add(wizard);
    }

    private List<WizardStep> createSteps() {
        LoadableDetachableModel<RequestAccess> model = getRequestAccessModel();

        PersonOfInterestPanel personOfInterest = new PersonOfInterestPanel(model, this);
        RelationPanel relationPanel = new RelationPanel(model, this);
        RoleCatalogPanel roleCatalog = new RoleCatalogPanel(model, this);
        ShoppingCartPanel shoppingCart = new ShoppingCartPanel(model, this);

        return Arrays.asList(personOfInterest, relationPanel, roleCatalog, shoppingCart);
    }

    private LoadableDetachableModel<RequestAccess> getRequestAccessModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected RequestAccess load() {
                return getSessionStorage().getRequestAccess();
            }
        };
    }
}
