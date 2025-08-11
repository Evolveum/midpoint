/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.StatusInfoDataProvider;
import com.evolveum.midpoint.gui.impl.component.tile.RadioTileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypeSuggestionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.*;

public class SmartSuggestedObjectTypeRadioTileTable extends BasePanel<ResourceDetailsModel> {

    private static final String ID_DATATABLE = "datatable";
    private final PageBase pageBase;
    private final IModel<List<Toggle<ViewToggle>>> items;

    private static final int MAX_TILE_COUNT = 6;

    private static final String OP_DETERMINE_STATUS =
            SmartSuggestedObjectTypeRadioTileTable.class.getName() + ".determineStatus";

    IModel<ObjectTypeSuggestionType> selectedTileModel;
    QName selectedObjectClassName;

    public SmartSuggestedObjectTypeRadioTileTable(@NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull IModel<ResourceDetailsModel> resourceDetailsModel,
            QName selectedObjectClassName,
            IModel<ObjectTypeSuggestionType> selectedModel) {
        super(id, resourceDetailsModel);
        this.pageBase = pageBase;
        this.selectedObjectClassName = selectedObjectClassName;

        selectedTileModel = selectedModel;

        items = getListToggleView(getTable());

        add(initTable());
        setDefaultPagingSize();
    }

    protected void setDefaultPagingSize() {
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        userProfile.setPagingSize(
                UserProfileStorage.TableId.PANEL_RESOURCE_OBJECT_TYPES_SUGGESTIONS,
                SmartSuggestedObjectTypeRadioTileTable.MAX_TILE_COUNT);
    }

    public RadioTileTablePanel<SmartSuggestObjectTypeTileModel<ObjectTypeSuggestionType>, ObjectTypeSuggestionType> initTable() {
        return new RadioTileTablePanel<>(
                ID_DATATABLE,
                Model.of(ViewToggle.TILE),
                selectedTileModel,
                UserProfileStorage.TableId.PANEL_RESOURCE_OBJECT_TYPES_SUGGESTIONS) {

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
                return "col-12 p-2";
            }

            @Override
            protected String getTileContainerCssClass() {
                return "justify-content-left pt-2 ";
            }

            @Override
            protected void onRadioTileSelected(IModel<ObjectTypeSuggestionType> selectedTileModel, AjaxRequestTarget target) {
                super.onRadioTileSelected(selectedTileModel, target);
            }

            @Override
            protected List<IColumn<ObjectTypeSuggestionType, String>> createColumns() {
                return SmartSuggestedObjectTypeRadioTileTable.this.initColumns();
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, "tableFooterFragment",
                        SmartSuggestedObjectTypeRadioTileTable.this);

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
                        target.add(SmartSuggestedObjectTypeRadioTileTable.this);
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
                Fragment fragment = new Fragment(id, "tableFooterFragment", SmartSuggestedObjectTypeRadioTileTable.this);

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
                        target.add(SmartSuggestedObjectTypeRadioTileTable.this);
                    }
                };

                viewToggle.add(new VisibleBehaviour(() -> isViewToggleVisible()));
                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected ISortableDataProvider<?, ?> createProvider() {
                ResourceType resource = SmartSuggestedObjectTypeRadioTileTable.this.getModelObject().getObjectType();
                Task task = getPageBase().createSimpleTask(OP_DETERMINE_STATUS);
                OperationResult result = task.getResult();

                LoadableModel<List<PrismContainerValueWrapper<ObjectTypeSuggestionType>>> objectTypeSuggestionTypeModel = new LoadableModel() {
                    @Override
                    protected List<PrismContainerValueWrapper<ObjectTypeSuggestionType>> load() {

                        StatusInfo<ObjectTypesSuggestionType> objectTypesSuggestionTypeStatusInfo = loadObjectClassSuggestions(getPageBase(),
                                resource.getOid(),
                                selectedObjectClassName,
                                task,
                                result);
                        ObjectTypesSuggestionType objectTypeSuggestionResult = null;
                        if (objectTypesSuggestionTypeStatusInfo != null) {
                            objectTypeSuggestionResult = objectTypesSuggestionTypeStatusInfo.result();
                        }

                        PrismContainerWrapper<ObjectTypeSuggestionType> itemWrapper;
                        try {
                            PrismContainer<ObjectTypeSuggestionType> container = objectTypeSuggestionResult
                                    .asPrismContainerValue().findContainer(ObjectTypesSuggestionType.F_OBJECT_TYPE);
                            itemWrapper = getPageBase().createItemWrapper(container, ItemStatus.NOT_CHANGED, new WrapperContext(task, result));
                        } catch (SchemaException e) {
                            throw new RuntimeException(e);
                        }
                        return itemWrapper.getValues();
                    }
                };

                StatusInfo<ObjectTypesSuggestionType> objectTypesSuggestionTypeStatusInfo = loadObjectClassSuggestions(getPageBase(),
                        resource.getOid(),
                        selectedObjectClassName,
                        task,
                        result);
                ObjectTypesSuggestionType objectTypeSuggestionResult = null;
                if (objectTypesSuggestionTypeStatusInfo != null) {
                    objectTypeSuggestionResult = objectTypesSuggestionTypeStatusInfo.result();
                }

                List<ObjectTypeSuggestionType> suggestedObjectTypes = extractObjectTypesFromStatusInfo(objectTypesSuggestionTypeStatusInfo);


                return new StatusInfoDataProvider(this, () -> suggestedObjectTypes);


//                return new MultivalueContainerListDataProvider<>(
//                        SmartSuggestedObjectTypeRadioTileTable.this,
//                        Model.of(),
//                        objectTypeSuggestionTypeModel) {
//                };

//
            }

            @Override
            protected SmartSuggestObjectTypeTileModel<ObjectTypeSuggestionType> createTileObject(ObjectTypeSuggestionType objectClassWrapper) {
                return new SmartSuggestObjectTypeTileModel<>(objectClassWrapper);
            }

            @Override
            protected Component createTile(String id, IModel<SmartSuggestObjectTypeTileModel<ObjectTypeSuggestionType>> model) {
                return new SmartSuggestObjectTypeTilePanel<>(id, model, selectedTileModel);
            }
        };
    }

    private @NotNull List<IColumn<ObjectTypeSuggestionType, String>> initColumns() {
        List<IColumn<ObjectTypeSuggestionType, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<ObjectTypeSuggestionType> rowModel) {
                return GuiDisplayTypeUtil.createDisplayType(GuiStyleConstants.CLASS_ICON_WIZARD);
            }
        });

        return columns;
    }

    @SuppressWarnings("unchecked")
    private TileTablePanel<SmartSuggestObjectTypeTileModel<StatusInfo<ObjectTypeSuggestionType>>, StatusInfo<ObjectTypeSuggestionType>> getTable() {
        return (TileTablePanel<SmartSuggestObjectTypeTileModel<StatusInfo<ObjectTypeSuggestionType>>, StatusInfo<ObjectTypeSuggestionType>>)
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

    public IModel<ObjectTypeSuggestionType> getSelected() {
        return selectedTileModel;
    }

    protected void onSelectionPerformed(IModel<ObjectTypeSuggestionType> selectedTileModel, AjaxRequestTarget target) {
        // Override this method to handle selection changes
    }
}
