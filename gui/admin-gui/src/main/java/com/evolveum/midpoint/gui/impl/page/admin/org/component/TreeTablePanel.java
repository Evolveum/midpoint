/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.org.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.model.SelectableObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrg;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreeAssignablePanel;
import com.evolveum.midpoint.web.page.admin.orgs.OrgTreePanel;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgTree;
import com.evolveum.midpoint.web.session.OrgTreeStateStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Used as a main component of the Org tree page.
 * <p>
 * todo create function computeHeight() in midpoint.js, update height properly
 * when in "mobile" mode... [lazyman] todo implement midpoint theme for tree
 * [lazyman]
 *
 * @author lazyman
 * @author katkav
 */
public class TreeTablePanel extends BasePanel<String> {

    private static final long serialVersionUID = 1L;

    protected static final String DOT_CLASS = TreeTablePanel.class.getName() + ".";
    protected static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    protected static final String OPERATION_MOVE_OBJECT = DOT_CLASS + "moveObject";
    protected static final String OPERATION_RECOMPUTE = DOT_CLASS + "recompute";
    protected static final String OPERATION_SEARCH_MANAGERS = DOT_CLASS + "searchManagers";
    protected static final String OPERATION_COUNT_CHILDREN = DOT_CLASS + "countChildren";

    private static final String OPERATION_LOAD_MANAGERS = DOT_CLASS + "loadManagers";
    private static final String ID_MANAGER_SUMMARY = "managerSummary";

    private static final String ID_TREE_PANEL = "treePanel";
    private static final String ID_MEMBER_PANEL = "memberPanel";
    protected static final String ID_CONTAINER_MANAGER = "managerContainer";
    protected static final String ID_MANAGER_TABLE = "managerTable";

    private static final Trace LOGGER = TraceManager.getTrace(TreeTablePanel.class);

    public TreeTablePanel(String id, IModel<String> rootOid) {
        super(id, rootOid);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        OrgTreePanel treePanel = new OrgTreePanel(ID_TREE_PANEL, getModel(), false, getPageBase()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void selectTreeItemPerformed(TreeSelectableBean<OrgType> selected,
                    AjaxRequestTarget target) {
                TreeTablePanel.this.selectTreeItemPerformed(selected, target);
            }

            protected List<InlineMenuItem> createTreeMenu() {
                return TreeTablePanel.this.createTreeMenu();
            }

            @Override
            protected List<InlineMenuItem> createTreeChildrenMenu(TreeSelectableBean<OrgType> org) {
                return TreeTablePanel.this.createTreeChildrenMenu(org);
            }

        };
        treePanel.setOutputMarkupId(true);
        add(treePanel);
        add(createMemberPanel());
//
        add(createManagerPanel());
        setOutputMarkupId(true);
    }

    private com.evolveum.midpoint.gui.impl.page.admin.org.component.OrgMemberPanel createMemberPanel() {
        FocusDetailsModels focusDetailsModels = new FocusDetailsModels(createOrgModel(), getPageBase());
        Optional<ContainerPanelConfigurationType> pc = focusDetailsModels.getPanelConfigurations()
                .stream()
                .filter(c -> "orgMembers".equals(((ContainerPanelConfigurationType) c).getIdentifier())).findFirst();
        ContainerPanelConfigurationType panelConfig = null;
        if (pc.isPresent()) {
            panelConfig = pc.get();
        }
        OrgMemberPanel memberPanel = new OrgMemberPanel(ID_MEMBER_PANEL, focusDetailsModels, panelConfig) {
                    @Override
                    protected CollectionPanelType getPanelType() {
                        return CollectionPanelType.MEMBER_ORGANIZATION;
                    }

        };
        memberPanel.setOutputMarkupId(true);
        return memberPanel;
    }

    class LoadableOrgModel extends LoadableDetachableModel<PrismObject<OrgType>> {

        private IModel<TreeSelectableBean<OrgType>> treeSelectableBean;

        LoadableOrgModel(IModel<TreeSelectableBean<OrgType>> selected) {
            super();
            this.treeSelectableBean = selected;
        }

        @Override
        protected PrismObject<OrgType> load() {
            OrgType orgType = treeSelectableBean.getObject().getValue();
            if (orgType == null) {
                return null;
            }
            return orgType.asPrismObject();
        }

        @Override
        protected void onDetach() {
            treeSelectableBean.detach();
        }
    }

    private LoadableDetachableModel<PrismObject<OrgType>> createOrgModel() {
        return new LoadableOrgModel(getTreePanel().getSelectedOrgModel());
    }

    private <F extends FocusType> WebMarkupContainer createManagerPanel() {
        WebMarkupContainer managerContainer = new WebMarkupContainer(ID_CONTAINER_MANAGER);
        managerContainer.setOutputMarkupId(true);
        managerContainer.setOutputMarkupPlaceholderTag(true);

        ListView<F> listView = new ListView<>(ID_MANAGER_TABLE, createManagersModel()) {

            @Override
            protected void populateItem(ListItem<F> item) {
                FocusSummaryPanel.addSummaryPanel(item, item.getModel(), ID_MANAGER_SUMMARY, WebComponentUtil.getSummaryPanelSpecification(item.getModelObject().getClass(), getPageBase().getCompiledGuiProfile()));
            }

            @Override
            protected IModel<F> getListItemModel(IModel<? extends List<F>> listViewModel, int index) {
                List<F> objects = listViewModel.getObject();
                if (objects == null) {
                    return super.getListItemModel(listViewModel, index);
                }

                F focus = objects.get(index);
                if (focus == null) {
                    return super.getListItemModel(listViewModel, index);
                }
                SelectableObjectModel<F> model = new SelectableObjectModel<>(focus, null) {
                    @Override
                    protected F load() {
                        Task task = getPageBase().createSimpleTask("Load manager");
                        OperationResult result = task.getResult();
                        PrismObject<F> manager = WebModelServiceUtils.loadObject(getType(), getOid(), getPageBase(), task, result);
                        if (manager == null) {
                            return null;
                        }
                        return manager.asObjectable();
                    }
                };
                return model;
            }
        };
        managerContainer.add(listView);

        return managerContainer;
    }

    private <F extends FocusType> LoadableDetachableModel<List<F>> createManagersModel() {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<F> load() {
                ObjectQuery managersQuery = createManagerQuery(getTreePanel().getSelected().getValue());

                OperationResult searchManagersResult = new OperationResult(OPERATION_SEARCH_MANAGERS);
                Collection<SelectorOptions<GetOperationOptions>> options = getSchemaService().getOperationOptionsBuilder()
                        .distinct()
                        .item(FocusType.F_JPEG_PHOTO).retrieve()
                        .build();
                @NotNull List<PrismObject<F>> managers = (List) WebModelServiceUtils.searchObjects(FocusType.class,
                        managersQuery, options, searchManagersResult, getPageBase());
                return managers.stream().map(manager -> manager.asObjectable()).collect(Collectors.toList());
            }
        };
    }

    private ObjectQuery createManagerQuery(OrgType org) {
        ObjectQuery query = ObjectTypeUtil.createManagerQuery(FocusType.class, org.getOid(),
                getPageBase().getRelationRegistry(), getPageBase().getPrismContext());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Searching members of org {} with query:\n{}", org.getOid(), query.debugDump());
        }
        return query;
    }

    private OrgTreePanel getTreePanel() {
        return (OrgTreePanel) get(ID_TREE_PANEL);
    }

    private List<InlineMenuItem> createTreeMenu() {
        return new ArrayList<>();
    }

    private List<InlineMenuItem> createTreeChildrenMenu(TreeSelectableBean<OrgType> org) {
        List<InlineMenuItem> items = new ArrayList<>();

        boolean isAllowModify = isAllowModify(org.getValue());
        boolean isAllowRead = isAllowRead(org.getValue());
        InlineMenuItem item;

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ORG_MOVE_ACTION_URI)) {
            item = new InlineMenuItem(createStringResource("TreeTablePanel.move")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<SelectableBeanImpl<OrgType>>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            moveRootPerformed(org, target);
                        }
                    };
                }
            };
            items.add(item);
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ORG_MAKE_ROOT_ACTION_URI)) {
            item = new InlineMenuItem(createStringResource("TreeTablePanel.makeRoot")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<SelectableBeanImpl<OrgType>>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            makeRootPerformed(org, target);
                        }
                    };
                }
            };
            items.add(item);
        }

        item = new InlineMenuItem(createStringResource("TreeTablePanel.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<OrgType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteNodePerformed(org, target);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(isAllowDelete(org.getValue()));
            }

        };
        items.add(item);

        item = new InlineMenuItem(createStringResource("TreeTablePanel.recompute")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<OrgType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        recomputeRootPerformed(org, target);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(isAllowModify);
            }
        };
        items.add(item);

        InlineMenuItem editMenuItem = new InlineMenuItem(createStringResource("TreeTablePanel.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<OrgType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editRootPerformed(org);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(isAllowModify);
            }
        };
        items.add(editMenuItem);

        item = new InlineMenuItem(createStringResource("TreeTablePanel.viewDetails")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<OrgType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editRootPerformed(org);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(!editMenuItem.getVisible().getObject() && isAllowRead);
            }
        };
        items.add(item);

        item = new InlineMenuItem(createStringResource("TreeTablePanel.createChild")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBeanImpl<OrgType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            initObjectForAdd(
                                    ObjectTypeUtil.createObjectRef(org.getValue(), getPageBase().getPrismContext()), target);
                        } catch (SchemaException e) {
                            throw new SystemException(e.getMessage(), e);
                        }
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(isAllowRead && isAllowAddNew());
            }
        };
        items.add(item);
        return items;
    }

    private boolean isAllowRead(OrgType org) {
        boolean allowRead = false;
        try {
            allowRead = org == null ||
                    getPageBase().isAuthorized(
                            ModelAuthorizationAction.GET.getUrl(),
                            AuthorizationPhaseType.REQUEST, org.asPrismObject(),
                            null, null);
        } catch (Throwable ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to check menu items authorizations", ex);
        }
        return allowRead;
    }

    private boolean isAllowModify(OrgType org) {
        boolean allowModify = false;
        try {
            allowModify = org == null ||
                    getPageBase().isAuthorized(
                            ModelAuthorizationAction.MODIFY.getUrl(),
                            AuthorizationPhaseType.REQUEST, org.asPrismObject(),
                            null, null);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException
                | CommunicationException | ConfigurationException | SecurityViolationException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to check menu items authorizations", ex);
        }
        return allowModify;
    }

    private boolean isAllowAddNew() {
        boolean allowAddNew = false;
        try {
            allowAddNew = getPageBase().isAuthorized(
                    ModelAuthorizationAction.ADD.getUrl(),
                    AuthorizationPhaseType.REQUEST, new OrgType().asPrismObject(),
                    null, null);
        } catch (Throwable ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to check menu items authorizations", ex);
        }
        return allowAddNew;
    }

    private boolean isAllowDelete(OrgType org) {
        boolean allowDelete = false;
        try {
            allowDelete = org == null ||
                    getPageBase().isAuthorized(
                            ModelAuthorizationAction.DELETE.getUrl(),
                            AuthorizationPhaseType.REQUEST, org.asPrismObject(),
                            null, null);
        } catch (Throwable ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to check menu items authorizations", ex);
        }
        return allowDelete;
    }

    // TODO: merge this with AbstractRoleMemberPanel.initObjectForAdd, also see MID-3233
    private <O extends ObjectType> void initObjectForAdd(ObjectReferenceType parentOrgRef, AjaxRequestTarget target) throws SchemaException {
        TreeTablePanel.this.getPageBase().hideMainPopup(target);
        PrismContext prismContext = TreeTablePanel.this.getPageBase().getPrismContext();
        PrismObjectDefinition<O> def = prismContext.getSchemaRegistry().findObjectDefinitionByType(OrgType.COMPLEX_TYPE);
        PrismObject<O> obj = def.instantiate();

        O objType = obj.asObjectable();
        if (obj.getCompileTimeClass() != null && FocusType.class.isAssignableFrom(obj.getCompileTimeClass())) {
            AssignmentType assignment = new AssignmentType();
            assignment.setTargetRef(parentOrgRef);
            ((FocusType) objType).getAssignment().add(assignment);
        }

        // Set parentOrgRef in any case. This is not strictly correct.
        // The parentOrgRef should be added by the projector. But
        // this is needed to successfully pass through security
        // TODO: fix MID-3234
        if (parentOrgRef == null) {
            ObjectType org = getTreePanel().getSelected().getValue();
            parentOrgRef = ObjectTypeUtil.createObjectRef(org, prismContext);
            parentOrgRef.setRelation(null);
            objType.getParentOrgRef().add(parentOrgRef);
        } else {
            objType.getParentOrgRef().add(parentOrgRef.clone());
        }

        DetailsPageUtil.dispatchToObjectDetailsPage(obj, true, this);

    }

    private void selectTreeItemPerformed(TreeSelectableBean<OrgType> selected, AjaxRequestTarget target) {
        if (selected.getValue() == null) {
            return;
        }
        getTreePanel().setSelected(selected);
        getTreePanel().refreshContentPannels();
        target.add(addOrReplace(createMemberPanel()));
        target.add(addOrReplace(createManagerPanel()));
    }

    private void moveRootPerformed(final TreeSelectableBean<OrgType> root, AjaxRequestTarget target) {

        OrgTreeAssignablePanel orgAssignablePanel = new OrgTreeAssignablePanel(
                getPageBase().getMainPopupBodyId(), false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onItemSelect(SelectableBeanImpl<OrgType> selected, AjaxRequestTarget target) {
                moveConfirmPerformed(root, selected, target);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected OrgType getAssignmentOwnerObject() {
                return root.getValue();
            }
        };

        orgAssignablePanel.setOutputMarkupId(true);
        getPageBase().showMainPopup(orgAssignablePanel, target);

    }

    private void moveConfirmPerformed(SelectableBeanImpl<OrgType> orgToMove, SelectableBeanImpl<OrgType> selected,
            AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);

        Task task = getPageBase().createSimpleTask(OPERATION_MOVE_OBJECT);
        OperationResult result = new OperationResult(OPERATION_MOVE_OBJECT);

        OrgType toMove = orgToMove.getValue();
        if (toMove == null || selected.getValue() == null) {
            return;
        }
        ObjectDelta<OrgType> moveOrgDelta = getPageBase().getPrismContext().deltaFactory().object()
                .createEmptyModifyDelta(OrgType.class, toMove.getOid()
                );

        try {
            for (ObjectReferenceType parentOrgRef : toMove.getParentOrgRef()) {
                AssignmentType oldRoot = new AssignmentType();
                oldRoot.setTargetRef(ObjectTypeUtil.createObjectRef(parentOrgRef.asReferenceValue().getObject(), getPageBase().getPrismContext()));

                moveOrgDelta.addModification(getPrismContext().deltaFactory().container().createModificationDelete(OrgType.F_ASSIGNMENT,
                        OrgType.class, oldRoot.asPrismContainerValue()));
            }

            AssignmentType newRoot = new AssignmentType();
            newRoot.setTargetRef(ObjectTypeUtil.createObjectRef(selected.getValue(), getPageBase().getPrismContext()));
            moveOrgDelta.addModification(getPrismContext().deltaFactory().container().createModificationAdd(OrgType.F_ASSIGNMENT,
                    OrgType.class, newRoot.asPrismContainerValue()));

            getPageBase().getPrismContext().adopt(moveOrgDelta);
            getPageBase().getModelService()
                    .executeChanges(MiscUtil.createCollection(moveOrgDelta), null, task, result);
            result.computeStatus();
        } catch (Throwable e) {
            result.recordFatalError(getString("TreeTablePanel.message.moveConfirmPerformed.fatalError", toMove), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to move organization unit" + toMove, e);
        }

        getPageBase().showResult(result);
        target.add(getPageBase().getFeedbackPanel());
        if (getPageBase() instanceof PageOrgTree && ((PageOrgTree) getPageBase()).getTabPanel() != null
                && ((PageOrgTree) getPageBase()).getTabPanel().getTabbedPanel() != null) {
            ((PageOrgTree) getPageBase()).getTabPanel().getTabbedPanel().setSelectedTab(0);
        }
        setResponsePage(PageOrgTree.class);

    }

    private void makeRootPerformed(SelectableBeanImpl<OrgType> newRoot, AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OPERATION_MOVE_OBJECT);
        OperationResult result = new OperationResult(OPERATION_MOVE_OBJECT);

        OrgType toMove = newRoot.getValue();
        if (toMove == null) {
            return;
        }
        ObjectDelta<OrgType> moveOrgDelta = getPageBase().getPrismContext().deltaFactory().object()
                .createEmptyModifyDelta(OrgType.class, toMove.getOid()
                );

        try {
            for (ObjectReferenceType parentOrg : toMove.getParentOrgRef()) {
                AssignmentType oldRoot = new AssignmentType();
                oldRoot.setTargetRef(parentOrg);

                moveOrgDelta.addModification(getPrismContext().deltaFactory().container().createModificationDelete(OrgType.F_ASSIGNMENT,
                        OrgType.class, oldRoot.asPrismContainerValue()));
            }

            getPageBase().getPrismContext().adopt(moveOrgDelta);
            getPageBase().getModelService()
                    .executeChanges(MiscUtil.createCollection(moveOrgDelta), null, task, result);
            result.computeStatus();
        } catch (Throwable e) {
            result.recordFatalError(getString("TreeTablePanel.message.moveConfirmPerformed.fatalError", toMove), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to move organization unit" + toMove, e);
        }

        getPageBase().showResult(result);
        target.add(getPageBase().getFeedbackPanel());
        // target.add(getTreePanel());
        setResponsePage(PageOrgTree.class);
    }

    private void recomputeRootPerformed(SelectableBeanImpl<OrgType> root, AjaxRequestTarget target) {
        if (root == null) {
            root = getTreePanel().getRootFromProvider();
        }

        recomputePerformed(root, target);
    }

    private void recomputePerformed(SelectableBeanImpl<OrgType> orgToRecompute, AjaxRequestTarget target) {

        Task task = getPageBase().createSimpleTask(OPERATION_RECOMPUTE);
        OperationResult result = new OperationResult(OPERATION_RECOMPUTE);
        if (orgToRecompute.getValue() == null) {
            return;
        }
        try {
            ObjectDelta emptyDelta = getPageBase().getPrismContext().deltaFactory().object().createEmptyModifyDelta(OrgType.class,
                    orgToRecompute.getValue().getOid());
            ModelExecuteOptions options = new ModelExecuteOptions();
            options.reconcile();
            getPageBase().getModelService().executeChanges(MiscUtil.createCollection(emptyDelta),
                    options, task, result);

            result.recordSuccess();
        } catch (Exception e) {
            result.recordFatalError(getString("TreeTablePanel.message.recomputeError"), e);
            LoggingUtils.logUnexpectedException(LOGGER, getString("TreeTablePanel.message.recomputeError"), e);
        }

        getPageBase().showResult(result);
        target.add(getPageBase().getFeedbackPanel());
        getTreePanel().refreshTabbedPanel(target);
    }

    private void deleteNodePerformed(final SelectableBeanImpl<OrgType> orgToDelete, AjaxRequestTarget target) {

        ConfirmationPanel confirmationPanel = new DeleteConfirmationPanel(getPageBase().getMainPopupBodyId(),
                new IModel<String>() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        if (hasChildren(orgToDelete)) {
                            return createStringResource("TreeTablePanel.message.warn.deleteTreeObjectConfirm",
                                    WebComponentUtil.getEffectiveName(orgToDelete.getValue(),
                                            OrgType.F_DISPLAY_NAME)).getObject();
                        }
                        return createStringResource("TreeTablePanel.message.deleteTreeObjectConfirm",
                                WebComponentUtil.getEffectiveName(orgToDelete.getValue(),
                                        OrgType.F_DISPLAY_NAME)).getObject();
                    }
                }) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteNodeConfirmedPerformed(orgToDelete, target);
            }
        };

        confirmationPanel.setOutputMarkupId(true);
        getPageBase().showMainPopup(confirmationPanel, target);
    }

    private boolean hasChildren(SelectableBeanImpl<OrgType> orgToDelete) {
        ObjectQuery query = getPageBase().getPrismContext().queryFor(ObjectType.class)
                .isChildOf(orgToDelete.getValue().getOid())            // TODO what if orgToDelete.getValue()==null
                .build();
        Task task = getPageBase().createSimpleTask(OPERATION_COUNT_CHILDREN);
        OperationResult result = new OperationResult(OPERATION_COUNT_CHILDREN);
        try {
            int count = getPageBase().getModelService().countObjects(ObjectType.class,
                    query, null, task, result);
            return (count > 0);
        } catch (Throwable e) {
            LoggingUtils.logUnexpectedException(LOGGER, e.getMessage(), e);
            result.recordFatalError(getString("TreeTablePanel.message.hasChildren.fatalError", orgToDelete.getValue()), e);
            return false;
        }
    }

    private void deleteNodeConfirmedPerformed(SelectableBeanImpl<OrgType> orgToDelete, AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);

        PageBase page = getPageBase();

        if (orgToDelete == null) {
            orgToDelete = getTreePanel().getRootFromProvider();
        }
        if (orgToDelete.getValue() == null) {
            return;
        }

        if (CollectionUtils.isEmpty(orgToDelete.getValue().getParentOrgRef())) {
            OrgTreeStateStorage storage = getTreePanel().getOrgTreeStateStorage();
            if (storage != null) {
                storage.setSelectedTabId(-1);
            }
            getTreePanel().setSelectedItem(null, storage);
        }
        String oidToDelete = orgToDelete.getValue().getOid();
        WebModelServiceUtils.deleteObject(OrgType.class, oidToDelete, result, page);

        result.computeStatusIfUnknown();
        page.showResult(result);
        target.add(getPageBase().getFeedbackPanel());

        // even if we theoretically could refresh page only if non-leaf node is deleted,
        // for simplicity we do it each time
        //
        // Instruction to refresh only the part would be:
        //  - getTreePanel().refreshTabbedPanel(target);
        //
        // But how to refresh whole page? target.add(getPage()) is not sufficient - content is unchanged;
        // so we use the following.
        // TODO is this ok? [pmed]
        throw new RestartResponseException(getPage().getClass());
    }

    private void editRootPerformed(SelectableBeanImpl<OrgType> root) {
        if (root == null) {
            root = getTreePanel().getRootFromProvider();
        }
        if (root.getValue() == null) {
            return;
        }
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, root.getValue().getOid());
        getPageBase().navigateToNext(PageOrg.class, parameters);
    }
}
