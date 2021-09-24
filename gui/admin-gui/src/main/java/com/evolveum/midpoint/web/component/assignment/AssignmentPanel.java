/*
 * Copyright (C) 2018-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.AssignmentPopupDto;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.AssignmentsDetailsPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.search.Search;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.AssignmentPopup;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
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
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.AssignmentListProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

//@PanelType(name = "allAssignments")
//@PanelInstance(identifier = "allAssignments",
//        applicableFor = AssignmentHolderType.class,
//        childOf = AssignmentHolderAssignmentPanel.class)
//@PanelDisplay(label = "All", icon = GuiStyleConstants.EVO_ASSIGNMENT_ICON, order = 10)
public class AssignmentPanel<AH extends AssignmentHolderType> extends BasePanel<PrismContainerWrapper<AssignmentType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentPanel.class);

    private static final String ID_ASSIGNMENTS = "assignments";

    private static final String DOT_CLASS = AssignmentPanel.class.getName() + ".";
    protected static final String OPERATION_LOAD_ASSIGNMENTS_LIMIT = DOT_CLASS + "loadAssignmentsLimit";
    protected static final String OPERATION_LOAD_ASSIGNMENTS_TARGET_OBJ = DOT_CLASS + "loadAssignmentsTargetRefObject";
    protected static final String OPERATION_LOAD_ASSIGNMENT_TARGET_RELATIONS = DOT_CLASS + "loadAssignmentTargetRelations";

    protected int assignmentsRequestsLimit = -1;

    private ContainerPanelConfigurationType config;

    public AssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel) {
        super(id, assignmentContainerWrapperModel);
    }

    public AssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel, ContainerPanelConfigurationType config) {
        super(id, assignmentContainerWrapperModel);
        this.config = config;
    }

    public AssignmentPanel(String id, LoadableModel<PrismObjectWrapper<AH>> assignmentContainerWrapperModel, ContainerPanelConfigurationType config) {
        super(id, PrismContainerWrapperModel.fromContainerWrapper(assignmentContainerWrapperModel, AssignmentHolderType.F_ASSIGNMENT));
        this.config = config;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        assignmentsRequestsLimit = AssignmentsUtil.loadAssignmentsLimit(new OperationResult(OPERATION_LOAD_ASSIGNMENTS_LIMIT), getPageBase());
        initLayout();
    }

    private void initLayout() {

        MultivalueContainerListPanelWithDetailsPanel<AssignmentType> multivalueContainerListPanel =
                new MultivalueContainerListPanelWithDetailsPanel<>(ID_ASSIGNMENTS, AssignmentType.class) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isCreateNewObjectVisible() {
                        return isNewObjectButtonVisible(getFocusObject());
                    }

                    @Override
                    protected IModel<PrismContainerWrapper<AssignmentType>> getContainerModel() {
                        return AssignmentPanel.this.getModel();
                    }

                    @Override
                    protected void cancelItemDetailsPerformed(AjaxRequestTarget target) {
                        AssignmentPanel.this.cancelAssignmentDetailsPerformed(target);
                    }

                    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> createDefaultColumns() {
                        if (AssignmentPanel.this.getModelObject() == null) {
                            return new ArrayList<>();
                        }
                        return initBasicColumns();
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
                            warn(getParentPage().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
                            target.add(getPageBase().getFeedbackPanel());
                            return;
                        }
                        super.deleteItemPerformed(target, toDeleteList);
                    }

                    @Override
                    protected AssignmentListProvider createProvider() {
                        PageStorage pageStorage = getPageStorage(getAssignmentsTabStorageKey());
                        return createAssignmentProvider(pageStorage, getSearchModel(), loadValuesModel());
                    }

                    @Override
                    protected MultivalueContainerDetailsPanel<AssignmentType> getMultivalueContainerDetailsPanel(
                            ListItem<PrismContainerValueWrapper<AssignmentType>> item) {
                        return createMultivalueContainerDetailsPanel(item);
                    }

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return AssignmentPanel.this.getTableId();
                    }

                    @Override
                    protected List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
                        return createSearchableItems(containerDef);
                    }

                    @Override
                    public void refreshTable(AjaxRequestTarget ajaxRequestTarget) {
                        super.refreshTable(ajaxRequestTarget);
                        AssignmentPanel.this.refreshTable(ajaxRequestTarget);
                    }

                    @Override
                    protected boolean isCollectionViewPanel() {
                        return config != null && config.getListView() !=null;
                    }

                    @Override
                    protected CompiledObjectCollectionView getObjectCollectionView() {
                        if (config == null) {
                            return super.getObjectCollectionView();
                        }
                        GuiObjectListViewType listView = config.getListView();
                        if (listView == null) {
                            return null;
                        }
                        CollectionRefSpecificationType collectionRefSpecificationType = listView.getCollection();
                        if (collectionRefSpecificationType == null) {
                            return null;
                        }
                        Task task = getPageBase().createSimpleTask("Compile collection");
                        OperationResult result = task.getResult();
                        try {
                            return getPageBase().getModelInteractionService().compileObjectCollectionView(collectionRefSpecificationType, AssignmentType.class, task, result);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
        multivalueContainerListPanel.add(new VisibleBehaviour(() -> getModel() != null && getModelObject() != null));
        add(multivalueContainerListPanel);

        setOutputMarkupId(true);
    }

    private AssignmentListProvider createAssignmentProvider(PageStorage pageStorage, IModel<Search<AssignmentType>> searchModel, IModel<List<PrismContainerValueWrapper<AssignmentType>>> assignments) {
        return new AssignmentListProvider(AssignmentPanel.this, searchModel, assignments) {

            @Override
            protected PageStorage getPageStorage() {
                return pageStorage;
            }

            @Override
            protected List<PrismContainerValueWrapper<AssignmentType>> postFilter(List<PrismContainerValueWrapper<AssignmentType>> assignmentList) {
                return customPostSearch(assignmentList);
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return AssignmentPanel.this.getCustomizeQuery();
            }
        };
    }

    protected IModel<List<PrismContainerValueWrapper<AssignmentType>>> loadValuesModel() {
        return new PropertyModel<>(getModel(), "values");
    }

    protected void refreshTable(AjaxRequestTarget ajaxRequestTarget) {
    }


    private IModel<AssignmentPopupDto> createAssignmentPopupModel() {
        return new LoadableModel<>(false) {

            @Override
            protected AssignmentPopupDto load() {
                List<AssignmentObjectRelation> assignmentObjectRelations = getAssignmentObjectRelationList();
                return new AssignmentPopupDto(assignmentObjectRelations);
            }
        };
    }
    private List<AssignmentObjectRelation> getAssignmentObjectRelationList() {
        if (AssignmentPanel.this.getModelObject() == null) {
            return null;
        }
        if (isInducement()) {
            return null;
        } else {
            List<AssignmentObjectRelation> assignmentRelationsList =
                    WebComponentUtil.divideAssignmentRelationsByAllValues(loadAssignmentTargetRelationsList());
            if (assignmentRelationsList == null || assignmentRelationsList.isEmpty()) {
                return assignmentRelationsList;
            }
            QName assignmentType = getAssignmentType();
            if (assignmentType == null) {
                return assignmentRelationsList;
            }
            List<AssignmentObjectRelation> assignmentRelationsListFilteredByType =
                    new ArrayList<>();
            assignmentRelationsList.forEach(assignmentRelation -> {
                QName objectType = assignmentRelation.getObjectTypes() != null
                        && !assignmentRelation.getObjectTypes().isEmpty()
                        ? assignmentRelation.getObjectTypes().get(0) : null;
                if (QNameUtil.match(assignmentType, objectType)) {
                    assignmentRelationsListFilteredByType.add(assignmentRelation);
                }
            });
            return assignmentRelationsListFilteredByType;
        }
    }

    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();

        if (getAssignmentType() == null) {
            SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_TARGET_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
            SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
            SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_NAME), defs, "AssignmentPanel.search.policyRule.name");
            SearchFactory.addSearchRefDef(containerDef,
                    ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS,
                            PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF), defs, AreaCategoryType.POLICY, getPageBase());
        }
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), defs);
        SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), defs);

        defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));

        return defs;

    }

    protected QName getAssignmentType() {
        return null;
    }

    protected String getAssignmentsTabStorageKey() {
        if (getModel() == null || getModelObject() == null) {
            return null;
        }
        if (isInducement()) {
            return SessionStorage.KEY_INDUCEMENTS_TAB;
        } else {
            return SessionStorage.KEY_ASSIGNMENTS_TAB;
        }
    }

    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE;
    }

    protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(List<PrismContainerValueWrapper<AssignmentType>> assignments) {
        return assignments;
    }

    protected <AH extends AssignmentHolderType> boolean isNewObjectButtonVisible(PrismObject<AH> focusObject) {
        try {
            return getParentPage().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI,
                    AuthorizationPhaseType.REQUEST, focusObject,
                    null, null, null);
        } catch (Exception ex) {
            return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_ASSIGN_ACTION_URI);
        }
    }

    protected ObjectQuery getCustomizeQuery() {
        Collection<QName> delegationRelations = getParentPage().getRelationRegistry()
                .getAllRelationsFor(RelationKindType.DELEGATION);

        //do not show archetype assignments
        ObjectReferenceType archetypeRef = new ObjectReferenceType();
        archetypeRef.setType(ArchetypeType.COMPLEX_TYPE);
        archetypeRef.setRelation(new QName(PrismConstants.NS_QUERY, "any"));
        RefFilter archetypeFilter = (RefFilter) getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .item(AssignmentType.F_TARGET_REF)
                .ref(archetypeRef.asReferenceValue())
                .buildFilter();
        archetypeFilter.setOidNullAsAny(true);

        QName targetType = getAssignmentType();
        RefFilter targetRefFilter = null;
        if (targetType != null) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setType(targetType);
            ort.setRelation(new QName(PrismConstants.NS_QUERY, "any"));
            targetRefFilter = (RefFilter) getParentPage().getPrismContext().queryFor(AssignmentType.class)
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(ort.asReferenceValue())
                    .buildFilter();
            targetRefFilter.setOidNullAsAny(true);
        }

        ObjectFilter relationFilter = getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .not()
                .item(AssignmentType.F_TARGET_REF)
                .refRelation(delegationRelations.toArray(new QName[0]))
                .buildFilter();

        ObjectQuery query = getPrismContext().queryFactory().createQuery(relationFilter);
        query.addFilter(getPrismContext().queryFactory().createNot(archetypeFilter));
        if (targetRefFilter != null) {
            query.addFilter(targetRefFilter);
        }
        return query;
    }

    protected void cancelAssignmentDetailsPerformed(AjaxRequestTarget target) {
    }

    @NotNull
    private <AH extends AssignmentHolderType> List<AssignmentObjectRelation> loadAssignmentTargetRelationsList() {
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_TARGET_RELATIONS);
        List<AssignmentObjectRelation> assignmentTargetRelations = new ArrayList<>();
        PrismObject<AH> obj = getMultivalueContainerListPanel().getFocusObject();
        try {
            AssignmentCandidatesSpecification spec = getPageBase().getModelInteractionService()
                    .determineAssignmentTargetSpecification(obj, result);
            assignmentTargetRelations = spec != null ? spec.getAssignmentObjectRelations() : new ArrayList<>();
        } catch (SchemaException | ConfigurationException ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load assignment target specification for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return assignmentTargetRelations;
    }

    private PrismObject<? extends FocusType> loadTargetObject(AssignmentType assignmentType) {
        if (assignmentType == null) {
            return null;
        }

        ObjectReferenceType targetRef = assignmentType.getTargetRef();
        if (targetRef == null || targetRef.getOid() == null) {
            return null;
        }

        PrismObject<? extends FocusType> targetObject = targetRef.getObject();
        if (targetObject == null) {
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNMENTS_TARGET_OBJ);
            OperationResult result = task.getResult();
            targetObject = WebModelServiceUtils.loadObject(targetRef, getPageBase(), task, result);
            result.recomputeStatus();
        }
        return targetObject;
    }

    private DisplayType loadIcon(AssignmentType assignment) {
        LOGGER.trace("Create icon for AssignmentType: " + assignment);
        PrismObject<? extends FocusType> object = loadTargetObject(assignment);

        DisplayType displayType = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(object, AssignmentPanel.this.getPageBase());

        if (displayType == null) {
            displayType = GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(
                    AssignmentsUtil.getTargetType(assignment)));
        }
        String disabledStyle = WebComponentUtil.getIconEnabledDisabled(object);
        if (displayType.getIcon() != null && StringUtils.isNotEmpty(displayType.getIcon().getCssClass()) &&
                disabledStyle != null) {
            displayType.getIcon().setCssClass(displayType.getIcon().getCssClass() + " " + disabledStyle);
            displayType.getIcon().setColor("");
        }

        return displayType;
    }

    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initBasicColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());
        columns.add(createAssignmentIconColumn());
        columns.add(createAssignmentNameColumn());
        columns.add(new PrismContainerWrapperColumn<>(getModel(), AssignmentType.F_ACTIVATION, getPageBase()));

        if (getAssignmentType() == null) {
            columns.add(createAssignmentMoreDataColumn());
        }

        columns.addAll(initColumns());
        columns.add(createAssignmentActionColumn());
        return columns;
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

    private IColumn<PrismContainerValueWrapper<AssignmentType>, String> createAssignmentNameColumn() {
        return new AjaxLinkColumn<>(createStringResource("PolicyRulesPanel.nameColumn")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                return new LoadableModel<>() {
                    @Override
                    protected String load() {
                        LOGGER.trace("Create name for AssignmentType: " + rowModel.getObject().getRealValue());
                        String name = AssignmentsUtil.getName(rowModel.getObject(), getParentPage());
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
                AssignmentPanel.this.assignmentDetailsPerformed(target);
                getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
            }
        };
    }

    private IColumn<PrismContainerValueWrapper<AssignmentType>, String> createAssignmentMoreDataColumn() {
        return new AbstractColumn<>(createStringResource("AssignmentPanel.moreData")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> cellItem, String componentId,
                    IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                AssignmentType assignmentType = rowModel != null && rowModel.getObject() != null ?
                        rowModel.getObject().getRealValue() : null;
                cellItem.add(new Label(componentId, AssignmentsUtil.getAssignmentSpecificInfoLabel(assignmentType, AssignmentPanel.this.getPageBase())));
            }
        };
    }

    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        return new ArrayList<>();
    }

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

    protected void assignmentDetailsPerformed(AjaxRequestTarget target) {
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
                return AssignmentPanel.this.getObjectTypesList();
            }

            @Override
            protected ObjectFilter getSubtypeFilter() {
                return AssignmentPanel.this.getSubtypeFilter();
            }

            @Override
            protected boolean isEntitlementAssignment() {
                return AssignmentPanel.this.isEntitlementAssignment();
            }

            @Override
            protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
                return AssignmentPanel.this.getModelObject();
            }

            @Override
            protected <F extends AssignmentHolderType> PrismObject<F> getFocusObject() {
                return getMultivalueContainerListPanel().getFocusObject();
            }
        };
        popupPanel.setOutputMarkupId(true);
        popupPanel.setOutputMarkupPlaceholderTag(true);
        getPageBase().showMainPopup(popupPanel, target);
    }

    //this is here just becasue we want to override default behaviour for GDPR assignment panel
    protected QName getPredefinedRelation() {
        return null;
    }

    protected List<ObjectTypes> getObjectTypesList() {
        if (getAssignmentType() == null) {
            return WebComponentUtil.createAssignableTypesList();
        } else {
            return Collections.singletonList(ObjectTypes.getObjectTypeFromTypeQName(getAssignmentType()));
        }
    }

    protected boolean isEntitlementAssignment() {
        return false;
    }

    protected void addSelectedAssignmentsPerformed(AjaxRequestTarget target, List<AssignmentType> newAssignmentsList) {
        if (CollectionUtils.isEmpty(newAssignmentsList)) {
            warn(getParentPage().getString("AssignmentTablePanel.message.noAssignmentSelected"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        boolean isAssignmentsLimitReached = isAssignmentsLimitReached(newAssignmentsList.size(), true);
        if (isAssignmentsLimitReached) {
            warn(getParentPage().getString("AssignmentPanel.assignmentsLimitReachedWarning", assignmentsRequestsLimit));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        newAssignmentsList.forEach(assignment -> {

            PrismContainerValue<AssignmentType> newAssignment = getModelObject().getItem().createNewValue();
            AssignmentType assignmentType = newAssignment.asContainerable();

            if (assignment.getConstruction() != null && assignment.getConstruction().getResourceRef() != null) {
                assignmentType.setConstruction(assignment.getConstruction());
            } else {
                assignmentType.setTargetRef(assignment.getTargetRef());
            }
            getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newAssignment, getModelObject(),
                    target);
            getMultivalueContainerListPanel().refreshTable(target);
            getMultivalueContainerListPanel().reloadSavePreviewButtons(target);

        });

    }

    private MultivalueContainerDetailsPanel<AssignmentType> createMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<AssignmentType>> item) {
        if (isAssignmentsLimitReached()) {
            item.getModelObject().setReadOnly(true, true);
        } else if (item.getModelObject().isReadOnly()) {
            item.getModelObject().setReadOnly(false, true);
        }
        return new AssignmentsDetailsPanel(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), isEntitlementAssignment(), config);
    }

    private <AH extends FocusType> List<InlineMenuItem> getAssignmentMenuActions() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        PrismObject<AH> obj = getMultivalueContainerListPanel().getFocusObject();
        try {
            boolean isUnassignAuthorized = getParentPage().isAuthorized(AuthorizationConstants.AUTZ_UI_ADMIN_UNASSIGN_ACTION_URI,
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
                        return getMultivalueContainerListPanel().createDeleteColumnAction();
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
                        return getMultivalueContainerListPanel().createDeleteColumnAction();
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
                return getMultivalueContainerListPanel().createEditColumnAction();
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
                                WebComponentUtil.dispatchToObjectDetailsPage(targetClass, refWrapper.getNewValue().getOid(), AssignmentPanel.this, false);
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

    @SuppressWarnings("unchecked")
    protected MultivalueContainerListPanelWithDetailsPanel<AssignmentType> getMultivalueContainerListPanel() {
        return ((MultivalueContainerListPanelWithDetailsPanel<AssignmentType>) get(ID_ASSIGNMENTS));
    }

    protected PageBase getParentPage() {
        return getPageBase();
    }

    private IModel<String> getAssignmentsLimitReachedUnassignTitleModel() {
        return new LoadableModel<>(true) {
            @Override
            protected String load() {
                return isAssignmentsLimitReached() ?
                        AssignmentPanel.this.getPageBase().createStringResource("RoleCatalogItemButton.assignmentsLimitReachedTitle",
                                assignmentsRequestsLimit).getString() : createStringResource("pageAdminFocus.menu.unassign").getString();
            }
        };
    }

    protected boolean isAssignmentsLimitReached() {
        return isAssignmentsLimitReached(0, false);
    }

    @SuppressWarnings("deprecation")
    protected boolean isAssignmentsLimitReached(int selectedAssignmentsCount, boolean actionPerformed) {
        if (assignmentsRequestsLimit < 0) {
            return false;
        }
        int changedItems = 0;
        List<PrismContainerValueWrapper<AssignmentType>> assignmentsList = getModelObject().getValues();
        for (PrismContainerValueWrapper<AssignmentType> assignment : assignmentsList) {
            if (assignment.hasChanged()) {
                changedItems++;
            }
        }
        return actionPerformed ? (changedItems + selectedAssignmentsCount) > assignmentsRequestsLimit :
                (changedItems + selectedAssignmentsCount) >= assignmentsRequestsLimit;
    }

    protected boolean isInducement() {
        return getModelObject() != null && getModelObject().getPath().containsNameExactly(AbstractRoleType.F_INDUCEMENT);
    }

    protected ObjectFilter getSubtypeFilter() {
        return null;
    }
}
