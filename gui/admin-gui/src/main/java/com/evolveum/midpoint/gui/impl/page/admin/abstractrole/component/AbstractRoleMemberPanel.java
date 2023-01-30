/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import static com.evolveum.midpoint.util.MiscUtil.sleepWatchfully;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.ChooseMemberPopup;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractRoleSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeAndRelationDialogPanel;
import com.evolveum.midpoint.web.component.dialog.ConfigureTaskConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.security.util.GuiAuthorizationConstants;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@PanelType(name = "members")
@PanelInstance(identifier = "roleMembers",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageRole.members", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 80))
@PanelInstance(identifier = "roleGovernance",
        applicableForType = RoleType.class,
        applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageRole.governance", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 90))
@PanelInstance(identifier = "serviceMembers",
        applicableForType = ServiceType.class,
        applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageRole.members", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 80))
@PanelInstance(identifier = "serviceGovernance",
        applicableForType = ServiceType.class,
        applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "pageRole.governance", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 90))
@PanelDisplay(label = "Members", order = 60)
public class AbstractRoleMemberPanel<R extends AbstractRoleType> extends AbstractObjectMainPanel<R, FocusDetailsModels<R>> {

    private static final long serialVersionUID = 1L;

    public enum QueryScope { // temporarily public because of migration
        SELECTED, ALL, ALL_DIRECT
    }

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);
    private static final String DOT_CLASS = AbstractRoleMemberPanel.class.getName() + ".";

    protected static final String OPERATION_LOAD_MEMBER_RELATIONS = DOT_CLASS + "loadMemberRelationsList";

    private static final String OPERATION_UNASSIGN_OBJECTS = DOT_CLASS + "unassignObjects";
    private static final String OPERATION_UNASSIGN_OBJECT = DOT_CLASS + "unassignObject";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_RECOMPUTE_OBJECT = DOT_CLASS + "recomputeObject";

    protected static final String ID_FORM = "form";

    protected static final String ID_CONTAINER_MEMBER = "memberContainer";
    protected static final String ID_MEMBER_TABLE = "memberTable";
    private static final Map<QName, Map<String, String>> AUTHORIZATIONS = new HashMap<>();
    private static final Map<QName, UserProfileStorage.TableId> TABLES_ID = new HashMap<>();

    static {
        TABLES_ID.put(RoleType.COMPLEX_TYPE, UserProfileStorage.TableId.ROLE_MEMBER_PANEL);
        TABLES_ID.put(ServiceType.COMPLEX_TYPE, UserProfileStorage.TableId.SERVICE_MEMBER_PANEL);
        TABLES_ID.put(OrgType.COMPLEX_TYPE, UserProfileStorage.TableId.ORG_MEMBER_PANEL);
        TABLES_ID.put(ArchetypeType.COMPLEX_TYPE, UserProfileStorage.TableId.ARCHETYPE_MEMBER_PANEL);
    }

    static {
        AUTHORIZATIONS.put(RoleType.COMPLEX_TYPE, GuiAuthorizationConstants.ROLE_MEMBERS_AUTHORIZATIONS);
        AUTHORIZATIONS.put(ServiceType.COMPLEX_TYPE, GuiAuthorizationConstants.SERVICE_MEMBERS_AUTHORIZATIONS);
        AUTHORIZATIONS.put(OrgType.COMPLEX_TYPE, GuiAuthorizationConstants.ORG_MEMBERS_AUTHORIZATIONS);
        AUTHORIZATIONS.put(ArchetypeType.COMPLEX_TYPE, GuiAuthorizationConstants.ARCHETYPE_MEMBERS_AUTHORIZATIONS);
    }

    private CompiledObjectCollectionView compiledCollectionViewFromPanelConfiguration;

    public AbstractRoleMemberPanel(String id, FocusDetailsModels<R> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        Form<?> form = new MidpointForm<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);
        initMemberTable(form);
        setOutputMarkupId(true);
    }

    protected Form<?> getForm() {
        return (Form<?>) get(ID_FORM);
    }

    private <AH extends AssignmentHolderType> Class<AH> getDefaultObjectTypeClass() {
        return (Class<AH>) UserType.class;
    }
    protected  <AH extends AssignmentHolderType> void initMemberTable(Form<?> form) {
        WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
        memberContainer.setOutputMarkupId(true);
        memberContainer.setOutputMarkupPlaceholderTag(true);
        form.add(memberContainer);

        //TODO QName defines a relation value which will be used for new member creation
        MainObjectListPanel<AH> childrenListPanel = new MainObjectListPanel<>(
                ID_MEMBER_TABLE, getDefaultObjectTypeClass(), getSearchOptions(), getPanelConfiguration()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return AbstractRoleMemberPanel.this.getTableId(getComplexTypeQName());
            }

            @Override
            protected List<IColumn<SelectableBean<AH>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<AH>, String>> columns = super.createDefaultColumns();
                columns.add(createRelationColumn());
                return columns;
            }

            @Override
            protected boolean isObjectDetailsEnabled(IModel<SelectableBean<AH>> rowModel) {
                if (rowModel == null || rowModel.getObject() == null
                        || rowModel.getObject().getValue() == null) {
                    return false;
                }
                Class<?> objectClass = rowModel.getObject().getValue().getClass();
                return WebComponentUtil.hasDetailsPage(objectClass);
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return AbstractRoleMemberPanel.this.createToolbarButtonList(buttonId, super.createToolbarButtonsList(buttonId));
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRowActions();
            }

            @Override
            protected String getStorageKey() {
                return AbstractRoleMemberPanel.this.createStorageKey();
            }

            protected PageStorage getPageStorage(String storageKey) {
                return getMemberPanelStorage();
            }

            @Override
            protected SearchContext createAdditionalSearchContext() {
                return getDefaultMemberSearchBoxConfig();
            }

            @Override
            protected SelectableBeanObjectDataProvider<AH> createProvider() {
                SelectableBeanObjectDataProvider<AH> provider = createSelectableBeanObjectDataProvider(() -> getCustomizedQuery(getSearchModel().getObject()), null);
                provider.addQueryVariables(ExpressionConstants.VAR_PARENT_OBJECT, ObjectTypeUtil.createObjectRef(AbstractRoleMemberPanel.this.getModelObject()));
                return provider;
            }

            @Override
            public void refreshTable(AjaxRequestTarget target) {
                if (reloadPageOnRefresh()) {
                    throw new RestartResponseException(getPage().getClass());
                } else {
                    super.refreshTable(target);
                }
            }

            @Override
            protected boolean showNewObjectCreationPopup() {
                return CollectionUtils.isNotEmpty(getNewObjectReferencesList(getObjectCollectionView(), null));
            }

            @Override
            protected List<ObjectReferenceType> getNewObjectReferencesList(CompiledObjectCollectionView collectionView, AssignmentObjectRelation relation) {
                List<ObjectReferenceType> refList = super.getNewObjectReferencesList(collectionView, relation);
                if (refList == null) {
                    refList = new ArrayList<>();
                }
                if (relation != null && CollectionUtils.isNotEmpty(relation.getArchetypeRefs())) {
                    refList.addAll(relation.getArchetypeRefs());
                }
                ObjectReferenceType membershipRef = new ObjectReferenceType();
                membershipRef.setOid(AbstractRoleMemberPanel.this.getModelObject().getOid());
                membershipRef.setType(AbstractRoleMemberPanel.this.getModelObject().asPrismObject().getComplexTypeDefinition().getTypeName());
                membershipRef.setRelation(relation != null && CollectionUtils.isNotEmpty(relation.getRelations()) ?
                        relation.getRelations().get(0) : null);
                refList.add(membershipRef);
                return refList;
            }

            @Override
            protected LoadableModel<MultiFunctinalButtonDto> loadButtonDescriptions() {
                return loadMultiFunctionalButtonModel(true);
            }

            @Override
            public ContainerPanelConfigurationType getPanelConfiguration() {
                return AbstractRoleMemberPanel.this.getPanelConfiguration();
            }

            @Override
            protected String getTitleForNewObjectButton() {
                return createStringResource("TreeTablePanel.menu.createMember").getString();
            }

            @Override
            protected boolean isCreateNewObjectEnabled() {
                return isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_CREATE);
            }
        };
        childrenListPanel.setOutputMarkupId(true);
        memberContainer.add(childrenListPanel);
    }

    protected  <AH extends AssignmentHolderType> SearchContext getDefaultMemberSearchBoxConfig() {
        SearchContext ctx = new SearchContext();
        ctx.setPanelType(getPanelType());
        return ctx;
    }

    protected CollectionPanelType getPanelType() {
        String panelId = getPanelConfiguration().getIdentifier();
        return CollectionPanelType.getPanelType(panelId);
    }

    protected List<Component> createToolbarButtonList(String buttonId, List<Component> defaultToolbarList) {
        AjaxIconButton assignButton = createAssignButton(buttonId);
        defaultToolbarList.add(1, assignButton);
        return defaultToolbarList;
    }

    protected boolean reloadPageOnRefresh() {
        return false;
    }

    private <AH extends AssignmentHolderType> IColumn<SelectableBean<AH>, String> createRelationColumn() {
        return new AbstractExportableColumn<>(
                createStringResource("roleMemberPanel.relation")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<AH>>> cellItem,
                    String componentId, IModel<SelectableBean<AH>> rowModel) {
                cellItem.add(new Label(componentId,
                        getRelationValue(rowModel.getObject().getValue())));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<AH>> rowModel) {
                return Model.of(getRelationValue(rowModel.getObject().getValue()));
            }

        };
    }

    private <AH extends AssignmentHolderType> String getRelationValue(AH value) {
        return value.getRoleMembershipRef().stream()
                .filter(roleMembershipRef -> isApplicableRoleMembershipRef(roleMembershipRef))
                .map(roleMembershipRef -> getTranslatedRelationValue(roleMembershipRef))
                .collect(Collectors.joining(","));
    }

    private boolean isApplicableRoleMembershipRef(ObjectReferenceType roleMembershipRef) {
        List<QName> defaultRelations = getDefaultRelationsForActions();
        return roleMembershipRef.getOid().equals(getModelObject().getOid())
                && (defaultRelations.contains(roleMembershipRef.getRelation())
                || defaultRelations.contains(PrismConstants.Q_ANY));
    }

    private String getTranslatedRelationValue(ObjectReferenceType roleMembershipRef) {
        QName relationQName = roleMembershipRef.getRelation();
        String relation = relationQName.getLocalPart();
        RelationDefinitionType relationDef = WebComponentUtil.getRelationDefinition(relationQName);
        if (relationDef != null) {
            PolyStringType label = GuiDisplayTypeUtil.getLabel(relationDef.getDisplay());
            if (PolyStringUtils.isNotEmpty(label)) {
                relation = WebComponentUtil.getTranslatedPolyString(label);
            }
        }
        return relation;
    }
    protected  <AH extends AssignmentHolderType> ObjectQuery getCustomizedQuery(Search search) {
        if (noMemberSearchItemVisible(search)) {
            PrismContext prismContext = getPageBase().getPrismContext();
            return prismContext.queryFor((Class<? extends Containerable>) search.getTypeClass())
                    .exists(AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(MemberOperationsHelper.createReferenceValuesList(getModelObject(), getRelationsForSearch()))
                    .endBlock().build();
        }
        return null;
    }

    private <AH extends AssignmentHolderType> boolean noMemberSearchItemVisible(Search search) {
        if (!SearchBoxModeType.BASIC.equals(search.getSearchMode())) {
            return true;
        }
        return false;
    }

    private List<QName> getRelationsForSearch() {
        List<QName> relations = new ArrayList<>();
        QName defaultRelation = getRelationValue();
        if (QNameUtil.match(PrismConstants.Q_ANY, defaultRelation)) {
            relations.addAll(getSupportedRelations());
        } else {
            relations.add(defaultRelation);
        }
        return relations;
    }

    private String createStorageKey() {
        UserProfileStorage.TableId tableId = getTableId(getComplexTypeQName());
        String collectionName = getPanelConfiguration() != null ? ("_" + getPanelConfiguration().getIdentifier()) : "";
        return tableId.name() + "_" + getStorageKeyTabSuffix() + collectionName;
    }

    protected LoadableModel<MultiFunctinalButtonDto> loadMultiFunctionalButtonModel(boolean useDefaultObjectRelations) {

        return new LoadableModel<>(false) {

            @Override
            protected MultiFunctinalButtonDto load() {
                MultiFunctinalButtonDto multiFunctinalButtonDto = new MultiFunctinalButtonDto();

                DisplayType mainButtonDisplayType = getCreateMemberButtonDisplayType();
                CompositedIconBuilder builder = new CompositedIconBuilder();
                Map<IconCssStyle, IconType> layerIcons = WebComponentUtil.createMainButtonLayerIcon(mainButtonDisplayType);
                for (Map.Entry<IconCssStyle, IconType> icon : layerIcons.entrySet()) {
                    builder.appendLayerIcon(icon.getValue(), icon.getKey());
                }
                CompositedIconButtonDto mainButton = createCompositedIconButtonDto(mainButtonDisplayType, null, builder.build());
                multiFunctinalButtonDto.setMainButton(mainButton);

                List<AssignmentObjectRelation> loadedRelations = loadMemberRelationsList();
                if (CollectionUtils.isEmpty(loadedRelations) && useDefaultObjectRelations) {
                    loadedRelations.addAll(getDefaultNewMemberRelations());
                }
                List<CompositedIconButtonDto> additionalButtons = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(loadedRelations)) {
                    List<AssignmentObjectRelation> relations = WebComponentUtil.divideAssignmentRelationsByAllValues(loadedRelations);
                    relations.forEach(relation -> {
                        DisplayType additionalButtonDisplayType = GuiDisplayTypeUtil.getAssignmentObjectRelationDisplayType(getPageBase(), relation,
                                "abstractRoleMemberPanel.menu.createMember");
                        CompositedIconButtonDto buttonDto = createCompositedIconButtonDto(additionalButtonDisplayType, relation, createCompositedIcon(relation, additionalButtonDisplayType));
                        additionalButtons.add(buttonDto);
                    });
                }
                multiFunctinalButtonDto.setAdditionalButtons(additionalButtons);
                return multiFunctinalButtonDto;
            }
        };
    }

    private CompositedIcon createCompositedIcon(AssignmentObjectRelation relation, DisplayType additionalButtonDisplayType) {
        CompositedIconBuilder builder = WebComponentUtil.getAssignmentRelationIconBuilder(getPageBase(), relation,
                additionalButtonDisplayType.getIcon(), WebComponentUtil.createIconType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green"));
        return builder.build();
    }

    protected List<AssignmentObjectRelation> getDefaultNewMemberRelations() {
        List<AssignmentObjectRelation> relationsList = new ArrayList<>();
        List<QName> newMemberObjectTypes = getNewMemberObjectTypes();
        if (newMemberObjectTypes != null) {
            newMemberObjectTypes.forEach(objectType -> {
                List<QName> supportedRelation = new ArrayList<>(getSupportedRelations());
                if (!UserType.COMPLEX_TYPE.equals(objectType) && !OrgType.COMPLEX_TYPE.equals(objectType)) {
                    supportedRelation.remove(RelationTypes.APPROVER.getRelation());
                    supportedRelation.remove(RelationTypes.OWNER.getRelation());
                    supportedRelation.remove(RelationTypes.MANAGER.getRelation());
                }
                AssignmentObjectRelation assignmentObjectRelation = new AssignmentObjectRelation();
                assignmentObjectRelation.addRelations(supportedRelation);
                assignmentObjectRelation.addObjectTypes(Collections.singletonList(objectType));
                assignmentObjectRelation.getArchetypeRefs().addAll(archetypeReferencesListForType(objectType));
                relationsList.add(assignmentObjectRelation);
            });
        } else {
            AssignmentObjectRelation assignmentObjectRelation = new AssignmentObjectRelation();
            assignmentObjectRelation.addRelations(getSupportedRelations());
            relationsList.add(assignmentObjectRelation);
        }
        return relationsList;
    }

    private List<ObjectReferenceType> archetypeReferencesListForType(QName type) {
        List<CompiledObjectCollectionView> views =
                getPageBase().getCompiledGuiProfile().findAllApplicableArchetypeViews(type, OperationTypeType.ADD);
        List<ObjectReferenceType> archetypeRefs = new ArrayList<>();
        views.forEach(view -> {
            if (view.getCollection() != null && view.getCollection().getCollectionRef() != null) {
                archetypeRefs.add(view.getCollection().getCollectionRef());
            }
        });
        return archetypeRefs;
    }

    protected AjaxIconButton createAssignButton(String buttonId) {
        AjaxIconButton assignButton = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.EVO_ASSIGNMENT_ICON),
                createStringResource("TreeTablePanel.menu.addMembers")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ChooseMemberPopup browser = new ChooseMemberPopup(AbstractRoleMemberPanel.this.getPageBase().getMainPopupBodyId(),
                        getMemberPanelStorage().getSearch(), loadMultiFunctionalButtonModel(false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected R getAssignmentTargetRefObject() {
                        return AbstractRoleMemberPanel.this.getModelObject();
                    }

                    @Override
                    protected List<ObjectReferenceType> getArchetypeRefList() {
                        return new ArrayList<>(); //todo
                    }

                    @Override
                    protected boolean isOrgTreeVisible() {
                        return true;
                    }

                    @Override
                    protected Task executeMemberOperation(AbstractRoleType targetObject, ObjectQuery query, @NotNull QName relation, QName type, AjaxRequestTarget target, PageBase pageBase) {
                        Task task = super.executeMemberOperation(targetObject, query, relation, type, target, pageBase);
                        processTaskAfterOperation(task, target);
                        return task;
                    }
                };
                browser.setOutputMarkupId(true);
                AbstractRoleMemberPanel.this.getPageBase().showMainPopup(browser, target);
            }
        };
        assignButton.add(new VisibleBehaviour(() -> isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_ASSIGN)));
        assignButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        return assignButton;
    }

    protected void processTaskAfterOperation(Task task, AjaxRequestTarget target) {
    }

    protected void waitWhileTaskFinish(Task task, AjaxRequestTarget target) {
        getSession().getFeedbackMessages().clear(message -> message.getMessage() instanceof OpResult
                && OperationResultStatus.IN_PROGRESS.equals(((OpResult) message.getMessage()).getStatus()));

        AtomicReference<OperationResult> result = new AtomicReference<>();
        long until = System.currentTimeMillis() + Duration.ofSeconds(3).toMillis();
        sleepWatchfully( until, 100, () -> {
            try {
                result.set(getPageBase().getTaskManager().getTaskWithResult(
                        task.getOid(), new OperationResult("reload task")).getResult());
            } catch (Throwable e) {
                //ignore exception
            }
            return result.get() == null ? false : result.get().isInProgress();
        });
        if (!result.get().isSuccess() && !result.get().isInProgress()) {
            getPageBase().showResult(result.get());
        } else if (result.get().isInProgress()) {
            getPageBase().showResult(task.getResult());
        } else {
            OperationResult showedResult = new OperationResult(task.getResult().getOperation());
            showedResult.setStatus(result.get().getStatus());
            getPageBase().showResult(showedResult);
        }

        refreshTable(target);
        target.add(getFeedback());
    }

    protected AjaxIconButton createUnassignButton(String buttonId) {
        AjaxIconButton assignButton = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.EVO_ASSIGNMENT_ICON),
                createStringResource("TreeTablePanel.menu.removeMembers")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                unassignMembersPerformed(null, target);
            }
        };
        assignButton.add(new VisibleBehaviour(() -> isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_UNASSIGN)));
        assignButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        return assignButton;
    }

    private CompositedIconButtonDto createCompositedIconButtonDto(DisplayType buttonDisplayType, AssignmentObjectRelation relation, CompositedIcon icon) {
        CompositedIconButtonDto compositedIconButtonDto = new CompositedIconButtonDto();
        compositedIconButtonDto.setAdditionalButtonDisplayType(buttonDisplayType);
        if (icon != null) {
            compositedIconButtonDto.setCompositedIcon(icon);
        } else {
            CompositedIconBuilder mainButtonIconBuilder = new CompositedIconBuilder();
            mainButtonIconBuilder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(buttonDisplayType), IconCssStyle.IN_ROW_STYLE)
                    .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(buttonDisplayType));
            compositedIconButtonDto.setCompositedIcon(mainButtonIconBuilder.build());
        }
        compositedIconButtonDto.setAssignmentObjectRelation(relation);
        return compositedIconButtonDto;
    }

    protected UserProfileStorage.TableId getTableId(QName complextType) {
        return TABLES_ID.get(complextType);
    }

    protected Map<String, String> getAuthorizations(QName complexType) {
        return AUTHORIZATIONS.get(complexType);
    }

    protected QName getComplexTypeQName() {
        return getModelObject().asPrismObject().getComplexTypeDefinition().getTypeName();
    }

    private DisplayType getCreateMemberButtonDisplayType() {
        return GuiDisplayTypeUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green",
                AbstractRoleMemberPanel.this.createStringResource("abstractRoleMemberPanel.menu.createMember", "", "").getString());
    }

    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        createAssignMemberRowAction(menu);
        createUnassignMemberRowAction(menu);
        createRecomputeMemberRowAction(menu);
        createAddMemberRowAction(menu);
        createDeleteMemberRowAction(menu);
        return menu;
    }

    protected void createAssignMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_ASSIGN)) {
            InlineMenuItem menuItem = new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.assign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            MemberOperationsGuiHelper.assignMembers(getPageBase(), AbstractRoleMemberPanel.this.getModelObject(),
                                    target, getMemberPanelStorage().getSearch(), null);
                        }
                    };
                }
            };
            menuItem.setVisibilityChecker((rowModel, isHeader) -> isHeader);
            menu.add(menuItem);
        }
    }

    protected void createUnassignMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_UNASSIGN)) {
            InlineMenuItem menuItem = new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.unassign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unassignMembersPerformed(getRowModel(), target);
                        }
                    };

                }

                @Override
                public CompositedIconBuilder getIconCompositedBuilder() {
                    return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_UNASSIGN);
                }
            };
            menuItem.setVisibilityChecker((rowModel, isHeader) -> isHeader ? true : containsDirectAssignment(rowModel, isHeader));
            menu.add(menuItem);
        }
    }

    private boolean containsDirectAssignment(IModel<?> rowModel, boolean isHeader) {
        AssignmentHolderType assignmentHolder = getAssignmetHolderFromRow(rowModel);

        if (assignmentHolder == null) {
            return isHeader;
        }
        R role = getModelObject();
        List<AssignmentType> assignments = assignmentHolder.getAssignment();
        for (AssignmentType assignment : assignments) {
            if (assignment != null && assignment.getTargetRef() != null && assignment.getTargetRef().getOid().equals(role.getOid())) {
                return true;
            }
        }
        return false;
    }

    private AssignmentHolderType getAssignmetHolderFromRow(IModel<?> rowModel) {
        if (rowModel != null && (rowModel.getObject() instanceof SelectableBean)
                && ((SelectableBean<?>) rowModel.getObject()).getValue() instanceof AssignmentHolderType) {
            return (AssignmentHolderType) ((SelectableBean<?>) rowModel.getObject()).getValue();
        }
        return null;
    }

    protected void createRecomputeMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_RECOMPUTE)) {
            menu.add(new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.recompute")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            recomputeMembersPerformed(getRowModel(), target);
                        }
                    };
                }

                @Override
                public CompositedIconBuilder getIconCompositedBuilder() {
                    return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM);
                }

            });
        }
    }

    protected void createAddMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_CREATE)) {
            InlineMenuItem menuItem = new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.create")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            createFocusMemberPerformed(target);
                        }
                    };
                }
            };
            menuItem.setVisibilityChecker((rowModel, isHeader) -> isHeader);
            menu.add(menuItem);
        }
    }

    protected void createDeleteMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_DELETE)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.delete")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            deleteMembersPerformed(getRowModel(), target);
                        }
                    };
                }

            });
        }
    }

    protected List<QName> getSupportedRelations() {
        AbstractRoleSearchItemWrapper memberSearchItems = getMemberSearchItems();
        if (memberSearchItems != null) {
            return memberSearchItems.getSupportedRelations();
        }
        return new ArrayList<>();
    }

    private <AH extends AssignmentHolderType> List<QName> getSupportedObjectTypes() {
        Search search = getMemberPanelStorage().getSearch();
        return search.getAllowedTypeList();
    }

    private boolean isAuthorized(String action) {
        Map<String, String> memberAuthz = getAuthorizations(getComplexTypeQName());
        return WebComponentUtil.isAuthorized(memberAuthz.get(action));
    }

    private List<AssignmentObjectRelation> loadMemberRelationsList() {
        AssignmentCandidatesSpecification spec = loadCandidateSpecification();
        return spec != null ? spec.getAssignmentObjectRelations() : new ArrayList<>();
    }

    private AssignmentCandidatesSpecification loadCandidateSpecification() {
        OperationResult result = new OperationResult(OPERATION_LOAD_MEMBER_RELATIONS);
        PrismObject<? extends AbstractRoleType> obj = getModelObject().asPrismObject();
        AssignmentCandidatesSpecification spec = null;
        try {
            spec = getPageBase().getModelInteractionService()
                    .determineAssignmentHolderSpecification(obj, result);
        } catch (Throwable ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load member relations list for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return spec;
    }

    private List<QName> getDefaultRelationsForActions() {
        List<QName> defaultRelations = new ArrayList<>();
        QName defaultRelation = getRelationValue();

        if (isSubtreeScope()) {
            defaultRelations.add(RelationTypes.MEMBER.getRelation());
            return defaultRelations;
        }

        if (defaultRelation != null) {
            defaultRelations.add(getRelationValue());
        } else {
            defaultRelations.add(RelationTypes.MEMBER.getRelation());
        }

        if (defaultRelations.contains(PrismConstants.Q_ANY)) {
            defaultRelations = getSupportedRelations();
        }

        return defaultRelations;
    }

    private void deleteMembersPerformed(IModel<?> rowModel, AjaxRequestTarget target) {
        StringResourceModel confirmModel;

        if (rowModel != null || getSelectedObjectsCount() > 0) {

            confirmModel = rowModel != null
                    ? createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject",
                    "delete", ((ObjectType) ((SelectableBean<?>) rowModel.getObject()).getValue()).getName())
                    : createStringResource("abstractRoleMemberPanel.deleteSelectedMembersConfirmationLabel",
                    getSelectedObjectsCount());

            executeSimpleDeleteOperation(rowModel, confirmModel, target);
        } else {
            confirmModel = createStringResource("abstractRoleMemberPanel.deleteAllMembersConfirmationLabel");

            QueryScope scope = getQueryScope();
            ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
                    ((PageBase) getPage()).getMainPopupBodyId(), confirmModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<QName> getSupportedObjectTypes() {
                    return AbstractRoleMemberPanel.this.getSupportedObjectTypes();//getSupportedObjectTypes(true);
                }

                @Override
                protected List<QName> getSupportedRelations() {
                    if (isSubtreeScope()) {
                        return getDefaultRelationsForActions();
                    }
                    return AbstractRoleMemberPanel.this.getSupportedRelations();
                }

                @Override
                protected List<QName> getDefaultRelations() {
                    return getDefaultRelationsForActions();
                }

                @Override
                protected IModel<String> getWarningMessageModel() {
                    if (isSubtreeScope()) {
                        return getPageBase().createStringResource("abstractRoleMemberPanel.delete.warning.subtree");
                    }
                    return null;
                }

                protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                    deleteMembersPerformed(rowModel, scope, type, relations, target);
                }

                @Override
                protected boolean isFocusTypeSelectorVisible() {
                    return getAssignmetHolderFromRow(rowModel) == null && !QueryScope.SELECTED.equals(scope);
                }

                @Override
                protected QName getDefaultObjectType() {
                    return getSearchType();
                }

            };
            getPageBase().showMainPopup(chooseTypePopupContent, target);
        }
    }

    protected int getSelectedObjectsCount() {
        return getMemberTable().getSelectedObjectsCount();
    }

    private void recomputeMembersPerformed(IModel<?> rowModel, AjaxRequestTarget target) {
        StringResourceModel confirmModel;

        if (rowModel != null || getSelectedObjectsCount() > 0) {

            confirmModel = rowModel != null
                    ? createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject",
                    "recompute", ((ObjectType) ((SelectableBean<?>) rowModel.getObject()).getValue()).getName())
                    : createStringResource("abstractRoleMemberPanel.recomputeSelectedMembersConfirmationLabel",
                    getSelectedObjectsCount());

            executeSimpleRecomputeOperation(rowModel, confirmModel, target);
            return;
        }

        confirmModel = createStringResource("abstractRoleMemberPanel.recomputeAllMembersConfirmationLabel");

        ConfirmationPanel dialog = new ConfigureTaskConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
                confirmModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> getWarningMessageModel() {
                if (isSubtreeScope()) {
                    return createStringResource("abstractRoleMemberPanel.recompute.warning.subtree");
                }
                return null;
            }

            @Override
            protected PrismObject<TaskType> getTask(AjaxRequestTarget target) {
                Task task = MemberOperationsHelper.createRecomputeMembersTask(
                        AbstractRoleMemberPanel.this.getModelObject(),
                        getQueryScope(),
                        getSearchType(),
                        getActionQuery(rowModel, getQueryScope(), getRelationsForRecomputeTask()),
                        target, getPageBase());
                if (task == null) {
                    return null;
                }
                PrismObject<TaskType> recomputeTask = task.getRawTaskObjectClone();
                TaskType recomputeTaskType = recomputeTask.asObjectable();
                recomputeTaskType.getAssignment().add(ObjectTypeUtil.createAssignmentTo(
                        SystemObjectsType.ARCHETYPE_RECOMPUTATION_TASK.value(), ObjectTypes.ARCHETYPE, getPrismContext()));
                return recomputeTask;
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("pageUsers.message.confirmActionPopupTitle");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                MemberOperationsHelper.createAndSubmitRecomputeMembersTask(
                        AbstractRoleMemberPanel.this.getModelObject(),
                        getQueryScope(),
                        getSearchType(),
                        getActionQuery(rowModel, getQueryScope(), getRelationsForRecomputeTask()),
                        target, getPageBase());
            }

        };
        ((PageBase) getPage()).showMainPopup(dialog, target);
    }

    protected void unassignMembersPerformed(IModel<?> rowModel, AjaxRequestTarget target) {
        QueryScope scope = getQueryScope();
        StringResourceModel confirmModel;

        if (rowModel != null || getSelectedObjectsCount() > 0) {
            confirmModel = rowModel != null
                    ? createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject",
                    "unassign", ((ObjectType) ((SelectableBean<?>) rowModel.getObject()).getValue()).getName())
                    : createStringResource("abstractRoleMemberPanel.unassignSelectedMembersConfirmationLabel",
                    getSelectedObjectsCount());

            executeSimpleUnassignedOperation(rowModel, confirmModel, target);
        } else {
            confirmModel = createStringResource("abstractRoleMemberPanel.unassignAllMembersConfirmationLabel");

            ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
                    getPageBase().getMainPopupBodyId(), confirmModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<QName> getSupportedObjectTypes() {
                    return AbstractRoleMemberPanel.this.getSupportedObjectTypes();//getSupportedObjectTypes(true);
                }

                @Override
                protected List<QName> getSupportedRelations() {
                    if (isSubtreeScope()) {
                        return getDefaultRelationsForActions();
                    }
                    return AbstractRoleMemberPanel.this.getSupportedRelations();
                }

                @Override
                protected List<QName> getDefaultRelations() {
                    return getDefaultRelationsForActions();
                }

                @Override
                protected boolean isFocusTypeSelectorVisible() {
                    return getAssignmetHolderFromRow(rowModel) == null && !QueryScope.SELECTED.equals(scope);
                }

                protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                    unassignMembersPerformed(rowModel, type, isSubtreeScope()
                            && QueryScope.ALL.equals(scope) ? QueryScope.ALL_DIRECT : scope, relations, target);
                }

                @Override
                protected PrismObject<TaskType> getTask(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                    if (checkRelationNotSelected(relations, "No relation was selected. Cannot perform unassign members", target)) {
                        return null;
                    }
                    Task task = MemberOperationsHelper.createUnassignMembersTask(
                            AbstractRoleMemberPanel.this.getModelObject(),
                            scope,
                            type,
                            getActionQuery(rowModel, scope, relations),
                            relations,
                            target, getPageBase());

                    if (task == null) {
                        return null;
                    }
                    return task.getRawTaskObjectClone();
                }

                @Override
                protected boolean isTaskConfigureButtonVisible() {
                    return true;
                }

                @Override
                protected QName getDefaultObjectType() {
                    if (QueryScope.SELECTED.equals(scope)) {
                        return FocusType.COMPLEX_TYPE;
                    }

                    return getSearchType();

                }

                @Override
                protected IModel<String> getWarningMessageModel() {
                    if (isSubtreeScope()) {
                        return getPageBase().createStringResource("abstractRoleMemberPanel.unassign.warning.subtree");
                    } else if (isIndirect()) {
                        return getPageBase().createStringResource("abstractRoleMemberPanel.unassign.warning.indirect");
                    }
                    return null;
                }

                @Override
                public int getHeight() {
                    if (isSubtreeScope()) {
                        return 325;
                    }
                    return 230;
                }
            };

            getPageBase().showMainPopup(chooseTypePopupContent, target);
        }
    }

    private void executeSimpleRecomputeOperation(IModel<?> rowModel, StringResourceModel confirmModel, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfirmationPanel(getPageBase().getMainPopupBodyId(), confirmModel) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {

                AssignmentHolderType object = getAssignmetHolderFromRow(rowModel);
                if (object != null) {
                    executeRecompute(object, target);
                } else {
                    MemberOperationsHelper.createAndSubmitRecomputeMembersTask(
                            AbstractRoleMemberPanel.this.getModelObject(),
                            getQueryScope(),
                            getSearchType(),
                            getActionQuery(rowModel, getQueryScope(), getSupportedRelations()),
                            target, getPageBase());
                }
            }
        };
        getPageBase().showMainPopup(dialog, target);
    }

    private void executeSimpleDeleteOperation(IModel<?> rowModel, StringResourceModel confirmModel, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new DeleteConfirmationPanel(getPageBase().getMainPopupBodyId(), confirmModel) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {

                AssignmentHolderType object = getAssignmetHolderFromRow(rowModel);
                if (object != null) {
                    executeDelete(object, target);

                } else {
                    MemberOperationsHelper.createAndSubmitDeleteMembersTask(
                            AbstractRoleMemberPanel.this.getModelObject(),
                            getQueryScope(),
                            getSearchType(),
                            getActionQuery(rowModel, getQueryScope(), getSupportedRelations()),
                            target, getPageBase());
                }
            }
        };
        getPageBase().showMainPopup(dialog, target);
    }

    protected void executeSimpleUnassignedOperation(IModel<?> rowModel, StringResourceModel confirmModel, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfigureTaskConfirmationPanel(getPageBase().getMainPopupBodyId(), confirmModel) {

            @Override
            protected IModel<String> getWarningMessageModel() {
                if (isSubtreeScope() && rowModel == null) {
                    return createStringResource("abstractRoleMemberPanel.unassign.warning.subtree");
                } else if (isIndirect() && rowModel == null) {
                    return createStringResource("abstractRoleMemberPanel.unassign.warning.indirect");
                }
                return null;
            }

            @Override
            public boolean isConfigurationTaskVisible() {
                return false;
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                AssignmentHolderType object = getAssignmetHolderFromRow(rowModel);
                if (object != null) {
                    executeUnassign(object, target);

                } else {

                    Task task = MemberOperationsHelper.createAndSubmitUnassignMembersTask(
                            AbstractRoleMemberPanel.this.getModelObject(),
                            getQueryScope(),
                            getSearchType(),
                            getActionQuery(rowModel, getQueryScope(), getSupportedRelations()),
                            getSupportedRelations(),
                            target, getPageBase());
                    processTaskAfterOperation(task, target);
                }
            }
        };
        getPageBase().showMainPopup(dialog, target);
    }

    protected void executeDelete(AssignmentHolderType object, AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_DELETE_OBJECT);
        try {
            Task task = getPageBase().createSimpleTask("Delete object");
            ObjectDelta<?> deleteDelta = getPrismContext()
                    .deltaFactory()
                    .object()
                    .createDeleteDelta(object.getClass(), object.getOid());
            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(deleteDelta);
            getPageBase().getModelService().executeChanges(deltas, null, task, result);
        } catch (Throwable e) {
            result.recordFatalError("Cannot delete object" + object + ", " + e.getMessage(), e);
            LOGGER.error("Error while deleting object {}, {}", object, e.getMessage(), e);
            target.add(getFeedback());
        }
        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        refreshTable(target);
    }

    protected WebMarkupContainer getFeedback() {
        return getPageBase().getFeedbackPanel();
    }

    protected void executeRecompute(AssignmentHolderType object, AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_RECOMPUTE_OBJECT);
        try {
            Task task = getPageBase().createSimpleTask("Recompute object");
            getPageBase().getModelService().recompute(object.getClass(), object.getOid(), null, task, result);
        } catch (Throwable e) {
            result.recordFatalError("Cannot recompute object" + object + ", " + e.getMessage(), e);
            LOGGER.error("Error while recomputing object {}, {}", object, e.getMessage(), e);
            target.add(getFeedback());
        }
        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        refreshTable(target);
    }

    protected void executeUnassign(AssignmentHolderType object, AjaxRequestTarget target) {
        List<AssignmentType> assignmentTypeList = getObjectAssignmentTypes(object);
        OperationResult result = new OperationResult(
                assignmentTypeList.size() == 1 ? OPERATION_UNASSIGN_OBJECT : OPERATION_UNASSIGN_OBJECTS);
        for (AssignmentType assignmentType : assignmentTypeList) {
            OperationResult subResult = result.createSubresult(OPERATION_UNASSIGN_OBJECT);
            try {
                Task task = getPageBase().createSimpleTask("Unassign object");

                ObjectDelta<?> objectDelta = getPrismContext()
                        .deltaFor(object.getClass())
                        .item(OrgType.F_ASSIGNMENT).delete(assignmentType.clone())
                        .asObjectDelta(object.getOid());

                Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
                getPageBase().getModelService().executeChanges(deltas, null, task, result);
                subResult.computeStatus();
            } catch (Throwable e) {
                subResult.recomputeStatus();
                subResult.recordFatalError("Cannot unassign object" + object + ", " + e.getMessage(), e);
                LOGGER.error("Error while unassigned object {}, {}", object, e.getMessage(), e);
                target.add(getFeedback());
            }
        }
        result.computeStatusComposite();
        getPageBase().showResult(result);
        refreshTable(target);
    }

    protected void refreshTable(AjaxRequestTarget target) {
        getMemberTable().refreshTable(target);
    }

    private String getTargetOrganizationOid() {
        ObjectReferenceType memberRef = ObjectTypeUtil.createObjectRef(AbstractRoleMemberPanel.this.getModelObject());
        return memberRef.getOid();
    }

    private List<AssignmentType> getObjectAssignmentTypes(AssignmentHolderType object) {
        return object.getAssignment().stream().filter(
                assignment -> assignment.getTargetRef().getOid().equals(getTargetOrganizationOid())).collect(Collectors.toList());
    }

    protected void createFocusMemberPerformed(AjaxRequestTarget target) {
        createFocusMemberPerformed(target, null);
    }

    protected void createFocusMemberPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec) {
        if (relationSpec != null) {
            try {
                List<ObjectReferenceType> newReferences = new ArrayList<>();
                if (CollectionUtils.isEmpty(relationSpec.getRelations())) {
                    relationSpec.setRelations(
                            Collections.singletonList(RelationTypes.MEMBER.getRelation()));
                }
                ObjectReferenceType memberRef = ObjectTypeUtil.createObjectRef(AbstractRoleMemberPanel.this.getModelObject(), relationSpec.getRelations().get(0));
                newReferences.add(memberRef);
                if (CollectionUtils.isNotEmpty(relationSpec.getArchetypeRefs())) {
                    newReferences.add(relationSpec.getArchetypeRefs().get(0));
                }
                QName newMemberType = CollectionUtils.isNotEmpty(relationSpec.getObjectTypes()) ? relationSpec.getObjectTypes().get(0) :
                        getSupportedObjectTypes().get(0); //getSupportedObjectTypes(false).get(0);
                WebComponentUtil.initNewObjectWithReference(AbstractRoleMemberPanel.this.getPageBase(), newMemberType, newReferences);
            } catch (SchemaException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } else {
            ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
                    getPageBase().getMainPopupBodyId()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<QName> getSupportedObjectTypes() {
                    return AbstractRoleMemberPanel.this.getNewMemberObjectTypes();
                }

                @Override
                protected List<QName> getSupportedRelations() {
                    if (isSubtreeScope()) {
                        return getDefaultRelationsForActions();
                    }
                    return AbstractRoleMemberPanel.this.getSupportedRelations();
                }

                @Override
                protected List<QName> getDefaultRelations() {
                    return getDefaultRelationsForActions();
                }

                protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                    if (type == null) {
                        getSession().warn("No type was selected. Cannot create member");
                        target.add(this);
                        target.add(getFeedback());
                        return;
                    }
                    if (checkRelationNotSelected(relations, "No relation was selected. Cannot create member", target)) {
                        return;
                    }
                    AbstractRoleMemberPanel.this.getPageBase().hideMainPopup(target);
                    try {
                        List<ObjectReferenceType> newReferences = new ArrayList<>();
                        for (QName relation : relations) {
                            newReferences.add(ObjectTypeUtil.createObjectRef(AbstractRoleMemberPanel.this.getModelObject(), relation));
                        }
                        WebComponentUtil.initNewObjectWithReference(AbstractRoleMemberPanel.this.getPageBase(), type, newReferences);
                    } catch (SchemaException e) {
                        throw new SystemException(e.getMessage(), e);
                    }

                }

                @Override
                protected QName getDefaultObjectType() {
                    if (relationSpec != null && CollectionUtils.isNotEmpty(relationSpec.getObjectTypes())) {
                        return relationSpec.getObjectTypes().get(0);
                    }
                    return super.getDefaultObjectType();
                }

                @Override
                protected boolean isFocusTypeSelectorVisible() {
                    return true;
                }
            };

            getPageBase().showMainPopup(chooseTypePopupContent, target);
        }
    }

    protected void deleteMembersPerformed(IModel<?> rowModel, QueryScope scope, QName memberType, Collection<QName> relations, AjaxRequestTarget target) {
        if (checkRelationNotSelected(relations, "No relation was selected. Cannot perform delete members", target)) {
            return;
        }
        MemberOperationsHelper.createAndSubmitDeleteMembersTask(
                getModelObject(),
                scope,
                memberType,
                getActionQuery(rowModel, scope, relations),
                target, getPageBase());
    }

    protected void unassignMembersPerformed(IModel<?> rowModel, QName type, QueryScope scope, Collection<QName> relations, AjaxRequestTarget target) {
        if (checkRelationNotSelected(relations, "No relation was selected. Cannot perform unassign members", target)) {
            return;
        }
        MemberOperationsHelper.createAndSubmitUnassignMembersTask(
                getModelObject(),
                scope,
                type,
                getActionQuery(rowModel, scope, relations),
                relations,
                target, getPageBase());
        target.add(this);
    }

    private boolean checkRelationNotSelected(Collection<QName> relations, String message, AjaxRequestTarget target) {
        if (CollectionUtils.isNotEmpty(relations)) {
            return false;
        }
        getSession().warn(message);
        target.add(this);
        target.add(getFeedback());
        return true;
    }

    protected @NotNull List<QName> getRelationsForRecomputeTask() {
        if (isSubtreeScope()) {
            return getDefaultRelationsForActions();
        }
        return getSupportedRelations();
    }

    protected ObjectQuery getActionQuery(IModel<?> rowModel, QueryScope scope, @NotNull Collection<QName> relations) {
        AssignmentHolderType assignmentHolder = getAssignmetHolderFromRow(rowModel);
        if (assignmentHolder == null) {
            return getActionQuery(scope, relations);
        }
        return MemberOperationsHelper.createSelectedObjectsQuery(Collections.singletonList(assignmentHolder));
    }

    protected ObjectQuery getActionQuery(QueryScope scope, @NotNull Collection<QName> relations) {
        switch (scope) {
            case ALL:
                return createAllMemberQuery(relations);
            case ALL_DIRECT:
                return MemberOperationsHelper.createDirectMemberQuery(
                        getModelObject(),
                        getSearchType(),
                        relations,
                        getTenantValue(),
                        getProjectValue());
            case SELECTED:
                return MemberOperationsHelper.createSelectedObjectsQuery(
                        getSelectedRealObjects());
        }

        return null;
    }

    private ObjectReferenceType getTenantValue() {
        AbstractRoleSearchItemWrapper memberSearchItems = getMemberSearchItems();
        if (memberSearchItems != null) {
            return memberSearchItems.getTenantValue();
        }
        return null;
    }

    private ObjectReferenceType getProjectValue() {
        AbstractRoleSearchItemWrapper memberSearchItems = getMemberSearchItems();
        if (memberSearchItems != null) {
            return memberSearchItems.getProjectValue();
        }
        return null;
    }

    protected List<? extends ObjectType> getSelectedRealObjects() {
        return getMemberTable().getSelectedRealObjects();
    }

    protected List<QName> getNewMemberObjectTypes() {
        return getSupportedObjectTypes();
    }

    protected MainObjectListPanel<FocusType> getMemberTable() {
        return (MainObjectListPanel<FocusType>) get(getPageBase().createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
    }

    protected WebMarkupContainer getMemberContainer() {
        return (WebMarkupContainer) get(getPageBase().createComponentPath(ID_FORM, ID_CONTAINER_MEMBER));
    }

    protected QueryScope getQueryScope() {
        // TODO if all selected objects have OIDs we can eliminate getOids call
        if (CollectionUtils.isNotEmpty(
                ObjectTypeUtil.getOids(getSelectedRealObjects()))) {
            return QueryScope.SELECTED;
        }

        if (isIndirect() || isSubtreeScope()) {
            return QueryScope.ALL;
        }

        return QueryScope.ALL_DIRECT;
    }

    protected boolean isSubtreeScope() {
        return SearchBoxScopeType.SUBTREE == getScopeValue();
    }

    private boolean isIndirect() {
        AbstractRoleSearchItemWrapper memberSearchItems = getMemberSearchItems();
        if (memberSearchItems != null) {
            return memberSearchItems.isIndirect();
        }
        return false;
    }

    protected @NotNull QName getSearchType() {
        //noinspection unchecked
        return ObjectTypes.getObjectType(getMemberPanelStorage().getSearch().getTypeClass())
                .getTypeQName();
    }

    protected SearchBoxScopeType getScopeValue() {
        AbstractRoleSearchItemWrapper memberSearchitem = getMemberSearchItems();
        if (memberSearchitem != null) {
            return memberSearchitem.getScopeValue();
        }
        return null;
    }

    private AbstractRoleSearchItemWrapper getMemberSearchItems() {
        return getMemberPanelStorage().getSearch().findMemberSearchItem();
    }

    protected QName getRelationValue() {
        AbstractRoleSearchItemWrapper memberSearchItems = getMemberSearchItems();
        if (memberSearchItems != null) {
            return memberSearchItems.getRelationValue();
        }
        return null;
    }

    protected ObjectQuery createAllMemberQuery(Collection<QName> relations) {
        return getPrismContext().queryFor(FocusType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(getModelObject(), relations))
                .build();
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return SelectorOptions.createCollection(GetOperationOptions.createDistinct());
    }

    protected MemberPanelStorage getMemberPanelStorage() {
        String storageKey = createStorageKey();
        if (StringUtils.isEmpty(storageKey)) {
            return null;
        }
        PageStorage storage = getPageStorage(storageKey);
        if (storage == null) {
            storage = getSessionStorage().initMemberStorage(storageKey);
        }
        return (MemberPanelStorage) storage;
    }

    private PageStorage getPageStorage(String storageKey) {
        return getSessionStorage().getPageStorageMap().get(storageKey);
    }

    private SessionStorage getSessionStorage() {
        return getPageBase().getSessionStorage();
    }

    protected String getStorageKeyTabSuffix() {
        return getPanelConfiguration().getIdentifier();
    }

    public R getModelObject() {
        return getObjectDetailsModels().getObjectType();
    }
}
