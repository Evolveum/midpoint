/*
 * Copyright (C) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.RelationDropDownChoicePanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar.
 */
public abstract class AbstractShoppingCartTabPanel<R extends AbstractRoleType> extends BasePanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_SHOPPING_CART_CONTAINER = "shoppingCartContainer";
    private static final String ID_SHOPPING_CART_ITEMS_PANEL = "shoppingCartItemsPanel";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH = "search";
    private static final String ID_ADD_ALL_BUTTON = "addAllButton";
    private static final String ID_GO_TO_SHOPPING_CART_BUTTON = "goToShoppingCart";
    private static final String ID_PARAMETERS_PANEL = "parametersPanel";
    private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_RELATION = "relation";
    private static final String ID_TARGET_USER_PANEL = "targetUserPanel";

    private static final String DOT_CLASS = AbstractShoppingCartTabPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";
    private static final String OPERATION_LOAD_ASSIGNABLE_RELATIONS_LIST = DOT_CLASS + "loadAssignableRelationsList";
    private static final String OPERATION_LOAD_ASSIGNMENTS_LIMIT = DOT_CLASS + "loadAssignmentsLimit";
    private static final String OPERATION_LOAD_ASSIGNMENT_TARGET_USER = DOT_CLASS + "loadAssignmentTargetUser";

    private final RoleManagementConfigurationType roleManagementConfig;
    private LoadableModel<UserType> targetUserModel;

    public AbstractShoppingCartTabPanel(String id, RoleManagementConfigurationType roleManagementConfig) {
        super(id);
        this.roleManagementConfig = roleManagementConfig;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        targetUserModel = new LoadableModel<UserType>(true) {
            @Override
            protected UserType load() {
                return getTargetUser();
            }
        };
        initLayout();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        if (targetUserModel != null) {
            targetUserModel.reset();
        }
    }

    private void initLayout() {
        setOutputMarkupId(true);

        initLeftSidePanel();

        WebMarkupContainer shoppingCartContainer = new WebMarkupContainer(ID_SHOPPING_CART_CONTAINER);
        shoppingCartContainer.setOutputMarkupId(true);
        appendItemsPanelStyle(shoppingCartContainer);
        add(shoppingCartContainer);

        initSearchPanel(shoppingCartContainer);
        initShoppingCartItemsPanel(shoppingCartContainer);
        initParametersPanel(shoppingCartContainer);
    }

    protected void initLeftSidePanel() {
    }

    private void initSearchPanel(WebMarkupContainer shoppingCartContainer) {
        final Form searchForm = new MidpointForm(ID_SEARCH_FORM);
        searchForm.setOutputMarkupId(true);

        IModel<Search> searchModel = Model.of(getRoleCatalogStorage().getSearch() != null ? getRoleCatalogStorage().getSearch() :
                createSearch());
        SearchPanel search = new SearchPanel(ID_SEARCH, searchModel, false) {
            private static final long serialVersionUID = 1L;

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                AbstractShoppingCartTabPanel.this.searchPerformed(target);
            }
        };
        getRoleCatalogStorage().setSearch(searchModel.getObject());
        searchForm.add(search);
        shoppingCartContainer.add(searchForm);
    }

    protected Search createSearch() {
        return SearchFactory.createSearch(getQueryClass(), getPageBase());
    }

    protected void searchPerformed(AjaxRequestTarget target) {
        getRoleCatalogStorage().setSearch(getSearchPanel().getModelObject());
        target.add(AbstractShoppingCartTabPanel.this);
    }

    private void initShoppingCartItemsPanel(WebMarkupContainer shoppingCartContainer) {
        GridViewComponent<ObjectDataProvider<AssignmentEditorDto, AbstractRoleType>> catalogItemsGrid =
                new GridViewComponent<ObjectDataProvider<AssignmentEditorDto, AbstractRoleType>>(ID_SHOPPING_CART_ITEMS_PANEL,
                        new LoadableModel<ObjectDataProvider<AssignmentEditorDto, AbstractRoleType>>() {
                            @Override
                            protected ObjectDataProvider<AssignmentEditorDto, AbstractRoleType> load() {
                                return getTabPanelProvider();
                            }
                        }) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(Item item) {
                        item.add(new RoleCatalogItemButton(getCellItemId(), item.getModel()) {
                            private static final long serialVersionUID = 1L;

                            @Override
                            protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target) {
                                int assignmentsLimit = getRoleCatalogStorage().getAssignmentRequestLimit();
                                if (AssignmentsUtil.isShoppingCartAssignmentsLimitReached(assignmentsLimit, AbstractShoppingCartTabPanel.this.getPageBase())) {
                                    target.add(AbstractShoppingCartTabPanel.this);
                                }
                                AbstractShoppingCartTabPanel.this.assignmentAddedToShoppingCartPerformed(target);
                            }

                            @Override
                            protected QName getNewAssignmentRelation() {
                                return AbstractShoppingCartTabPanel.this.getNewAssignmentRelation();
                            }
                        });
                    }
                };
        catalogItemsGrid.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isShoppingCartItemsPanelVisible();
            }
        });
        catalogItemsGrid.setOutputMarkupId(true);
        shoppingCartContainer.add(catalogItemsGrid);
    }

    private void initParametersPanel(WebMarkupContainer shoppingCartContainer) {
        WebMarkupContainer parametersPanel = new WebMarkupContainer(ID_PARAMETERS_PANEL);
        parametersPanel.setOutputMarkupId(true);
        shoppingCartContainer.add(parametersPanel);

        initTargetUserSelectionPanel(parametersPanel);
        initRelationPanel(parametersPanel);
        initButtonsPanel(parametersPanel);
    }

    private void initTargetUserSelectionPanel(WebMarkupContainer parametersPanel) {
        IModel<List<UserType>> selectionModel = () -> WebComponentUtil.loadTargetUsersListForShoppingCart(OPERATION_LOAD_ASSIGNMENT_TARGET_USER, AbstractShoppingCartTabPanel.this.getPageBase());

        UserSelectionButton targetUserPanel = new UserSelectionButton(ID_TARGET_USER_PANEL, selectionModel, true,
                createStringResource("AssignmentCatalogPanel.selectTargetUser")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected String getUserButtonLabel() {
                return getTargetUserSelectionButtonLabel(getModelObject());
            }

            @Override
            protected String getTargetUserButtonClass() {
                return "btn-sm";
            }

            @Override
            protected void onDeleteSelectedUsersPerformed(AjaxRequestTarget target) {
                super.onDeleteSelectedUsersPerformed(target);
                getRoleCatalogStorage().setTargetUserOidsList(new ArrayList<>());

                target.add(AbstractShoppingCartTabPanel.this);
//                target.add(parametersPanel.get(ID_TARGET_USER_PANEL));
            }

            @Override
            protected void multipleUsersSelectionPerformed(AjaxRequestTarget target, List<UserType> usersList) {
                if (CollectionUtils.isNotEmpty(usersList)) {
                    List<String> usersOidsList = new ArrayList<>();
                    usersList.forEach(user -> usersOidsList.add(user.getOid()));
                    getRoleCatalogStorage().setTargetUserOidsList(usersOidsList);
                }
                target.add(AbstractShoppingCartTabPanel.this);
//                target.add(parametersPanel.get(ID_TARGET_USER_PANEL));
            }

        };
        targetUserPanel.setOutputMarkupId(true);
        parametersPanel.add(targetUserPanel);
    }

    private void initRelationPanel(WebMarkupContainer parametersPanel) {
        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        relationContainer.setOutputMarkupId(true);
        parametersPanel.add(relationContainer);

        List<QName> assignableRelationsList = getAvailableRelationsList();
        if (CollectionUtils.isNotEmpty(assignableRelationsList)) {
            if (assignableRelationsList.contains(SchemaConstants.ORG_DEFAULT)) {
                getRoleCatalogStorage().setSelectedRelation(SchemaConstants.ORG_DEFAULT);
            } else {
                getRoleCatalogStorage().setSelectedRelation(assignableRelationsList.get(0));
            }
        }
        relationContainer.add(new RelationDropDownChoicePanel(ID_RELATION, getRoleCatalogStorage().getSelectedRelation(),
                assignableRelationsList, false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onValueChanged(AjaxRequestTarget target) {
                getRoleCatalogStorage().setSelectedRelation(getRelationValue());
                target.add(AbstractShoppingCartTabPanel.this);
            }

            @Override
            protected IModel<String> getRelationLabelModel() {
                return Model.of();
            }
        });
    }

    private List<QName> getAvailableRelationsList() {
        List<QName> availableRelations = WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.SELF_SERVICE, getPageBase());
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNABLE_RELATIONS_LIST);
        OperationResult result = task.getResult();
        List<QName> assignableRelationsList = WebComponentUtil.getAssignableRelationsList(
                getTargetUser().asPrismObject(), ObjectTypes.getObjectTypeClass(getQueryType()),
                WebComponentUtil.AssignmentOrder.ASSIGNMENT, result, task, getPageBase());
        if (CollectionUtils.isEmpty(assignableRelationsList)) {
            return availableRelations;
        }
        return assignableRelationsList;
    }

    private void initButtonsPanel(WebMarkupContainer parametersPanel) {
        AjaxButton addAllButton = new AjaxButton(ID_ADD_ALL_BUTTON, createStringResource("AbstractShoppingCartTabPanel.addAllButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                AbstractShoppingCartTabPanel.this.addAllAssignmentsPerformed(ajaxRequestTarget);
            }
        };
        addAllButton.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                ObjectDataProvider provider = getGridViewComponent().getProvider();
                return provider != null && provider.size() > 0;

            }

            @Override
            public boolean isEnabled() {
                int assignmentsLimit = AssignmentsUtil.loadAssignmentsLimit(new OperationResult(OPERATION_LOAD_ASSIGNMENTS_LIMIT),
                        AbstractShoppingCartTabPanel.this.getPageBase());
                return !AssignmentsUtil.isShoppingCartAssignmentsLimitReached(assignmentsLimit, AbstractShoppingCartTabPanel.this.getPageBase());
            }
        });
        addAllButton.add(AttributeAppender.append("title",
                AssignmentsUtil.getShoppingCartAssignmentsLimitReachedTitleModel(getPageBase())));
        parametersPanel.add(addAllButton);

        AjaxButton goToShoppingCartButton = new AjaxButton(ID_GO_TO_SHOPPING_CART_BUTTON, createStringResource("AbstractShoppingCartTabPanel.goToShoppingCartButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                AbstractShoppingCartTabPanel.this.getPageBase().navigateToNext(new PageAssignmentsList(true));
            }
        };
        goToShoppingCartButton.setOutputMarkupId(true);
        goToShoppingCartButton.add(new VisibleBehaviour(() -> {
            boolean isShoppingCartEmpty = AbstractShoppingCartTabPanel.this.getRoleCatalogStorage().getAssignmentShoppingCart().size() == 0;
            return !isShoppingCartEmpty;
        }));
        parametersPanel.add(goToShoppingCartButton);
    }

    private String getTargetUserSelectionButtonLabel(List<UserType> usersList) {
        if (usersList == null || usersList.size() == 0) {
            return createStringResource("AssignmentCatalogPanel.requestForMe").getString();
        } else if (usersList.size() == 1) {
            if (usersList.get(0).getOid().equals(getPageBase().getPrincipalFocus().getOid())) {
                return createStringResource("AssignmentCatalogPanel.requestForMe").getString();
            } else {
                return usersList.get(0).getName().getOrig();
            }
        } else {
            return createStringResource("AssignmentCatalogPanel.requestForMultiple",
                    usersList.size()).getString();
        }
    }

    private RelationDropDownChoicePanel getRelationDropDown() {
        return (RelationDropDownChoicePanel) get(createComponentPath(ID_SHOPPING_CART_CONTAINER, ID_PARAMETERS_PANEL, ID_RELATION_CONTAINER, ID_RELATION));
    }

    private void addAllAssignmentsPerformed(AjaxRequestTarget target) {

        ObjectDataProvider provider = getGridViewComponent().getProvider();
        List<AssignmentEditorDto> availableProviderData = provider.getAvailableData();

        if (availableProviderData == null) {
            return;
        }

        int limit = AssignmentsUtil.loadAssignmentsLimit(new OperationResult(OPERATION_LOAD_ASSIGNMENTS_LIMIT), getPageBase());
        int addedAssignmentsCount = availableProviderData.size() + getRoleCatalogStorage().getAssignmentShoppingCart().size();
        if (limit >= 0 && addedAssignmentsCount > limit) {
            warn(createStringResource("AssignmentPanel.assignmentsLimitReachedWarning", limit).getString());
            target.add(AbstractShoppingCartTabPanel.this.getPageBase().getFeedbackPanel());
            return;
        }

        availableProviderData.forEach(newAssignment -> {
            AssignmentEditorDto assignmentToAdd = newAssignment.clone();
            assignmentToAdd.getTargetRef().setRelation(getNewAssignmentRelation());
            getRoleCatalogStorage().getAssignmentShoppingCart().add(assignmentToAdd);
        });

        target.add(AbstractShoppingCartTabPanel.this);
        assignmentAddedToShoppingCartPerformed(target);
    }

    private ObjectDataProvider<AssignmentEditorDto, AbstractRoleType> getTabPanelProvider() {
        IModel<Search<AbstractRoleType>> searchModel = new IModel<Search<AbstractRoleType>>() {

            @Override
            public Search<AbstractRoleType> getObject() {
                return getRoleCatalogStorage().getSearch();
            }
        };
        ObjectDataProvider provider = new ObjectDataProvider<AssignmentEditorDto, AbstractRoleType>(AbstractShoppingCartTabPanel.this,
                searchModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public AssignmentEditorDto createDataObjectWrapper(PrismObject<AbstractRoleType> obj) {

                AssignmentEditorDto dto = AssignmentEditorDto.createDtoFromObject(obj.asObjectable(), UserDtoStatus.ADD, getPageBase());
                if (!getRoleCatalogStorage().isMultiUserRequest()) {
                    dto.setAlreadyAssigned(isAlreadyAssigned(obj, dto));
                    dto.setDefaultAssignmentConstraints(roleManagementConfig == null ? null : roleManagementConfig.getDefaultAssignmentConstraints());
                }
                dto.setSimpleView(true);
                return dto;
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return createContentQuery();
            }
        };
//        searchModel.getObject().setTypeClass(getQueryClass());
        return provider;
    }

    private boolean isAlreadyAssigned(PrismObject<AbstractRoleType> obj, AssignmentEditorDto assignmentDto) {
        UserType user = targetUserModel.getObject();
        if (user == null || user.getAssignment() == null) {
            return false;
        }
        boolean isAssigned = false;
        List<QName> assignedRelationsList = new ArrayList<>();
        for (AssignmentType assignment : user.getAssignment()) {
            if (assignment.getTargetRef() != null && assignment.getTargetRef().getOid().equals(obj.getOid())) {
                isAssigned = true;
                assignedRelationsList.add(assignment.getTargetRef().getRelation());
            }
        }
        assignmentDto.setAssignedRelationsList(assignedRelationsList);
        return isAssigned;
    }

    protected boolean isShoppingCartItemsPanelVisible() {
        return true;
    }

    protected void appendItemsPanelStyle(WebMarkupContainer container) {
        container.add(AttributeAppender.append("class", "col-md-12"));
    }

    protected ObjectQuery createContentQuery() {
        ObjectQuery memberQuery = getPrismContext().queryFor(getQueryClass())
                .type(getQueryClass())
                .build();
        ObjectFilter assignableRolesFilter = getAssignableRolesFilter();
        if (assignableRolesFilter != null) {
            memberQuery.addFilter(assignableRolesFilter);
        }
        return memberQuery;
    }

    private SearchPanel<R> getSearchPanel() {
        return (SearchPanel) get(createComponentPath(ID_SHOPPING_CART_CONTAINER, ID_SEARCH_FORM, ID_SEARCH));
    }

    private ObjectFilter getAssignableRolesFilter() {
        // When multiple users are selected, filter the roles by targeting one of them
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNABLE_ROLES);
        OperationResult result = task.getResult();
        UserType targetUser = targetUserModel.getObject();
        if (targetUser == null) {
            return null;
        }
        return WebComponentUtil.getAssignableRolesFilter(
                targetUser.asPrismObject(), ObjectTypes.getObjectTypeClass(getQueryType()),
                getNewAssignmentRelation(), WebComponentUtil.AssignmentOrder.ASSIGNMENT,
                result, task, getPageBase());
    }

    private Class<R> getQueryClass() {
        return (Class<R>) WebComponentUtil.qnameToClass(getPageBase().getPrismContext(), getQueryType());
    }

    protected abstract QName getQueryType();

    private UserType getTargetUser() {
        String targetUserOid = getRoleCatalogStorage().isSelfRequest() ?
                getPageBase().getPrincipalFocus().getOid()
                : getRoleCatalogStorage().getTargetUserOidsList().get(0);
        if (StringUtils.isEmpty(targetUserOid)) {
            return null;
        }
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_TARGET_USER);
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNMENT_TARGET_USER);

        PrismObject<UserType> targetUser = WebModelServiceUtils.loadObject(UserType.class, targetUserOid, getPageBase(), task, result);
        return targetUser != null ? targetUser.asObjectable() : null;
    }

    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target) {
        getPage().success(getPageBase().createStringResource("AbstractShoppingCartTabPanel.itemIsAddedToShoppingCart",
                getRoleCatalogStorage().getAssignmentShoppingCart().size()).getString());
        getPageBase().reloadShoppingCartIcon(target);
        target.add(AbstractShoppingCartTabPanel.this);
        target.add(getPageBase().getFeedbackPanel());
    }

    protected QName getNewAssignmentRelation() {
        return getRoleCatalogStorage().getSelectedRelation() != null ?
                getRoleCatalogStorage().getSelectedRelation() : WebComponentUtil.getDefaultRelationOrFail();
    }

    protected RoleCatalogStorage getRoleCatalogStorage() {
        return getPageBase().getSessionStorage().getRoleCatalog();
    }

    protected GridViewComponent getGridViewComponent() {
        return (GridViewComponent) get(createComponentPath(ID_SHOPPING_CART_CONTAINER, ID_SHOPPING_CART_ITEMS_PANEL));
    }
}
