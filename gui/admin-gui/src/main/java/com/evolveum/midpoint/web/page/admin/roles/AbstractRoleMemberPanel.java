/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.*;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.web.component.MultifunctionalButton;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.dialog.ConfigureTaskConfirmationPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeAndRelationDialogPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.RelationDropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.GuiAuthorizationConstants;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractRoleMemberPanel<R extends AbstractRoleType> extends BasePanel<R> {

    private static final long serialVersionUID = 1L;

    protected enum QueryScope {
        SELECTED, ALL, ALL_DIRECT
    }

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);
    private static final String DOT_CLASS = AbstractRoleMemberPanel.class.getName() + ".";

    protected static final String OPERATION_LOAD_MEMBER_RELATIONS = DOT_CLASS + "loadMemberRelationsList";

    protected static final String ID_FORM = "form";

    protected static final String ID_CONTAINER_MEMBER = "memberContainer";
    protected static final String ID_MEMBER_TABLE = "memberTable";

    private ScopeSearchItemConfigurationType defaultScopeConfiguration = null;
    private ObjectTypeSearchItemConfigurationType defaultObjectTypeConfiguration = null;
    private Class<ObjectType> defaultObjectTypeClass = null;
    private RelationSearchItemConfigurationType defaultRelationConfiguration = null;
    private IndirectSearchItemConfigurationType defaultIndirectConfiguration = null;
    private UserInterfaceFeatureType defaultTenantConfiguration = null;
    private UserInterfaceFeatureType defaultProjectConfiguration = null;
    private PageBase pageBase;

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

    public AbstractRoleMemberPanel(String id, IModel<R> model, PageBase parentPage) {
        super(id, model);
        this.pageBase = parentPage;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Form<?> form = new MidpointForm(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);
        initDefaultSearchParameters();
        initMemberTable(form);
        setOutputMarkupId(true);

    }

    protected void initDefaultSearchParameters() {
        GuiObjectListPanelConfigurationType additionalPanel = getAdditionalPanelConfig();
        if (additionalPanel != null && additionalPanel.getSearchBoxConfiguration() != null) {
            defaultScopeConfiguration = additionalPanel.getSearchBoxConfiguration().getScopeConfiguration();
            if (defaultScopeConfiguration.getDefaultValue() == null) {
                defaultScopeConfiguration.setDefaultValue(additionalPanel.getSearchBoxConfiguration().getDefaultScope());
            }

            defaultObjectTypeConfiguration = additionalPanel.getSearchBoxConfiguration().getObjectTypeConfiguration();
            if (defaultObjectTypeConfiguration.getDefaultValue() == null) {
                if (additionalPanel.getSearchBoxConfiguration().getDefaultObjectType() != null) {
                    defaultObjectTypeClass = (Class<ObjectType>) WebComponentUtil.qnameToClass(getPageBase().getPrismContext(),
                            additionalPanel.getSearchBoxConfiguration().getDefaultObjectType());
                }
            } else {
                defaultObjectTypeClass = (Class<ObjectType>) WebComponentUtil.qnameToClass(getPageBase().getPrismContext(),
                        defaultObjectTypeConfiguration.getDefaultValue());
            }

            defaultRelationConfiguration = additionalPanel.getSearchBoxConfiguration().getRelationConfiguration();
            defaultIndirectConfiguration = additionalPanel.getSearchBoxConfiguration().getIndirectConfiguration();
            defaultTenantConfiguration = additionalPanel.getSearchBoxConfiguration().getTenantConfiguration();
            defaultProjectConfiguration = additionalPanel.getSearchBoxConfiguration().getProjectConfiguration();
        }
        if (defaultScopeConfiguration == null) {
            defaultScopeConfiguration = new ScopeSearchItemConfigurationType();
        }
        if (defaultScopeConfiguration.getDefaultValue() == null) {
            defaultScopeConfiguration.setDefaultValue(SearchBoxScopeType.ONE_LEVEL);
        }
        setDisplay(defaultScopeConfiguration, "scope", "abstractRoleMemberPanel.searchScope", "abstractRoleMemberPanel.searchScope.tooltip");

        if (defaultObjectTypeClass == null) {
            defaultObjectTypeClass = getDefaultObjectType();
        }
        if (defaultObjectTypeConfiguration == null) {
            defaultObjectTypeConfiguration = new ObjectTypeSearchItemConfigurationType();
        }
        if (defaultObjectTypeConfiguration.getVisibility() == null) {
            defaultObjectTypeConfiguration.setVisibility(UserInterfaceElementVisibilityType.AUTOMATIC);
        }

        if (defaultRelationConfiguration == null) {
            defaultRelationConfiguration = new RelationSearchItemConfigurationType();
        }
        setDisplay(defaultRelationConfiguration, "relation","relationDropDownChoicePanel.relation", "relationDropDownChoicePanel.tooltip.relation");

        if (defaultIndirectConfiguration == null) {
            defaultIndirectConfiguration = new IndirectSearchItemConfigurationType();
        }
        if (defaultIndirectConfiguration.isIndirect() == null) {
            defaultIndirectConfiguration.setIndirect(false);
        }
        setDisplay(defaultIndirectConfiguration, "indirect", "abstractRoleMemberPanel.indirectMembers", "abstractRoleMemberPanel.indirectMembers.tooltip");

        if (defaultTenantConfiguration == null) {
            defaultTenantConfiguration = new UserInterfaceFeatureType();
        }
        setDisplay(defaultTenantConfiguration, "tenant","abstractRoleMemberPanel.tenant", null);

        if (defaultProjectConfiguration == null) {
            defaultProjectConfiguration = new UserInterfaceFeatureType();
        }
        setDisplay(defaultProjectConfiguration, "project/org", "abstractRoleMemberPanel.project", null);
    }

    private void setDisplay(UserInterfaceFeatureType configuration, String orig, String labelKey, String helpKey) {
        if (configuration.getDisplay() == null) {
            DisplayType display = new DisplayType();
            configuration.setDisplay(display);
        }
        if (configuration.getDisplay().getLabel() == null) {
            DisplayType display = configuration.getDisplay();
            PolyStringType label = new PolyStringType(orig);
            PolyStringTranslationType translationLabel = new PolyStringTranslationType();
            translationLabel.setKey(labelKey);
            label.setTranslation(translationLabel);
            display.setLabel(label);
            configuration.setDisplay(display);
        }
        if (helpKey != null && configuration.getDisplay().getHelp() == null) {
            DisplayType display = configuration.getDisplay();
            PolyStringType help = new PolyStringType("");
            PolyStringTranslationType translationHelp = new PolyStringTranslationType();
            translationHelp.setKey(helpKey);
            help.setTranslation(translationHelp);
            display.setHelp(help);
            configuration.setDisplay(display);
        }
        if (configuration.getVisibility() == null) {
            configuration.setVisibility(UserInterfaceElementVisibilityType.AUTOMATIC);
        }
    }

    protected Form<?> getForm() {
        return (Form) get(ID_FORM);
    }

    private void initMemberTable(Form<?> form) {
        WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
        memberContainer.setOutputMarkupId(true);
        memberContainer.setOutputMarkupPlaceholderTag(true);
        form.add(memberContainer);

        PageBase pageBase = getPageBase();
        //TODO QName defines a relation value which will be used for new member creation
        MainObjectListPanel<ObjectType> childrenListPanel = new MainObjectListPanel<ObjectType>(
                ID_MEMBER_TABLE, defaultObjectTypeClass, getSearchOptions()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return AbstractRoleMemberPanel.this.getTableId(getComplexTypeQName());
            }

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectType object) {
                detailsPerformed(target, object);
            }

            @Override
            protected boolean isObjectDetailsEnabled(IModel<SelectableBean<ObjectType>> rowModel) {
                if (rowModel == null || rowModel.getObject() == null
                        || rowModel.getObject().getValue() == null) {
                    return false;
                }
                Class<?> objectClass = rowModel.getObject().getValue().getClass();
                return WebComponentUtil.hasDetailsPage(objectClass);
            }

            @Override
            protected DisplayType getNewObjectButtonSpecialDisplayType() {
                return getCreateMemberButtonDisplayType();
            }

            @Override
            protected DisplayType getNewObjectButtonStandardDisplayType() {
                return WebComponentUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green",
                        createStringResource("abstractRoleMemberPanel.menu.createMember", "", "").getString());
            }

            @Override
            protected List<MultiFunctinalButtonDto> loadButtonDescriptions() {
                return createAdditionalButtonsDescription();
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
                AbstractRoleMemberPanel.this.createFocusMemberPerformed(target, relation);
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                List<Component> buttonsList = super.createToolbarButtonsList(buttonId);
                MultifunctionalButton assignButton = createAssignButton(buttonId);
                buttonsList.add(1, assignButton);
                return buttonsList;
            }

            @Override
            protected IColumn<SelectableBean<ObjectType>, String> createIconColumn() {
                return ColumnUtils.createIconColumn(pageBase);
            }

            @Override
            protected List<IColumn<SelectableBean<ObjectType>, String>> createDefaultColumns() {
                return (List) createMembersColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRowActions();
            }

            @Override
            protected String getStorageKey() {
                return AbstractRoleMemberPanel.this.createStorageKey();
            }

            @Override
            protected ContainerTypeSearchItem getTypeItem(Class<? extends ObjectType> type, List<DisplayableValue<Class<? extends ObjectType>>> allowedValues) {
                ContainerTypeSearchItem item = super.getTypeItem(type, allowedValues);
                item.setConfiguration(defaultObjectTypeConfiguration);
                return item;
            }

            protected PageStorage getPageStorage(String storageKey){
                PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
                if (storage == null) {
                    storage = getSession().getSessionStorage().initPageStorage(storageKey);
                    if (storage instanceof MemberPanelStorage) {
                        ((MemberPanelStorage) storage).setIndirect(defaultIndirectConfiguration.isIndirect());
                        ((MemberPanelStorage) storage).setRelation(defaultRelationConfiguration.getDefaultValue());
                        ((MemberPanelStorage) storage).setOrgSearchScope(defaultScopeConfiguration.getDefaultValue());
                    }
                }
                return storage;
            }

            @Override
            protected Search createSearch(Class<? extends ObjectType> type) {
                Search search = null;
                if (getMemberPanelStorage() != null) {
                    if (getMemberPanelStorage().getRelation() == null) {
                        getMemberPanelStorage().setRelation(getSupportedRelations().getDefaultRelation());
                    }
                    if (getMemberPanelStorage().getSearch() != null) {
                        search = getMemberPanelStorage().getSearch();
                    }
                }

                ContainerTypeSearchItem oldTypeItem;
                if (search != null) {
                    if (!search.isTypeChanged()) {
                        return search;
                    }
                    oldTypeItem = search.getType();
                } else {
                    oldTypeItem = getTypeItem(type, getAllowedTypes());
                }

                search = SearchFactory.createSearch(oldTypeItem, null, null, null, pageBase, null, true, true, Search.PanelType.MEMBER_PANEL);
                search.getType().setVisible(true);

                if (CompiledGuiProfile.isVisible(defaultRelationConfiguration.getVisibility(), null)) {
                    search.addSpecialItem(createRelationItem(search));
                }
                if (CompiledGuiProfile.isVisible(defaultIndirectConfiguration.getVisibility(), null)) {
                    search.addSpecialItem(createIndirectItem(search));
                }
                if (AbstractRoleMemberPanel.this.getModelObject() instanceof OrgType
                        && CompiledGuiProfile.isVisible(defaultScopeConfiguration.getVisibility(), null)) {
                    search.addSpecialItem(createScopeItem(search));
                }
                if (AbstractRoleMemberPanel.this.getModelObject() instanceof RoleType) {
                    if (CompiledGuiProfile.isVisible(defaultTenantConfiguration.getVisibility(), null)) {
                        search.addSpecialItem(createTenantItem(search));
                    }
                    if (CompiledGuiProfile.isVisible(defaultProjectConfiguration.getVisibility(), null)) {
                        search.addSpecialItem(createProjectItem(search));
                    }
                }

                GuiObjectListPanelConfigurationType additionalPanel = getAdditionalPanelConfig();
                if (additionalPanel != null && additionalPanel.getSearchBoxConfiguration() != null){
                    search.setCanConfigure(!Boolean.FALSE.equals(additionalPanel.getSearchBoxConfiguration().isAllowToConfigureSearchItems()));
                }
                return search;
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                if (!CompiledGuiProfile.isVisible(defaultRelationConfiguration.getVisibility(), null)
                        && !CompiledGuiProfile.isVisible(defaultIndirectConfiguration.getVisibility(), null)
                        && (!(AbstractRoleMemberPanel.this.getModelObject() instanceof OrgType) || !CompiledGuiProfile.isVisible(defaultScopeConfiguration.getVisibility(), null))
                        && (!(AbstractRoleMemberPanel.this.getModelObject() instanceof RoleType) || !CompiledGuiProfile.isVisible(defaultTenantConfiguration.getVisibility(), null))
                        && (!(AbstractRoleMemberPanel.this.getModelObject() instanceof RoleType) || !CompiledGuiProfile.isVisible(defaultProjectConfiguration.getVisibility(), null))){
                    PrismContext prismContext = getPageBase().getPrismContext();
                    List relations = new ArrayList();
                    if (QNameUtil.match(PrismConstants.Q_ANY, getSupportedRelations().getDefaultRelation())) {
                        relations.addAll(getSupportedRelations().getAvailableRelationList());
                    } else {
                        relations.add(getSupportedRelations().getDefaultRelation());
                    }

                    R object = AbstractRoleMemberPanel.this.getModelObject();
                    Class type = getSearchModel().getObject().getTypeClass();
                    return prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                            .block()
                            .item(AssignmentType.F_TARGET_REF)
                            .ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
                            .endBlock().build();
                }
                return super.getCustomizeContentQuery();
            }

            @Override
            protected ISelectableDataProvider createProvider() {
                SelectableBeanObjectDataProvider provider = (SelectableBeanObjectDataProvider) super.createProvider();
                provider.setIsMemberPanel(true);
                provider.addQueryVariables(ExpressionConstants.VAR_PARENT_OBJECT, AbstractRoleMemberPanel.this.getModelObject());
                return provider;
            }

            protected boolean isTypeChanged(Class<ObjectType> newTypeClass) {
                return true;
            }

            @Override
            public void refreshTable(AjaxRequestTarget target) {
                if (getSearchModel().isLoaded() && getSearchModel().getObject()!= null
                        && getSearchModel().getObject().isTypeChanged()) {
                    clearCache();
                }
                super.refreshTable(target);
            }
        };
        childrenListPanel.setOutputMarkupId(true);
        memberContainer.add(childrenListPanel);
    }

    private String createStorageKey() {
        UserProfileStorage.TableId tableId = getTableId(getComplexTypeQName());
        GuiObjectListPanelConfigurationType view = getAdditionalPanelConfig();
        String collectionName = view != null ? ("_" + view.getIdentifier()) : "";
        return tableId.name() + "_" + getStorageKeyTabSuffix() + collectionName;
    }

    private SearchItem createRelationItem(Search search) {
        return new SpecialSearchItem(search) {
            @Override
            public ObjectFilter createFilter(PageBase pageBase, ExpressionVariables variables) {
                R object = getParentVariables(variables);
                if (object == null) {
                    return null;
                }
                PrismContext prismContext = pageBase.getPrismContext();
                List relations;
                QName relation = getMemberPanelStorage().getRelation();
                if (QNameUtil.match(relation, PrismConstants.Q_ANY)){
                    relations = getSupportedRelations().getAvailableRelationList();
                } else {
                    relations = Collections.singletonList(relation);
                }

                ObjectFilter filter;
                Boolean indirect = getMemberPanelStorage().getIndirect();
                Class type = search.getTypeClass();
                if(!Boolean.TRUE.equals(indirect)) {
                    S_AtomicFilterExit q = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                            .block()
                            .item(AssignmentType.F_TARGET_REF)
                            .ref(MemberOperationsHelper.createReferenceValuesList(object, relations));

                    if (!getMemberPanelStorage().isTenantEmpty()) {
                        q = q.and().item(AssignmentType.F_TENANT_REF).ref(getMemberPanelStorage().getTenant().getOid());
                    }

                    if (!getMemberPanelStorage().isProjectEmpty()) {
                        q = q.and().item(AssignmentType.F_ORG_REF).ref(getMemberPanelStorage().getProject().getOid());
                    }
                    filter = q.endBlock().buildFilter();
                } else {
                    filter = prismContext.queryFor(type)
                            .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
                            .buildFilter();
                }
                return filter;
            }

            @Override
            public boolean isApplyFilter() {
                return !CompiledGuiProfile.isVisible(defaultScopeConfiguration.getVisibility(), null)
                        || !SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope());
            }

            @Override
            public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
                return new SearchSpecialItemPanel(id, new PropertyModel(getMemberPanelStorage(), MemberPanelStorage.F_RELATION)) {
                    @Override
                    protected WebMarkupContainer initSearchItemField(String id) {

                        List<QName> choices = new ArrayList();
                        List<QName> relations = getSupportedRelations().getAvailableRelationList();
                        if (relations != null && relations.size() > 1) {
                            choices.add(PrismConstants.Q_ANY);
                        }
                        choices.addAll(relations);

                        DropDownChoicePanel inputPanel = new DropDownChoicePanel(id, getModelValue(), Model.of(choices), new QNameIChoiceRenderer() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public Object getDisplayValue(QName relation) {
                                RelationDefinitionType relationDef = WebComponentUtil.getRelationDefinition(relation);
                                if (relationDef != null) {
                                    DisplayType display = relationDef.getDisplay();
                                    if (display != null) {
                                        PolyStringType label = display.getLabel();
                                        if (PolyStringUtils.isNotEmpty(label)) {
                                            return WebComponentUtil.getTranslatedPolyString(label);
                                        }
                                    }
                                }
                                if (QNameUtil.match(PrismConstants.Q_ANY, relation)) {
                                    return new ResourceModel("RelationTypes.ANY", relation.getLocalPart()).getObject();
                                }
                                return super.getDisplayValue(relation);
                            }
                        }, false);
                        inputPanel.getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                        inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 100px; max-width: 400px !important;"));
                        inputPanel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                searchPerformedConsumer.accept(target);
                            }
                        });

                        inputPanel.getBaseFormComponent().add(new EnableBehaviour(() -> getSupportedRelations().getAvailableRelationList().size() > 1));
                        inputPanel.setOutputMarkupId(true);
                        return inputPanel;
                    }

                    @Override
                    protected IModel<String> createLabelModel() {
                        return Model.of(WebComponentUtil.getTranslatedPolyString(defaultRelationConfiguration.getDisplay().getLabel()));
                    }

                    @Override
                    protected IModel<String> createHelpModel(){
                        return Model.of(WebComponentUtil.getTranslatedPolyString(defaultRelationConfiguration.getDisplay().getHelp()));
                    }
                };
            }
        };
    }

    private R getParentVariables(ExpressionVariables variables) {
        try {
            return (R) variables.getValue(ExpressionConstants.VAR_PARENT_OBJECT, AbstractRoleType.class);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load parent object.");
        }
        return null;
    }

    private SearchItem createScopeItem(Search search) {
        return new SpecialSearchItem(search) {
            @Override
            public ObjectFilter createFilter(PageBase pageBase, ExpressionVariables variables) {
                R object = getParentVariables(variables);
                if (object == null) {
                    return null;
                }
                Class type = search.getTypeClass();
                ObjectReferenceType ref = MemberOperationsHelper.createReference(object, getSupportedRelations().getDefaultRelation());
                return pageBase.getPrismContext().queryFor(type).isChildOf(ref.asReferenceValue()).buildFilter();
            }

            @Override
            public boolean isApplyFilter() {
                return SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope());
            }

            @Override
            public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
                return new SearchSpecialItemPanel(id, new PropertyModel(getMemberPanelStorage(), MemberPanelStorage.F_ORG_SEARCH_SCOPE)) {
                    @Override
                    protected WebMarkupContainer initSearchItemField(String id) {
                        DropDownChoicePanel inputPanel = new DropDownChoicePanel(id, getModelValue(), Model.of(Arrays.asList(SearchBoxScopeType.values())), new EnumChoiceRenderer(), false);
                        inputPanel.getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                        inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 88px; max-width: 400px !important;"));
                        inputPanel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                searchPerformedConsumer.accept(target);
                            }
                        });
                        inputPanel.setOutputMarkupId(true);
                        return inputPanel;
                    }

                    @Override
                    protected IModel<String> createLabelModel() {
                        return Model.of(WebComponentUtil.getTranslatedPolyString(defaultScopeConfiguration.getDisplay().getLabel()));
                    }

                    @Override
                    protected IModel<String> createHelpModel(){
                        return Model.of(WebComponentUtil.getTranslatedPolyString(defaultScopeConfiguration.getDisplay().getHelp()));
                    }
                };
            }
        };
    }

    private SearchItem createIndirectItem(Search search) {
        return new SpecialSearchItem(search) {
            @Override
            public ObjectFilter createFilter(PageBase pageBase, ExpressionVariables variables) {
                R object = getParentVariables(variables);
                if (object == null) {
                    return null;
                }
                List relations = new ArrayList();
                if (QNameUtil.match(PrismConstants.Q_ANY, getSupportedRelations().getDefaultRelation())) {
                    relations.addAll(getSupportedRelations().getAvailableRelationList());
                } else {
                    relations.add(getSupportedRelations().getDefaultRelation());
                }

                ObjectFilter filter;
                PrismContext prismContext = pageBase.getPrismContext();
                Class type = search.getTypeClass();
                if(!Boolean.TRUE.equals(getMemberPanelStorage().getIndirect())) {
                    filter = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                            .block()
                            .item(AssignmentType.F_TARGET_REF)
                            .ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
                            .endBlock().buildFilter();
                } else {
                    filter = prismContext.queryFor(type)
                            .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
                            .buildFilter();
                }
                return filter;
            }

            @Override
            public boolean isApplyFilter() {
                return !CompiledGuiProfile.isVisible(defaultScopeConfiguration.getVisibility(), null)
                        || !SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope());
            }

            @Override
            public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
                SearchSpecialItemPanel panel = new SearchSpecialItemPanel(id, new PropertyModel(getMemberPanelStorage(), MemberPanelStorage.F_INDIRECT)) {
                    @Override
                    protected WebMarkupContainer initSearchItemField(String id) {
                        List<Boolean> choices = new ArrayList<>();
                        choices.add(Boolean.TRUE);
                        choices.add(Boolean.FALSE);
                        DropDownChoicePanel inputPanel = new DropDownChoicePanel(id, getModelValue(), Model.ofList(choices), new ChoiceRenderer<Boolean>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public Object getDisplayValue(Boolean val) {
                                if (val) {
                                    return getPageBase().createStringResource("Boolean.TRUE").getString();
                                }
                                return getPageBase().createStringResource("Boolean.FALSE").getString();
                            }
                        }, false);
                        inputPanel.getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                        inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 68"
                                + "px; max-width: 400px !important;"));
                        inputPanel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                searchPerformedConsumer.accept(target);
                            }
                        });
                        inputPanel.getBaseFormComponent().add(new EnableBehaviour(() -> !SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())));
                        inputPanel.setOutputMarkupId(true);
                        return inputPanel;
                    }

                    @Override
                    protected IModel<String> createLabelModel() {
                        return Model.of(WebComponentUtil.getTranslatedPolyString(defaultIndirectConfiguration.getDisplay().getLabel()));
                    }

                    @Override
                    protected IModel<String> createHelpModel(){
                        return Model.of(WebComponentUtil.getTranslatedPolyString(defaultIndirectConfiguration.getDisplay().getHelp()));
                    }
                };
                panel.add(new VisibleBehaviour(() -> getMemberPanelStorage() == null
                        || (getSupportedRelations().getAvailableRelationList() != null
                        && !SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope()))));
                return panel;
            }
        };
    }

    private SearchItem createTenantItem(Search search) {
        return new SpecialSearchItem(search) {

            @Override
            public boolean isApplyFilter() {
                return !CompiledGuiProfile.isVisible(defaultScopeConfiguration.getVisibility(), null)
                        || (!SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())
                        && !CompiledGuiProfile.isVisible(defaultRelationConfiguration.getVisibility(), null)
                        && !Boolean.TRUE.equals(getMemberPanelStorage().getIndirect()));
            }

            @Override
            public ObjectFilter createFilter(PageBase pageBase, ExpressionVariables variables) {
                R object = getParentVariables(variables);
                if (object == null) {
                    return null;
                }
                PrismContext prismContext = pageBase.getPrismContext();
                List relations = new ArrayList();
                if (QNameUtil.match(PrismConstants.Q_ANY, getSupportedRelations().getDefaultRelation())) {
                    relations.addAll(getSupportedRelations().getAvailableRelationList());
                } else {
                    relations.add(getSupportedRelations().getDefaultRelation());
                }

                ObjectFilter filter;
                Class type = search.getTypeClass();
                S_AtomicFilterExit q = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                        .block()
                        .item(AssignmentType.F_TARGET_REF)
                        .ref(MemberOperationsHelper.createReferenceValuesList(object, relations));

                if (!getMemberPanelStorage().isTenantEmpty()) {
                    q = q.and().item(AssignmentType.F_TENANT_REF).ref(getMemberPanelStorage().getTenant().getOid());
                }

                if (!getMemberPanelStorage().isProjectEmpty()) {
                    q = q.and().item(AssignmentType.F_ORG_REF).ref(getMemberPanelStorage().getProject().getOid());
                }
                filter = q.endBlock().buildFilter();
                return filter;
            }

            @Override
            public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
                IModel tenantModel = new PropertyModel(getMemberPanelStorage(), MemberPanelStorage.F_TENANT) {
                    @Override
                    public void setObject(Object object) {
                        if (object == null) {
                            getMemberPanelStorage().resetTenantRef();
                        } else {
                            super.setObject(object);
                        }
                    }
                };
                PrismReferenceDefinition tenantRefDef = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class)
                        .findReferenceDefinition(AssignmentType.F_TENANT_REF);
                SearchSpecialItemPanel panel = new SearchSpecialItemPanel(id, tenantModel) {
                    @Override
                    protected WebMarkupContainer initSearchItemField(String id) {
                        ReferenceValueSearchPanel searchItemField = new ReferenceValueSearchPanel(id, getModelValue(), tenantRefDef) {
                            @Override
                            protected void referenceValueUpdated(ObjectReferenceType ort, AjaxRequestTarget target) {
                                searchPerformedConsumer.accept(target);
                            }

                            @Override
                            public Boolean isItemPanelEnabled() {
                                return !Boolean.TRUE.equals(getMemberPanelStorage().getIndirect());
                            }

                            @Override
                            protected List<QName> getAllowedRelations() {
                                return Collections.singletonList(RelationTypes.MEMBER.getRelation());
                            }
                        };
                        return searchItemField;
                    }

                    @Override
                    protected IModel<String> createLabelModel() {
                        return Model.of(WebComponentUtil.getTranslatedPolyString(defaultTenantConfiguration.getDisplay().getLabel()));
                    }

                    @Override
                    protected IModel<String> createHelpModel() {
                        if (defaultProjectConfiguration.getDisplay().getHelp() != null){
                            return Model.of(WebComponentUtil.getTranslatedPolyString(defaultTenantConfiguration.getDisplay().getHelp()));
                        }
                        String help = tenantRefDef.getHelp();
                        if (StringUtils.isNotEmpty(help)) {
                            return getPageBase().createStringResource(help);
                        }
                        return Model.of(tenantRefDef.getDocumentation());
                    }
                };
                panel.add(new VisibleBehaviour(() -> getMemberPanelStorage() == null
                        || !Boolean.TRUE.equals(getMemberPanelStorage().getIndirect())));
                return panel;
            }
        };
    }

    private SearchItem createProjectItem(Search search) {
        return new SpecialSearchItem(search) {

            @Override
            public boolean isApplyFilter() {
                return !CompiledGuiProfile.isVisible(defaultScopeConfiguration.getVisibility(), null)
                        || (!SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())
                        && !CompiledGuiProfile.isVisible(defaultRelationConfiguration.getVisibility(), null)
                        && !CompiledGuiProfile.isVisible(defaultTenantConfiguration.getVisibility(), null)
                        && !Boolean.TRUE.equals(getMemberPanelStorage().getIndirect()));
            }

            @Override
            public ObjectFilter createFilter(PageBase pageBase, ExpressionVariables variables) {
                R object = getParentVariables(variables);
                if (object == null) {
                    return null;
                }
                PrismContext prismContext = pageBase.getPrismContext();
                List relations = new ArrayList();
                if (QNameUtil.match(PrismConstants.Q_ANY, getSupportedRelations().getDefaultRelation())) {
                    relations.addAll(getSupportedRelations().getAvailableRelationList());
                } else {
                    relations.add(getSupportedRelations().getDefaultRelation());
                }

                ObjectFilter filter;
                Class type = search.getTypeClass();
                S_AtomicFilterExit q = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
                        .block()
                        .item(AssignmentType.F_TARGET_REF)
                        .ref(MemberOperationsHelper.createReferenceValuesList(object, relations));

                if (!getMemberPanelStorage().isProjectEmpty()) {
                    q = q.and().item(AssignmentType.F_ORG_REF).ref(getMemberPanelStorage().getProject().getOid());
                }
                filter = q.endBlock().buildFilter();
                return filter;
            }

            @Override
            public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
                IModel projectModel = new PropertyModel(getMemberPanelStorage(), MemberPanelStorage.F_PROJECT) {
                    @Override
                    public void setObject(Object object) {
                        if (object == null) {
                            getMemberPanelStorage().resetProjectRef();
                        } else {
                            super.setObject(object);
                        }
                    }
                };
                PrismReferenceDefinition projectRefDef = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class)
                        .findReferenceDefinition(AssignmentType.F_ORG_REF);
                SearchSpecialItemPanel panel = new SearchSpecialItemPanel(id, projectModel) {
                    @Override
                    protected WebMarkupContainer initSearchItemField(String id) {
                        ReferenceValueSearchPanel searchItemField = new ReferenceValueSearchPanel(id, getModelValue(), projectRefDef) {
                            @Override
                            protected void referenceValueUpdated(ObjectReferenceType ort, AjaxRequestTarget target) {
                                searchPerformedConsumer.accept(target);
                            }

                            @Override
                            public Boolean isItemPanelEnabled() {
                                return !Boolean.TRUE.equals(getMemberPanelStorage().getIndirect());
                            }

                            @Override
                            protected List<QName> getAllowedRelations() {
                                return Collections.singletonList(RelationTypes.MEMBER.getRelation());
                            }
                        };
                        return searchItemField;
                    }

                    @Override
                    protected IModel<String> createLabelModel() {
                        return Model.of(WebComponentUtil.getTranslatedPolyString(defaultProjectConfiguration.getDisplay().getLabel()));
                    }

                    @Override
                    protected IModel<String> createHelpModel() {
                        if (defaultProjectConfiguration.getDisplay().getHelp() != null){
                            return Model.of(WebComponentUtil.getTranslatedPolyString(defaultProjectConfiguration.getDisplay().getHelp()));
                        }
                        String help = projectRefDef.getHelp();
                        if (StringUtils.isNotEmpty(help)) {
                            return getPageBase().createStringResource(help);
                        }
                        return Model.of(projectRefDef.getDocumentation());
                    }
                };
                panel.add(new VisibleBehaviour(() -> getMemberPanelStorage() == null
                        || !Boolean.TRUE.equals(getMemberPanelStorage().getIndirect())));
                return panel;
            }
        };
    }

    protected Class<? extends ObjectType> getChoiceForAllTypes () {
        return FocusType.class;
    }

    private List<DisplayableValue<Class<? extends ObjectType>>> getAllowedTypes() {
        List<DisplayableValue<Class<? extends ObjectType>>> ret = new ArrayList<>();
        List<QName> types = getSupportedObjectTypes(false);

        ret.add(new SearchValue<>(getChoiceForAllTypes(), "ObjectTypes.all"));

        String prefix = "ObjectType.";
        for (QName type : types) {
            @NotNull ObjectTypes objectType = ObjectTypes.getObjectType(type.getLocalPart());
            ret.add(new SearchValue<>(objectType.getClassDefinition(), prefix + objectType.getTypeQName().getLocalPart()));
        }
        return ret;
    }

    private List<MultiFunctinalButtonDto> createAdditionalButtonsDescription() {
        List<MultiFunctinalButtonDto> multiFunctinalButtonDtos = new ArrayList<>();
        List<AssignmentObjectRelation> loadedRelations = loadMemberRelationsList();
        if (CollectionUtils.isNotEmpty(loadedRelations)) {
            List<AssignmentObjectRelation> relations = WebComponentUtil.divideAssignmentRelationsByAllValues(loadedRelations);
            relations.forEach(relation -> {
                MultiFunctinalButtonDto buttonDto = new MultiFunctinalButtonDto();
                DisplayType additionalButtonDisplayType = WebComponentUtil.getAssignmentObjectRelationDisplayType(getPageBase(), relation,
                        "abstractRoleMemberPanel.menu.createMember");
                buttonDto.setAdditionalButtonDisplayType(additionalButtonDisplayType);
                buttonDto.setCompositedIcon(createCompositedIcon(relation, additionalButtonDisplayType));
                buttonDto.setAssignmentObjectRelation(relation);
                multiFunctinalButtonDtos.add(buttonDto);
            });
        }
        return multiFunctinalButtonDtos;
    }

    private CompositedIcon createCompositedIcon(AssignmentObjectRelation relation, DisplayType additionalButtonDisplayType) {
        CompositedIconBuilder builder = WebComponentUtil.getAssignmentRelationIconBuilder(getPageBase(), relation,
                additionalButtonDisplayType.getIcon(), WebComponentUtil.createIconType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green"));
        if (builder == null) {
            return null;
        }
        return builder.build();
    }

    private MultifunctionalButton createAssignButton(String buttonId) {
        MultifunctionalButton assignButton = new MultifunctionalButton(buttonId, createAssignmentAdditionalButtons()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
                List<QName> relations = relation != null && !CollectionUtils.isEmpty(relation.getRelations())
                        ? Collections.singletonList(relation.getRelations().get(0))
                        : getSupportedRelations().getAvailableRelationList();
                AvailableRelationDto availableRelations = new AvailableRelationDto(relations, getSupportedRelations().getDefaultRelation(), defaultRelationConfiguration);
                List<QName> objectTypes = relation != null && !CollectionUtils.isEmpty(relation.getObjectTypes()) ?
                        relation.getObjectTypes() : null;
                List<ObjectReferenceType> archetypeRefList = relation != null && !CollectionUtils.isEmpty(relation.getArchetypeRefs()) ?
                        relation.getArchetypeRefs() : null;
                assignMembers(target, availableRelations, objectTypes, archetypeRefList, relation == null);
            }

            @Override
            protected DisplayType getMainButtonDisplayType() {
                return getAssignMemberButtonDisplayType();
            }

            @Override
            protected DisplayType getDefaultObjectButtonDisplayType() {
                return getAssignMemberButtonDisplayType();
            }

        };
        assignButton.add(AttributeAppender.append("class", "btn-margin-right"));

        return assignButton;
    }

    private List<MultiFunctinalButtonDto> createAssignmentAdditionalButtons() {
        List<MultiFunctinalButtonDto> additionalAssignmentButtons = new ArrayList<>();
        List<AssignmentObjectRelation> assignmentObjectRelations = WebComponentUtil.divideAssignmentRelationsByAllValues(loadMemberRelationsList());
        if (assignmentObjectRelations == null) {
            return additionalAssignmentButtons;
        }
        assignmentObjectRelations.forEach(relation -> {
            MultiFunctinalButtonDto buttonDto = new MultiFunctinalButtonDto();
            buttonDto.setAssignmentObjectRelation(relation);

            DisplayType additionalDispayType = WebComponentUtil.getAssignmentObjectRelationDisplayType(AbstractRoleMemberPanel.this.getPageBase(), relation,
                    "abstractRoleMemberPanel.menu.assignMember");
            //TODO null additinalDisplayType
            CompositedIconBuilder builder = WebComponentUtil.getAssignmentRelationIconBuilder(AbstractRoleMemberPanel.this.getPageBase(), relation,
                    additionalDispayType.getIcon(), WebComponentUtil.createIconType(GuiStyleConstants.EVO_ASSIGNMENT_ICON, "green"));
            CompositedIcon icon = builder.build();
            buttonDto.setAdditionalButtonDisplayType(additionalDispayType);
            buttonDto.setCompositedIcon(icon);
            additionalAssignmentButtons.add(buttonDto);
        });

        return additionalAssignmentButtons;

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
        return WebComponentUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green",
                AbstractRoleMemberPanel.this.createStringResource("abstractRoleMemberPanel.menu.createMember", "", "").getString());
    }

    private DisplayType getAssignMemberButtonDisplayType() {
        return WebComponentUtil.createDisplayType(GuiStyleConstants.EVO_ASSIGNMENT_ICON, "green",
                AbstractRoleMemberPanel.this.createStringResource("abstractRoleMemberPanel.menu.assignMember", "", "").getString());
    }

    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        createAssignMemberRowAction(menu);

        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_UNASSIGN)) {
            menu.add(new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.unassign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unassignMembersPerformed(target);
                        }
                    };

                }

                @Override
                public CompositedIconBuilder getIconCompositedBuilder() {
                    return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_UNASSIGN);
                }
            });
        }

        createRecomputeMemberRowAction(menu);

        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_CREATE)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.create")) {
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
            });
        }
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_DELETE)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.delete")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            deleteMembersPerformed(target);
                        }
                    };
                }

            });
        }
        return menu;
    }

    protected void createAssignMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_ASSIGN)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.assign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            MemberOperationsHelper.assignMembers(getPageBase(), AbstractRoleMemberPanel.this.getModelObject(), target, getSupportedRelations(), null);
                        }
                    };
                }
            });
        }
    }

    protected void createRecomputeMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_RECOMPUTE)) {
            menu.add(new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.recompute")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            recomputeMembersPerformed(target);
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

    protected abstract AvailableRelationDto getSupportedRelations();

    protected GuiObjectListPanelConfigurationType getAdditionalPanelConfig() {
        CompiledObjectCollectionView view = WebComponentUtil.getCollectionViewByObject(getModelObject(), getParentPage());
        if (view != null && view.getAdditionalPanels() != null) {
            return view.getAdditionalPanels().getMemberPanel();
        }
        return null;
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
        PrismObject obj = getModelObject().asPrismObject();
        AssignmentCandidatesSpecification spec = null;
        try {
            spec = getPageBase().getModelInteractionService()
                    .determineAssignmentHolderSpecification(obj, result);
        } catch (SchemaException | ConfigurationException ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load member relations list for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return spec;
    }

    protected void assignMembers(AjaxRequestTarget target, AvailableRelationDto availableRelationList,
            List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList, boolean isOrgTreePanelVisible) {
        MemberOperationsHelper.assignMembers(getPageBase(), getModelObject(), target, availableRelationList,
                objectTypes, archetypeRefList, isOrgTreePanelVisible);
    }

    private void unassignMembersPerformed(AjaxRequestTarget target) {
        QueryScope scope = getQueryScope();

        ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(getPageBase().getMainPopupBodyId(),
                createStringResource("abstractRoleMemberPanel.unassignAllMembersConfirmationLabel")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getSupportedObjectTypes() {
                return AbstractRoleMemberPanel.this.getSupportedObjectTypes(true);
            }

            @Override
            protected List<QName> getSupportedRelations() {
                return AbstractRoleMemberPanel.this.getSupportedRelations().getAvailableRelationList();
            }

            @Override
            protected boolean isFocusTypeSelectorVisible() {
                return !QueryScope.SELECTED.equals(scope);
            }

            protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                unassignMembersPerformed(type, SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())
                        && QueryScope.ALL.equals(scope) ? QueryScope.ALL_DIRECT : scope, relations, target);
            }

            @Override
            protected QName getDefaultObjectType() {
                return WebComponentUtil.classToQName(AbstractRoleMemberPanel.this.getPrismContext(),
                        AbstractRoleMemberPanel.this.getDefaultObjectType());
            }
        };

        getPageBase().showMainPopup(chooseTypePopupContent, target);
    }

    private void deleteMembersPerformed(AjaxRequestTarget target) {
        QueryScope scope = getQueryScope();
        StringResourceModel confirmModel;
        if (SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())) {
            confirmModel = createStringResource("abstractRoleMemberPanel.deleteAllSubtreeMembersConfirmationLabel");
        } else {
            confirmModel = getMemberTable().getSelectedObjectsCount() > 0 ?
                    createStringResource("abstractRoleMemberPanel.deleteSelectedMembersConfirmationLabel")
                    : createStringResource("abstractRoleMemberPanel.deleteAllMembersConfirmationLabel");
        }
        ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(getPageBase().getMainPopupBodyId(),
                confirmModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getSupportedObjectTypes() {
                return AbstractRoleMemberPanel.this.getSupportedObjectTypes(true);
            }

            @Override
            protected List<QName> getSupportedRelations() {
                return AbstractRoleMemberPanel.this.getSupportedRelations().getAvailableRelationList();
            }

            protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                deleteMembersPerformed(scope, relations, target);
            }

            @Override
            protected boolean isFocusTypeSelectorVisible() {
                return !QueryScope.SELECTED.equals(scope);
            }

            @Override
            protected QName getDefaultObjectType() {
                return WebComponentUtil.classToQName(AbstractRoleMemberPanel.this.getPrismContext(),
                        AbstractRoleMemberPanel.this.getDefaultObjectType());
            }
        };

        getPageBase().showMainPopup(chooseTypePopupContent, target);
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
                        getSupportedObjectTypes(false).get(0);
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
                    return AbstractRoleMemberPanel.this.getSupportedRelations().getAvailableRelationList();
                }

                protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                    if (type == null) {
                        getSession().warn("No type was selected. Cannot create member");
                        target.add(this);
                        target.add(getPageBase().getFeedbackPanel());
                        return;
                    }
                    if (relations == null || relations.isEmpty()) {
                        getSession().warn("No relation was selected. Cannot create member");
                        target.add(this);
                        target.add(getPageBase().getFeedbackPanel());
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
                    return relationSpec == null;
                }
            };

            getPageBase().showMainPopup(chooseTypePopupContent, target);
        }
    }

    protected void deleteMembersPerformed(QueryScope scope, Collection<QName> relations, AjaxRequestTarget target) {
        if (relations == null || relations.isEmpty()) {
            getSession().warn("No relation was selected. Cannot perform delete members");
            target.add(this);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        MemberOperationsHelper.deleteMembersPerformed(getPageBase(), scope, getActionQuery(scope, relations), target);
    }

    protected void unassignMembersPerformed(QName type, QueryScope scope, Collection<QName> relations, AjaxRequestTarget target) {
        if (relations == null || relations.isEmpty()) {
            getSession().warn("No relation was selected. Cannot perform unassign members");
            target.add(this);
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        MemberOperationsHelper.unassignMembersPerformed(getPageBase(), getModelObject(), scope, getActionQuery(scope, relations), relations, type, target);
    }

    protected ObjectQuery getActionQuery(QueryScope scope, Collection<QName> relations) {
        switch (scope) {
            case ALL:
                return createAllMemberQuery(relations);
            case ALL_DIRECT:
                return MemberOperationsHelper.createDirectMemberQuery(getModelObject(), getSearchType(), relations,
                        getMemberPanelStorage().getTenant(), getMemberPanelStorage().getProject(), getPrismContext());
            case SELECTED:
                return MemberOperationsHelper.createSelectedObjectsQuery(getMemberTable().getSelectedRealObjects(), getPrismContext());
        }

        return null;
    }

    protected List<QName> getSupportedObjectTypes(boolean includeAbstractTypes) {
        if (!CollectionUtils.isEmpty(defaultObjectTypeConfiguration.getSupportedTypes())) {
            return defaultObjectTypeConfiguration.getSupportedTypes();
        }
        return getDefaultSupportedObjectTypes(includeAbstractTypes);
    }


    protected List<QName> getDefaultSupportedObjectTypes(boolean includeAbstractTypes) {
        return WebComponentUtil.createFocusTypeList(includeAbstractTypes);
    }

    protected List<QName> getNewMemberObjectTypes() {
        return WebComponentUtil.createFocusTypeList();
    }

    protected MainObjectListPanel<FocusType> getMemberTable() {
        return (MainObjectListPanel<FocusType>) get(createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
    }

    protected QueryScope getQueryScope() {
        if (CollectionUtils.isNotEmpty(MemberOperationsHelper.getFocusOidToRecompute(getMemberTable().getSelectedRealObjects()))) {
            return QueryScope.SELECTED;
        }

        if (getMemberPanelStorage().getIndirect()
                || SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())) {
            return QueryScope.ALL;
        }

        return QueryScope.ALL_DIRECT;
    }

    protected void recomputeMembersPerformed(AjaxRequestTarget target) {

        StringResourceModel confirmModel;
        if (SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope())) {
            confirmModel = createStringResource("abstractRoleMemberPanel.recomputeAllSubtreeMembersConfirmationLabel");
        } else {
            confirmModel = getMemberTable().getSelectedObjectsCount() > 0 ?
                    createStringResource("abstractRoleMemberPanel.recomputeSelectedMembersConfirmationLabel")
                    : createStringResource("abstractRoleMemberPanel.recomputeAllMembersConfirmationLabel");
        }
        ConfigureTaskConfirmationPanel dialog = new ConfigureTaskConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
                confirmModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected PrismObject<TaskType> getTask(AjaxRequestTarget target) {
                Task task = MemberOperationsHelper.createRecomputeMembersTask(getPageBase(), getQueryScope(),
                        getActionQuery(getQueryScope(), getSupportedRelations().getAvailableRelationList()), target);
                if (task == null) {
                    return null;
                }
                PrismObject<TaskType> recomputeTask = task.getClonedTaskObject();
                TaskType recomputeTaskType = recomputeTask.asObjectable();
                recomputeTaskType.getAssignment().add(ObjectTypeUtil.createAssignmentTo(SystemObjectsType.ARCHETYPE_RECOMPUTATION_TASK.value(), ObjectTypes.ARCHETYPE, getPrismContext()));
                return recomputeTask;
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("pageUsers.message.confirmActionPopupTitle");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                MemberOperationsHelper.recomputeMembersPerformed(getPageBase(), getQueryScope(),
                        getActionQuery(getQueryScope(), getSupportedRelations().getAvailableRelationList()), target);
            }
        };
        ((PageBase) getPage()).showMainPopup(dialog, target);
    }

    protected QName getSearchType(){
        return ObjectTypes.getObjectType(getMemberPanelStorage().getSearch().getTypeClass()).getTypeQName();
    }

    protected ObjectQuery createAllMemberQuery(Collection<QName> relations) {
        return getPrismContext().queryFor(FocusType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(getModelObject(), relations))
                .build();
    }

    protected ObjectReferenceType createReference() {
        ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getModelObject(), getPageBase().getPrismContext());
        return ref;
    }

    protected void detailsPerformed(AjaxRequestTarget target, ObjectType object) {
        if (WebComponentUtil.hasDetailsPage(object.getClass())) {
            WebComponentUtil.dispatchToObjectDetailsPage(object.getClass(), object.getOid(), this, true);
        } else {
            error("Could not find proper response page");
            throw new RestartResponseException(getPageBase());
        }
    }

    protected List<IColumn<SelectableBean<ObjectType>, String>> createMembersColumns() {
        List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

        IColumn<SelectableBean<ObjectType>, String> column = new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
                createStringResource("TreeTablePanel.fullName.displayName")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
                    String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
                SelectableBean<ObjectType> bean = rowModel.getObject();
                ObjectType object = bean.getValue();
                cellItem.add(new Label(componentId, getMemberObjectDisplayName(object, true)));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
                return Model.of(getMemberObjectDisplayName(rowModel.getObject().getValue(), true));
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
                createStringResource("TreeTablePanel.identifier.description")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
                    String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
                SelectableBean<ObjectType> bean = rowModel.getObject();
                ObjectType object = bean.getValue();
                cellItem.add(new Label(componentId, getMemberObjectIdentifier(object)));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
                return Model.of(getMemberObjectIdentifier(rowModel.getObject().getValue()));
            }

        };
        columns.add(column);
        columns.add(createRelationColumn());
        return columns;
    }

    protected IColumn<SelectableBean<ObjectType>, String> createRelationColumn() {
        return new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
                createStringResource("roleMemberPanel.relation")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
                    String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
                cellItem.add(new Label(componentId,
                        getRelationValue(rowModel.getObject().getValue())));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
                return Model.of(getRelationValue(rowModel.getObject().getValue()));
            }

        };
    }

    protected boolean isRelationColumnVisible() {
        return false;
    }

    private String getMemberObjectDisplayName(ObjectType object, boolean translate) {
        if (object == null) {
            return "";
        }
        if (object instanceof UserType) {
            return WebComponentUtil.getTranslatedPolyString(((UserType) object).getFullName());
        } else if (object instanceof AbstractRoleType) {
            return WebComponentUtil.getTranslatedPolyString(((AbstractRoleType) object).getDisplayName());
        } else {
            return "";
        }
    }

    private String getMemberObjectIdentifier(ObjectType object) {
        if (object == null) {
            return "";
        }
        if (object instanceof UserType) {
            return ((UserType) object).getEmailAddress();
        } else if (object instanceof AbstractRoleType) {
            return ((AbstractRoleType) object).getIdentifier();
        } else {
            return object.getDescription();
        }
    }

    private Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return SelectorOptions
                .createCollection(GetOperationOptions.createDistinct());
    }

    protected <O extends ObjectType> Class<O> getDefaultObjectType() {
        return (Class<O>) FocusType.class;
    }

    protected Form getFormComponent() {
        return (Form) get(ID_FORM);
    }

    private String getRelationValue(ObjectType focusObject) {
        String relation = "";
        if (FocusType.class.isAssignableFrom(focusObject.getClass())) {
            // Do NOT take relation from an assignment. Use roleMembershipRef instead. Reasons:
            // 1. Authorizations (MID-4893). User may be authorized just for roleMemberhsipRef and not for assignment
            //    Authorization for roleMembershipRef is enough to display member panel.
            // 2. There may be assignments that are not valid. We do not want to display relation for those.
            for (ObjectReferenceType roleMembershipRef : getMembershipReferenceList((FocusType) focusObject)) {
                relation = buildRelation(roleMembershipRef, relation);
            }

        }
        return relation;

    }

    protected List<ObjectReferenceType> getMembershipReferenceList(FocusType focusObject) {
        return focusObject.getRoleMembershipRef();
    }

    private String buildRelation(ObjectReferenceType roleMembershipRef, String relation) {
        if (roleMembershipRef.getOid().equals(getModelObject().getOid())) {
            QName assignmentRelation = roleMembershipRef.getRelation();
            if (getSupportedRelations().getAvailableRelationList().stream().anyMatch(r -> QNameUtil.match(r, assignmentRelation))) {
                if (!StringUtils.isBlank(relation)) {
                    relation += ",";
                }
                String relationDisplayName = WebComponentUtil.getRelationHeaderLabelKeyIfKnown(assignmentRelation);
                relation += StringUtils.isNotEmpty(relationDisplayName) ?
                        getPageBase().createStringResource(relationDisplayName).getString() :
                        getPageBase().createStringResource(assignmentRelation.getLocalPart()).getString();
            }
        }
        return relation;
    }

    protected MemberPanelStorage getMemberPanelStorage() {
        String storageKey = createStorageKey();
        if (StringUtils.isNotEmpty(storageKey)) {
            PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
            if (storage == null) {
                storage = getSession().getSessionStorage().initPageStorage(storageKey);
            }
            return (MemberPanelStorage) storage;
        }
        return null;
    }

    protected PageBase getParentPage() {
        return pageBase;
    }

    protected String getStorageKeyTabSuffix(){
        return "";
    }

    protected RelationSearchItemConfigurationType getDefaultRelationConfiguration() {
        return defaultRelationConfiguration;
    }
}
