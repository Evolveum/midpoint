/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self;

import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.AssignmentsDetailsPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
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

    public PageRequestAccess() {
    }

    public PageRequestAccess(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
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


//        IModel<PrismContainerValueWrapper<AssignmentType>> model = new LoadableModel<>(false) {
//            @Override
//            protected PrismContainerValueWrapper load() {
//                try {
//                    Task task = PageRequestAccess.this.getPageTask();
//                    OperationResult result = task.getResult();
//
        //virtaul containers are now collected for Objects, not containers, therefore empty user is created here
//                    UserType user = new UserType();
//                    final AssignmentType assigment = new AssignmentType();
//                    user.getAssignment().add(assigment);
//                    PrismObjectWrapperFactory<UserType> userWrapperFactory = PageRequestAccess.this.findObjectWrapperFactory(user.asPrismObject().getDefinition());
//
//                    WrapperContext context = new WrapperContext(task, result);
//
//                    ContainerPanelConfigurationType c = new ContainerPanelConfigurationType();
//                    c.identifier("sample-panel");
//                    c.type(AssignmentType.COMPLEX_TYPE);
//                    c.panelType("formPanel");
//                    VirtualContainersSpecificationType vcs =
//                            c.beginContainer()
//                                    .beginDisplay()
//                                    .label("Exxxxx")
//                                    .end();
//                    vcs.identifier("some-identifier");
//                    vcs.beginItem().path(new ItemPathType(ItemPath.create(AssignmentType.F_EXTENSION))).end();
//                    context.setDetailsPageTypeConfiguration(Arrays.asList(c));
//                    context.setCreateIfEmpty(true);

        //create whole wrapper, instead of only the concrete container value wrapper
//                    PrismObjectWrapper<UserType> userWrapper = userWrapperFactory.createObjectWrapper(user.asPrismObject(), ItemStatus.NOT_CHANGED, context);
//
//                    PrismContainerWrapper<AssignmentType> assignmentWrapper = userWrapper.findContainer(UserType.F_ASSIGNMENT);
//                    return assignmentWrapper.getValues().iterator().next();
//
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//                return null;
//            }
//        };
//
        //maybe we can use assignmentDetailsPanel here with some improvemenets, such as hiding condition? so it will be the same as in the admin interface?
//        AssignmentsDetailsPanel container = new AssignmentsDetailsPanel("sample", model, false, c);
//        mainForm.add(container);
    }

    private List<WizardStep> createSteps() {
        IModel<RequestAccess> model = () -> getSessionStorage().getRequestAccess();

        PersonOfInterestPanel personOfInterest = new PersonOfInterestPanel(model, this);
        RelationPanel relationPanel = new RelationPanel(model, this);
        RoleCatalogPanel roleCatalog = new RoleCatalogPanel(model, this);
        ShoppingCartPanel shoppingCart = new ShoppingCartPanel(model, this);

        return Arrays.asList(personOfInterest, relationPanel, roleCatalog, shoppingCart);
    }
}
