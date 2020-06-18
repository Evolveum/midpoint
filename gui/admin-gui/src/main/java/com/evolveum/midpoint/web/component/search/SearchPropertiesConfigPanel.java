/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

/**
 * @author Kateryna Honchar
 */
public abstract class SearchPropertiesConfigPanel extends AbstractSearchConfigurationPanel implements Popupable {
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

    public SearchPropertiesConfigPanel(String id, IModel<Search> searchModel) {
        super(id, searchModel);
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

//        TextPanel valuePanel = new TextPanel(ID_PROPERTY_VALUE, Model.of());
//        valuePanel.setOutputMarkupId(true);
//        propertyConfigContainer.add(valuePanel);
//
//        DropDownChoicePanel<SearchConfigDto.FilterType> filterChoice = WebComponentUtil.createEnumPanel(ID_FILTER,
//                Model.ofList(Arrays.asList(SearchConfigDto.FilterType.values())), Model.of(),
//                SearchPropertiesConfigPanel.this, true, "Select"); //todo allow null?
//        filterChoice.setOutputMarkupId(true);
//        propertyConfigContainer.add(filterChoice);
//
//        DropDownChoicePanel<SearchConfigDto.MatchingRule> matchingRuleChoice = WebComponentUtil.createEnumPanel(ID_MATCHING_RULE,
//                Model.ofList(Arrays.asList(SearchConfigDto.MatchingRule.values())), Model.of(),
//                SearchPropertiesConfigPanel.this, true, "Select");  //todo allow null?
//        matchingRuleChoice.setOutputMarkupId(true);
//        propertyConfigContainer.add(matchingRuleChoice);
//
//        CheckBoxPanel negationChoice = new CheckBoxPanel(ID_NEGATION, Model.of());
//        negationChoice.setOutputMarkupId(true);
//        propertyConfigContainer.add(negationChoice);

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
        SelectableListDataProvider<SelectableBean<SearchConfigDto>, SearchConfigDto> provider =
                new SelectableListDataProvider<SelectableBean<SearchConfigDto>, SearchConfigDto>(getPageBase(), getSearchConfigModel());
        List<IColumn<SelectableBean<SearchConfigDto>, String>> columns = getTableColumns();
        BoxedTablePanel<SelectableBean<SearchConfigDto>> table =
                new BoxedTablePanel<SelectableBean<SearchConfigDto>>(ID_PROPERTIES_TABLE, provider, columns, null, 20) {
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

    private List<IColumn<SelectableBean<SearchConfigDto>, String>> getTableColumns() {
        List<IColumn<SelectableBean<SearchConfigDto>, String>> columns = new ArrayList<>();

        CheckBoxHeaderColumn<SelectableBean<SearchConfigDto>> checkboxColumn = new CheckBoxHeaderColumn<>();
        columns.add(checkboxColumn);

        IColumn<SelectableBean<SearchConfigDto>, String> propertyColumn = new PropertyColumn<SelectableBean<SearchConfigDto>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.property"),
                "value." + SearchConfigDto.F_PROPERTY + "." + Property.F_NAME);
        columns.add(propertyColumn);

        IColumn<SelectableBean<SearchConfigDto>, String> valueColumn = new AbstractColumn<SelectableBean<SearchConfigDto>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.value")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<SearchConfigDto>>> item, String id, IModel<SelectableBean<SearchConfigDto>> rowModel) {
                TextPanel valuePanel = new TextPanel(id, new PropertyModel(rowModel, "value." + SearchConfigDto.F_VALUE));
                valuePanel.setOutputMarkupId(true);
                valuePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                item.add(valuePanel);

            }
        };
        columns.add(valueColumn);

        IColumn<SelectableBean<SearchConfigDto>, String> filterColumn = new AbstractColumn<SelectableBean<SearchConfigDto>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.filter")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<SearchConfigDto>>> item, String id, IModel<SelectableBean<SearchConfigDto>> rowModel) {
                DropDownChoicePanel<SearchConfigDto.FilterType> filterPanel = WebComponentUtil.createEnumPanel(id,
                        Model.ofList(Arrays.asList(SearchConfigDto.FilterType.values())),
                        new PropertyModel<>(rowModel, "value." + SearchConfigDto.F_FILTER_TYPE),
                        SearchPropertiesConfigPanel.this, true,
                        getPageBase().createStringResource("SearchPropertiesConfigPanel.selectFilter").getString());
                filterPanel.setOutputMarkupId(true);
                filterPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                item.add(filterPanel);
            }
        };
        columns.add(filterColumn);

        IColumn<SelectableBean<SearchConfigDto>, String> matchingRuleColumn = new AbstractColumn<SelectableBean<SearchConfigDto>, String>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.matchingRule")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<SearchConfigDto>>> item, String id, IModel<SelectableBean<SearchConfigDto>> rowModel) {
                DropDownChoicePanel<SearchConfigDto.FilterType> matchingRulePanel = WebComponentUtil.createEnumPanel(id,
                        Model.ofList(Arrays.asList(SearchConfigDto.FilterType.values())),
                        new PropertyModel<>(rowModel, "value." + SearchConfigDto.F_MATCHING_RULE),
                        SearchPropertiesConfigPanel.this, true,
                        getPageBase().createStringResource("SearchPropertiesConfigPanel.selectMatchingRule").getString());
                matchingRulePanel.setOutputMarkupId(true);
                matchingRulePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                item.add(matchingRulePanel);
            }
        };
        columns.add(matchingRuleColumn);

        CheckBoxColumn<SelectableBean<SearchConfigDto>> negationColumn = new CheckBoxColumn<SelectableBean<SearchConfigDto>>(getPageBase()
                .createStringResource("SearchPropertiesConfigPanel.table.column.applyNegotiation"),
                "value." + SearchConfigDto.F_NEGATION);
        columns.add(negationColumn);
        return columns;
    }

    private List<Property> getAvailablePropertiesList() {
        PrismObjectDefinition objectDef = SearchFactory.findObjectDefinition(getObjectClass(), null, getPageBase());
        List<SearchItemDefinition> availableDefs = SearchFactory.getAvailableDefinitions(objectDef, true);
        List<Property> propertiesList = new ArrayList<>();
        availableDefs.forEach(searchItemDef -> propertiesList.add(new Property(searchItemDef.getDef())));
        return propertiesList;
    }

    private LoadableModel<List<SearchConfigDto>> getSearchConfigModel() {
        return new LoadableModel<List<SearchConfigDto>>(true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<SearchConfigDto> load() {
                List<SearchConfigDto> searchConfigDtos = new ArrayList<>();
                getModelObject().getItems().forEach(searchItem -> {
                    searchConfigDtos.add(SearchConfigDto.createSearchConfigDto(searchItem));
                });
                return searchConfigDtos;
            }
        };
    }

    private void propertyAddedPerformed(AjaxRequestTarget target) {
        Property newPropertyValue = propertyChoiceModel.getObject();
        if (newPropertyValue != null) {
            getModelObject().addItem(newPropertyValue.getDefinition());
            initTable((WebMarkupContainer) get(ID_CONFIGURATION_PANEL)); //todo don't re-init table!
            target.add(get(createComponentPath(ID_CONFIGURATION_PANEL, ID_PROPERTIES_TABLE)));
        }
        target.add(SearchPropertiesConfigPanel.this);
    }

    private Property getSelectedProperty() {
        DropDownChoicePanel<Property> propertyChoicePanel = (DropDownChoicePanel<Property>) get(getPageBase()
                .createComponentPath(ID_CONFIGURATION_PANEL, ID_PROPERTY_CONFIG_CONTAINER, ID_PROPERTY_CHOICE));
        return propertyChoicePanel.getModel().getObject();
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
