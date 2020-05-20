/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public SearchPropertiesConfigPanel(String id){
        super(id, null);
    }

    @Override
    protected void initConfigurationPanel(WebMarkupContainer configPanel){
        WebMarkupContainer propertyConfigContainer = new WebMarkupContainer(ID_PROPERTY_CONFIG_CONTAINER);
        propertyConfigContainer.setOutputMarkupId(true);
        configPanel.add(propertyConfigContainer);

        DropDownChoicePanel<Property> propertyChoicePanel = new DropDownChoicePanel<Property>(ID_PROPERTY_CHOICE,
                Model.of(), Model.ofList(getAvailablePropertiesList()), new IChoiceRenderer<Property>() {

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
        });
        propertyChoicePanel.setOutputMarkupId(true);
        propertyConfigContainer.add(propertyChoicePanel);

        TextPanel valuePanel = new TextPanel(ID_PROPERTY_VALUE, Model.of());
        valuePanel.setOutputMarkupId(true);
        propertyConfigContainer.add(valuePanel);

        DropDownChoicePanel<SearchConfigDto.FilterType> filterChoice = WebComponentUtil.createEnumPanel(ID_FILTER,
                Model.ofList(Arrays.asList(SearchConfigDto.FilterType.values())), Model.of(),
                SearchPropertiesConfigPanel.this, true, "Select"); //todo allow null?
        filterChoice.setOutputMarkupId(true);
        propertyConfigContainer.add(filterChoice);

        DropDownChoicePanel<SearchConfigDto.MatchingRule> matchingRuleChoice = WebComponentUtil.createEnumPanel(ID_MATCHING_RULE,
                Model.ofList(Arrays.asList(SearchConfigDto.MatchingRule.values())), Model.of(),
                SearchPropertiesConfigPanel.this, true, "Select");  //todo allow null?
        matchingRuleChoice.setOutputMarkupId(true);
        propertyConfigContainer.add(matchingRuleChoice);

        CheckBoxPanel negationChoice = new CheckBoxPanel(ID_NEGATION, Model.of());
        negationChoice.setOutputMarkupId(true);
        propertyConfigContainer.add(negationChoice);

        AjaxButton addButton = new AjaxButton(ID_ADD_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        addButton.setOutputMarkupId(true);
        propertyConfigContainer.add(addButton);


    }

    private List<Property> getAvailablePropertiesList(){
        PrismObjectDefinition objectDef = SearchFactory.findObjectDefinition(getObjectClass(), null, getPageBase());
        List<SearchItemDefinition> availableDefs = SearchFactory.getAvailableDefinitions(objectDef, true);
        List<Property> propertiesList = new ArrayList<>();
        availableDefs.forEach(searchItemDef -> propertiesList.add(new Property(searchItemDef.getDef())));
        return propertiesList;
    }

    public int getWidth(){
        return 80;
    }

    public int getHeight(){
        return 80;
    }

    @Override
    public String getWidthUnit(){
        return "%";
    }

    @Override
    public String getHeightUnit(){
        return "%";
    }

    public StringResourceModel getTitle(){
        return createStringResource("TypedAssignablePanel.selectObjects");
    }

    public Component getComponent(){
        return this;
    }
}
