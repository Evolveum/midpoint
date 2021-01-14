/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author lskublik
 */
public abstract class SearchSpecialItemPanel<T extends Serializable> extends AbstractSearchItemPanel<SpecialSearchItem> {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_ITEM_FIELD = "searchItemField";
    private static final String ID_SEARCH_ITEM_CONTAINER = "searchItemContainer";
    private static final String ID_SEARCH_ITEM_LABEL = "searchItemLabel";
    private static final String ID_HELP = "help";

    private final IModel<T> modelValue;

    public SearchSpecialItemPanel(String id, IModel<T> model) {
        super(id, null);
        this.modelValue = model;
    }

    public IModel<T> getModelValue() {
        return modelValue;
    }

    protected void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer searchItemContainer = new WebMarkupContainer(ID_SEARCH_ITEM_CONTAINER);
        searchItemContainer.setOutputMarkupId(true);
        add(searchItemContainer);

        IModel<String> labelModel = createLabelModel();
        Label searchItemLabel = new Label(ID_SEARCH_ITEM_LABEL, labelModel);
        searchItemLabel.setOutputMarkupId(true);
        searchItemLabel.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(labelModel.getObject())));

        IModel<String> titleModel = createTitleModel();
        if (StringUtils.isNotEmpty(titleModel.getObject())) {
            searchItemLabel.add(AttributeAppender.append("title", titleModel));
        }
        searchItemContainer.add(searchItemLabel);

        Label help = new Label(ID_HELP);
        IModel<String> helpModel = createHelpModel();
        help.add(AttributeModifier.replace("title",createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new InfoTooltipBehavior(){
            @Override
            public String getDataPlacement() {
                return "left";
            }
        });
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
        searchItemContainer.add(help);

        WebMarkupContainer inputPanel = initSearchItemField(ID_SEARCH_ITEM_FIELD);
        if (inputPanel instanceof InputPanel && !(inputPanel instanceof AutoCompleteTextPanel)) {
            ((InputPanel) inputPanel).getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
        }
        inputPanel.setOutputMarkupId(true);
        searchItemContainer.add(inputPanel);
    }

    protected IModel<String> createHelpModel(){
        return Model.of();
    }

    protected abstract WebMarkupContainer initSearchItemField(String id);

    protected boolean canRemoveSearchItem() {
        return true;
    }

    protected IModel<String> createLabelModel() {
        return Model.of();
    }

    protected IModel<String> createTitleModel() {
        return Model.of();
    }
}
