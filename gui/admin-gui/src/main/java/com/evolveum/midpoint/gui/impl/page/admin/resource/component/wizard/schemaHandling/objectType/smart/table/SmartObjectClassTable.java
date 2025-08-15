/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.SingleSelectContainerTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.CardWithTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.PrismItemDefinitionsTable;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PageableListView;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.RadioColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.computeObjectClassSizeEstimationType;

public class SmartObjectClassTable<O extends PrismContainerValueWrapper<ComplexTypeDefinitionType>> extends SingleSelectContainerTileTablePanel<ComplexTypeDefinitionType> {

    protected static final String ID_TILES_RADIO_FRAGMENT = "tilesRadioFragment";
    protected static final String ID_TILES_RADIO_FORM = "tileForm";
    protected static final String ID_TILES_RADIO = "radioGroup";

    protected static final String ID_TABLE_RADIO_FORM = "tableForm";
    protected static final String ID_TABLE_RADIO = "radioGroup";

    static final String ID_TILE_VIEW = "tileView";
    static final String ID_TILES_CONTAINER = "tilesContainer";

    private static final String OP_DETERMINE_STATUS = SmartObjectClassTable.class.getName() + ".determineStatus";

    private static final int MAX_TILE_COUNT = 6;

    IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> selectedTileModel;
    String resourceOid;

    public SmartObjectClassTable(
            @NotNull String id,
            @NotNull UserProfileStorage.TableId tableId,
            @NotNull IModel<List<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> model,
            @NotNull IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> selectedModel,
            @NotNull String resourceOid) {
        super(id, tableId, model);
        this.selectedTileModel = selectedModel;
        this.resourceOid = resourceOid;
        setDefaultPagingSize(tableId);
    }

    @Override
    protected MultivalueContainerListDataProvider<ComplexTypeDefinitionType> createProvider() {
        return super.createProvider();
    }

    @Override
    protected void togglePanelItemSelectPerformed(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
        if (item == null || item.getObject() == null || item.getObject().getValue() == null) {
            return;
        }
        ViewToggle value = item.getObject().getValue();
        if (value.equals(ViewToggle.TABLE)) {
            add(AttributeModifier.replace("class", "card"));
        } else {
            add(AttributeModifier.replace("class", ""));
        }
        super.togglePanelItemSelectPerformed(target, item);
    }

    @Override
    protected IModel<Search> createSearchModel() {

        IModel<Search<?>> searchModel = new LoadableDetachableModel<>() {

            @Override
            protected Search<?> load() {
                SearchBuilder<?> searchBuilder = new SearchBuilder<>(ComplexTypeDefinitionType.class)
                        .modelServiceLocator(getPageBase());
                return searchBuilder.build();
            }
        };

        return (IModel) searchModel;
    }

    @Override
    protected boolean isFullTextSearchEnabled() {
        return true;
    }



    @Override
    public Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        return super.getSearchOptions();
    }

    @Override
    protected boolean isTogglePanelVisible() {
        return true;
    }

    @Override
    protected WebMarkupContainer createTilesContainer(
            String idTilesContainer,
            ISortableDataProvider<PrismContainerValueWrapper<ComplexTypeDefinitionType>, String> provider,
            UserProfileStorage.TableId tableId) {
        Fragment tilesFragment = new Fragment(idTilesContainer, ID_TILES_RADIO_FRAGMENT, this);
        tilesFragment.add(AttributeModifier.replace("class", getTileContainerCssClass()));

        initializeSelectedTile(provider);

        PageableListView<TemplateTile<PrismContainerValueWrapper<ComplexTypeDefinitionType>>,
                PrismContainerValueWrapper<ComplexTypeDefinitionType>> tiles = createTilesPanel(ID_TILES, provider);
        tiles.setOutputMarkupId(true);

        RadioGroup<PrismContainerValueWrapper<ComplexTypeDefinitionType>> radioGroup = buildRadioGroup(ID_TILES_RADIO);
        radioGroup.add(tiles);

        Form<Void> form = new Form<>(ID_TILES_RADIO_FORM);
        form.setOutputMarkupId(true);
        form.add(radioGroup);

        tilesFragment.add(form);
        return tilesFragment;
    }

    private @NotNull RadioGroup<PrismContainerValueWrapper<ComplexTypeDefinitionType>> buildRadioGroup(String id) {
        RadioGroup<PrismContainerValueWrapper<ComplexTypeDefinitionType>> radioGroup = new RadioGroup<>(
                id, getSelectedTileModel());
        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                onRadioTileSelected(getSelectedTileModel(), ajaxRequestTarget);
                ajaxRequestTarget.add(SmartObjectClassTable.this);
            }
        });
        return radioGroup;
    }

    private void initializeSelectedTile(
            @NotNull ISortableDataProvider<PrismContainerValueWrapper<ComplexTypeDefinitionType>, String> provider) {
        if (selectedTileModel.getObject() != null) {
            return;
        }
        IModel<O> def = getDefaultSelectedTileModel();
        if (def != null && def.getObject() != null) {
            selectedTileModel.setObject(def.getObject());
        } else {
            Iterator<? extends PrismContainerValueWrapper<ComplexTypeDefinitionType>> it = provider.iterator(0, 1);
            if (it.hasNext()) {
                selectedTileModel.setObject(it.next());
            }
        }
    }

    @Override
    protected Class<? extends Containerable> getType() {
        return ComplexTypeDefinitionType.class;
    }

    @Override
    protected TemplateTile<PrismContainerValueWrapper<ComplexTypeDefinitionType>> createTileObject(
            PrismContainerValueWrapper<ComplexTypeDefinitionType> object) {

        ObjectClassSizeEstimationType sizeEstimation = getObjectClassSizeEstimationType(object);

        return new SmartObjectClassTileModel<>(object, sizeEstimation);
    }

    private @Nullable ObjectClassSizeEstimationType getObjectClassSizeEstimationType(
            @NotNull PrismContainerValueWrapper<ComplexTypeDefinitionType> object) {
        ComplexTypeDefinitionType realValue = object.getRealValue();
        QName objectClassName = realValue.getName();

        Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUS);
        OperationResult result = task.getResult();

        ObjectClassSizeEstimationType sizeEstimation = computeObjectClassSizeEstimationType(
                getPageBase(),
                resourceOid,
                objectClassName,
                task,
                result);

        result.computeStatusIfUnknown();
        if (!result.isSuccess()) {
            getPageBase().showResult(result);
        }
        return sizeEstimation;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected Component createTile(String id, IModel<TemplateTile<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> model) {
        return new SmartObjectClassPanel(id, model, selectedTileModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onViewSchema(AjaxRequestTarget target) {
                SmartObjectClassTileModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> smartModel;
                if (getModelObject() instanceof SmartObjectClassTileModel) {
                    smartModel =
                            (SmartObjectClassTileModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>>) getModelObject();
                } else {
                    return;
                }

                PrismContainerValueWrapper<ComplexTypeDefinitionType> value = ((SmartObjectClassTileModel<?>) getModelObject()).getValue();

                displaySchemaViewTablePopup(target, () -> value);
                super.onViewSchema(target);
            }
        };
    }

    private void displaySchemaViewTablePopup(
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> valueModel) {

        CardWithTablePanel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> cardWithTablePanel =
                new CardWithTablePanel<>(getPageBase().getMainPopupBodyId(), valueModel) {

                    @Override
                    protected @NotNull WebMarkupContainer createComponent(String id) {
                        PrismItemDefinitionsTable schemaViewTable = new PrismItemDefinitionsTable(id,
                                valueModel, null) {
                            @Override
                            protected boolean showTableAsCard() {
                                return false;
                            }

                            @Contract(pure = true)
                            @Override
                            protected @Nullable List<InlineMenuItem> createInlineMenu() {
                                return null;
                            }

                            @Override
                            protected boolean isCreateNewObjectVisible() {
                                return false;
                            }
                        };
                        schemaViewTable.setOutputMarkupId(true);
                        return schemaViewTable;
                    }

                    @Override
                    public IModel<String> getTitle() {
                        PrismContainerValueWrapper<ComplexTypeDefinitionType> object = valueModel.getObject();
                        if (object == null
                                || object.getRealValue() == null
                                || object.getRealValue().getName() == null) {
                            return createStringResource("SuggestTilePanel.schemaView.title", "Unknown");
                        }
                        return createStringResource("SuggestTilePanel.schemaView.title",
                                valueModel.getObject().getRealValue().getName().getLocalPart());
                    }
                };

        getPageBase().showMainPopup(cardWithTablePanel, target);
    }

    protected void createToolbarButtons(RepeatingView repeatingView) {
    }

    @Override
    protected WebMarkupContainer createTilesButtonToolbar(String id) {
        RepeatingView repView = new RepeatingView(id);
        createToolbarButtons(repView);
        return repView;
    }

    @Override
    protected Component createHeader(String id) {
        return super.createHeader(id);
    }

    private IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> getSelectedTileModel() {
        return selectedTileModel;
    }

    protected @Nullable IModel<O> getDefaultSelectedTileModel() {
        return null;
    }

    protected void onRadioTileSelected(
            IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> selectedTileModel,
            AjaxRequestTarget target) {

    }

    @SuppressWarnings("unchecked")
    @Override
    protected PageableListView<ComplexTypeDefinitionType, PrismContainerValueWrapper<ComplexTypeDefinitionType>> getTiles() {
        WebMarkupContainer container = (WebMarkupContainer) get(ID_TILE_VIEW).get(ID_TILES_CONTAINER);
        return (PageableListView<ComplexTypeDefinitionType, PrismContainerValueWrapper<ComplexTypeDefinitionType>>) container
                .get(ID_TILES_RADIO_FORM).get(ID_TILES_RADIO).get(ID_TILES);
    }

    protected void setDefaultPagingSize(UserProfileStorage.@NotNull TableId tableId) {
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        userProfile.setPagingSize(
                tableId,
                getMaxTileCount());
    }

    @Override
    protected String getAdditionalTableCssClasses() {
        return "table-td-middle";
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ComplexTypeDefinitionType>, String>> createColumns() {
        List<IColumn<PrismContainerValueWrapper<ComplexTypeDefinitionType>, String>> columns = new ArrayList<>();

        LoadableDetachableModel<PrismContainerDefinition<ComplexTypeDefinitionType>> defModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerDefinition<ComplexTypeDefinitionType> load() {
                ComplexTypeDefinition resourceDef =
                        PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(ResourceType.class);
                return resourceDef.findContainerDefinition(
                        ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_CLASS));
            }
        };

        columns.add(new RadioColumn<>(
                getPageBase().createStringResource("SearchPropertiesConfigPanel.table.column.select"),
                getSelectedTileModel()));

        columns.add(new PrismPropertyWrapperColumn<>(
                defModel,
                ComplexTypeDefinitionType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.STRING,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "text-capitalize";
            }

            @Override
            protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<ComplexTypeDefinitionType>> mainModel) {
                return new Label(
                        componentId,
                        createStringResource("SearchPropertiesConfigPanel.table.column.name"));
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> item,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> rowModel) {
                String description = "Description for this object class is not ready yet, but it will be available soon."; // TODO
                item.add(new Label(componentId, description));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId,
                        createStringResource("Description")); //TODO
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> item,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> rowModel) {
                ObjectClassSizeEstimationType sizeEstimation = getObjectClassSizeEstimationType(rowModel.getObject());
                String estimationValue = "Unknown";
                if (sizeEstimation != null && sizeEstimation.getValue() != null) {
                    estimationValue = String.valueOf(sizeEstimation.getValue());
                }
                item.add(new Label(componentId, estimationValue));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId,
                        createStringResource("Object count")); //TODO
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ComplexTypeDefinitionType>>> item,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> rowModel) {

                AjaxIconButton viewSchemaLink = new AjaxIconButton(componentId, Model.of("fa fa-eye"),
                        createStringResource("SuggestTilePanel.view.schema")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        displaySchemaViewTablePopup(target, rowModel);
                    }
                };
                viewSchemaLink.setOutputMarkupId(true);
                viewSchemaLink.add(AttributeModifier.append("class", "btn btn-link"));
                viewSchemaLink.showTitleAsLabel(true);
                item.add(viewSchemaLink);

            }

            @Override
            public String getCssClass() {
                return "text-right";
            }
        });

        return columns;
    }

    @Override
    protected String getAdditionalBoxCssClasses() {
        return " m-0";
    }

    @Override
    protected String getTilesFooterCssClasses() {
        return "pt-1";
    }

    @Override
    protected String getTileCssStyle() {
        return "";
    }

    @Override
    protected String getTileCssClasses() {
        return "col-12 col-sm-12 col-md-6 col-lg-4 p-2";
    }

    @Override
    protected String getTileContainerCssClass() {
        return "row justify-content-left pt-2 ";
    }

    @Override
    protected String getTilesContainerAdditionalClass() {
        return "";
    }

    @Override
    protected boolean showFooter() {
        return getTilesModel().getObject().size() > getMaxTileCount();
    }

    protected int getMaxTileCount() {
        return SmartObjectClassTable.MAX_TILE_COUNT;
    }

    @Override
    protected void initTable(@NotNull BoxedTablePanel<?> table) {
        RadioGroup<PrismContainerValueWrapper<ComplexTypeDefinitionType>> radioGroup = buildRadioGroup(ID_TABLE_RADIO);
        radioGroup.add(table);

        Form<Void> form = new Form<>(ID_TABLE_RADIO_FORM);
        form.setOutputMarkupId(true);
        form.add(radioGroup);
        add(form);
    }

    @Override
    public BoxedTablePanel<?> getTable() {
        QName name = getSelectedTileModel().getObject().getRealValue().getName();
        System.out.println("Selected tile name: " + name);
        return (BoxedTablePanel<?>) get(createComponentPath(ID_TABLE_RADIO_FORM, ID_TABLE_RADIO, ID_TABLE));
    }

    @Override
    public void refresh(AjaxRequestTarget target) {

    }
}
