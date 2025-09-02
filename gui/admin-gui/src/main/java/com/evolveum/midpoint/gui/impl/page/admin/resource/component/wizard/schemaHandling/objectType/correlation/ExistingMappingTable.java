package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUsedFor;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundAttributeMappingsTable;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ExistingMappingTable<P extends Containerable> extends BasePanel<PrismContainerValueWrapper<P>> implements Popupable {

    private static final String ID_TABLE = "table";
    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";

    private static final String ID_CANCEL = "cancel";
    private static final String ID_ADD_SELECTED_MAPPINGS = "addSelectedMappings";
    private static final String ID_BUTTONS = "buttons";

    private Fragment footerFragment;

    public ExistingMappingTable(String id, IModel<PrismContainerValueWrapper<P>> valueModel) {
        super(id, valueModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Label textLabel = new Label(ID_TEXT, createStringResource("ExistingMappingTable.text"));
        textLabel.setOutputMarkupId(true);
        add(textLabel);

        Label subTextLabel = new Label(ID_SUBTEXT, createStringResource("ExistingMappingTable.subText"));
        subTextLabel.setOutputMarkupId(true);
        add(subTextLabel);

        initTable();
    }

    private void initTable() {
        InboundAttributeMappingsTable<P> table = new InboundAttributeMappingsTable<>(
                ID_TABLE,
                getModel(),
                null) {

            @Override
            protected ItemName getItemNameOfContainerWithMappings() {
                return ResourceObjectTypeDefinitionType.F_ATTRIBUTE;
            }

            @Override
            protected void customProcessNewRowItem(Item<PrismContainerValueWrapper<MappingType>> item,
                    IModel<PrismContainerValueWrapper<MappingType>> model) {
                super.customProcessNewRowItem(item, model);
                if (model.getObject().isSelected()) {
                    item.add(AttributeModifier.append("class", "table-primary"));
                }
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getAdditionalFooterCssClasses() {
                return "bg-white border-top";
            }

            @Override
            protected Component createHeader(String headerId) {
                return initSearch(headerId);
            }

            @Override
            protected MappingUsedFor getSelectedTypeOfMappings() {
                return MappingUsedFor.ALL;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return super.createInlineMenu();
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Contract(pure = true)
            @Override
            protected @Nullable IColumn<PrismContainerValueWrapper<MappingType>, String> createActionsColumn() {
                return null;
            }

            @Override
            protected @NotNull List<IColumn<PrismContainerValueWrapper<MappingType>, String>> createDefaultColumns() {
                List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

                columns.add(new CheckBoxHeaderColumn<>() {
                    @Override
                    protected void onUpdateRow(Item<ICellPopulator<PrismContainerValueWrapper<MappingType>>> cellItem,
                            AjaxRequestTarget target, DataTable table, IModel<PrismContainerValueWrapper<MappingType>> rowModel,
                            IModel<Boolean> selected) {
                        super.onUpdateRow(cellItem, target, table, rowModel, selected);
                        target.add(getFooter());
                        target.add(getTable().getDataTableContainer());
                    }

                    @Override
                    protected IModel<Boolean> getHeaderDisplayModel() {
                        int selectedObjectsCount = ExistingMappingTable.this.getTable().getSelectedObjectsCount();
                        boolean initialState = selectedObjectsCount == getDataProvider().size();
                        return new Model<>(initialState);
                    }

                    @Override
                    protected void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
                        super.onUpdateHeader(target, selected, table);
                        target.add(getFooter());
                        target.add(getTable().getDataTableContainer());
                    }
                });

                IModel<PrismContainerDefinition<MappingType>> mappingTypeDef =
                        getMappingTypeDefinition();

                columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                        mappingTypeDef,
                        MappingType.F_NAME,
                        AbstractItemWrapperColumn.ColumnType.STRING,
                        getPageBase()) {

                    @Override
                    public String getCssClass() {
                        return "col-xl-2 col-lg-2 col-md-2";
                    }
                });

                columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                        mappingTypeDef,
                        ResourceAttributeDefinitionType.F_REF,
                        AbstractItemWrapperColumn.ColumnType.STRING,
                        getPageBase()) {
                    @Override
                    protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<MappingType>> mainModel) {
                        return new Label(componentId, getPageBase().createStringResource(
                                getRefColumnPrefix() + getMappingType().name() + "." + getItemNameOfRefAttribute()));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<MappingType>>> cellItem, String componentId,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        initMappingPathLabel(componentId, cellItem, rowModel);
                    }
                });

                columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                        mappingTypeDef,
                        MappingType.F_TARGET,
                        AbstractItemWrapperColumn.ColumnType.STRING,
                        getPageBase()) {
                    @Override
                    public Component getHeader(String componentId) {
                        return new Label(componentId, getPageBase().createStringResource("MappingTable.column.target"));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<MappingType>>> cellItem, String componentId,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        initMappingPathLabel(componentId, cellItem, rowModel);
                    }
                });

                columns.add(new IconColumn<>(createStringResource("MappingTable.column.help")) {
                    @Override
                    protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        return new DisplayType().beginIcon().cssClass(GuiStyleConstants.CLASS_INFO_CIRCLE + " text-info").end();
                    }

                    @Override
                    public void populateItem(
                            Item<ICellPopulator<PrismContainerValueWrapper<MappingType>>> cellItem,
                            String componentId,
                            IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                        MappingType realValue = rowModel.getObject().getRealValue();
                        String description = realValue.getDescription();
                        String tooltip = "";

                        if (description != null && !description.isEmpty()) {
                            tooltip = description;
                        }

                        ImagePanel panel = new ImagePanel(componentId, () -> getIconDisplayType(rowModel));
                        panel.setIconRole(ImagePanel.IconRole.IMAGE);
                        panel.add(AttributeModifier.replace("title", tooltip));
                        panel.add(new TooltipBehavior());
                        cellItem.add(panel);
                    }
                });
                return columns;
            }

            private static void initMappingPathLabel(
                    @NotNull String componentId,
                    @NotNull Item<ICellPopulator<PrismContainerValueWrapper<MappingType>>> cellItem,
                    @NotNull IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                MappingType realValue = rowModel.getObject().getRealValue();
                VariableBindingDefinitionType target = realValue.getTarget();
                ItemPathType path = target.getPath();
                Label label = new Label(componentId, path != null ? path.toString() : "");
                cellItem.add(label);
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    @Override
    public @NotNull Fragment getFooter() {
        if (footerFragment == null) {
            footerFragment = initFooter();
        }
        return footerFragment;
    }

    @SuppressWarnings("unchecked")
    private InboundAttributeMappingsTable<P> getTable() {
        return (InboundAttributeMappingsTable<P>) get(ID_TABLE);
    }

    private @NotNull Fragment initFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxIconButton addSelectedMappingsButton = new AjaxIconButton(ID_ADD_SELECTED_MAPPINGS,
                Model.of("fa fas fa-link"),
                () -> {
                    int count = getTable().getSelectedObjectsCount();
                    return createStringResource("ExistingMappingTable.addMappings", count)
                            .getString();
                }) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                onAddSelectedMappings(target, getTable().getSelectedObjects());
            }
        };
        addSelectedMappingsButton.showTitleAsLabel(true);
        addSelectedMappingsButton.setOutputMarkupId(true);
        footer.add(addSelectedMappingsButton);

        footer.add(new AjaxLink<>(ID_CANCEL) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        });

        footer.setOutputMarkupId(true);
        return footer;

    }

    protected void onAddSelectedMappings(AjaxRequestTarget target, List<PrismContainerValueWrapper<MappingType>> selectedObjectsCount) {
        // Override
    }

    @Override
    public int getWidth() {
        return 70;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("ExistingMappingTable.title");
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of("fa fas fa-link");
    }

    @Override
    public Component getContent() {
        return this;
    }

}
