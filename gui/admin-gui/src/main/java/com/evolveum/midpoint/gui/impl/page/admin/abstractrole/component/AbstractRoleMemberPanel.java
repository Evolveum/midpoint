/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import java.io.Serial;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.NewObjectCreationPopup;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.web.component.dialog.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
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
import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractRoleSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
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

import org.jetbrains.annotations.Nullable;

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

    @Serial private static final long serialVersionUID = 1L;

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

    private static final int DEFAULT_REFRESH_INTERVAL = 10;

    private boolean isRefreshEnabled = true;

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

    protected <AH extends AssignmentHolderType> void initMemberTable(Form<?> form) {
        WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
        memberContainer.setOutputMarkupId(true);
        memberContainer.setOutputMarkupPlaceholderTag(true);
        form.add(memberContainer);

        //TODO QName defines a relation value which will be used for new member creation
        MainObjectListPanel<AH> childrenListPanel = new MainObjectListPanel<>(
                ID_MEMBER_TABLE, getDefaultObjectTypeClass(), getPanelConfiguration()) {

            @Serial private static final long serialVersionUID = 1L;

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
                return DetailsPageUtil.hasDetailsPage(objectClass);
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
                SelectableBeanObjectDataProvider<AH> provider =
                        createSelectableBeanObjectDataProvider(
                                () -> getCustomizedQuery(getSearchModel().getObject()),
                                null,
                                getSearchOptions());
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
                return AbstractRoleMemberPanel.this.showNewObjectCreationPopup();
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation,
                    CompiledObjectCollectionView collectionView) {
                AbstractRoleMemberPanel.this.newObjectPerformed(relation, target);
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
                return loadMultiFunctionalButtonModel();
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
            protected boolean isCreateNewObjectVisible() {
                return isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_CREATE);
            }
        };
        childrenListPanel.setOutputMarkupId(true);
        memberContainer.add(childrenListPanel);
    }

    protected SearchContext getDefaultMemberSearchBoxConfig() {
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
            @Serial private static final long serialVersionUID = 1L;

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
                .filter(this::isApplicableRoleMembershipRef)
                .map(this::getTranslatedRelationValue)
                .collect(Collectors.joining(","));
    }

    private <AH extends AssignmentHolderType> List<QName> getMembershipAvailableRelations(AH value) {
        return value.getRoleMembershipRef().stream()
                .filter(this::isApplicableRoleMembershipRef)
                .map(ObjectReferenceType::getRelation)
                .collect(Collectors.toList());
    }

    private <AH extends AssignmentHolderType> List<QName> getAllMembershipRelations(AH value) {
        return value.getRoleMembershipRef().stream()
                .filter(ref -> ref.getOid().equals(getModelObject().getOid()))
                .map(ObjectReferenceType::getRelation)
                .collect(Collectors.toList());
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
        RelationDefinitionType relationDef = RelationUtil.getRelationDefinition(relationQName);
        if (relationDef != null) {
            PolyStringType label = GuiDisplayTypeUtil.getLabel(relationDef.getDisplay());
            if (PolyStringUtils.isNotEmpty(label)) {
                relation = WebComponentUtil.getTranslatedPolyString(label);
            }
        }
        return relation;
    }

    protected ObjectQuery getCustomizedQuery(Search search) {
        if (noMemberSearchItemVisible(search)) {
            PrismContext prismContext = getPageBase().getPrismContext();
            return prismContext.queryFor((Class<? extends Containerable>) search.getTypeClass())
                    .exists(AssignmentHolderType.F_ASSIGNMENT)
                    .block()
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(MemberOperationsQueryUtil.createReferenceValuesList(getModelObject(), getRelationsForSearch()))
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

    protected LoadableModel<MultiFunctinalButtonDto> loadMultiFunctionalButtonModel() {

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
                CompositedIconButtonDto mainButton = createCompositedIconButtonDto(mainButtonDisplayType, null, null);
                multiFunctinalButtonDto.setMainButton(mainButton);

                List<AssignmentObjectRelation> loadedRelations = loadMemberRelationsList();
                List<CompositedIconButtonDto> additionalButtons = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(loadedRelations)) {
                    List<AssignmentObjectRelation> relations =
                            WebComponentUtil.divideAssignmentRelationsByAllValues(loadedRelations);
                    relations.forEach(relation -> {
                        DisplayType additionalButtonDisplayType = GuiDisplayTypeUtil.getAssignmentObjectRelationDisplayType(getPageBase(), relation,
                                "abstractRoleMemberPanel.menu.createMember");
                        CompositedIconButtonDto buttonDto = createCompositedIconButtonDto(additionalButtonDisplayType, relation, createCompositedIcon(relation, additionalButtonDisplayType));
                        additionalButtons.add(buttonDto);
                    });
                }
                if (CollectionUtils.isNotEmpty(additionalButtons)) {
                    additionalButtons.add(createCompositedIconButtonDto(mainButtonDisplayType, null, null));
                }

                multiFunctinalButtonDto.setAdditionalButtons(additionalButtons);
                return multiFunctinalButtonDto;
            }
        };
    }

    private CompositedIcon createCompositedIcon(AssignmentObjectRelation relation, DisplayType additionalButtonDisplayType) {
        CompositedIconBuilder builder = WebComponentUtil.getAssignmentRelationIconBuilder(getPageBase(), relation,
                additionalButtonDisplayType.getIcon(), IconAndStylesUtil.createIconType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green"));
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
                if (supportedRelation.isEmpty()) {
                    return;
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
                createStringResource(getButtonTranslationPrefix() + ".addMembers")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                assignMemberPerformed(target);
            }
        };
        assignButton.add(new VisibleBehaviour(() -> isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_ASSIGN)));
        assignButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        return assignButton;
    }

    protected Popupable createAssignPopup(QName stableRelation) {
        ChooseMemberPopup browser = new ChooseMemberPopup(AbstractRoleMemberPanel.this.getPageBase().getMainPopupBodyId(),
                getMemberPanelStorage().getSearch(), loadMultiFunctionalButtonModel()) {
            @Serial private static final long serialVersionUID = 1L;

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
            protected QName getRelationIfIsStable() {
                return stableRelation;
            }

            @Override
            protected boolean shouldHideTaskLink() {
                return AbstractRoleMemberPanel.this.shouldHideTaskLink();
            }

            @Override
            public Component getFeedbackPanel() {
                return AbstractRoleMemberPanel.this.getFeedback();
            }
        };
        browser.setOutputMarkupId(true);
        return browser;
    }

    /**
     * Should the "show task" link be hidden for tasks submitted from this panel?
     * This feature is used in wizards to avoid complexity for users.
     *
     * TODO originally, the role wizard showed "AbstractRoleMemberPanel.message.info.created.task"
     *  ("Task "{0}" has been created in the background") when there was a background task started.
     *  I originally planned to do so for any tasks. But is that really better than simply showing
     *  the original operation name with a blue color indicating "in progress" state and a text note
     *  "(running in background)"?
     */
    protected boolean shouldHideTaskLink() {
        return false;
    }

    protected AjaxIconButton createUnassignButton(String buttonId) {
        AjaxIconButton assignButton = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_UNASSIGN),
                createStringResource(getButtonTranslationPrefix() + ".removeMembers")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                unassignMembersPerformed(null, target);
            }
        };
        assignButton.add(new VisibleBehaviour(() -> isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_UNASSIGN)));
        assignButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        return assignButton;
    }

    protected String getButtonTranslationPrefix() {
        return "TreeTablePanel.menu";
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
                AbstractRoleMemberPanel.this.createStringResource("abstractRoleMemberPanel.menu.createMember", "", "").getString(),
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
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            assignMemberPerformed(target);
                        }
                    };
                }
            };
            menuItem.setVisibilityChecker((rowModel, isHeader) -> isHeader);
            menu.add(menuItem);
        }
    }

    private void assignMemberPerformed(AjaxRequestTarget target) {
        AbstractRoleMemberPanel.this.getPageBase().showMainPopup(
                createAssignPopup(null),
                target);
    }

    protected <AH extends AssignmentHolderType> void createUnassignMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_UNASSIGN)) {
            InlineMenuItem menuItem = new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.unassign")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<SelectableBean<AH>>() {
                        @Serial private static final long serialVersionUID = 1L;

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
        AssignmentHolderType assignmentHolder = getAssignmentHolderFromRow(rowModel);

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

    private AssignmentHolderType getAssignmentHolderFromRow(@Nullable IModel<?> rowModel) {
        if (rowModel != null
                && (rowModel.getObject() instanceof SelectableBean<?> selectableBean)
                && (selectableBean.getValue() instanceof AssignmentHolderType assignmentHolder)) {
            return assignmentHolder;
        }
        return null;
    }

    protected void createRecomputeMemberRowAction(List<InlineMenuItem> menu) {
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_RECOMPUTE)) {
            menu.add(new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.recompute")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        @Serial private static final long serialVersionUID = 1L;

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
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            AbstractRoleMemberPanel.this.createMemberMenuActionPerformed(target);
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
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new ColumnMenuAction<>() {
                        @Serial private static final long serialVersionUID = 1L;

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
            String deleteActionTranslated =
                    createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject.delete")
                            .getString();
            confirmModel = rowModel != null
                    ? createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject",
                    deleteActionTranslated, ((ObjectType) ((SelectableBean<?>) rowModel.getObject()).getValue()).getName())
                    : createStringResource("abstractRoleMemberPanel.deleteSelectedMembersConfirmationLabel",
                    getSelectedObjectsCount());

            executeSimpleDeleteOperation(rowModel, confirmModel, target);
        } else {
            confirmModel = createStringResource("abstractRoleMemberPanel.deleteAllMembersConfirmationLabel");

            QueryScope scope = getMemberQueryScope();
            ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
                    ((PageBase) getPage()).getMainPopupBodyId(), confirmModel) {
                @Serial private static final long serialVersionUID = 1L;

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
                    deleteMembersPerformed(scope, type, relations, target);
                }

                @Override
                protected boolean isFocusTypeSelectorVisible() {
                    return !QueryScope.SELECTED.equals(scope);
                }

                @Override
                protected QName getDefaultObjectType() {
                    return getMemberSearchType();
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
            String recomputeActionTranslated =
                    createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject.recompute")
                            .getString();
            confirmModel = rowModel != null
                    ? createStringResource("abstractRoleMemberPanel.message.confirmationMessageForSingleObject",
                    recomputeActionTranslated, ((ObjectType) ((SelectableBean<?>) rowModel.getObject()).getValue()).getName())
                    : createStringResource("abstractRoleMemberPanel.recomputeSelectedMembersConfirmationLabel",
                    getSelectedObjectsCount());

            executeSimpleRecomputeOperation(rowModel, confirmModel, target);
            return;
        }

        confirmModel = createStringResource("abstractRoleMemberPanel.recomputeAllMembersConfirmationLabel");

        ConfirmationPanel dialog = new ConfigureTaskConfirmationPanel(((PageBase) getPage()).getMainPopupBodyId(),
                confirmModel) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createWarningMessageModel() {
                if (isSubtreeScope()) {
                    return createStringResource("abstractRoleMemberPanel.recompute.warning.subtree");
                }
                return null;
            }

            @Override
            protected PrismObject<TaskType> createTask(AjaxRequestTarget target) {
                var pageBase = getPageBase();
                var taskCreator = new MemberOperationsTaskCreator.Recompute(
                        AbstractRoleMemberPanel.this.getModelObject(),
                        getMemberSearchType(),
                        getMemberQuery(null, getMemberQueryScope(), getRelationsForRecomputeTask()),
                        getMemberQueryScope(),
                        pageBase);
                return pageBase.taskAwareExecutor(target, taskCreator.getOperationName())
                        .hideSuccessfulStatus()
                        .run(taskCreator::createTask);
            }

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("pageUsers.message.confirmActionPopupTitle");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                var pageBase = getPageBase();
                var taskCreator = new MemberOperationsTaskCreator.Recompute(
                        AbstractRoleMemberPanel.this.getModelObject(),
                        getMemberSearchType(),
                        getMemberQuery(null, getMemberQueryScope(), getRelationsForRecomputeTask()),
                        getMemberQueryScope(),
                        pageBase);
                pageBase.taskAwareExecutor(target, taskCreator.getOperationName())
                        .runVoid(taskCreator::createAndSubmitTask);
            }

        };
        ((PageBase) getPage()).showMainPopup(dialog, target);
    }

    protected <AH extends AssignmentHolderType> void unassignMembersPerformed(IModel<SelectableBean<AH>> rowModel, AjaxRequestTarget target) {
        unassignMembersPerformed(rowModel, null, target);
    }

    protected <AH extends AssignmentHolderType> void unassignMembersPerformed(IModel<SelectableBean<AH>> rowModel, QName relation, AjaxRequestTarget target) {
        QueryScope scope = getMemberQueryScope();
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
                    @Serial private static final long serialVersionUID = 1L;

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

//                    @Override
//                    protected IModel<String> getWarningMessageModel() {
//                        if (isSubtreeScope()) {
//                            return getPageBase().createStringResource("abstractRoleMemberPanel.unassign.warning.subtree");
//                        } else if (isIndirect()) {
//                            return getPageBase().createStringResource("abstractRoleMemberPanel.unassign.warning.indirect");
//                        }
//                        return null;
//                    }
                };

                getPageBase().showMainPopup(chooseTypePopupContent, target);

            } else {
                showConfirmDialog(rowModel, relation, confirmModel, target);
            }
        } else {
            confirmModel = createStringResource("abstractRoleMemberPanel.unassignAllMembersConfirmationLabel");

            ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
                    getPageBase().getMainPopupBodyId(), confirmModel) {
                @Serial private static final long serialVersionUID = 1L;

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
                    return !QueryScope.SELECTED.equals(scope);
                }

                protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                    unassignMembersPerformed(
                            null,
                            type,
                            isSubtreeScope() && QueryScope.ALL.equals(scope) ? QueryScope.ALL_DIRECT : scope,
                            relations,
                            target);
                }

                @Override
                protected PrismObject<TaskType> createTask(QName type, Collection<QName> relations, AjaxRequestTarget target) {
                    if (checkRelationNotSelected(
                            relations, "No relation was selected. Cannot perform unassign members", target)) {
                        return null;
                    }
                    var pageBase = getPageBase();
                    var taskCreator = new MemberOperationsTaskCreator.Unassign(
                            AbstractRoleMemberPanel.this.getModelObject(),
                            type,
                            getMemberQuery(null, scope, relations),
                            scope,
                            relations,
                            pageBase);

                    return pageBase.taskAwareExecutor(target, taskCreator.getOperationName())
                            .hideSuccessfulStatus()
                            .run(taskCreator::createTask);
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

                    return getMemberSearchType();

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

                AssignmentHolderType object = getAssignmentHolderFromRow(rowModel);
                if (object != null) {
                    executeRecompute(object, target);
                } else {
                    var pageBase = getPageBase();
                    var taskCreator = new MemberOperationsTaskCreator.Recompute(
                            AbstractRoleMemberPanel.this.getModelObject(),
                            getMemberSearchType(),
                            getMemberQuery(rowModel, getMemberQueryScope(), getSupportedRelations()),
                            getMemberQueryScope(),
                            getPageBase());
                    pageBase.taskAwareExecutor(target, taskCreator.getOperationName())
                            .runVoid(taskCreator::createAndSubmitTask);
                }
            }
        };
        getPageBase().showMainPopup(dialog, target);
    }

    private void executeSimpleDeleteOperation(IModel<?> rowModel, StringResourceModel confirmModel, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new DeleteConfirmationPanel(getPageBase().getMainPopupBodyId(), confirmModel) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {

                AssignmentHolderType object = getAssignmentHolderFromRow(rowModel);
                if (object != null) {
                    executeDelete(object, target);
                } else {
                    var pageBase = getPageBase();
                    var taskCreator = new MemberOperationsTaskCreator.Delete(
                            AbstractRoleMemberPanel.this.getModelObject(),
                            getMemberSearchType(),
                            getMemberQuery(rowModel, getMemberQueryScope(), getSupportedRelations()),
                            getMemberQueryScope(),
                            pageBase);
                    pageBase.taskAwareExecutor(target, taskCreator.getOperationName())
                            .runVoid(taskCreator::createAndSubmitTask);
                }
            }
        };
        getPageBase().showMainPopup(dialog, target);
    }

    private void showConfirmDialog(
            IModel<?> rowModel, QName relation, StringResourceModel confirmModel, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfigureTaskConfirmationPanel(getPageBase().getMainPopupBodyId(), confirmModel) {

            @Override
            protected IModel<String> createWarningMessageModel() {
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

        } else {
            var pageBase = getPageBase();
            var taskCreator = new MemberOperationsTaskCreator.Unassign(
                    AbstractRoleMemberPanel.this.getModelObject(),
                    getMemberSearchType(),
                    getMemberQuery(rowModel, getMemberQueryScope(), getSupportedRelations()),
                    getMemberQueryScope(),
                    getSupportedRelations(),
                    pageBase);
            pageBase.taskAwareExecutor(target, taskCreator.getOperationName())
                    .withOpResultOptions(OpResult.Options.create()
                            .withHideTaskLinks(shouldHideTaskLink()))
                    .runVoid(taskCreator::createAndSubmitTask);
        }
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

    protected Component getFeedback() {
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

    private List<AssignmentType> getObjectAssignmentTypes(AssignmentHolderType object, QName relation) {
        return object.getAssignment().stream()
                .filter(
                        assignment -> assignment.getTargetRef() != null
                                && assignment.getTargetRef().getOid().equals(getTargetOrganizationOid())
                                && (relation == null || QNameUtil.match(relation, assignment.getTargetRef().getRelation())))
                .collect(Collectors.toList());
    }

    protected void createMemberMenuActionPerformed(AjaxRequestTarget target) {
        if (!showNewObjectCreationPopup()) {
            createAssignmentObjectRelationDefinitionDialog(target);
            return;
        }

        LoadableModel<MultiFunctinalButtonDto> buttonDescriptionsModel = loadMultiFunctionalButtonModel();
        NewObjectCreationPopup buttonsPanel = new NewObjectCreationPopup(getPageBase().getMainPopupBodyId(),
                new PropertyModel<>(buttonDescriptionsModel, MultiFunctinalButtonDto.F_ADDITIONAL_BUTTONS)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec,
                    CompiledObjectCollectionView collectionViews, Class<? extends WebPage> page) {
                getPageBase().hideMainPopup(target);
                AbstractRoleMemberPanel.this.newObjectPerformed(relationSpec, target);
            }

        };

        getPageBase().showMainPopup(buttonsPanel, target);
    }

    private void createAssignmentObjectRelationDefinitionDialog(AjaxRequestTarget target) {
        AssignmentObjectRelationDefinitionDialog chooseTypePopupContent = new AssignmentObjectRelationDefinitionDialog(
                getPageBase().getMainPopupBodyId()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<QName> getSupportedObjectTypes() {
                return AbstractRoleMemberPanel.this.getNewMemberObjectTypes();
            }

            @Override
            protected List<QName> getSupportedRelations() {
                return AbstractRoleMemberPanel.this.getSupportedRelations();
            }

            protected void okPerformed(AssignmentObjectRelation assignmentObjectRelation, AjaxRequestTarget target) {
                newObjectPerformed(assignmentObjectRelation, target);
            }

        };

        getPageBase().showMainPopup(chooseTypePopupContent, target);
    }

    protected void newObjectPerformed(AssignmentObjectRelation relationSpec, AjaxRequestTarget target) {
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
                DetailsPageUtil.initNewObjectWithReference(AbstractRoleMemberPanel.this.getPageBase(), newMemberType, newReferences);
            } catch (SchemaException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } else {
            createAssignmentObjectRelationDefinitionDialog(target);
        }
    }

    private void deleteMembersPerformed(
            QueryScope scope, QName memberType, Collection<QName> relations, AjaxRequestTarget target) {
        if (checkRelationNotSelected(relations, "No relation was selected. Cannot perform delete members", target)) {
            return;
        }
        var pageBase = getPageBase();
        var helper = new MemberOperationsTaskCreator.Delete(
                getModelObject(),
                memberType,
                getMemberQuery(null, scope, relations),
                scope,
                pageBase);
        pageBase.taskAwareExecutor(target, helper.getOperationName())
                .runVoid(helper::createAndSubmitTask);
    }

    protected void unassignMembersPerformed(
            IModel<?> rowModel, QName type, QueryScope scope, Collection<QName> relations, AjaxRequestTarget target) {
        if (checkRelationNotSelected(relations, "No relation was selected. Cannot perform unassign members", target)) {
            return;
        }
        var pageBase = getPageBase();
        var helper = new MemberOperationsTaskCreator.Unassign(
                getModelObject(),
                type,
                getMemberQuery(rowModel, scope, relations),
                scope,
                relations,
                pageBase);
        pageBase.taskAwareExecutor(target, helper.getOperationName())
                .runVoid(helper::createAndSubmitTask);
        target.add(this); // Why not for other actions?
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

    private ObjectQuery getMemberQuery(IModel<?> rowModel, QueryScope scope, @NotNull Collection<QName> relations) {
        AssignmentHolderType assignmentHolder = getAssignmentHolderFromRow(rowModel);
        if (assignmentHolder != null) {
            return MemberOperationsQueryUtil.createSelectedObjectsQuery(List.of(assignmentHolder));
        } if (!getSelectedRealObjects().isEmpty()) {
            return MemberOperationsQueryUtil.createSelectedObjectsQuery(getSelectedRealObjects());
        } else {
            return getMemberQuery(scope, relations);
        }
    }

    protected ObjectQuery getMemberQuery(@NotNull QueryScope scope, @NotNull Collection<QName> relations) {
        return switch (scope) {
            case ALL -> createAllMemberQuery(relations);
            case ALL_DIRECT ->
                    MemberOperationsQueryUtil.createDirectMemberQuery(
                            getModelObject(),
                            getMemberSearchType(),
                            relations,
                            getTenantValue(),
                            getProjectValue());
            case SELECTED ->
                    MemberOperationsQueryUtil.createSelectedObjectsQuery(
                            getSelectedRealObjects());
        };
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

    protected QueryScope getMemberQueryScope() {
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

    protected @NotNull QName getMemberSearchType() {
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
                .item(FocusType.F_ROLE_MEMBERSHIP_REF)
                .ref(MemberOperationsQueryUtil.createReferenceValuesList(getModelObject(), relations))
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

    private boolean isRefreshEnabled() {
        return Objects.requireNonNullElse(isRefreshEnabled, true);
    }

    private Duration getAutoRefreshInterval(CompiledObjectCollectionView view) {
        if (view == null) {
            return Duration.ofSeconds(DEFAULT_REFRESH_INTERVAL);
        }

        Integer autoRefreshInterval = view.getRefreshInterval();
        return Duration.ofSeconds(
                Objects.requireNonNullElse(
                        autoRefreshInterval,
                        DEFAULT_REFRESH_INTERVAL));
    }

    protected AjaxIconButton createRefreshButton(String buttonId) {
        AjaxIconButton refreshIcon = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                createStringResource("MainObjectListPanel.refresh")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                refreshTable(target);
            }
        };
        refreshIcon.add(AttributeAppender.append("class", "btn btn-outline-primary ml-2"));
        refreshIcon.showTitleAsLabel(true);
        return refreshIcon;
    }

    protected AjaxIconButton createPlayPauseButton(String buttonId) {
        AjaxIconButton playPauseIcon = new AjaxIconButton(buttonId, getRefreshPausePlayButtonModel(),
                getRefreshPausePlayButtonTitleModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                isRefreshEnabled = !isRefreshEnabled;
                refreshTable(target);
            }
        };
        playPauseIcon.add(AttributeAppender.append("class", "btn btn-outline-primary ml-2"));
        playPauseIcon.showTitleAsLabel(true);
        return playPauseIcon;
    }

    private IModel<String> getRefreshPausePlayButtonModel() {
        return () -> {
            if (isRefreshEnabled()) {
                return GuiStyleConstants.CLASS_PAUSE;
            }

            return GuiStyleConstants.CLASS_PLAY;
        };
    }

    private IModel<String> getRefreshPausePlayButtonTitleModel() {
        return () -> {
            if (isRefreshEnabled()) {
                return createStringResource("MainObjectListPanel.refresh.pause").getString();
            }
            return createStringResource("MainObjectListPanel.refresh.start").getString();
        };
    }

    protected AjaxSelfUpdatingTimerBehavior createRefreshBehaviour(CompiledObjectCollectionView view) {
        return new AjaxSelfUpdatingTimerBehavior(getAutoRefreshInterval(view)) {
            @Override
            protected boolean shouldTrigger() {
                return isRefreshEnabled();
            }
        };
    }

    private boolean showNewObjectCreationPopup() {
        LoadableModel<MultiFunctinalButtonDto> buttonDescriptionsModel = loadMultiFunctionalButtonModel();
        List<CompositedIconButtonDto> additionalButtons = buttonDescriptionsModel != null &&
                buttonDescriptionsModel.getObject() != null ?
                buttonDescriptionsModel.getObject().getAdditionalButtons() : null;
        return CollectionUtils.isNotEmpty(additionalButtons);
    }
}
