/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.component.NumberFormatSelectorPanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;

import org.jetbrains.annotations.Nullable;

public class RoleAnalysisClusteringAttributeTable extends BasePanel<PrismContainerWrapper<ClusteringAttributeRuleType>> {

    private static final String ID_DATATABLE = "datatable";
    boolean isRoleMode;

    public RoleAnalysisClusteringAttributeTable(
            @NotNull String id,
            @NotNull IModel<PrismContainerWrapper<ClusteringAttributeRuleType>> rulesModel,
            boolean isRoleMode) {
        super(id, rulesModel);
        this.isRoleMode = isRoleMode;
        //TODO use multivalue container panel instead
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        IModel<Search<ClusteringAttributeRuleType>> searchModel = createSearchModel();

        MultivalueContainerListDataProvider<ClusteringAttributeRuleType> provider = new MultivalueContainerListDataProvider<>(
                this, searchModel, () -> getModel().getObject()
                .getValues()
                .stream()
                .filter(v -> v.getStatus() != ValueStatus.DELETED)
                .toList());

        BoxedTablePanel<PrismContainerValueWrapper<ClusteringAttributeRuleType>> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns()) {

            @Override
            protected @NotNull WebMarkupContainer createButtonToolbar(String id) {
                AjaxIconButton refreshIcon = new AjaxIconButton(id, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                        createStringResource("MainObjectListPanel.refresh")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefresh(target);
                    }
                };
                refreshIcon.add(AttributeModifier.append("class", "btn btn-default btn-sm"));
                return refreshIcon;
            }
        };
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(10);
        table.enableSavePageSize();

        add(table);
    }

    private @NotNull IModel<Search<ClusteringAttributeRuleType>> createSearchModel() {
        return () -> {
            SearchBuilder<ClusteringAttributeRuleType> searchBuilder = new SearchBuilder<>(ClusteringAttributeRuleType.class)
                    .modelServiceLocator(getPageBase());
            return searchBuilder.build();
        };
    }

    public List<IColumn<PrismContainerValueWrapper<ClusteringAttributeRuleType>, String>> initColumns() {

        Class<? extends ObjectType> parentType = isRoleMode ? RoleType.class : UserType.class;

        PrismObjectDefinition<? extends ObjectType> parentDefinition = PrismContext.get()
                .getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(parentType);

        List<IColumn<PrismContainerValueWrapper<ClusteringAttributeRuleType>, String>> columns = new ArrayList<>();

        initIconColumn(columns);

        initIdentifierColumn(columns, parentDefinition);

        initWeightColumn(columns);

        initSimilarityColumn(columns);

        return columns;
    }

    private static void initIconColumn(
            @NotNull List<IColumn<PrismContainerValueWrapper<ClusteringAttributeRuleType>, String>> columns) {

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<ClusteringAttributeRuleType>> rowModel) {
                return createDisplayType(GuiStyleConstants.CLASS_TASK_ACTIVITY_ICON);
            }
        });
    }

    private void initIdentifierColumn(
            @NotNull List<IColumn<PrismContainerValueWrapper<ClusteringAttributeRuleType>, String>> columns,
            @NotNull PrismObjectDefinition<? extends ObjectType> parentDefinition) {

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ClusteringAttributeRuleType>>> item,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ClusteringAttributeRuleType>> rowModel) {
                PrismContainerValueWrapper<ClusteringAttributeRuleType> object = rowModel.getObject();
                ItemPathType pathType = object.getRealValue().getPath();
                String displayName = getItemDisplayName(parentDefinition, pathType);

                item.add(new Label(componentId, displayName));
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId,
                        createStringResource("RoleAnalysisClusteringAttributeTable.column.title.identifier"));
            }

        });
    }

    private void initSimilarityColumn(
            @NotNull List<IColumn<PrismContainerValueWrapper<ClusteringAttributeRuleType>, String>> columns) {

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ClusteringAttributeRuleType>>> item,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ClusteringAttributeRuleType>> rowModel) {
                PrismContainerValueWrapper<ClusteringAttributeRuleType> prismContainerValueObjectWrapper = rowModel.getObject();
                if (prismContainerValueObjectWrapper != null) {
                    PrismPropertyWrapperModel<ClusteringAttributeRuleType, Double> propertyModel = PrismPropertyWrapperModel
                            .fromContainerValueWrapper(rowModel, ClusteringAttributeRuleType.F_SIMILARITY);
                    ItemRealValueModel<Double> realValueModel = new ItemRealValueModel<>(
                            new PropertyModel<>(propertyModel, "value"));

                    NumberFormatSelectorPanel field = new NumberFormatSelectorPanel(componentId,
                            realValueModel) {

                        @Override
                        public @NotNull RangeValidator<Double> getRangeValidator() {
                            return RangeValidator.range(0.0, 100.0);
                        }

                        @Override
                        public double stepValue() {
                            return 1.0;
                        }

                        @Override
                        public @NotNull DisplayType getImage() {
                            DisplayType displayType = new DisplayType();
                            IconType iconType = new IconType();
                            iconType.setCssClass("fa fa-percent");
                            displayType.setIcon(iconType);
                            return displayType;
                        }
                    };

                    field.add(new EnableBehaviour(() -> isEditable()
                            && isMultivaluedAttribute(prismContainerValueObjectWrapper)));
                    item.add(field);
                }
            }

            private static Boolean isMultivaluedAttribute(
                    @NotNull PrismContainerValueWrapper<ClusteringAttributeRuleType> prismContainerValueObjectWrapper) {
                return prismContainerValueObjectWrapper.getRealValue().getIsMultiValue();
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId,
                        createStringResource("RoleAnalysisClusteringAttributeTable.column.title.similarity"));
            }

        });
    }

    private void initWeightColumn(
            @NotNull List<IColumn<PrismContainerValueWrapper<ClusteringAttributeRuleType>, String>> columns) {

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ClusteringAttributeRuleType>>> item,
                    String componentId,
                    IModel<PrismContainerValueWrapper<ClusteringAttributeRuleType>> rowModel) {
                if (rowModel.getObject() != null) {
                    PrismPropertyWrapperModel<ClusteringAttributeRuleType, Double> propertyModel = PrismPropertyWrapperModel
                            .fromContainerValueWrapper(rowModel, ClusteringAttributeRuleType.F_WEIGHT);
                    ItemRealValueModel<Double> realValueModel = new ItemRealValueModel<>(
                            new PropertyModel<>(propertyModel, "value"));

                    NumberFormatSelectorPanel field = new NumberFormatSelectorPanel(componentId, realValueModel) {

                        @Override
                        public @NotNull DisplayType getImage() {
                            DisplayType displayType = new DisplayType();
                            IconType iconType = new IconType();
                            iconType.setCssClass("fa fa-cube");
                            displayType.setIcon(iconType);
                            return displayType;
                        }
                    };

                    field.add(new EnableBehaviour(() -> isEditable()));
                    item.add(field);
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(
                        componentId,
                        createStringResource("RoleAnalysisClusteringAttributeTable.column.title.weight"));
            }

        });
    }

    protected static <O extends Objectable> @NotNull String getItemDisplayName(
            @NotNull PrismObjectDefinition<O> parentDefinition,
            @Nullable ItemPathType pathType) {

        if (pathType == null) {
            return "N/A";
        }

        ItemDefinition<?> itemDefinition = parentDefinition.findItemDefinition(pathType.getItemPath());

        String displayName = itemDefinition.getDisplayName();
        return displayName != null ? LocalizationUtil.translate(displayName)
                : "N/A";
    }

    @Override
    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected DataTable<?, ?> getDataTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected BoxedTablePanel<?> getTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }

    public boolean isEditable() {
        return true;
    }
}
