/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.AssignmentPopup;
import com.evolveum.midpoint.gui.api.component.AssignmentPopupDto;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.AssignmentsDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment.AbstractAssignmentPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.AssignmentListProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractAssignmentTypePanel extends MultivalueContainerListPanelWithDetailsPanel<AssignmentType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentPanel.class);

    private static final String DOT_CLASS = AssignmentPanel.class.getName() + ".";
    protected static final String OPERATION_LOAD_ASSIGNMENTS_LIMIT = DOT_CLASS + "loadAssignmentsLimit";
    protected static final String OPERATION_LOAD_ASSIGNMENTS_TARGET_OBJ = DOT_CLASS + "loadAssignmentsTargetRefObject";
    protected static final String OPERATION_LOAD_ASSIGNMENT_TARGET_RELATIONS = DOT_CLASS + "loadAssignmentTargetRelations";

    private IModel<PrismContainerWrapper<AssignmentType>> model;
    protected int assignmentsRequestsLimit = -1;

    public AbstractAssignmentTypePanel(String id, IModel<PrismContainerWrapper<AssignmentType>> model, ContainerPanelConfigurationType config) {
        super(id, AssignmentType.class, config);
        this.model = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        assignmentsRequestsLimit = AssignmentsUtil.loadAssignmentsLimit(new OperationResult(OPERATION_LOAD_ASSIGNMENTS_LIMIT), getPageBase());
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return isNewObjectButtonVisible(getFocusObject());
    }

    @Override
    protected IModel<PrismContainerWrapper<AssignmentType>> getContainerModel() {
        return model;
    }

    @Override
    protected void cancelItemDetailsPerformed(AjaxRequestTarget target) {
        target.add(AbstractAssignmentTypePanel.this);
    }

    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());
        columns.add(createAssignmentIconColumn());
        columns.add(createAssignmentNameColumn());
        columns.add(new PrismContainerWrapperColumn<>(getContainerModel(), AssignmentType.F_ACTIVATION, getPageBase()));

        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> additionalColumns = initColumns();
        if (additionalColumns != null) {
            columns.addAll(additionalColumns);
        }
        columns.add(createAssignmentActionColumn());
        return columns;
    }

    @NotNull
    protected abstract List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns();

    private IColumn<PrismContainerValueWrapper<AssignmentType>, String> createAssignmentActionColumn() {
        return new InlineMenuButtonColumn<>(getAssignmentMenuActions(), getPageBase()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isButtonMenuItemEnabled(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                if (rowModel != null
                        && ValueStatus.ADDED.equals(rowModel.getObject().getStatus())) {
                    return true;
                }
                return !isAssignmentsLimitReached();
            }
        };
    }

    private <AH extends FocusType> List<InlineMenuItem> getAssignmentMenuActions() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        PrismObject<AH> obj = getFocusObject();
        try {
            boolean isUnassignAuthorized = getPageBase().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI,
                    AuthorizationPhaseType.REQUEST, obj,
                    null, null, null);
            if (isUnassignAuthorized) {
                menuItems.add(new ButtonInlineMenuItem(getAssignmentsLimitReachedUnassignTitleModel()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public CompositedIconBuilder getIconCompositedBuilder() {
                        return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
                    }

                    @Override
                    public InlineMenuItemAction initAction() {
                        return AbstractAssignmentTypePanel.this.createDeleteColumnAction();
                    }
                });
            }

        } catch (Exception ex) {
            LOGGER.error("Couldn't check unassign authorization for the object: {}, {}", obj.getName(), ex.getLocalizedMessage());
            if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI)) {
                menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.unassign")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public CompositedIconBuilder getIconCompositedBuilder() {
                        return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
                    }

                    @Override
                    public InlineMenuItemAction initAction() {
                        return AbstractAssignmentTypePanel.this.createDeleteColumnAction();
                    }
                });
            }
        }
        menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return AbstractAssignmentTypePanel.this.createEditColumnAction();
            }
        });
        ButtonInlineMenuItem menu = new ButtonInlineMenuItem(createStringResource("AssignmentPanel.viewTargetObject")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_NAVIGATE_ARROW);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<PrismContainerValueWrapper<AssignmentType>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PrismContainerValueWrapper<AssignmentType> assignmentContainer = getRowModel().getObject();
                        PrismReferenceWrapper<ObjectReferenceType> targetRef;
                        try {
                            targetRef = assignmentContainer.findReference(ItemPath.create(AssignmentType.F_TARGET_REF));
                        } catch (SchemaException e) {
                            getSession().error("Couldn't show details page. More information provided in log.");
                            LOGGER.error("Couldn't show details page, no targetRef reference wrapper found: {}", e.getMessage(), e);
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }

                        if (targetRef != null && targetRef.getValues() != null && targetRef.getValues().size() > 0) {
                            PrismReferenceValueWrapperImpl<ObjectReferenceType> refWrapper = targetRef.getValues().get(0);
                            if (!StringUtils.isEmpty(refWrapper.getNewValue().getOid())) {
                                Class<? extends ObjectType> targetClass = ObjectTypes.getObjectTypeFromTypeQName(refWrapper.getRealValue().getType()).getClassDefinition();
                                WebComponentUtil.dispatchToObjectDetailsPage(targetClass, refWrapper.getNewValue().getOid(), AbstractAssignmentTypePanel.this, false);
                            }
                        }
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

        };
        menu.setVisibilityChecker((InlineMenuItem.VisibilityChecker) (rowModel, isHeader) -> {
            PrismContainerValueWrapper<AssignmentType> assignment =
                    (PrismContainerValueWrapper<AssignmentType>) rowModel.getObject();
            if (assignment == null) {
                return false;
            }
            PrismReferenceWrapper<Referencable> target = null;
            try {
                target = assignment.findReference(AssignmentType.F_TARGET_REF);
            } catch (Exception e) {
                LOGGER.error("Couldn't find targetRef in assignment");
            }
            return target != null && !target.isEmpty();
        });
        menuItems.add(menu);
        return menuItems;
    }

    private IColumn<PrismContainerValueWrapper<AssignmentType>, String> createAssignmentNameColumn() {
        return new AjaxLinkColumn<>(createStringResource("PolicyRulesPanel.nameColumn")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                return new LoadableModel<>() {
                    @Override
                    protected String load() {
                        LOGGER.trace("Create name for AssignmentType: " + rowModel.getObject().getRealValue());
                        String name = AssignmentsUtil.getName(rowModel.getObject(), getPageBase());
                        LOGGER.trace("Name for AssignmentType: " + name);
                        if (StringUtils.isBlank(name)) {
                            return createStringResource("AssignmentPanel.noName").getString();
                        }

                        return name;
                    }
                };

            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                return rowModel.getObject().getRealValue().getFocusMappings() == null;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                AbstractAssignmentTypePanel.this.itemDetailsPerformed(target, rowModel);
            }
        };
    }

    private IColumn<PrismContainerValueWrapper<AssignmentType>, String> createAssignmentIconColumn() {
        return new CompositedIconColumn<>(Model.of("")) {

            @Override
            protected CompositedIcon getCompositedIcon(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                AssignmentType assignment = rowModel.getObject().getRealValue();
                LOGGER.trace("Create icon for AssignmentType: " + assignment);
                PrismObject<? extends FocusType> object = loadTargetObject(assignment);
                if (object != null) {
                    return WebComponentUtil.createCompositeIconForObject(object.asObjectable(),
                            new OperationResult("create_assignment_composited_icon"), getPageBase());
                }
                String displayType = WebComponentUtil.createDefaultBlackIcon(AssignmentsUtil.getTargetType(assignment));
                CompositedIconBuilder iconBuilder = new CompositedIconBuilder();
                iconBuilder.setBasicIcon(displayType, IconCssStyle.IN_ROW_STYLE);
                return iconBuilder.build();
            }
        };
    }

    protected <F extends FocusType> PrismObject<F> loadTargetObject(AssignmentType assignmentType) {
        if (assignmentType == null) {
            return null;
        }

        ObjectReferenceType targetRef = assignmentType.getTargetRef();
        if (targetRef == null || targetRef.getOid() == null) {
            return null;
        }

        PrismObject<F> targetObject = targetRef.getObject();
        if (targetObject == null) {
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNMENTS_TARGET_OBJ);
            OperationResult result = task.getResult();
            targetObject = WebModelServiceUtils.loadObject(targetRef, getPageBase(), task, result);
            result.recomputeStatus();
        }
        return targetObject;
    }
    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> bar = new ArrayList<>();

        AjaxIconButton newObjectButton = new AjaxIconButton(idButton, new Model<>(GuiStyleConstants.EVO_ASSIGNMENT_ICON),
                createStringResource("MainObjectListPanel.newObject")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                newAssignmentClickPerformed(target);
            }
        };
        newObjectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        bar.add(newObjectButton);

        newObjectButton.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isNewObjectButtonVisible(getFocusObject());
            }

            @Override
            public boolean isEnabled() {
                return !isAssignmentsLimitReached();
            }
        });
        return bar;
    }

    protected void newAssignmentClickPerformed(AjaxRequestTarget target) {
        AssignmentPopup popupPanel = new AssignmentPopup(getPageBase().getMainPopupBodyId(), createAssignmentPopupModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
                super.addPerformed(target, newAssignmentsList);
                addSelectedAssignmentsPerformed(target, newAssignmentsList);
            }

            @Override
            protected List<ObjectTypes> getObjectTypesList() {
                return AbstractAssignmentTypePanel.this.getObjectTypesList();
            }

            @Override
            protected ObjectFilter getSubtypeFilter() {
                return AbstractAssignmentTypePanel.this.getSubtypeFilter();
            }

            @Override
            protected boolean isEntitlementAssignment() {
                return AbstractAssignmentTypePanel.this.isEntitlementAssignment();
            }

            @Override
            protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                return AbstractAssignmentTypePanel.this.getContainerModel().getObject();
            }

            @Override
            protected <F extends AssignmentHolderType> PrismObject<F> getFocusObject() {
                return AbstractAssignmentTypePanel.this.getFocusObject();
            }
        };
        popupPanel.setOutputMarkupId(true);
        popupPanel.setOutputMarkupPlaceholderTag(true);
        getPageBase().showMainPopup(popupPanel, target);
    }

    protected List<ObjectTypes> getObjectTypesList() {
        QName assignmentType = getAssignmentType();
        if (assignmentType == null) {
            return Collections.EMPTY_LIST;
        }
        return Collections.singletonList(ObjectTypes.getObjectTypeFromTypeQName(assignmentType));
    }

    protected RefFilter getTargetTypeFilter() {
        QName targetType = getAssignmentType();
        RefFilter targetRefFilter = null;
        if (targetType != null) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setType(targetType);
            ort.setRelation(new QName(PrismConstants.NS_QUERY, "any"));
            targetRefFilter = (RefFilter) getPageBase().getPrismContext().queryFor(AssignmentType.class)
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(ort.asReferenceValue())
                    .buildFilter();
            targetRefFilter.setOidNullAsAny(true);
        }
        return targetRefFilter;
    }

    protected ObjectFilter getSubtypeFilter() {
        return null;
    }

    protected abstract IModel<AssignmentPopupDto> createAssignmentPopupModel();

    protected abstract QName getAssignmentType();

    protected void addSelectedAssignmentsPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
        if (CollectionUtils.isEmpty(newAssignmentsList)) {
            warn(getPageBase().getString("AssignmentTablePanel.message.noAssignmentSelected"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        boolean isAssignmentsLimitReached = isAssignmentsLimitReached(newAssignmentsList.size(), true);
        if (isAssignmentsLimitReached) {
            warn(getPageBase().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        newAssignmentsList.forEach(assignment -> {

            PrismContainerValue<AssignmentType> newAssignment = getContainerModel().getObject().getItem().createNewValue();
            AssignmentType assignmentType = newAssignment.asContainerable();

            if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
                assignmentType.setConstruction(assignment.getConstruction());
            } else {
                assignmentType.setTargetRef(assignment.getTargetRef());
            }
            AbstractAssignmentTypePanel.this.createNewItemContainerValueWrapper(newAssignment, getContainerModel().getObject(),
                    target);
        });
        AbstractAssignmentTypePanel.this.refreshTable(target);
        AbstractAssignmentTypePanel.this.reloadSavePreviewButtons(target);
        getPageBase().hideMainPopup(target);
    }

    @Override
    public void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<AssignmentType>> toDeleteList) {
        int countAddedAssignments = 0;
        for (PrismContainerValueWrapper<AssignmentType> assignment : toDeleteList) {
            if (ValueStatus.ADDED.equals(assignment.getStatus())) {
                countAddedAssignments++;
            }
        }
        boolean isLimitReached = isAssignmentsLimitReached(toDeleteList.size() - countAddedAssignments, true);
        if (isLimitReached) {
            warn(getPageBase().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        super.deleteItemPerformed(target, toDeleteList);
    }

    @Override
    protected AssignmentListProvider createProvider() {
        return createAssignmentProvider(getSearchModel(), loadValuesModel());
    }

    private AssignmentListProvider createAssignmentProvider(IModel<Search<AssignmentType>> searchModel, IModel<List<PrismContainerValueWrapper<AssignmentType>>> assignments) {
        AssignmentListProvider assignmentListProvider = new AssignmentListProvider(AbstractAssignmentTypePanel.this, searchModel, assignments) {

            @Override
            protected PageStorage getPageStorage() {
                return AbstractAssignmentTypePanel.this.getPageStorage(getAssignmentsTabStorageKey());
            }

            @Override
            protected List<PrismContainerValueWrapper<AssignmentType>> postFilter(List<PrismContainerValueWrapper<AssignmentType>> assignmentList) {
                return customPostSearch(assignmentList);
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return AbstractAssignmentTypePanel.this.getCustomizeQuery();
            }
        };
        assignmentListProvider.setCompiledObjectCollectionView(getObjectCollectionView());
        return assignmentListProvider;
    }

    protected abstract String getAssignmentsTabStorageKey();

    protected IModel<List<PrismContainerValueWrapper<AssignmentType>>> loadValuesModel() {
        return new PropertyModel<>(getContainerModel(), "values");
    }

    protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(List<PrismContainerValueWrapper<AssignmentType>> assignments) {
        return assignments;
    }

    protected abstract ObjectQuery getCustomizeQuery();

    @Override
    protected MultivalueContainerDetailsPanel<AssignmentType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<AssignmentType>> item) {
        return createMultivalueContainerDetailsPanel(item);
    }

    private MultivalueContainerDetailsPanel<AssignmentType> createMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<AssignmentType>> item) {
        if (isAssignmentsLimitReached()) {
            item.getModelObject().setReadOnly(true, true);
        } else if (item.getModelObject().isReadOnly()) {
            item.getModelObject().setReadOnly(false, true);
        }
        return new AssignmentsDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), isEntitlementAssignment(), getPanelConfiguration());
    }

    protected boolean isEntitlementAssignment() {
        return false;
    }

    @Override
    protected abstract UserProfileStorage.TableId getTableId();

    @Override
    protected List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        return createSearchableItems(containerDef);
    }

    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();

        addSpecificSearchableItems(containerDef, defs);
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);

        defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));

        return defs;

    }

    protected abstract void addSpecificSearchableItems(PrismContainerDefinition<AssignmentType> containerDef, List<SearchItemDefinition> defs);

    protected <AH extends AssignmentHolderType> boolean isNewObjectButtonVisible(PrismObject<AH> focusObject) {
        try {
            return getPageBase().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI,
                    AuthorizationPhaseType.REQUEST, focusObject,
                    null, null, null);
        } catch (Exception ex) {
            return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
        }
    }

    private boolean isAssignmentsLimitReached() {
        return isAssignmentsLimitReached(0, false);
    }

    @SuppressWarnings("deprecation")
    private boolean isAssignmentsLimitReached(int selectedAssignmentsCount, boolean actionPerformed) {
        if (assignmentsRequestsLimit < 0) {
            return false;
        }
        int changedItems = 0;
        List<PrismContainerValueWrapper<AssignmentType>> assignmentsList = getContainerModel().getObject().getValues();
        for (PrismContainerValueWrapper<AssignmentType> assignment : assignmentsList) {
            if (assignment.hasChanged()) {
                changedItems++;
            }
        }
        return actionPerformed ? (changedItems + selectedAssignmentsCount) > assignmentsRequestsLimit :
                (changedItems + selectedAssignmentsCount) >= assignmentsRequestsLimit;
    }

    private IModel<String> getAssignmentsLimitReachedUnassignTitleModel() {
        return new LoadableModel<>(true) {
            @Override
            protected String load() {
                return isAssignmentsLimitReached() ?
                        AbstractAssignmentTypePanel.this.getPageBase().createStringResource("RoleCatalogItemButton.assignmentsLimitReachedTitle",
                                assignmentsRequestsLimit).getString() : createStringResource("pageAdminFocus.menu.unassign").getString();
            }
        };
    }

}
