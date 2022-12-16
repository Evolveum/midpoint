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
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.IResource;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

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

                SearchFactory<FocusType> searchFactory = new SearchFactory<>()
                        .type(FocusType.class)
                        .defaultSearchBoxConfig(getDefaultMemberSearchBoxConfig(FocusType.class))
                        .collectionView(getObjectCollectionView())
                        .modelServiceLocator(getPageBase());

                return searchFactory.createSearch();



//                return createMemberSearch(FocusType.class);
            }
        };
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

    protected List<QName> getSupportedRelations() {
        return getSupportedGovernanceTabRelations();
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

        SelectableBeanObjectDataProvider<FocusType> provider = new SelectableBeanObjectDataProvider<>(
                getPageBase(), searchModel, null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return getMemberPanelStorage();
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return GovernanceCardsPanel.this.getCustomizedQuery(getSearchModel().getObject());
            }

            @Override
            public void detach() {
                preprocessSelectedDataInternal();
                super.detach();
            }
        };
        provider.setCompiledObjectCollectionView(getCompiledCollectionViewFromPanelConfiguration());
        provider.setOptions(getSearchOptions());
        provider.addQueryVariables(ExpressionConstants.VAR_PARENT_OBJECT, ObjectTypeUtil.createObjectRef(getModelObject()));

        TileTablePanel<TemplateTile<SelectableBean<FocusType>>, SelectableBean<FocusType>> tilesTable =
                new TileTablePanel<>(
                        ID_MEMBER_TABLE,
                        provider,
                        Collections.emptyList(),
                        Model.of(ViewToggle.TILE),
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

                        List<InlineMenuItem> actions = new ArrayList<>();
                        createRecomputeMemberRowAction(actions);
                        createAddMemberRowAction(actions);
                        createDeleteMemberRowAction(actions);
                        createUnselectAllAction(actions);

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
                        FocusType obj = object.getValue();
                        PrismObject prism = obj != null ? obj.asPrismObject() : null;
                        String icon = WebComponentUtil.createDefaultColoredIcon(prism.getValue().getTypeName());

                        String description = object.getValue().getDescription();
                        if (obj instanceof UserType) {
                            DisplayType displayType = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(obj, getPageBase());
                            if (displayType != null && displayType.getLabel() != null) {
                                description = WebComponentUtil.getTranslatedPolyString(displayType.getLabel());
                            }
                        }

                        TemplateTile<SelectableBean<FocusType>> t = new TemplateTile<>(
                                icon, WebComponentUtil.getDisplayNameOrName(prism), object)
                                .description(description);
                        t.setSelected(object.isSelected());

                        obj.getAssignment().stream()
                                .filter(assignment -> assignment.getTargetRef() != null
                                        && getObjectWrapper().getOid().equals(assignment.getTargetRef().getOid())
                                        && WebComponentUtil.getRelationDefinition(assignment.getTargetRef().getRelation()).getCategory().contains(AreaCategoryType.GOVERNANCE))
                                .forEach(assignment -> t.addTag(WebComponentUtil.getRelationDefinition(assignment.getTargetRef().getRelation()).getDisplay()));

                        return t;
                    }

                    @Override
                    protected Component createTile(String id, IModel<TemplateTile<SelectableBean<FocusType>>> model) {
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
                                List<InlineMenuItem> menu = new ArrayList<>();
                                createRecomputeMemberRowAction(menu);
                                createDeleteMemberRowAction(menu);
                                return menu;
                            }
                        };
                    }

                    @Override
                    protected String getTileCssClasses() {
                        return "col-xs-6 col-sm-6 col-md-6 col-lg-4 col-xl-3 col-xxl-3 px-4 mb-3";
                    }

                    @Override
                    protected String getTilesFooterCssClasses() {
                        return "card-footer";
                    }

                    @Override
                    protected String getTilesHeaderCssClasses() {
                        return getTilesFooterCssClasses();
                    }

                    @Override
                    protected IModel<Search<? extends ObjectType>> createSearchModel() {
                        return (IModel) searchModel;
                    }
                };
        memberContainer.add(tilesTable);
    }

    private void createUnselectAllAction(List<InlineMenuItem> menu) {
        menu.add(new InlineMenuItem(createStringResource("GovernanceCardsPanel.menu.unselect")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ((SelectableBeanObjectDataProvider)getMemberTileTable().getProvider()).clearSelectedObjects();
                        target.add(getMemberTileTable());
                    }
                };
            }

        });
    }

    @Override
    protected boolean isVisibleAdvanceSearchItem() {
        return false;
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
}
