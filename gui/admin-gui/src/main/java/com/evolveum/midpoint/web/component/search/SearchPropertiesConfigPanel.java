/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.component.search.SearchableItemsDefinitions;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathComparatorUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.filter.BasicSearchFilter;
import com.evolveum.midpoint.web.component.search.filter.ValueSearchFilterItem;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableListDataProvider;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author Kateryna Honchar
 */
public class SearchPropertiesConfigPanel<O extends ObjectType> extends AbstractSearchConfigurationPanel<BasicSearchFilter<O>, O> implements Popupable {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PROPERTY_CONFIG_CONTAINER = "propertyConfigContainer";
    private static final String ID_PROPERTY_CHOICE = "propertyChoice";
    private static final String ID_PROPERTIES_TABLE = "propertiesTable";
    private static final String ID_ADD_BUTTON = "addButton";

    private SelectableListDataProvider<SelectableBean<ValueSearchFilterItem>, ValueSearchFilterItem> provider;
    private Property selectedPropertyChoice = null;

    public SearchPropertiesConfigPanel(String id, IModel<BasicSearchFilter<O>> searchModel, LoadableModel<Class<O>> typeModel) {
        super(id, searchModel, typeModel);
    }

    @Override
    protected void initConfigurationPanel(WebMarkupContainer configPanel) {
        provider = new SelectableListDataProvider<>(getPageBase(), getSearchFilterItemModel());

        setOutputMarkupId(true);

        WebMarkupContainer propertyConfigContainer = new WebMarkupContainer(ID_PROPERTY_CONFIG_CONTAINER);
        propertyConfigContainer.setOutputMarkupId(true);
        configPanel.add(propertyConfigContainer);

        DropDownChoicePanel<Property> propertyChoicePanel = new DropDownChoicePanel<Property>(ID_PROPERTY_CHOICE,
                getDefaultPropertyChoiceModel(), getAvailablePropertiesListModel(), new IChoiceRenderer<Property>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(Property property) {
                return property.getName();
            }

            @Override
            public String getIdValue(Property property, int index) {
                return Integer.toString(index);
            }

            @Override
            public Property getObject(String id, IModel<? extends List<? extends Property>> choices) {
                return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
            }
        }) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String getNullValidDisplayValue() {
                return getPageBase().createStringResource("SearchPropertiesConfigPanel.selectProperty").getString();
            }
        };
        propertyChoicePanel.setOutputMarkupId(true);
        propertyChoicePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        propertyConfigContainer.add(propertyChoicePanel);

        AjaxButton addButton = new AjaxButton(ID_ADD_BUTTON) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                propertyAddedPerformed(ajaxRequestTarget);
            }
        };
        addButton.setOutputMarkupId(true);
        propertyConfigContainer.add(addButton);

        initTable(configPanel);
    }

    private IModel<Property> getDefaultPropertyChoiceModel() {
        return new IModel<Property>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public Property getObject() {
                if (selectedPropertyChoice == null) {
                    List<Property> availablePropertiesList = getAvailablePropertiesListModel().getObject();
                    if (CollectionUtils.isNotEmpty(availablePropertiesList)) {
                        selectedPropertyChoice = availablePropertiesList.get(0);
                    }
                }
                return selectedPropertyChoice;
            }

            public void setObject(Property property) {
                selectedPropertyChoice = property;
            }
        };
    }

    private void initTable(WebMarkupContainer configPanel) {
        List<IColumn<SelectableBean<ValueSearchFilterItem>, String>> columns = getTableColumns();
        BoxedTablePanel<SelectableBean<ValueSearchFilterItem>> table =
                new BoxedTablePanel<SelectableBean<ValueSearchFilterItem>>(ID_PROPERTIES_TABLE, provider, columns) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected WebMarkupContainer createHeader(String headerId) {
                        return new WebMarkupContainer(headerId);
                    }

                    @Override
                    public String getAdditionalBoxCssClasses() {
                        return null;
                    }

                    @Override
                    protected WebMarkupContainer createButtonToolbar(String id) {
                        AjaxButton addRowButton = new AjaxButton(id) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                propertyAddedPerformed(ajaxRequestTarget);
                            }
                        };
                        addRowButton.setOutputMarkupId(true);
                        addRowButton.add(AttributeAppender.append("class", "btn btn-sm btn-default fa fa-plus"));
                        addRowButton.add(AttributeAppender.append("style", "color: green;"));
                        return addRowButton;
                    }

                    @Override
                    protected boolean hideFooterIfSinglePage() {
                        return true;
                    }

                    @Override
                    public int getAutoRefreshInterval() {
                        return 0;
                    }

                    @Override
                    public boolean isAutoRefreshEnabled() {
                        return false;
                    }
                };
        table.setOutputMarkupId(true);
        configPanel.add(table);
    }

    private List<IColumn<SelectableBean<ValueSearchFilterItem>, String>> getTableColumns() {
        List<IColumn<SelectableBean<ValueSearchFilterItem>, String>> columns = new ArrayList<>();

        CheckBoxHeaderColumn<SelectableBean<ValueSearchFilterItem>> checkboxColumn = new CheckBoxHeaderColumn<>();
        columns.add(checkboxColumn);

        IColumn<SelectableBean<ValueSearchFilterItem>, String> propertyColumn = new PropertyColumn<SelectableBean<ValueSearchFilterItem>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.property"),
                "value." + ValueSearchFilterItem.F_PROPERTY_NAME) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return getColumnStyleClass();
            }
        };
        columns.add(propertyColumn);

        IColumn<SelectableBean<ValueSearchFilterItem>, String> valueColumn = new AbstractColumn<SelectableBean<ValueSearchFilterItem>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.value")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ValueSearchFilterItem>>> item, String id, IModel<SelectableBean<ValueSearchFilterItem>> rowModel) {
                item.add(new SwitchablePropertyValuePanel(id, rowModel));
            }

            @Override
            public String getCssClass() {
                return "max-width-column";
            }
        };
        columns.add(valueColumn);

        IColumn<SelectableBean<ValueSearchFilterItem>, String> filterColumn = new AbstractColumn<SelectableBean<ValueSearchFilterItem>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.filter")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ValueSearchFilterItem>>> item, String id, IModel<SelectableBean<ValueSearchFilterItem>> rowModel) {
                List<ValueSearchFilterItem.FilterName> availableFilterNames =
                        rowModel.getObject().getValue().getAvailableFilterNameList();
                DropDownChoicePanel<ValueSearchFilterItem.FilterName> filterPanel = WebComponentUtil.createEnumPanel(id,
                        Model.ofList(availableFilterNames),
                        new PropertyModel<>(rowModel, "value." + ValueSearchFilterItem.F_FILTER_TYPE_NAME),
                        SearchPropertiesConfigPanel.this, false,
                        getPageBase().createStringResource("SearchPropertiesConfigPanel.selectFilter").getString());
                filterPanel.setOutputMarkupId(true);
                filterPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                filterPanel.getBaseFormComponent().add(new EnableBehaviour(() -> availableFilterNames.size() > 1));
                item.add(filterPanel);
            }

            @Override
            public String getCssClass() {
                return getColumnStyleClass();
            }
        };
        columns.add(filterColumn);

        IColumn<SelectableBean<ValueSearchFilterItem>, String> matchingRuleColumn = new AbstractColumn<SelectableBean<ValueSearchFilterItem>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.matchingRule")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ValueSearchFilterItem>>> item, String id, IModel<SelectableBean<ValueSearchFilterItem>> rowModel) {
                DropDownChoicePanel<ValueSearchFilterItem.MatchingRule> matchingRulePanel = WebComponentUtil.createEnumPanel(id,
                        Model.ofList(rowModel.getObject().getValue().getAvailableMatchingRuleList()),
                        new PropertyModel<>(rowModel, "value." + ValueSearchFilterItem.F_MATCHING_RULE),
                        SearchPropertiesConfigPanel.this, true,
                        getPageBase().createStringResource("SearchPropertiesConfigPanel.selectMatchingRule").getString());
                matchingRulePanel.setOutputMarkupId(true);
                matchingRulePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                item.add(matchingRulePanel);
            }

            @Override
            public String getCssClass() {
                return getColumnStyleClass();
            }
        };
        columns.add(matchingRuleColumn);

        CheckBoxColumn<SelectableBean<ValueSearchFilterItem>> negationColumn = new CheckBoxColumn<SelectableBean<ValueSearchFilterItem>>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.applyNegotiation"),
                "value." + ValueSearchFilterItem.F_APPLY_NEGATION) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return "min-width-column";
            }

        };
        columns.add(negationColumn);

        InlineMenuButtonColumn<SelectableBean<ValueSearchFilterItem>> actionsColumn = new InlineMenuButtonColumn<SelectableBean<ValueSearchFilterItem>>
                (getTableMenuItems(), getPageBase()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return "min-width-column";
            }
        };
        columns.add(actionsColumn);

        return columns;
    }

    private List<InlineMenuItem> getTableMenuItems() {
        InlineMenuItem deleteMenuItem = new ButtonInlineMenuItem(createStringResource("PageBase.button.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<ValueSearchFilterItem>>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        propertyDeletedPerformed(getRowModel(), target);
                    }
                };
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
            }
        };
        return Collections.singletonList(deleteMenuItem);
    }

    private String getColumnStyleClass() {
        return "mid-width-column";
    }

    private LoadableModel<List<Property>> getAvailablePropertiesListModel() {
        return new LoadableModel<List<Property>>() {
            @Override
            protected List<Property> load() {
                Class<? extends Containerable> type = getType();

                if (ObjectType.class.isAssignableFrom(type)) {
                    Map<ItemPath, ItemDefinition<?>> availableDefs = new SearchableItemsDefinitions(type, getPageBase())
                            .createAvailableSearchItems();
                    List<Property> propertiesList = availableDefs.entrySet()
                            .stream()
                            .filter(searchItemDef -> !isPropertyAlreadyAdded(searchItemDef.getKey()))
                            .map(searchItemDef -> new Property(searchItemDef.getValue(), searchItemDef.getKey()))
                            .collect(Collectors.toList());

                    return propertiesList;
                }
                if (AuditEventRecordType.class.isAssignableFrom(type)) {
                    PrismContainerDefinition<AuditEventRecordType> auditDefs = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AuditEventRecordType.class);
                    List<Property> propertiesList = auditDefs.getDefinitions()
                            .stream()
                            .map(def -> new Property(def, def.getItemName()))
                            .collect(Collectors.toList());
                    return propertiesList;
                }
                return Collections.emptyList();
            }
        };
    }

    private boolean isPropertyAlreadyAdded(ItemPath itemName) {
        List<SelectableBean<ValueSearchFilterItem>> properties = new ArrayList<>();//provider.getAvailableData();
        for (SelectableBean<ValueSearchFilterItem> prop : properties) {
            if (ItemPathComparatorUtil.equivalent(prop.getValue().getPropertyPath(), itemName)) {
                return true;
            }
        }
        return false;
    }

    private LoadableModel<List<ValueSearchFilterItem>> getSearchFilterItemModel() {
        return new LoadableModel<List<ValueSearchFilterItem>>(true) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ValueSearchFilterItem> load() {
                BasicSearchFilter basicSearchFilter = getModelObject();
                if (basicSearchFilter == null) {
                    return new ArrayList<>();
                }
                return basicSearchFilter.getValueSearchFilterItems();
            }
        };
    }

    private void propertyAddedPerformed(AjaxRequestTarget target) {
        Property newPropertyValue = getPropertyChoicePanel().getBaseFormComponent().getModelObject();
        if (newPropertyValue != null) {
            getModelObject().addSearchFilterItem(createDefaultValueFilter(newPropertyValue));
            resetPropertyChoicePanelModel();
        }
        target.add(SearchPropertiesConfigPanel.this);
    }

    private void propertyDeletedPerformed(IModel<SelectableBean<ValueSearchFilterItem>> rowModel, AjaxRequestTarget target) {
        if (rowModel == null || rowModel.getObject() == null || rowModel.getObject().getValue() == null) {
            return;
        }
        getModelObject().deleteSearchFilterItem(rowModel.getObject().getValue().getPropertyDef());
        target.add(SearchPropertiesConfigPanel.this);
    }

    private DropDownChoicePanel<Property> getPropertyChoicePanel() {
        return (DropDownChoicePanel<Property>) get(createComponentPath(ID_CONFIGURATION_PANEL, ID_PROPERTY_CONFIG_CONTAINER, ID_PROPERTY_CHOICE));
    }

    private void resetPropertyChoicePanelModel() {
        getPropertyChoicePanel().getModel().setObject(null);
    }

    private ValueSearchFilterItem createDefaultValueFilter(Property property) {
        if (property == null) {
            return null;
        }
        return new ValueSearchFilterItem(property, false);
    }

    @Override
    protected void okButtonClicked(AjaxRequestTarget target) {
        ObjectFilter configuredFilter = null;
        if (getModelObject() != null && !getModelObject().getObjectFilterList().isEmpty()) {
            configuredFilter = getModelObject().buildObjectFilter();
        }
        filterConfiguredPerformed(configuredFilter, target);
    }

    @Override
    protected void cancelButtonClicked(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    protected void filterConfiguredPerformed(ObjectFilter configuredFilter, AjaxRequestTarget target) {
    }

    public int getWidth() {
        return 80;
    }

    public int getHeight() {
        return 800;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    public Component getContent() {
        return this;
    }

    public StringResourceModel getTitle() {
        return createStringResource("SearchPropertiesConfigPanel.title");
    }
}
