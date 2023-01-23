/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.*;
import com.evolveum.midpoint.gui.impl.component.tile.*;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.*;
import org.apache.wicket.request.resource.IResource;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.sleepWatchfully;

@PanelType(name = "governanceCards")
@PanelDisplay(label = "GovernanceCardsPanel.label", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 91)
public class GovernanceCardsPanel<AR extends AbstractRoleType> extends AbstractRoleMemberPanel<AR> {

    private static final String ID_TITLE = "title";

    private IModel<Search<FocusType>> searchModel;

    public GovernanceCardsPanel(String id, FocusDetailsModels<AR> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void onInitialize() {
        initSearchModel();
        super.onInitialize();
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        Label title = new Label(ID_TITLE, createStringResource("GovernanceCardsPanel.tiles.title"));
        getMemberContainer().add(title);
    }

    private void initSearchModel() {
        searchModel = new LoadableDetachableModel<>() {
            @Override
            protected Search<FocusType> load() {

                SearchFactory<FocusType> searchFactory = new SearchFactory<>(FocusType.class)
                        .collectionView(getObjectCollectionView())
                        .additionalSearchContext(createAdditionalSearchContext())
                        .modelServiceLocator(getPageBase());

                Search<FocusType> search = searchFactory.createSearch();
                MemberPanelStorage storage = getMemberPanelStorage();
                storage.setSearch(search);
                return search;
            }
        };
    }

    private SearchContext createAdditionalSearchContext() {
        SearchContext ctx = new SearchContext();
        ctx.setPanelType(PredefinedSearchableItems.PanelType.CARDS_GOVERNANCE);
        return ctx;
    }

    private CompiledObjectCollectionView getObjectCollectionView() {
        ContainerPanelConfigurationType config = getPanelConfiguration();
        if (config == null) {
            return null;
        }
        GuiObjectListViewType listViewType = config.getListView();
        return WebComponentUtil.getCompiledObjectCollectionView(listViewType, config, getPageBase());
    }

    @Override
    public QName getType() {
        QName type = super.getType();
        if (type == null) {
            return AbstractRoleType.COMPLEX_TYPE;
        }
        return type;
    }

    @Override
    protected UserProfileStorage.TableId getTableId(QName complexType) {
        return UserProfileStorage.TableId.PANEL_GOVERNANCE_CARDS;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        Collection<SelectorOptions<GetOperationOptions>> options = super.getSearchOptions();
        options.addAll(getPageBase().getOperationOptionsBuilder()
                .item(FocusType.F_JPEG_PHOTO).retrieve()
                .build());
        return options;
    }

    @Override
    protected <AH extends AssignmentHolderType> void initMemberTable(Form<?> form) {
        WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
        memberContainer.setOutputMarkupId(true);
        memberContainer.setOutputMarkupPlaceholderTag(true);
        form.add(memberContainer);

        MultiSelectTileTablePanel<FocusType, FocusType> tilesTable =
                new MultiSelectTileTablePanel<>(
                        ID_MEMBER_TABLE,
                        getTableId(getType())) {

                    @Override
                    protected WebMarkupContainer createTilesButtonToolbar(String id) {
                        RepeatingView repView = new RepeatingView(id);
                        AjaxIconButton assignButton = createAssignButton(repView.newChildId());
                        assignButton.add(AttributeAppender.replace("class", "btn btn-primary"));
                        assignButton.showTitleAsLabel(true);
                        repView.add(assignButton);

                        AjaxIconButton unassignButton = createUnassignButton(repView.newChildId());
                        unassignButton.add(AttributeAppender.replace("class", "btn btn-outline-primary ml-2"));
                        unassignButton.showTitleAsLabel(true);
                        repView.add(unassignButton);

                        List<InlineMenuItem> actions = createToolbarMenuActions();

                        DropdownButtonPanel menu = new DropdownButtonPanel(
                                repView.newChildId(),
                                new DropdownButtonDto(
                                        null,
                                        null,
                                        getString("GovernanceCardsPanel.menu.actions"),
                                        actions)) {
                            @Override
                            protected String getSpecialButtonClass() {
                                return "btn-outline-primary";
                            }
                        };
                        menu.add(new VisibleBehaviour(() -> !menu.getModel().getObject().getMenuItems().isEmpty()));
                        menu.add(AttributeAppender.replace("class", "ml-2"));
                        repView.add(menu);

                        return repView;
                    }

                    @Override
                    protected TemplateTile<SelectableBean<FocusType>> createTileObject(SelectableBean<FocusType> object) {
                        TemplateTile<SelectableBean<FocusType>> t = super.createTileObject(object);
                        object.getValue().getAssignment().stream()
                                .filter(assignment -> assignment.getTargetRef() != null
                                        && getObjectWrapper().getOid().equals(assignment.getTargetRef().getOid())
                                        && WebComponentUtil.getRelationDefinition(assignment.getTargetRef().getRelation()).getCategory().contains(AreaCategoryType.GOVERNANCE))
                                .forEach(assignment -> t.addTag(WebComponentUtil.getRelationDefinition(assignment.getTargetRef().getRelation()).getDisplay()));
                        return t;
                    }

                    @Override
                    protected void deselectItem(FocusType entry) {
                        getProvider().getSelected().remove(entry);
                    }

                    @Override
                    protected IModel<String> getItemLabelModel(FocusType entry) {
                        return Model.of(WebComponentUtil.getDisplayNameOrName(entry.asPrismObject()));
                    }

                    @Override
                    protected IModel<List<FocusType>> getSelectedItemsModel() {
                        return () -> new ArrayList<>(getProvider().getSelected());
                    }

                    @Override
                    protected Component createTile(String id, IModel<TemplateTile<SelectableBean<FocusType>>> model) {
                        return createTilePanel(id, model);
                    }

                    @Override
                    protected String getTileCssClasses() {
                        return GovernanceCardsPanel.this.getTileCssClasses();
                    }

                    @Override
                    protected String getTilesFooterCssClasses() {
                        return "card-footer";
                    }

                    @Override
                    protected SelectableBeanObjectDataProvider<FocusType> createProvider() {
                        SelectableBeanObjectDataProvider<FocusType> provider = super.createProvider();
                        provider.addQueryVariables(
                                ExpressionConstants.VAR_PARENT_OBJECT,
                                ObjectTypeUtil.createObjectRef(GovernanceCardsPanel.this.getModelObject()));
                        return provider;
                    }

                    @Override
                    protected PageStorage getPageStorage() {
                        return getMemberPanelStorage();
                    }

                    @Override
                    protected String getTilesHeaderCssClasses() {
                        return getTilesFooterCssClasses();
                    }

                    @Override
                    protected IModel<Search<? extends ObjectType>> createSearchModel() {
                        return (IModel) searchModel;
                    }

                    @Override
                    protected boolean isSelectedItemsPanelVisible() {
                        return false;
                    }
                };
        memberContainer.add(tilesTable);
    }

    protected String getTileCssClasses() {
        return "col-xs-6 col-sm-6 col-md-6 col-lg-4 col-xl-3 col-xxl-3 px-4 mb-3";
    }

    protected List<InlineMenuItem> createToolbarMenuActions() {
        List<InlineMenuItem> actions = new ArrayList<>();
        createRecomputeMemberRowAction(actions);
        createAddMemberRowAction(actions);
        createDeleteMemberRowAction(actions);
        createUnselectAllAction(actions);
        return actions;
    }

    private Component createTilePanel(String id, IModel<TemplateTile<SelectableBean<FocusType>>> model) {
        return new MemberTilePanel<>(id, model) {

            @Override
            protected void onUnassign(AjaxRequestTarget target) {
                unassignMembersPerformed(new PropertyModel<>(model, "value"), target);
            }

            @Override
            protected void onDetails(AjaxRequestTarget target) {
                SelectableBean<FocusType> bean = model.getObject().getValue();
                if (WebComponentUtil.hasDetailsPage(bean.getValue().getClass())) {
                    WebComponentUtil.dispatchToObjectDetailsPage(
                            bean.getValue().getClass(), bean.getValue().getOid(), this, true);
                } else {
                    error("Could not find proper response page");
                    throw new RestartResponseException(getPageBase());
                }
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                super.onClick(target);
                getModelObject().getValue().setSelected(getModelObject().isSelected());
            }

            @Override
            protected IModel<IResource> createPreferredImage(IModel<TemplateTile<SelectableBean<FocusType>>> model) {
                return new LoadableModel<>(false) {
                    @Override
                    protected IResource load() {
                        FocusType object = model.getObject().getValue().getValue();
                        return WebComponentUtil.createJpegPhotoResource(object);
                    }
                };
            }

            @Override
            protected List<InlineMenuItem> createMenuItems() {
                return createCardHeaderMenuActions();
            }

            @Override
            protected Behavior createDetailsBehaviour() {
                return createCardDetailsButtonBehaviour();
            }

            @Override
            protected String getCssForUnassignButton() {
                return getCssForCardUnassignButton(super.getCssForUnassignButton());
            }
        };
    }

    protected String getCssForCardUnassignButton(String defaultCss) {
        return defaultCss;
    }

    protected List<InlineMenuItem> createCardHeaderMenuActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        createRecomputeMemberRowAction(menu);
        createDeleteMemberRowAction(menu);
        return menu;
    }

    protected Behavior createCardDetailsButtonBehaviour() {
        return VisibleBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    protected void createUnselectAllAction(List<InlineMenuItem> menu) {
        menu.add(new InlineMenuItem(createStringResource("GovernanceCardsPanel.menu.unselect")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        unselectAllPerformed(target);
                    }
                };
            }

        });
    }

    private void unselectAllPerformed(AjaxRequestTarget target) {
        ((SelectableBeanObjectDataProvider)getMemberTileTable().getProvider()).clearSelectedObjects();
        target.add(getMemberTileTable());
    }
    protected TileTablePanel<TemplateTile<SelectableBean<FocusType>>, SelectableBean<FocusType>> getMemberTileTable() {
        return (TileTablePanel<TemplateTile<SelectableBean<FocusType>>, SelectableBean<FocusType>>)
                get(getPageBase().createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
    }

    @Override
    protected int getSelectedObjectsCount() {
        return getSelectedRealObjects().size();
    }

    @Override
    protected List<? extends ObjectType> getSelectedRealObjects() {
        SelectableBeanObjectDataProvider<FocusType> provider =
                (SelectableBeanObjectDataProvider<FocusType>) getMemberTileTable().getProvider();
        return provider.getSelected().stream().collect(Collectors.toList());
    }

    @Override
    protected void refreshTable(AjaxRequestTarget target) {
        target.add(getMemberTileTable());
        getMemberTileTable().getProvider().detach();
        getMemberTileTable().getTilesModel().detach();
        getMemberTileTable().refresh(target);
    }

    @Override
    protected void processTaskAfterOperation(Task task, AjaxRequestTarget target) {
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
        if (!result.get().isSuccess()) {
            getPageBase().showResult(result.get());
        }
        refreshTable(target);
        target.add(getPageBase().getFeedbackPanel());
    }

    @Override
    protected void unassignMembersPerformed(IModel<?> rowModel, AjaxRequestTarget target) {
        super.unassignMembersPerformed(rowModel, target);
    }

    @Override
    protected void executeSimpleUnassignedOperation(IModel<?> rowModel, StringResourceModel confirmModel, AjaxRequestTarget target) {
        super.executeSimpleUnassignedOperation(rowModel, confirmModel, target);
        unselectAllPerformed(target);
    }
}
