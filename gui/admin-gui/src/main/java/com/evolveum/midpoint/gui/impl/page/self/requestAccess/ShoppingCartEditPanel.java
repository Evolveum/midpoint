/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serial;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;

import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.AssignmentsDetailsPanel;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.enforcer.api.ItemSecurityConstraints;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShoppingCartEditPanel extends BasePanel<ShoppingCartItem> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ShoppingCartEditPanel.class);

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_SAVE = "save";
    private static final String ID_CLOSE = "close";
    private static final String ID_RELATION = "relation";
    private static final String ID_TENANT_REF = "tenantRef";
    private static final String ID_ORG_REF = "orgRef";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_FOCUS_TYPE = "focusType";
    private static final String ID_ADMINISTRATIVE_STATUS = "administrativeStatus";
    private static final String ID_CUSTOM_VALIDITY = "customValidity";
    private static final String ID_EXTENSION = "extension";
    private static final String ID_MESSAGE = "message";

    private Fragment footer;

    private IModel<ItemSecurityConstraints> assignmentSecurityConstraints;

    private IModel<RequestAccess> requestAccess;

    private IModel<List<QName>> relationChoices;

    private IModel<CustomValidity> customValidityModel;

    private IModel<PrismContainerValueWrapper<AssignmentType>> assignmentExtension;

    private boolean validitySettingsEnabled;

    public ShoppingCartEditPanel(IModel<ShoppingCartItem> model, IModel<RequestAccess> requestAccess, boolean validitySettingsEnabled) {
        super(Popupable.ID_CONTENT, model);

        this.requestAccess = requestAccess;
        this.validitySettingsEnabled = validitySettingsEnabled;

        initModels();
        initLayout();
        initFooter();
    }

    private void initModels() {
        relationChoices = new LoadableModel<>(false) {

            @Override
            protected List<QName> load() {
                return requestAccess.getObject().getAvailableRelations(getPageBase());
            }
        };

        customValidityModel = new LoadableModel<>(false) {

            @Override
            protected CustomValidity load() {
                ShoppingCartItem item = getModelObject();

                ActivationType activation = item.getAssignment().getActivation();
                if (activation == null) {
                    return new CustomValidity();
                }

                CustomValidity cv = new CustomValidity();
                cv.setFrom(XmlTypeConverter.toDate(activation.getValidFrom()));
                cv.setTo(XmlTypeConverter.toDate(activation.getValidTo()));

                return cv;
            }
        };

        assignmentExtension = new LoadableModel<>(false) {
            @Override
            protected PrismContainerValueWrapper load() {
                try {
                    Task task = getPageBase().getPageTask();
                    OperationResult result = task.getResult();

                    // we'll clone our assignment for this assignment/extension prism container value wrapper black magic
                    // hack as not to polute original object with mess from wrappers
                    // we'll copy new extension container value to original assignment during save operation
                    AssignmentType assigment = getModelObject().getAssignment().clone();

                    // virtual containers are now collected for Objects, not containers, therefore empty user is created here
                    UserType user = new UserType();
                    // we'll set user principal oid to our fake user to get object wrapper with properties/containers
                    // that were filters through authorization against "self".
                    // TODO this should be improved
                    user.setOid(SecurityUtil.getPrincipalOidIfAuthenticated());
                    user.getAssignment().add(assigment);
                    PrismObjectWrapperFactory<UserType> userWrapperFactory = getPageBase().findObjectWrapperFactory(user.asPrismObject().getDefinition());

                    WrapperContext context = new WrapperContext(task, result);

                    context.setDetailsPageTypeConfiguration(Arrays.asList(createExtensionPanelConfiguration()));
                    context.setCreateIfEmpty(true);

                    // create whole wrapper, instead of only the concrete container value wrapper
                    PrismObjectWrapper<UserType> userWrapper = userWrapperFactory.createObjectWrapper(user.asPrismObject(), ItemStatus.ADDED, context);
                    PrismContainerWrapper<AssignmentType> assignmentWrapper = userWrapper.findContainer(UserType.F_ASSIGNMENT);
                    if (assignmentWrapper == null) {
                        return null;
                    }

                    PrismContainerValueWrapper<AssignmentType> valueWrapper = assignmentWrapper.getValues().iterator().next();
                    // todo this should be done automatically by wrappers - if parent ADDED child should probably have ADDED status as well...
                    valueWrapper.setStatus(ValueStatus.ADDED);
                    valueWrapper.setShowEmpty(true);

                    return valueWrapper;
                } catch (Exception ex) {
                    LOGGER.debug("Couldn't load extensions", ex);
                }

                return null;
            }
        };

        assignmentSecurityConstraints = new LoadableModel<>() {

            @Override
            protected ItemSecurityConstraints load() {
                List<ObjectReferenceType> pois = requestAccess.getObject().getPersonOfInterest();
                if (pois == null || pois.isEmpty()) {
                    return null;
                }

                Task task = getPageBase().createSimpleTask("Load assignment edit schema");
                try {
                    PrismObject<UserType> user = WebModelServiceUtils.loadObject(
                            pois.get(0), true, getPageBase(), task, task.getResult());
                    if (user == null) {
                        return null;
                    }

                    PrismObject<AbstractRoleType> target = WebModelServiceUtils.loadObject(
                            getModelObject().getAssignment().getTargetRef(), true, getPageBase(), task, task.getResult());
                    return getPageBase()
                            .getModelInteractionService()
                            .getAllowedRequestAssignmentItems(user, target, task, task.getResult());
                } catch (Exception ex) {
                    getPageBase().error(
                            getString("ShoppingCartEditPanel.message.couldNotLoadAssignmentEditSchema", ex.getMessage()));
                    LOGGER.debug("Couldn't load assignment edit schema", ex);
                }

                return null;
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        FeedbackAlerts message = new FeedbackAlerts(ID_MESSAGE);
        message.setOutputMarkupId(true);
        message.setFilter(new ContainerFeedbackMessageFilter(findParent(Form.class)));
        add(message);
    }

    private ContainerPanelConfigurationType createExtensionPanelConfiguration() {
        ContainerPanelConfigurationType c = new ContainerPanelConfigurationType();
        c.identifier("sample-panel");
        c.type(AssignmentType.COMPLEX_TYPE);
        c.panelType("formPanel");

        return c;
    }

    private void initLayout() {
        TextArea<String> description = new TextArea<>(ID_DESCRIPTION, new PropertyModel<>(getModel(), "assignment.description"));
        description.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        description.add(new VisibleBehaviour(() -> isItemVisible(AssignmentType.F_DESCRIPTION)));
        description.setOutputMarkupId(true);
        add(description);

        DropDownChoice relation = new DropDownChoice(ID_RELATION, () -> requestAccess.getObject().getRelation(), relationChoices,
                RelationUtil.getRelationChoicesRenderer());
        relation.add(new EnableBehaviour(() -> false));
        add(relation);

        initFocusTypePanel();
        initTenantRefPanel();
        initOrgRefPanel();

        AssignmentsDetailsPanel extension = new AssignmentsDetailsPanel(ID_EXTENSION, assignmentExtension, false, createExtensionPanelConfiguration()) {

            @Override
            protected DisplayNamePanel<AssignmentType> createDisplayNamePanel(String displayNamePanelId) {
                DisplayNamePanel panel;
                if (getModelObject() == null) {
                    panel = new DisplayNamePanel(displayNamePanelId, Model.of((Containerable) null));
                } else {
                    panel = super.createDisplayNamePanel(displayNamePanelId);
                }
                panel.add(new VisibleBehaviour(() -> false));
                return panel;
            }

            @Override
            protected @NotNull List<ITab> createTabs() {
                return new ArrayList<>();
            }

            @Override
            protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
                return itemWrapper.getPath().startsWith(ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_EXTENSION)) ? ItemVisibility.AUTO : ItemVisibility.HIDDEN;
            }
        };
        extension.add(new VisibleBehaviour(() -> {
            try {
                PrismContainerValueWrapper<AssignmentType> wrapper = assignmentExtension.getObject();
                if (wrapper == null) {
                    return false;
                }
                PrismContainerWrapper cw = wrapper.findItem(ItemPath.create(AssignmentType.F_EXTENSION));
                if (cw == null || cw.isEmpty()) {
                    return false;
                }
                PrismContainerValueWrapper pcvw = (PrismContainerValueWrapper) cw.getValue();
                List items = pcvw.getItems();

                return items != null && !items.isEmpty();
            } catch (SchemaException ex) {
                return true;
            }
        }));
        add(extension);

        IModel<ActivationStatusType> model = new Model<>() {

            @Override
            public ActivationStatusType getObject() {
                AssignmentType assignment = getModelObject().getAssignment();
                ActivationType activation = assignment.getActivation();
                if (activation == null) {
                    return null;
                }

                return activation.getAdministrativeStatus();
            }

            @Override
            public void setObject(ActivationStatusType status) {
                AssignmentType assignment = getModelObject().getAssignment();
                ActivationType activation = assignment.getActivation();
                if (activation == null && status != null) {
                    activation = new ActivationType();
                    assignment.setActivation(activation);
                }

                activation.setAdministrativeStatus(status);
            }
        };

        DropDownChoice administrativeStatus = new DropDownChoice(ID_ADMINISTRATIVE_STATUS, model,
                WebComponentUtil.createReadonlyModelFromEnum(ActivationStatusType.class),
                WebComponentUtil.getEnumChoiceRenderer(this));
        administrativeStatus.add(
                new VisibleBehaviour(
                        () -> isItemVisible(ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS))));
        administrativeStatus.setNullValid(true);
        add(administrativeStatus);

        CustomValidityPanel customValidity = new CustomValidityPanel(ID_CUSTOM_VALIDITY, customValidityModel);
        customValidity.add(new VisibleEnableBehaviour(
                () -> isItemVisible(ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM))
                        || isItemVisible(ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO)),
                () -> validitySettingsEnabled));
        add(customValidity);
    }

    private void initFocusTypePanel() {
        List<QName> focusTypes = ObjectTypeListUtil.createFocusTypeList();
        DropDownChoice<QName> focusType = new DropDownChoice<>(ID_FOCUS_TYPE,
                new PropertyModel<>(getModel(), "assignment.focusType"),
                Model.ofList(focusTypes), new QNameObjectTypeChoiceRenderer());
        focusType.setOutputMarkupId(true);
        focusType.add(new VisibleBehaviour(() -> isItemVisible(AssignmentType.F_FOCUS_TYPE)));
        add(focusType);
    }

    private void initTenantRefPanel() {
        ValueChoosePanel<ObjectReferenceType> valueChoosePanel = new ValueChoosePanel<>(ID_TENANT_REF,
                new PropertyModel<>(getModel(), "assignment.tenantRef")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected ObjectFilter createCustomFilter() {
                return getPageBase().getPrismContext().queryFor(OrgType.class)
                        .item(OrgType.F_TENANT).eq(true)
                        .buildFilter();
            }

            @Override
            public List<QName> getSupportedTypes() {
                return Collections.singletonList(OrgType.COMPLEX_TYPE);
            }
        };
        valueChoosePanel.setOutputMarkupId(true);
        valueChoosePanel.add(new VisibleBehaviour(() -> isItemVisible(AssignmentType.F_TENANT_REF)));
        add(valueChoosePanel);
    }

    private void initOrgRefPanel() {
        ValueChoosePanel<ObjectReferenceType> valueChoosePanel = new ValueChoosePanel<>(ID_ORG_REF,
                new PropertyModel<>(getModel(), "assignment.orgRef")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public List<QName> getSupportedTypes() {
                return Collections.singletonList(OrgType.COMPLEX_TYPE);
            }
        };
        valueChoosePanel.setOutputMarkupId(true);
        valueChoosePanel.add(new VisibleBehaviour(() -> isItemVisible(AssignmentType.F_ORG_REF)));
        add(valueChoosePanel);
    }

    private boolean isItemVisible(ItemPath path) {
        ItemSecurityConstraints constraints = assignmentSecurityConstraints.getObject();
        if (constraints == null) {
            return false;
        }

        AuthorizationDecisionType decision = constraints.findItemDecision(
                AssignmentHolderType.F_ASSIGNMENT.append(path));

        return decision == AuthorizationDecisionType.ALLOW;
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.add(new AjaxSubmitLink(ID_SAVE) {

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(ShoppingCartEditPanel.this.get(ID_MESSAGE));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                CustomValidity cv = customValidityModel.getObject();
                XMLGregorianCalendar from = XmlTypeConverter.createXMLGregorianCalendar(cv.getFrom());
                XMLGregorianCalendar to = XmlTypeConverter.createXMLGregorianCalendar(cv.getTo());

                ShoppingCartItem item = getModelObject();
                AssignmentType assignment = item.getAssignment();
                ActivationType activation = assignment.getActivation();

                if (from != null || to != null) {
                    if (activation == null) {
                        activation = new ActivationType();
                        assignment.setActivation(activation);
                    }

                    activation.validFrom(from).validTo(to);
                }

                savePerformed(target, ShoppingCartEditPanel.this.getModel());
            }
        });
        footer.add(new AjaxLink<>(ID_CLOSE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closePerformed(target, ShoppingCartEditPanel.this.getModel());
            }
        });
    }

    @Override
    public Component getFooter() {
        return footer;
    }

    @Override
    public int getWidth() {
        return 1000;
    }

    @Override
    public int getHeight() {
        return 100;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return () -> getString("ShoppingCartEditPanel.title", getModelObject().getName());
    }

    @Override
    public Component getContent() {
        return this;
    }

    protected void savePerformed(AjaxRequestTarget target, IModel<ShoppingCartItem> model) {
        try {
            // this is just a nasty "pre-save" code to handle assignment extension via wrappers -> apply it to our assignment stored in request access
            PrismContainerValueWrapper<AssignmentType> containerValueWrapper = assignmentExtension.getObject();
            if (containerValueWrapper == null) {
                updateSelectedAssignment();
                return;
            }

            PrismObjectWrapper<UserType> wrapper = containerValueWrapper.getParent().findObjectWrapper();
            if (wrapper.getObjectDelta().isEmpty()) {
                updateSelectedAssignment();
                return;
            }

            UserType user = wrapper.getObjectApplyDelta().asObjectable();
            // TODO wrappers for some reason create second assignment with (first one was passed from this shopping cart to fake user)
            // that second assignment contains modified extension...very nasty hack
            List<AssignmentType> assignments = user.getAssignment();
            if (assignments.size() < 2) {
                updateSelectedAssignment();
                return;
            }
            AssignmentType modified = user.getAssignment().get(1);

            AssignmentType a = getModelObject().getAssignment();
            a.setExtension(modified.getExtension());

            updateSelectedAssignment();
        } catch (CommonException ex) {
            getPageBase().error(getString("ShoppingCartEditPanel.message.couldntProcessExtension", ex.getMessage()));
            LOGGER.debug("Couldn't process extension attributes", ex);
        }
    }

    private void updateSelectedAssignment() {
        AssignmentType a = getModelObject().getAssignment();
        requestAccess.getObject().updateSelectedAssignment(a);
    }

    protected void closePerformed(AjaxRequestTarget target, IModel<ShoppingCartItem> model) {

    }
}
