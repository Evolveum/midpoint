/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.prism.query.EqualFilter;

import com.evolveum.midpoint.prism.query.ObjectFilter;

import com.evolveum.midpoint.web.component.input.TextPanel;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.search.filter.BasicSearchFilter;
import com.evolveum.midpoint.web.component.search.filter.ValueSearchFilterItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author Kateryna Honchar
 */
public class SearchPropertiesConfigPanel<O extends ObjectType> extends AbstractSearchConfigurationPanel<BasicSearchFilter<O>, O> implements Popupable {
    private static final long serialVersionUID = 1L;

    private static final String ID_PROPERTY_CONFIG_CONTAINER = "propertyConfigContainer";
    private static final String ID_PROPERTY_CHOICE = "propertyChoice";
    private static final String ID_PROPERTY_VALUE = "propertyValue";
    private static final String ID_FILTER = "filter";
    private static final String ID_MATCHING_RULE = "matchingRule";
    private static final String ID_NEGATION = "negation";
    private static final String ID_PROPERTIES_TABLE = "propertiesTable";
    private static final String ID_ADD_BUTTON = "addButton";

    IModel<Property> propertyChoiceModel = Model.of();
    private SelectableListDataProvider<SelectableBean<ValueSearchFilterItem>, ValueSearchFilterItem> provider;

    public SearchPropertiesConfigPanel(String id, IModel<BasicSearchFilter<O>> searchModel, Class<O> type) {
        super(id, searchModel, type);
    }

    @Override
    protected void initConfigurationPanel(WebMarkupContainer configPanel) {
        WebMarkupContainer propertyConfigContainer = new WebMarkupContainer(ID_PROPERTY_CONFIG_CONTAINER);
        propertyConfigContainer.setOutputMarkupId(true);
        configPanel.add(propertyConfigContainer);

        DropDownChoicePanel<Property> propertyChoicePanel = new DropDownChoicePanel<Property>(ID_PROPERTY_CHOICE,
                propertyChoiceModel, Model.ofList(getAvailablePropertiesList()), new IChoiceRenderer<Property>() {

            private static final long serialVersionUID = 1L;

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
            private static final long serialVersionUID = 1L;

            @Override
            protected String getNullValidDisplayValue() {
                return getPageBase().createStringResource("SearchPropertiesConfigPanel.selectProperty").getString();
            }
        };
        propertyChoicePanel.setOutputMarkupId(true);
        propertyChoicePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        propertyConfigContainer.add(propertyChoicePanel);

        AjaxButton addButton = new AjaxButton(ID_ADD_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                propertyAddedPerformed(ajaxRequestTarget);
            }
        };
        addButton.setOutputMarkupId(true);
        propertyConfigContainer.add(addButton);

        initTable(configPanel);
    }

    private void initTable(WebMarkupContainer configPanel) {
        provider =
                new SelectableListDataProvider<SelectableBean<ValueSearchFilterItem>, ValueSearchFilterItem>(getPageBase(), getSearchFilterItemModel());
        List<IColumn<SelectableBean<ValueSearchFilterItem>, String>> columns = getTableColumns();
        BoxedTablePanel<SelectableBean<ValueSearchFilterItem>> table =
                new BoxedTablePanel<SelectableBean<ValueSearchFilterItem>>(ID_PROPERTIES_TABLE, provider, columns, null, 20) {
                    private static final long serialVersionUID = 1L;

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
                            private static final long serialVersionUID = 1L;

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
        configPanel.addOrReplace(table);
    }

    private List<IColumn<SelectableBean<ValueSearchFilterItem>, String>> getTableColumns() {
        List<IColumn<SelectableBean<ValueSearchFilterItem>, String>> columns = new ArrayList<>();

        CheckBoxHeaderColumn<SelectableBean<ValueSearchFilterItem>> checkboxColumn = new CheckBoxHeaderColumn<>();
        columns.add(checkboxColumn);

        IColumn<SelectableBean<ValueSearchFilterItem>, String> propertyColumn = new PropertyColumn<SelectableBean<ValueSearchFilterItem>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.property"),
                "value." + ValueSearchFilterItem.F_PROPERTY_NAME);
        columns.add(propertyColumn);

        IColumn<SelectableBean<ValueSearchFilterItem>, String> valueColumn = new AbstractColumn<SelectableBean<ValueSearchFilterItem>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.value")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ValueSearchFilterItem>>> item, String id, IModel<SelectableBean<ValueSearchFilterItem>> rowModel) {
                item.add(getPropertyValueField( id, rowModel));

            }
        };
        columns.add(valueColumn);

        IColumn<SelectableBean<ValueSearchFilterItem>, String> filterColumn = new AbstractColumn<SelectableBean<ValueSearchFilterItem>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.filter")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ValueSearchFilterItem>>> item, String id, IModel<SelectableBean<ValueSearchFilterItem>> rowModel) {
                DropDownChoicePanel<ValueSearchFilterItem.FilterName> filterPanel = WebComponentUtil.createEnumPanel(id,
                        Model.ofList(Arrays.asList(ValueSearchFilterItem.FilterName.values())),
                        new PropertyModel<>(rowModel, "value." + ValueSearchFilterItem.F_FILTER_NAME),
                        SearchPropertiesConfigPanel.this, false,
                        getPageBase().createStringResource("SearchPropertiesConfigPanel.selectFilter").getString());
                filterPanel.setOutputMarkupId(true);
                filterPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                item.add(filterPanel);
            }
        };
        columns.add(filterColumn);

        IColumn<SelectableBean<ValueSearchFilterItem>, String> matchingRuleColumn = new AbstractColumn<SelectableBean<ValueSearchFilterItem>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.matchingRule")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ValueSearchFilterItem>>> item, String id, IModel<SelectableBean<ValueSearchFilterItem>> rowModel) {
                DropDownChoicePanel<ValueSearchFilterItem.MatchingRule> matchingRulePanel = WebComponentUtil.createEnumPanel(id,
                        Model.ofList(Arrays.asList(ValueSearchFilterItem.MatchingRule.values())),
                        new PropertyModel<>(rowModel, "value." + ValueSearchFilterItem.F_MATCHING_RULE),
                        SearchPropertiesConfigPanel.this, true,
                        getPageBase().createStringResource("SearchPropertiesConfigPanel.selectMatchingRule").getString());
                matchingRulePanel.setOutputMarkupId(true);
                matchingRulePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                item.add(matchingRulePanel);
            }
        };
        columns.add(matchingRuleColumn);

        CheckBoxColumn<SelectableBean<ValueSearchFilterItem>> negationColumn = new CheckBoxColumn<SelectableBean<ValueSearchFilterItem>>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.applyNegotiation"),
                "value." + ValueSearchFilterItem.F_APPLY_NEGATION);
        columns.add(negationColumn);
        return columns;
    }

    private List<Property> getAvailablePropertiesList() {
        PrismObjectDefinition objectDef = SearchFactory.findObjectDefinition(getType(), null, getPageBase());
        List<SearchItemDefinition> availableDefs = SearchFactory.getAvailableDefinitions(objectDef, true);
        List<Property> propertiesList = new ArrayList<>();
        availableDefs.forEach(searchItemDef -> propertiesList.add(new Property(searchItemDef.getDef())));
        return propertiesList;
    }

    private LoadableModel<List<ValueSearchFilterItem>> getSearchFilterItemModel() {
        return new LoadableModel<List<ValueSearchFilterItem>>(true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<ValueSearchFilterItem> load() {
                BasicSearchFilter basicSearchFilter = getModelObject();
                if (basicSearchFilter == null){
                    return new ArrayList<>();
                }
                return basicSearchFilter.getValueSearchFilterItems();
            }
        };
    }

    private void propertyAddedPerformed(AjaxRequestTarget target) {
        Property newPropertyValue = propertyChoiceModel.getObject();
        if (newPropertyValue != null) {
            getModelObject().addSearchFilterItem(createDefaultValueFilter(newPropertyValue));
//            initTable((WebMarkupContainer) get(ID_CONFIGURATION_PANEL)); //todo don't re-init table!
        }
        target.add(SearchPropertiesConfigPanel.this);
    }

    private ValueSearchFilterItem createDefaultValueFilter(Property property) {
        if (property == null){
            return null;
        }
        ObjectFilter newFilter = getPageBase().getPrismContext().queryFor(getType())
                .item(property.getDefinition().getItemName())
                .isNull()
                .buildFilter();
        return new ValueSearchFilterItem(newFilter, false);
    }

    private Property getSelectedProperty() {
        DropDownChoicePanel<Property> propertyChoicePanel = (DropDownChoicePanel<Property>) get(getPageBase()
                .createComponentPath(ID_CONFIGURATION_PANEL, ID_PROPERTY_CONFIG_CONTAINER, ID_PROPERTY_CHOICE));
        return propertyChoicePanel.getModel().getObject();
    }

    private Component getPropertyValueField(String id, IModel<SelectableBean<ValueSearchFilterItem>> rowModel) {
        Component searchItemField = null;
        ValueSearchFilterItem valueSearchFilter = rowModel.getObject().getValue();
        if (valueSearchFilter.getFilter() == null){
            return new WebMarkupContainer(id);
        }
        ObjectFilter filter = valueSearchFilter.getFilter();
        if (filter instanceof ValueFilter) {
            ItemDefinition propertyDef = ((ValueFilter)filter).getDefinition();
            PrismValue propertyValue = valueSearchFilter.getValue();
            PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(propertyDef, getPageBase());

            if (propertyDef instanceof PrismReferenceDefinition) {
                searchItemField = new ReferenceValueSearchPanel(id, Model.of(
                        propertyValue != null ? (ObjectReferenceType) propertyValue.getRealValue() : null),
                        (PrismReferenceDefinition) propertyDef);
            } else if (propertyDef instanceof PrismPropertyDefinition) {
                List<DisplayableValue> allowedValues = new ArrayList<>();
                if (((PrismPropertyDefinition) propertyDef).getAllowedValues() != null) {
                    allowedValues.addAll(((PrismPropertyDefinition) propertyDef).getAllowedValues());
                }
                if (lookupTable != null) {
                    searchItemField = new AutoCompleteTextPanel<String>(id,
                            new PropertyModel<>(rowModel, "value." + ValueSearchFilterItem.F_VALUE + ".realValue"), String.class,
                            true, lookupTable.asObjectable()) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public Iterator<String> getIterator(String input) {
                            return WebComponentUtil.prepareAutoCompleteList(lookupTable.asObjectable(), input,
                                    ((PageBase) getPage()).getLocalizationService()).iterator();
                        }
                    };
                } else if (CollectionUtils.isNotEmpty(allowedValues)) {
                    searchItemField = new DropDownChoicePanel<DisplayableValue>(id,
                            new PropertyModel<>(rowModel, "value." + ValueSearchFilterItem.F_VALUE + ".realValue"),
                            Model.ofList(allowedValues), new IChoiceRenderer<DisplayableValue>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public Object getDisplayValue(DisplayableValue val) {
                            return val.getLabel();
                        }

                        @Override
                        public String getIdValue(DisplayableValue val, int index) {
                            return Integer.toString(index);
                        }

                        @Override
                        public DisplayableValue getObject(String id, IModel<? extends List<? extends DisplayableValue>> choices) {
                            return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
                        }
                    }, true);
                } else {
                    searchItemField = new TextPanel<String>(id, new PropertyModel<>(rowModel, "value." + ValueSearchFilterItem.F_VALUE + ".realValue"));

                }
            }
        }
        return searchItemField != null ? searchItemField : new WebMarkupContainer(id);
    }

    @Override
    protected void okButtonClicked(AjaxRequestTarget target){
        ObjectFilter configuredFilter = getModelObject().buildObjectFilter();
        filterConfiguredPerformed(configuredFilter, target);
    }

    protected void filterConfiguredPerformed(ObjectFilter configuredFilter, AjaxRequestTarget target){
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

    public StringResourceModel getTitle() {
        return createStringResource("SearchPropertiesConfigPanel.title");
    }

    public Component getComponent() {
        return this;
    }
}
