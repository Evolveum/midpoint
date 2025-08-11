/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.provider.ObjectClassDataProvider;
import com.evolveum.midpoint.gui.impl.component.tile.RadioTileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.CardWithTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.page.ResourceGeneratingSuggestionObjectClassWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.component.PrismItemDefinitionsTable;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassSizeEstimationType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;

public class SmartObjectClassRadioTileTable extends BasePanel<ResourceDetailsModel> {

    private static final String OP_DETERMINE_STATUS = ResourceGeneratingSuggestionObjectClassWizardPanel.class.getName() + ".determineStatus";

    private static final String ID_DATATABLE = "datatable";
    private final PageBase pageBase;
    private final IModel<List<Toggle<ViewToggle>>> items;

    private static final int MAX_TILE_COUNT = 6;

    IModel<SelectableBean<ObjectClassWrapper>> selectedTileModel;

    public SmartObjectClassRadioTileTable(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull IModel<ResourceDetailsModel> resourceDetailsModel,
            IModel<SelectableBean<ObjectClassWrapper>> selectedModel) {
        super(id, resourceDetailsModel);
        this.pageBase = pageBase;

        selectedTileModel = selectedModel;

        items = getListToggleView(getTable());

        add(initTable());
        setDefaultPagingSize();
    }

    private @NotNull List<ObjectClassWrapper> getObjectClassWrappers() {
        Collection<ResourceObjectClassDefinition> objectClassDefinitions;
        try {
            objectClassDefinitions = SmartObjectClassRadioTileTable.this.getModelObject().getRefinedSchema()
                    .getObjectClassDefinitions();
        } catch (SchemaException | ConfigurationException e) {
            throw new RuntimeException("Error while fetching object class definitions", e);
        }

        List<ObjectClassWrapper> objectClassWrappers = new ArrayList<>();
        for (ResourceObjectClassDefinition definition : objectClassDefinitions) {
            ObjectClassWrapper wrapper = new ObjectClassWrapper(definition);
            objectClassWrappers.add(wrapper);
        }
        return objectClassWrappers;
    }

    protected void setDefaultPagingSize() {
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        userProfile.setPagingSize(
                UserProfileStorage.TableId.PANEL_RESOURCE_OBJECT_CLASSES,
                SmartObjectClassRadioTileTable.MAX_TILE_COUNT);
    }

    public RadioTileTablePanel<SmartObjectClassTileModel<SelectableBean<ObjectClassWrapper>>, SelectableBean<ObjectClassWrapper>> initTable() {
        return new RadioTileTablePanel<>(
                ID_DATATABLE,
                Model.of(ViewToggle.TILE),
                selectedTileModel,
                UserProfileStorage.TableId.PANEL_RESOURCE_OBJECT_CLASSES) {

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
            protected void onRadioTileSelected(IModel<SelectableBean<ObjectClassWrapper>> selectedTileModel, AjaxRequestTarget target) {
                onSelectionPerformed(selectedTileModel, target);
            }

            @Override
            protected List<IColumn<SelectableBean<ObjectClassWrapper>, String>> createColumns() {
                return SmartObjectClassRadioTileTable.this.initColumns();
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment", SmartObjectClassRadioTileTable.this);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                        Model.of("fa fa-refresh"), Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onRefresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                refreshTable.add(AttributeModifier.replace("title", createStringResource("Refresh table")));
                refreshTable.add(new TooltipBehavior());
                fragment.add(refreshTable);

                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {
                    @Override
                    protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
                        target.add(this);
                        target.add(SmartObjectClassRadioTileTable.this);
                    }
                };
                viewToggle.add(AttributeModifier.replace("title", createStringResource("Change view")));
                viewToggle.add(new TooltipBehavior());
                viewToggle.add(new VisibleBehaviour(() -> isViewToggleVisible()));
                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected WebMarkupContainer createTilesButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment", SmartObjectClassRadioTileTable.this);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable",
                        Model.of("fa fa-refresh"), Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        onRefresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                refreshTable.add(new VisibleBehaviour(() -> isViewRefreshButtonVisible()));
                fragment.add(refreshTable);

                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {
                    @Override
                    protected void itemSelected(@NotNull AjaxRequestTarget target, @NotNull IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
                        getTable().refreshSearch();
                        target.add(SmartObjectClassRadioTileTable.this);
                    }
                };

                viewToggle.add(new VisibleBehaviour(() -> isViewToggleVisible()));
                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected ISortableDataProvider<?, ?> createProvider() {
                List<ObjectClassWrapper> objectClassWrappers = getObjectClassWrappers();
                return new ObjectClassDataProvider(this, () -> objectClassWrappers) {
                    @Override
                    protected SelectableBean<ObjectClassWrapper> createObjectWrapper(ObjectClassWrapper object) {
                        return super.createObjectWrapper(object);
                    }
                };
            }

            @Override
            protected SmartObjectClassTileModel<SelectableBean<ObjectClassWrapper>> createTileObject(
                    SelectableBean<ObjectClassWrapper> objectClassWrapper) {
                String oid = SmartObjectClassRadioTileTable.this.getModelObject().getObjectType().getOid();
                QName objectClassName = objectClassWrapper.getValue().getObjectClassName();
                Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUS);
                OperationResult result = task.getResult();

                ObjectClassSizeEstimationType sizeEstimation = computeObjectClassSizeEstimationType(
                        getPageBase(),
                        oid,
                        objectClassName,
                        task,
                        result);

                result.computeStatusIfUnknown();
                if (!result.isSuccess()) {
                    getPageBase().showResult(result);
                }

                return new SmartObjectClassTileModel<>(objectClassWrapper, sizeEstimation);
            }

            @Override
            protected Component createTile(String id, IModel<SmartObjectClassTileModel<SelectableBean<ObjectClassWrapper>>> model) {
                return new SmartObjectClassTilePanel<>(id, model, selectedTileModel) {
                    @Override
                    protected void onViewSchema(AjaxRequestTarget target) {
                        displaySchemaViewTablePopup(target, getModelObject());
                        super.onViewSchema(target);
                    }
                };
            }
        };
    }

    private void displaySchemaViewTablePopup(
            @NotNull AjaxRequestTarget target,
            @NotNull SmartObjectClassTileModel<SelectableBean<ObjectClassWrapper>> tileModel) {
        ObjectClassWrapper objectClassWrapper = tileModel.getValue().getValue();
        ResourceDetailsModel resourceDetailsModel = getModelObject();
        IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> complexTypeValueModel = getComplexTypeValueModel(
                objectClassWrapper, resourceDetailsModel);

        CardWithTablePanel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> cardWithTablePanel =
                new CardWithTablePanel<>(getPageBase().getMainPopupBodyId(), complexTypeValueModel) {

                    @Override
                    protected @NotNull WebMarkupContainer createComponent(String id) {
                        PrismItemDefinitionsTable schemaViewTable = new PrismItemDefinitionsTable(id,
                                complexTypeValueModel, null) {
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
                        return createStringResource("SuggestTilePanel.schemaView.title",
                                tileModel.getName());
                    }
                };

        getPageBase().showMainPopup(cardWithTablePanel, target);
    }

    protected CompiledObjectCollectionView getObjectCollectionView() {
        return null;
    }

    private @NotNull List<IColumn<SelectableBean<ObjectClassWrapper>, String>> initColumns() {
        List<IColumn<SelectableBean<ObjectClassWrapper>, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<SelectableBean<ObjectClassWrapper>> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(GuiStyleConstants.CLASS_ICON_WIZARD);
            }
        });

        return columns;
    }

    @SuppressWarnings("unchecked")
    private TileTablePanel<SmartObjectClassTileModel<SelectableBean<ObjectClassWrapper>>, SelectableBean<ObjectClassWrapper>> getTable() {
        return (TileTablePanel<SmartObjectClassTileModel<SelectableBean<ObjectClassWrapper>>, SelectableBean<ObjectClassWrapper>>)
                get(createComponentPath(ID_DATATABLE));
    }

    public IModel<List<Toggle<ViewToggle>>> getItems() {
        return items;
    }

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }

    protected void onRefresh(@NotNull AjaxRequestTarget target) {
        target.add(this);
    }

    protected boolean isViewToggleVisible() {
        return false;
    }

    protected boolean isViewRefreshButtonVisible() {
        return false;
    }

    public IModel<SelectableBean<ObjectClassWrapper>> getSelected() {
        return selectedTileModel;
    }

    protected void onSelectionPerformed(IModel<SelectableBean<ObjectClassWrapper>> selectedTileModel, AjaxRequestTarget target) {
        // Override this method to handle selection changes
    }
}
