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
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.input.CheckPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItemPanel<S extends SearchItem, T extends Serializable> extends BasePanel<S> {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_ITEM_CONTAINER = "searchItemContainer";
    private static final String ID_SEARCH_ITEM_LABEL = "searchItemLabel";
    private static final String ID_SEARCH_ITEM_FIELD = "searchItemField";
    private static final String ID_REMOVE_BUTTON = "removeButton";

    public SearchItemPanel(String id, IModel<S> model) {
        super(id, model);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        if (getModelObject() instanceof PropertySearchItem) {
            PropertySearchItem<T> item = (PropertySearchItem<T>) getModelObject();
            if (!item.isEditWhenVisible()) {
                return;
            }
            item.setEditWhenVisible(false);
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
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

        initSearchItemField(searchItemContainer);

        AjaxSubmitButton removeButton = new AjaxSubmitButton(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                deletePerformed(target);
            }
        };
        removeButton.add(new VisibleBehaviour(() -> !(getModelObject() instanceof FilterSearchItem) && canRemoveSearchItem()));
        removeButton.setOutputMarkupId(true);
        searchItemContainer.add(removeButton);
    }

    private void initSearchItemField(WebMarkupContainer searchItemContainer) {
        Component searchItemField;
        if (getModelObject() instanceof FilterSearchItem) {
            searchItemField = new CheckPanel(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), FilterSearchItem.F_APPLY_FILTER));
            ((CheckPanel) searchItemField).getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                    searchPerformed(ajaxRequestTarget);
                }
            });
            searchItemField.add(AttributeModifier.append("class", "pull-right"));
            searchItemField.add(AttributeAppender.append("style", "margin-top: 3px;"));
            ((InputPanel) searchItemField).getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        } else {
            PropertySearchItem<T> item = (PropertySearchItem<T>) getModelObject();
            IModel<List<DisplayableValue<T>>> choices = null;
            switch (item.getType()) {
                case REFERENCE:
                    searchItemField = new ReferenceValueSearchPanel(ID_SEARCH_ITEM_FIELD,
                            new PropertyModel<>(getModel(), "value.value"),
                            (PrismReferenceDefinition) item.getDefinition());
                    break;
                case BOOLEAN:
                    choices = (IModel) createBooleanChoices();
                case ENUM:
                    if (choices == null) {
                        choices = new ListModel<>(item.getAllowedValues());
                    }
                    searchItemField = new DropDownChoicePanel<>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), "value.value"),
                            choices, new IChoiceRenderer<DisplayableValue>() {
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
                    break;
                case DATE:
                    searchItemField = new DateIntervalSearchPanel(ID_SEARCH_ITEM_FIELD,
                            new PropertyModel(getModel(), "fromDate"),
                            new PropertyModel(getModel(), "toDate"));
                    break;
                case ITEM_PATH:
                    searchItemField = new ItemPathSearchPanel(ID_SEARCH_ITEM_FIELD,
                            new PropertyModel(getModel(), "value.value"));
                    break;
                case TEXT:
                    PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(item.getDefinition(), getPageBase());
                    if (lookupTable != null) {
                        searchItemField = new AutoCompleteTextPanel<String>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), "value.value"), String.class,
                                true, lookupTable.asObjectable()) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public Iterator<String> getIterator(String input) {
                                return WebComponentUtil.prepareAutoCompleteList(lookupTable.asObjectable(), input,
                                        ((PageBase) getPage()).getLocalizationService()).iterator();
                            }
                        };

                        ((AutoCompleteTextPanel) searchItemField).getBaseFormComponent().add(new Behavior() {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public void bind(Component component) {
                                super.bind(component);

                                component.add(AttributeModifier.replace("onkeydown",
                                        Model.of(
                                                "if (event.keyCode == 13){"
                                                        + "var autocompletePopup = document.getElementsByClassName(\"wicket-aa-container\");"
                                                        + "if(autocompletePopup != null && autocompletePopup[0].style.display == \"none\"){"
                                                        + "$('[about=\"searchSimple\"]').click();}}"
                                        )));
                            }
                        });
                    } else {
                        searchItemField = new TextPanel<String>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), "value.value"));
                    }
                    break;
                default:
                    searchItemField = new TextPanel<String>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), "value"));
            }
            if (searchItemField instanceof InputPanel && !(searchItemField instanceof AutoCompleteTextPanel)) {
                ((InputPanel) searchItemField).getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                ((InputPanel) searchItemField).getBaseFormComponent().add(AttributeAppender.append("style", "width: 140px; max-width: 400px !important;"));
                ((InputPanel) searchItemField).getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            }
        }

        searchItemField.setOutputMarkupId(true);
        searchItemContainer.add(searchItemField);
    }

    protected boolean canRemoveSearchItem() {
        return false;
    }

    private IModel<List<DisplayableValue<Boolean>>> createBooleanChoices() {
        List<DisplayableValue<Boolean>> list = new ArrayList<>();
        list.add(new SearchValue<>(Boolean.TRUE, getString("Boolean.TRUE")));
        list.add(new SearchValue<>(Boolean.FALSE, getString("Boolean.FALSE")));
        return Model.ofList(list);
    }

    private IModel<String> createLabelModel() {
        SearchItem item = getModelObject();
        if (item == null) {
            return Model.of();
        }
        return Model.of(item.getName());
    }

    private IModel<String> createTitleModel() {
        SearchItem item = getModelObject();
        if (item == null) {
            return Model.of();
        }
        return Model.of(item.getTitle(getPageBase()));
    }

    protected void searchPerformed(AjaxRequestTarget target){
    }

    private void deletePerformed(AjaxRequestTarget target) {
        SearchItem item = getModelObject();
        Search search = item.getSearch();
        search.delete(item);

        SearchPanel panel = findParent(SearchPanel.class);
        panel.refreshSearchForm(target);
        panel.searchPerformed(target);
    }

}
