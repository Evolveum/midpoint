/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
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
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
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
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelInstances;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeAndRelationDialogPanel;
import com.evolveum.midpoint.web.component.dialog.ConfigureTaskConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.ContainerTypeSearchItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleCompositedSearchItem;
import com.evolveum.midpoint.web.page.admin.roles.SearchBoxConfigurationHelper;
import com.evolveum.midpoint.web.security.GuiAuthorizationConstants;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.Nullable;

@PanelType(name = "members")
@PanelInstances(value = {
        @PanelInstance(identifier = "roleMembers",
                applicableForType = RoleType.class,
                applicableForOperation = OperationTypeType.MODIFY,
                display = @PanelDisplay(label = "pageRole.members", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 80)),
        @PanelInstance(identifier = "roleGovernance",
                applicableForType = RoleType.class,
                applicableForOperation = OperationTypeType.MODIFY,
                display = @PanelDisplay(label = "pageRole.governance", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 90)),
        @PanelInstance(identifier = "serviceMembers",
                applicableForType = ServiceType.class,
                applicableForOperation = OperationTypeType.MODIFY,
                display = @PanelDisplay(label = "pageRole.members", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 80)),
        @PanelInstance(identifier = "serviceGovernance",
                applicableForType = ServiceType.class,
                applicableForOperation = OperationTypeType.MODIFY,
                display = @PanelDisplay(label = "pageRole.governance", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 90))
})
@PanelDisplay(label = "Members", order = 60)
public class AbstractRoleMemberPanel<R extends AbstractRoleType> extends AbstractObjectMainPanel<R, FocusDetailsModels<R>> {

    private static final long serialVersionUID = 1L;

    public enum QueryScope { // temporarily public because of migration
        SELECTED, ALL, ALL_DIRECT
    }

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);
    private static final String DOT_CLASS = AbstractRoleMemberPanel.class.getName() + ".";

    protected static final String OPERATION_LOAD_MEMBER_RELATIONS = DOT_CLASS + "loadMemberRelationsList";

    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    private static final String OPERATION_RECOMPUTE_OBJECT = DOT_CLASS + "recomputeObject";
    private static final String OPERATION_UNASSIGN_OBJECT = DOT_CLASS + "unassignObject";
    private static final String OPERATION_UNASSIGN_OBJECTS = DOT_CLASS + "unassignObjects";

    protected static final String ID_FORM = "form";

    protected static final String ID_CONTAINER_MEMBER = "memberContainer";
    protected static final String ID_MEMBER_TABLE = "memberTable";

    private SearchBoxConfigurationType additionalPanelConfig;
    private SearchBoxConfigurationHelper searchBoxConfiguration;

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
        this.additionalPanelConfig = getAdditionalPanelConfig();
        initMemberTable(form);
        setOutputMarkupId(true);
    }

    protected Form<?> getForm() {
        return (Form<?>) get(ID_FORM);
    }

    private <AH extends AssignmentHolderType> void initMemberTable(Form<?> form) {
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
                List<Component> buttonsList = super.createToolbarButtonsList(buttonId);
                AjaxIconButton assignButton = createAssignButton(buttonId);
                buttonsList.add(1, assignButton);
                return buttonsList;
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
                return getSession().getSessionStorage().getPageStorageMap().get(storageKey);
            }

            @Override
            protected Search<AH> createSearch(Class<AH> type) {
                return createMemberSearch(type);
            }

            @Override
            protected SelectableBeanObjectDataProvider<AH> createProvider() {
                SelectableBeanObjectDataProvider<AH> provider = createSelectableBeanObjectDataProvider(() -> getCustomizedQuery(getSearchModel().getObject()), null);
                provider.addQueryVariables(ExpressionConstants.VAR_PARENT_OBJECT, ObjectTypeUtil.createObjectRef(AbstractRoleMemberPanel.this.getModelObject()));
                return provider;
            }

            @Override
            public void refreshTable(AjaxRequestTarget target) {
                if (getSearchModel().isAttached() && getSearchModel().getObject() != null
                        && getSearchModel().getObject().isTypeChanged()) {
                    clearCache();
                }
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
        List<String> relations = new ArrayList<>();
        for (ObjectReferenceType roleMembershipRef : value.getRoleMembershipRef()) {
            List<QName> defaultRelations = getDefaultRelationsForActions();
            if (roleMembershipRef.getOid().equals(getModelObject().getOid())
                    && (defaultRelations.contains(roleMembershipRef.getRelation())
                    || defaultRelations.contains(PrismConstants.Q_ANY))) {
                String relation = roleMembershipRef.getRelation().getLocalPart();
                RelationDefinitionType relationDef = WebComponentUtil.getRelationDefinition(roleMembershipRef.getRelation());
                if (relationDef != null) {
                    DisplayType display = relationDef.getDisplay();
                    if (display != null) {
                        PolyStringType label = display.getLabel();
                        if (PolyStringUtils.isNotEmpty(label)) {
                            relation = WebComponentUtil.getTranslatedPolyString(label);
                        }
                    }
                }
                relations.add(relation);
            }
        }
        return String.join(", ", relations);
    }

    private CompiledObjectCollectionView getCompiledCollectionViewFromPanelConfiguration() {
        if (compiledCollectionViewFromPanelConfiguration != null) {
            return compiledCollectionViewFromPanelConfiguration;
        }
        if (getPanelConfiguration() == null) {
            return null;
        }
        if (getPanelConfiguration().getListView() == null) {
            return null;
        }
        CollectionRefSpecificationType collectionRefSpecificationType = getPanelConfiguration().getListView().getCollection();

        if (collectionRefSpecificationType == null) {
            compiledCollectionViewFromPanelConfiguration = new CompiledObjectCollectionView();
            getPageBase().getModelInteractionService().applyView(compiledCollectionViewFromPanelConfiguration, getPanelConfiguration().getListView());
            return null;
        }
        Task task = getPageBase().createSimpleTask("Compile collection");
        OperationResult result = task.getResult();
        try {
            compiledCollectionViewFromPanelConfiguration = getPageBase().getModelInteractionService().compileObjectCollectionView(collectionRefSpecificationType, AssignmentType.class, task, result);
        } catch (Throwable e) {
            LOGGER.error("Cannot compile object collection view for panel configuration {}. Reason: {}", getPanelConfiguration(), e.getMessage(), e);
            result.recordFatalError("Cannot compile object collection view for panel configuration " + getPanelConfiguration() + ". Reason: " + e.getMessage(), e);
            getPageBase().showResult(result);
        }
        return compiledCollectionViewFromPanelConfiguration;

    }

    private <AH extends AssignmentHolderType> Class<AH> getDefaultObjectTypeClass() {
        QName objectTypeQname = getSearchBoxConfiguration().getDefaultObjectTypeConfiguration().getDefaultValue();
        return ObjectTypes.getObjectTypeClass(objectTypeQname);
    }

    private <AH extends AssignmentHolderType> Search<AH> createMemberSearch(Class<AH> type) {
        MemberPanelStorage memberPanelStorage = getMemberPanelStorage();
        if (memberPanelStorage == null) { //normally, this should not happen
            return SearchFactory.createSearch(new ContainerTypeSearchItem<>(type), null, null,
                    null, getPageBase(), null, true, true, Search.PanelType.MEMBER_PANEL);
        }

        if (memberPanelStorage.getSearch() != null && !memberPanelStorage.getSearch().isTypeChanged()) {
            return memberPanelStorage.getSearch();
        }

        SearchBoxConfigurationHelper searchBoxConfig = getSearchBoxConfiguration();

        ContainerTypeSearchItem newSearchTypeItem = createSearchTypeItem(searchBoxConfig);
        if (memberPanelStorage.getSearch() != null && memberPanelStorage.getSearch().isTypeChanged()) {
            Class<AH> newType = memberPanelStorage.getSearch().getTypeClass();
            newSearchTypeItem.setType(createTypeSearchValue(newType));
        }
        Search<AH> search = SearchFactory.createSearch(newSearchTypeItem, null, null,
                null, getPageBase(), null, true, true, Search.PanelType.MEMBER_PANEL);
        search.addCompositedSpecialItem(createMemberSearchPanel(search, searchBoxConfig));

        if (additionalPanelConfig != null) {
            search.setCanConfigure(!Boolean.FALSE.equals(additionalPanelConfig.isAllowToConfigureSearchItems()));
        }
        memberPanelStorage.setSearch(search);
        return search;
    }

    private <AH extends AssignmentHolderType> ContainerTypeSearchItem<AH> createSearchTypeItem(SearchBoxConfigurationHelper searchBoxConfigurationHelper) {
        ContainerTypeSearchItem<AH> searchTypeItem = new ContainerTypeSearchItem<>(createTypeSearchValue(searchBoxConfigurationHelper.getDefaultObjectTypeConfiguration().getDefaultValue()), getAllowedTypes());
        searchTypeItem.setConfiguration(searchBoxConfigurationHelper.getDefaultObjectTypeConfiguration());
        searchTypeItem.setVisible(true);
        return searchTypeItem;
    }

    private AbstractRoleCompositedSearchItem<?> createMemberSearchPanel(Search search, SearchBoxConfigurationHelper searchBoxConfig) {
        return new AbstractRoleCompositedSearchItem<>(search, searchBoxConfig, !isNotRole(), isOrg());
    }

    private <AH extends AssignmentHolderType> ObjectQuery getCustomizedQuery(Search<AH> search) {
        SearchBoxConfigurationHelper memberPanelStorage = getSearchBoxConfiguration();
        if (noMemberSearchItemVisible(memberPanelStorage)) {
            PrismContext prismContext = getPageBase().getPrismContext();
            return prismContext.queryFor(search.getTypeClass())
                    .exists(AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(MemberOperationsHelper.createReferenceValuesList(getModelObject(), getRelationsForSearch(memberPanelStorage)))
                    .endBlock().build();
        }
        return null;
    }

    private boolean noMemberSearchItemVisible(SearchBoxConfigurationHelper memberPanelStorage) {
        return !memberPanelStorage.isRelationVisible() && !memberPanelStorage.isIndirectVisible()
                && (!isOrg() || !memberPanelStorage.isSearchScopeVisible())
                && (isNotRole() || !memberPanelStorage.isTenantVisible())
                && (isNotRole() || !memberPanelStorage.isProjectVisible());
    }

    private List<QName> getRelationsForSearch(SearchBoxConfigurationHelper memberPanelStorage) {
        List<QName> relations = new ArrayList<>();
        if (QNameUtil.match(PrismConstants.Q_ANY, memberPanelStorage.getDefaultRelation())) {
            relations.addAll(memberPanelStorage.getSupportedRelations());
        } else {
            relations.add(memberPanelStorage.getDefaultRelation());
        }
        return relations;
    }

    private boolean isOrg() {
        return getModelObject() instanceof OrgType;
    }

    private boolean isNotRole() {
        return !(getModelObject() instanceof RoleType);
    }

    private String createStorageKey() {
        UserProfileStorage.TableId tableId = getTableId(getComplexTypeQName());
        String collectionName = getPanelConfiguration() != null ? ("_" + getPanelConfiguration().getIdentifier()) : "";
        return tableId.name() + "_" + getStorageKeyTabSuffix() + collectionName;
    }

    private <AH extends AssignmentHolderType> List<DisplayableValue<Class<AH>>> getAllowedTypes() {
        List<DisplayableValue<Class<AH>>> ret = new ArrayList<>();
        ret.add(new SearchValue<>(getChoiceForAllTypes(), "ObjectTypes.all"));

        List<QName> types = getSearchBoxConfiguration().getSupportedObjectTypes();
        for (QName type : types) {
            ret.add(createTypeSearchValue(type));
        }
        return ret;
    }

    protected <AH extends AssignmentHolderType> Class<AH> getChoiceForAllTypes() {
        return (Class<AH>) FocusType.class;
    }

    private <AH extends AssignmentHolderType> SearchValue<Class<AH>> createTypeSearchValue(QName type) {
        Class<AH> typeClass = ObjectTypes.getObjectTypeClass(type);
        return createTypeSearchValue(typeClass);
    }

    private <AH extends AssignmentHolderType> SearchValue<Class<AH>> createTypeSearchValue(Class<AH> type) {
        return new SearchValue<>(type, "ObjectType." + type.getSimpleName());
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
                List<QName> supportedRelation = getSupportedRelations();
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

    private AjaxIconButton createAssignButton(String buttonId) {
        AjaxIconButton assignButton = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.EVO_ASSIGNMENT_ICON),
                createStringResource("TreeTablePanel.menu.addMembers")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ChooseMemberPopup browser = new ChooseMemberPopup(AbstractRoleMemberPanel.this.getPageBase().getMainPopupBodyId(),
                        getSearchBoxConfiguration().getDefaultRelationConfiguration(), loadMultiFunctionalButtonModel(false)) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected R getAssignmentTargetRefObject() {
                        return AbstractRoleMemberPanel.this.getModelObject();
                    }

                    @Override
                    protected List<QName> getAvailableObjectTypes() {
                        return null;
                    }

                    @Override
                    protected List<ObjectReferenceType> getArchetypeRefList() {
                        return new ArrayList<>(); //todo
                    }

                    @Override
                    protected boolean isOrgTreeVisible() {
                        return true;
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

    private CompositedIconButtonDto createCompositedIconButtonDto(DisplayType buttonDisplayType, AssignmentObjectRelation relation, CompositedIcon icon) {
        CompositedIconButtonDto compositedIconButtonDto = new CompositedIconButtonDto();
        compositedIconButtonDto.setAdditionalButtonDisplayType(buttonDisplayType);
        if (icon != null) {
            compositedIconButtonDto.setCompositedIcon(icon);
        } else {
            CompositedIconBuilder mainButtonIconBuilder = new CompositedIconBuilder();
            mainButtonIconBuilder.setBasicIcon(WebComponentUtil.getIconCssClass(buttonDisplayType), IconCssStyle.IN_ROW_STYLE)
                    .appendColorHtmlValue(WebComponentUtil.getIconColor(buttonDisplayType));
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
                                    target, getSearchBoxConfiguration().getDefaultRelationConfiguration(), null);
                        }
                    };
                }
            };
            menuItem.setVisibilityChecker((rowModel, isHeader) -> isHeader);
            menu.add(menuItem);
        }
    }

    protected <AH extends AssignmentHolderType> void createUnassignMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_UNASSIGN)) {
            InlineMenuItem menuItem = new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.unassign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<SelectableBean<AH>>() {
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
                && ((SelectableBean) rowModel.getObject()).getValue() instanceof AssignmentHolderType) {
            return (AssignmentHolderType) ((SelectableBean) rowModel.getObject()).getValue();
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

    protected List<QName> getSupportedRelations() {
        if ("roleMembers".equals(getPanelConfiguration().getIdentifier()) || "serviceMembers".equals(getPanelConfiguration().getIdentifier())) {
            return getSupportedMembersTabRelations();
        }
        if ("roleGovernance".equals(getPanelConfiguration().getIdentifier()) || "serviceGovernance".equals(getPanelConfiguration().getIdentifier())) {
            return getSupportedGovernanceTabRelations();
        }
        return new ArrayList<>();
    }

    protected List<QName> getSupportedMembersTabRelations() {
        List<QName> relations = WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ADMINISTRATION, getPageBase());
        List<QName> governance = WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.GOVERNANCE, getPageBase());
        governance.forEach(relations::remove);
        return relations;
//        return new AvailableRelationDto(relations, defaultRelationConfiguration);
    }

    protected List<QName> getSupportedGovernanceTabRelations() {
        return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.GOVERNANCE, getPageBase());
    }

    protected SearchBoxConfigurationType getAdditionalPanelConfig() {
        CompiledObjectCollectionView collectionViewFromPanelConfig = getCompiledCollectionViewFromPanelConfiguration();
        if (collectionViewFromPanelConfig != null) {
            return collectionViewFromPanelConfig.getSearchBoxConfiguration();
        }
        CompiledObjectCollectionView view = WebComponentUtil.getCollectionViewByObject(getModelObject(), getPageBase());
        if (view != null && view.getAdditionalPanels() != null) {
            GuiObjectListPanelConfigurationType config = view.getAdditionalPanels().getMemberPanel();
            return config == null ? new SearchBoxConfigurationType() : config.getSearchBoxConfiguration();
        }
        return new SearchBoxConfigurationType();
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

    protected <AH extends AssignmentHolderType> void unassignMembersPerformed(IModel<SelectableBean<AH>> rowModel, AjaxRequestTarget target) {
        unassignMembersPerformed(rowModel, null, target);
    }

    private <AH extends AssignmentHolderType> void unassignMembersPerformed(IModel<SelectableBean<AH>> rowModel, QName relation, AjaxRequestTarget target) {
        QueryScope scope = getQueryScope();
        StringResourceModel confirmModel;

        if (rowModel != null || getSelectedObjectsCount() > 0) {
            var singleObj = rowModel != null ? rowModel.getObject() : null;
            if (singleObj == null) {
                singleObj = getSelectedObjectsCount() == 1 ? (SelectableBean<AH>) getMemberTable().getSelectedObjects().get(0) : null;
            }
            final List<QName> membershipAvailableRelations = new ArrayList<>();
            if (singleObj != null) {
                //there can be a situation when there is only one membership relation (for the current panel) but
                //in general the object can have more membership relations assigned (e.g. one membership within Members panel and
                // another membership within Governance panel)
                // in this case we need to specify the relation to be unassigned (ticket #9936)
                membershipAvailableRelations.addAll(getMembershipAvailableRelations(singleObj.getValue()));
                final List<QName> allMembershipRelations = new ArrayList<>(getAllMembershipRelations(singleObj.getValue()));
                if (membershipAvailableRelations.size() == 1 && allMembershipRelations.size() > 1 && relation == null) {
                    relation = membershipAvailableRelations.get(0);
                }

            }

            String unassignActionTranslated =
                    createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject.unassign")
                            .getString();
            confirmModel = rowModel != null
                    ? createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject",
                    unassignActionTranslated, ((ObjectType) ((SelectableBean<?>) rowModel.getObject()).getValue()).getName())
                    : createStringResource("abstractRoleMemberPanel.unassignSelectedMembersConfirmationLabel",
                    getSelectedObjectsCount());

            if (membershipAvailableRelations.size() > 1) {
                //if there are more than 1 membership's relation, we should give a possibility
                //to the user to select which relation they want to unassign

                ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
                        getPageBase().getMainPopupBodyId(), confirmModel) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isFocusTypeSelectorVisible() {
                        return false;
                    }

                    @Override
                    protected List<QName> getSupportedRelations() {
                        return membershipAvailableRelations;
                    }

                    protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                        unassignMembersPerformed(
                                rowModel,
                                type,
                                scope,
                                relations,
                                target);
                    }
                };

                getPageBase().showMainPopup(chooseTypePopupContent, target);

            } else {
                showConfirmDialog(rowModel, relation, confirmModel, target);
            }
        } else {
            confirmModel = createStringResource("abstractRoleMemberPanel.unassignAllMembersConfirmationLabel");

            ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
                    getPageBase().getMainPopupBodyId(), confirmModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<QName> getSupportedObjectTypes() {
                    return AbstractRoleMemberPanel.this.getSearchBoxConfiguration().getSupportedObjectTypes();//getSupportedObjectTypes(true);
                }

                @Override
                protected List<QName> getSupportedRelations() {
                    if (isSubtreeScope()) {
                        return getDefaultRelationsForActions();
                    }
                    return AbstractRoleMemberPanel.this.getSearchBoxConfiguration().getSupportedRelations();
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
                    if (getSearchBoxConfiguration().isSearchScope(SearchBoxScopeType.SUBTREE)) {
                        return 325;
                    }
                    return 230;
                }
            };

            getPageBase().showMainPopup(chooseTypePopupContent, target);
        }
    }

    private int getSelectedObjectsCount() {
        return getMemberTable().getSelectedObjectsCount();
    }

//    private void executeSimpleUnassignedOperation(IModel<?> rowModel, StringResourceModel confirmModel, AjaxRequestTarget target) {
//        ConfirmationPanel dialog = new ConfigureTaskConfirmationPanel(getPageBase().getMainPopupBodyId(), confirmModel) {
//            @Override
//            protected IModel<String> getWarningMessageModel() {
//                if (isSubtreeScope() && rowModel == null) {
//                    return createStringResource("abstractRoleMemberPanel.unassign.warning.subtree");
//                } else if (isIndirect() && rowModel == null) {
//                    return createStringResource("abstractRoleMemberPanel.unassign.warning.indirect");
//                }
//                return null;
//            }
//
//            @Override
//            public boolean isConfigurationTaskVisible() {
//                return false;
//            }
//
//            @Override
//            public void yesPerformed(AjaxRequestTarget target) {
//                AssignmentHolderType object = getAssignmetHolderFromRow(rowModel);
//                if (object != null) {
//                    executeUnassign(object, target);
//
//                } else {
//
//                    MemberOperationsHelper.createAndSubmitUnassignMembersTask(
//                            AbstractRoleMemberPanel.this.getModelObject(),
//                            getQueryScope(),
//                            getSearchType(),
//                            getActionQuery(rowModel, getQueryScope(), getSearchBoxConfiguration().getSupportedRelations()),
//                            getSearchBoxConfiguration().getSupportedRelations(),
//                            target, getPageBase());
//                }
//            }
//        };
//        getPageBase().showMainPopup(dialog, target);
//    }

    protected void executeUnassign(AssignmentHolderType object, QName relation, AjaxRequestTarget target) {
        List<AssignmentType> assignmentTypeList = getObjectAssignmentTypes(object, relation);
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
                target.add(getPageBase().getFeedbackPanel());
            }
        }
        result.computeStatusComposite();
        getPageBase().showResult(result);
        getMemberTable().refreshTable(target);
    }

    private String getTargetOrganizationOid() {
        ObjectReferenceType memberRef = ObjectTypeUtil.createObjectRef(AbstractRoleMemberPanel.this.getModelObject());
        return memberRef.getOid();
    }

    private List<AssignmentType> getObjectAssignmentTypes(AssignmentHolderType object, QName relation) {
        return object.getAssignment().stream()
                .filter(
                        assignment -> assignment.getTargetRef() != null
                                && assignment.getTargetRef().getOid().equals(getTargetOrganizationOid())
                                && (relation == null || QNameUtil.match(relation, assignment.getTargetRef().getRelation())))
                .collect(Collectors.toList());
    }

    private List<QName> getDefaultRelationsForActions() {

        List<QName> defaultRelations = new ArrayList<>();
        QName defaultRelation = getSearchRelationValue();

        if (isSubtreeScope()) {
            defaultRelations.add(RelationTypes.MEMBER.getRelation());
            return defaultRelations;
        }

        if (defaultRelation != null) {
            defaultRelations.add(defaultRelation);
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

        if (rowModel != null || getMemberTable().getSelectedObjectsCount() > 0) {

            confirmModel = rowModel != null
                    ? createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject",
                    "delete", ((ObjectType) ((SelectableBean<?>) rowModel.getObject()).getValue()).getName())
                    : createStringResource("abstractRoleMemberPanel.deleteSelectedMembersConfirmationLabel",
                    getMemberTable().getSelectedObjectsCount());

            executeSimpleDeleteOperation(rowModel, confirmModel, target);
        } else {
            confirmModel = createStringResource("abstractRoleMemberPanel.deleteAllMembersConfirmationLabel");

            QueryScope scope = getQueryScope();
            ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
                    ((PageBase) getPage()).getMainPopupBodyId(), confirmModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected List<QName> getSupportedObjectTypes() {
                    return AbstractRoleMemberPanel.this.getSearchBoxConfiguration().getSupportedObjectTypes();//getSupportedObjectTypes(true);
                }

                @Override
                protected List<QName> getSupportedRelations() {
                    if (isSubtreeScope()) {
                        return getDefaultRelationsForActions();
                    }
                    return AbstractRoleMemberPanel.this.getSearchBoxConfiguration().getSupportedRelations();
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
                            getActionQuery(rowModel, getQueryScope(), getSearchBoxConfiguration().getSupportedRelations()),
                            target, getPageBase());
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
            target.add(getPageBase().getFeedbackPanel());
        }
        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        getMemberTable().refreshTable(target);
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
                        getSearchBoxConfiguration().getSupportedObjectTypes().get(0); //getSupportedObjectTypes(false).get(0);
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
                    return AbstractRoleMemberPanel.this.getSearchBoxConfiguration().getSupportedRelations();
                }

                @Override
                protected List<QName> getDefaultRelations() {
                    return getDefaultRelationsForActions();
                }

                protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                    if (type == null) {
                        getSession().warn("No type was selected. Cannot create member");
                        target.add(this);
                        target.add(getPageBase().getFeedbackPanel());
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

    private boolean checkRelationNotSelected(Collection<QName> relations, String message, AjaxRequestTarget target) {
        if (CollectionUtils.isNotEmpty(relations)) {
            return false;
        }
        getSession().warn(message);
        target.add(this);
        target.add(getPageBase().getFeedbackPanel());
        return true;
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
                        getSearchBoxConfiguration().getTenant(),
                        getSearchBoxConfiguration().getProject());
            case SELECTED:
                return MemberOperationsHelper.createSelectedObjectsQuery(
                        getMemberTable().getSelectedRealObjects());
        }

        return null;
    }

    protected List<QName> getDefaultSupportedObjectTypes(boolean includeAbstractTypes) {
        return WebComponentUtil.createFocusTypeList(includeAbstractTypes);
    }

    protected List<QName> getNewMemberObjectTypes() {
        return WebComponentUtil.createFocusTypeList();
    }

    protected MainObjectListPanel<FocusType> getMemberTable() {
        return (MainObjectListPanel<FocusType>) get(getPageBase().createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
    }

    private Search<FocusType> getSearch() {
        return getMemberTable().getSearchModel().getObject();
    }

    protected QueryScope getQueryScope() {
        // TODO if all selected objects have OIDs we can eliminate getOids call
        if (CollectionUtils.isNotEmpty(
                ObjectTypeUtil.getOids(getMemberTable().getSelectedRealObjects()))) {
            return QueryScope.SELECTED;
        }

        if (isIndirect() || isSubtreeScope()) {
            return QueryScope.ALL;
        }

        return QueryScope.ALL_DIRECT;
    }

    private boolean isSubtreeScope() {
        return getSearchScopeType().equals(SearchBoxScopeType.SUBTREE);
    }

    private boolean isIndirect() {
        return BooleanUtils.isTrue(getSearchIndirect());
    }

    private void recomputeMembersPerformed(IModel<?> rowModel, AjaxRequestTarget target) {
        StringResourceModel confirmModel;

        if (rowModel != null || getMemberTable().getSelectedObjectsCount() > 0) {

            confirmModel = rowModel != null
                    ? createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject",
                    "recompute", ((ObjectType) ((SelectableBean<?>) rowModel.getObject()).getValue()).getName())
                    : createStringResource("abstractRoleMemberPanel.recomputeSelectedMembersConfirmationLabel",
                    getMemberTable().getSelectedObjectsCount());

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
                            getActionQuery(rowModel, getQueryScope(), getSearchBoxConfiguration().getSupportedRelations()),
                            target, getPageBase());
                }
            }
        };
        getPageBase().showMainPopup(dialog, target);
    }

    protected void executeRecompute(AssignmentHolderType object, AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_RECOMPUTE_OBJECT);
        try {
            Task task = getPageBase().createSimpleTask("Recompute object");
            getPageBase().getModelService().recompute(object.getClass(), object.getOid(), null, task, result);
        } catch (Throwable e) {
            result.recordFatalError("Cannot recompute object" + object + ", " + e.getMessage(), e);
            LOGGER.error("Error while recomputing object {}, {}", object, e.getMessage(), e);
            target.add(getPageBase().getFeedbackPanel());
        }
        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        getMemberTable().refreshTable(target);
    }

    protected @NotNull List<QName> getRelationsForRecomputeTask() {
        return getDefaultRelationsForActions();
    }

    protected @NotNull QName getSearchType() {
        //noinspection unchecked
        return ObjectTypes.getObjectType(getMemberPanelStorage().getSearch().getTypeClass())
                .getTypeQName();
    }

    protected ObjectQuery createAllMemberQuery(Collection<QName> relations) {
        return getPrismContext().queryFor(FocusType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(getModelObject(), relations))
                .build();
    }

    private Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return SelectorOptions.createCollection(GetOperationOptions.createDistinct());
    }

    protected Class<? extends ObjectType> getDefaultObjectType() {
        return FocusType.class;
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

    protected SearchBoxConfigurationHelper getSearchBoxConfiguration() {
        if (searchBoxConfiguration != null) {
            return searchBoxConfiguration;
        }
        searchBoxConfiguration = new SearchBoxConfigurationHelper(additionalPanelConfig);
        searchBoxConfiguration.setDefaultSupportedRelations(getSupportedRelations());
        searchBoxConfiguration.setDefaultSupportedObjectTypes(getDefaultSupportedObjectTypes(true));
        searchBoxConfiguration.setDefaultObjectType(WebComponentUtil.classToQName(getPrismContext(), getDefaultObjectType()));

        return searchBoxConfiguration;
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

    private AbstractRoleCompositedSearchItem<?> getAbstractRoleCompositedItems() {
        AbstractRoleCompositedSearchItem<?> filter = null;
        if (getSearch().getCompositedSpecialItem() instanceof AbstractRoleCompositedSearchItem) {
            filter = ((AbstractRoleCompositedSearchItem<?>) getSearch().getCompositedSpecialItem());
        }
        return filter;
    }

    private SearchBoxScopeType getSearchScopeType() {
        AbstractRoleCompositedSearchItem<?> filter = getAbstractRoleCompositedItems();
        if (filter != null) {
            if (filter.getScope() != null) {
                return filter.getScope();
            }
        }
        return null;
    }

    private QName getSearchRelationValue() {
        AbstractRoleCompositedSearchItem<?> filter = getAbstractRoleCompositedItems();
        if (filter != null) {
            if (filter.getRelation() != null) {
                return filter.getRelation();
            }
        }
        return null;
    }

    private Boolean getSearchIndirect() {
        AbstractRoleCompositedSearchItem<?> filter = getAbstractRoleCompositedItems();
        if (filter != null) {
            if (filter.getIndirect() != null) {
                return filter.getIndirect();
            }
        }
        return null;
    }

    private <AH extends AssignmentHolderType> List<QName> getMembershipAvailableRelations(AH value) {
        return value.getRoleMembershipRef().stream()
                .filter(this::isApplicableRoleMembershipRef)
                .map(ObjectReferenceType::getRelation)
                .collect(Collectors.toList());
    }

    private boolean isApplicableRoleMembershipRef(ObjectReferenceType roleMembershipRef) {
        List<QName> defaultRelations = getDefaultRelationsForActions();
        return roleMembershipRef.getOid().equals(getModelObject().getOid())
                && (defaultRelations.contains(roleMembershipRef.getRelation())
                || defaultRelations.contains(PrismConstants.Q_ANY));
    }

    private <AH extends AssignmentHolderType> List<QName> getAllMembershipRelations(AH value) {
        return value.getRoleMembershipRef().stream()
                .filter(ref -> ref.getOid().equals(getModelObject().getOid()))
                .map(ObjectReferenceType::getRelation)
                .collect(Collectors.toList());
    }

    private void showConfirmDialog(
            IModel<?> rowModel, QName relation, StringResourceModel confirmModel, AjaxRequestTarget target) {
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
                executeUnassignedOperationAfterConfirm(rowModel, relation, target);
            }
        };
        getPageBase().showMainPopup(dialog, target);
    }

    protected void executeUnassignedOperationAfterConfirm(IModel<?> rowModel, QName relation, AjaxRequestTarget target) {
        AssignmentHolderType object = getAssignmentHolderFromRow(rowModel);
        if (object != null) {
            executeUnassign(object, relation, target);

        }
    }

    private AssignmentHolderType getAssignmentHolderFromRow(@Nullable IModel<?> rowModel) {
        if (rowModel != null
                && rowModel.getObject() instanceof SelectableBean<?>
                && ((SelectableBean<?>)rowModel.getObject()).getValue() instanceof AssignmentHolderType) {
            return (AssignmentHolderType)((SelectableBean<?>)rowModel.getObject()).getValue();
        }
        return null;
    }
}
