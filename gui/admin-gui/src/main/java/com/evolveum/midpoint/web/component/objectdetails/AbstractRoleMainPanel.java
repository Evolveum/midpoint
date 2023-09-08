/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.assignment.SwitchAssignmentTypePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.page.self.PageAssignmentShoppingCart;
import com.evolveum.midpoint.web.security.GuiAuthorizationConstants;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public abstract class AbstractRoleMainPanel<R extends AbstractRoleType> extends FocusMainPanel<R> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMainPanel.class);

    private static final String DOT_CLASS = AbstractRoleMainPanel.class.getName();
    private static final String OPERATION_CAN_SEARCH_ROLE_MEMBERSHIP_ITEM = DOT_CLASS + "canSearchRoleMembershipItem";
    private static final String OPERATION_LOAD_ASSIGNMENTS_LIMIT = DOT_CLASS + "loadAssignmentsLimit";
    private static final String ID_SHOPPING_CART_BUTTONS_PANEL = "shoppingCartButtonsPanel";
    private static final String ID_ADD_TO_CART_BUTTON = "addToCartButton";

    public AbstractRoleMainPanel(String id, LoadableModel<PrismObjectWrapper<R>> objectModel,
            LoadableModel<List<ShadowWrapper>> projectionModel,
            PageAdminFocus<R> parentPage) {
        super(id, objectModel, projectionModel, parentPage);
    }

    @Override
    protected void initLayoutButtons(PageAdminObjectDetails<R> parentPage) {
        super.initLayoutButtons(parentPage);
        initShoppingCartPanel(parentPage);
    }

    private void initShoppingCartPanel(PageAdminObjectDetails<R> parentPage) {
        RoleCatalogStorage storage = parentPage.getSessionStorage().getRoleCatalog();

        WebMarkupContainer shoppingCartButtonsPanel = new WebMarkupContainer(ID_SHOPPING_CART_BUTTONS_PANEL);
        shoppingCartButtonsPanel.setOutputMarkupId(true);
        shoppingCartButtonsPanel.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                //show panel only in case if user came to object details from
                // Role Catalog page
                return PageAssignmentShoppingCart.class.equals(WebComponentUtil.getPreviousPageClass(parentPage));
            }
        });
        getMainForm().add(shoppingCartButtonsPanel);

        AjaxButton addToCartButton = new AjaxButton(ID_ADD_TO_CART_BUTTON, parentPage
                .createStringResource("PageAssignmentDetails.addToCartButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentEditorDto dto = AssignmentEditorDto.createDtoFromObject(getObject().asObjectable(), UserDtoStatus.ADD, parentPage);
                dto.setSimpleView(true);
                storage.getAssignmentShoppingCart().add(dto);
                parentPage.redirectBack();
            }
        };
        addToCartButton.add(AttributeAppender.append("class", new LoadableModel<String>() {
            @Override
            protected String load() {
                return addToCartButton.isEnabled() ? "btn btn-success" : "btn btn-success disabled";
            }
        }));
        addToCartButton.setOutputMarkupId(true);
        addToCartButton.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                int assignmentsLimit = AssignmentsUtil.loadAssignmentsLimit(new OperationResult(OPERATION_LOAD_ASSIGNMENTS_LIMIT),
                        parentPage);
                AssignmentEditorDto dto = AssignmentEditorDto.createDtoFromObject(AbstractRoleMainPanel.this.getObject().asObjectable(),
                        UserDtoStatus.ADD, parentPage);
                return !AssignmentsUtil.isShoppingCartAssignmentsLimitReached(assignmentsLimit, parentPage)
                        && (storage.isMultiUserRequest() || dto.isAssignable(SchemaConstants.ORG_DEFAULT));
            }
        });
        addToCartButton.add(AttributeAppender.append("title",
                AssignmentsUtil.getShoppingCartAssignmentsLimitReachedTitleModel(parentPage)));
        shoppingCartButtonsPanel.add(addToCartButton);
    }

    @Override
    protected List<ITab> createTabs(final PageAdminObjectDetails<R> parentPage) {
        List<ITab> tabs = super.createTabs(parentPage);

        tabs.add(
                new PanelTab(parentPage.createStringResource("pageAdminFocus.applicablePolicies"),
                        getTabVisibility(ComponentConstants.UI_FOCUS_TAB_APPLICABLE_POLICIES_URL, false, parentPage)) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new FocusApplicablePoliciesTabPanel<>(panelId, getObjectModel());
                    }
                });

        tabs.add(new CountablePanelTab(parentPage.createStringResource("FocusType.inducement"),
                getTabVisibility(ComponentConstants.UI_FOCUS_TAB_INDUCEMENTS_URL, false, parentPage)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                SwitchAssignmentTypePanel panel = new SwitchAssignmentTypePanel(panelId,
                        PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), AbstractRoleType.F_INDUCEMENT), new ContainerPanelConfigurationType( )) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isInducement() {
                        return true;
                    }
                };
                return panel;
            }

            @Override
            public String getCount() {
                return getInducementsCount();
            }

        });

        tabs.add(new PanelTab(parentPage.createStringResource("pageRole.members"),
                getTabVisibility(ComponentConstants.UI_FOCUS_TAB_MEMBERS_URL, false, parentPage)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createMemberPanel(panelId, parentPage);
            }

            @Override
            public boolean isVisible() {
                return super.isVisible() &&
                        getObjectWrapper().getStatus() != ItemStatus.ADDED &&
                        isAllowedToReadRoleMembership(getObjectWrapper().getOid(), parentPage);
            }
        });

        tabs.add(new PanelTab(parentPage.createStringResource("pageRole.governance"),
                getTabVisibility(ComponentConstants.UI_FOCUS_TAB_GOVERNANCE_URL, false, parentPage)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createGovernancePanel(panelId, parentPage);
            }

            @Override
            public boolean isVisible() {
                return super.isVisible() && getObjectWrapper().getStatus() != ItemStatus.ADDED;
            }
        });

        return tabs;
    }

    public AbstractRoleMemberPanel<R> createMemberPanel(String panelId, PageBase parentPage) {

        return new AbstractRoleMemberPanel<R>(panelId, new Model<>(getObject().asObjectable())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getSupportedRelations() {
                return getSupportedMembersTabRelations();
            }

            @Override
            protected String getStorageKeyTabSuffix() {
                return "abstractRoleMembers";
            }

        };
    }

    public AbstractRoleMemberPanel<R> createGovernancePanel(String panelId, PageBase parentPage) {

        return new AbstractRoleMemberPanel<R>(panelId, new Model<>(getObject().asObjectable())) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getSupportedRelations() {
                List<QName> availableRelations = getSupportedGovernanceTabRelations();
                return availableRelations;
            }

            @Override
            protected Map<String, String> getAuthorizations(QName complexType) {
                return getGovernanceTabAuthorizations();
            }

            @Override
            protected String getStorageKeyTabSuffix() {
                return "abstractRoleGovernance";
            }

        };
    }

    protected List<QName> getSupportedMembersTabRelations() {
        List<QName> relations = WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ADMINISTRATION, getDetailsPage());
        List<QName> governance = WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.GOVERNANCE, getDetailsPage());
        governance.forEach(r -> relations.remove(r));
        return relations;
//        return new AvailableRelationDto(relations, defaultRelationConfiguration);
    }

    protected List<QName> getSupportedGovernanceTabRelations() {
        return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.GOVERNANCE, getDetailsPage());
    }

    protected Map<String, String> getGovernanceTabAuthorizations() {
        return GuiAuthorizationConstants.GOVERNANCE_MEMBERS_AUTHORIZATIONS;
    }

    private boolean isAllowedToReadRoleMembership(String abstractRoleOid, PageBase parentPage) {
        return isAllowedToReadRoleMembershipItemForType(abstractRoleOid, UserType.class, parentPage)
                || isAllowedToReadRoleMembershipItemForType(abstractRoleOid, RoleType.class, parentPage)
                || isAllowedToReadRoleMembershipItemForType(abstractRoleOid, OrgType.class, parentPage)
                || isAllowedToReadRoleMembershipItemForType(abstractRoleOid, ServiceType.class, parentPage);
    }

    private <F extends FocusType> boolean isAllowedToReadRoleMembershipItemForType(String abstractRoleOid, Class<F> type, PageBase parentPage) {
        ObjectQuery query = parentPage.getPrismContext().queryFor(type)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(abstractRoleOid).build();
        Task task = parentPage.createSimpleTask(OPERATION_CAN_SEARCH_ROLE_MEMBERSHIP_ITEM);
        OperationResult result = task.getResult();
        boolean isAllowed = false;
        try {
            isAllowed = parentPage.getModelInteractionService()
                    .canSearch(type, null, null, false, query, task, result);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check if user is allowed to search for roleMembershipRef item", ex);
        }
        return isAllowed;
    }

    private String getInducementsCount() {
        PrismObject<R> focus = getObjectModel().getObject().getObject();
        List<AssignmentType> inducements = focus.asObjectable().getInducement();
        if (inducements == null) {
            return "";
        }
        return Integer.toString(inducements.size());
    }

    //TODO what? why? when?
    @Override
    protected boolean areSavePreviewButtonsEnabled() {
        PrismObjectWrapper<R> focusWrapper = getObjectModel().getObject();
        PrismContainerWrapper<AssignmentType> assignmentsWrapper;
        try {
            assignmentsWrapper = focusWrapper.findContainer(AbstractRoleType.F_INDUCEMENT);
        } catch (SchemaException e) {
            return false;
        }
        return super.areSavePreviewButtonsEnabled() || isAssignmentsModelChanged(assignmentsWrapper);
    }
}
