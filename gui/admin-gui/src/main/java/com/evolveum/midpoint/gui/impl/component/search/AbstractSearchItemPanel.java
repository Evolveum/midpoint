/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

public abstract class AbstractSearchItemPanel<S extends AbstractSearchItemWrapper> extends BasePanel<S> {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_ITEM_CONTAINER = "searchItemContainer";
    protected static final String ID_SEARCH_ITEM_FIELD = "searchItemField";
    private static final String ID_SEARCH_ITEM_LABEL = "searchItemLabel";
    private static final String ID_HELP = "help";
    private static final String ID_REMOVE_BUTTON = "removeButton";

    public AbstractSearchItemPanel(String id, IModel<S> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
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
        help.add(AttributeModifier.replace("title", createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new InfoTooltipBehavior(){
            @Override
            public String getDataPlacement() {
                return "left";
            }
        });
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
        searchItemContainer.add(help);

        Component searchItemField = initSearchItemField();
        if (searchItemField instanceof InputPanel && !(searchItemField instanceof AutoCompleteTextPanel)) {
            FormComponent<?> baseFormComponent = ((InputPanel) searchItemField).getBaseFormComponent();
            baseFormComponent.add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
            baseFormComponent.add(AttributeAppender.append("style", "max-width: 400px !important;"));
            baseFormComponent.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            baseFormComponent.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isEnabled() {
                    return isFieldEnabled();
                }

                @Override
                public boolean isVisible() {
                    return getModelObject().isVisible();
                }
            });
        }
        searchItemField.setOutputMarkupId(true);
        searchItemContainer.add(searchItemField);

        AjaxSubmitButton removeButton = new AjaxSubmitButton(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                deletePerformed(target);
            }
        };
        removeButton.add(new VisibleBehaviour(() -> canRemoveSearchItem()));
        removeButton.setOutputMarkupId(true);
        searchItemContainer.add(removeButton);
    }

    protected boolean isFieldEnabled (){
        return getModelObject().isEnabled();
    }

    protected WebMarkupContainer getSearchItemContainer() {
        return (WebMarkupContainer) get(ID_SEARCH_ITEM_CONTAINER);
    }

    private IModel<String> createHelpModel(){
        return Model.of(getModelObject().getHelp());
    }

    protected abstract Component initSearchItemField();

    protected boolean canRemoveSearchItem() {
        return getModelObject().canRemoveSearchItem();
    }

    protected IModel<String> createLabelModel() {
        return StringUtils.isNotEmpty(getModelObject().getName()) ? createStringResource(getModelObject().getName()) : Model.of("");
    }

    private IModel<String> createTitleModel() {
        return Model.of(getModelObject().getTitle());
    }

    protected void searchPerformed(AjaxRequestTarget target){
    }

    private void deletePerformed(AjaxRequestTarget target) {
        getModelObject().setVisible(false);
        SearchPanel panel = findParent(SearchPanel.class);
        panel.displayedSearchItemsModelReset();
        panel.refreshSearchForm(target);
        panel.searchPerformed(target);
    }

    protected IModel<List<DisplayableValue<Boolean>>> createBooleanChoices() {
        List<DisplayableValue<Boolean>> list = new ArrayList<>();
        list.add(new SearchValue<>(Boolean.TRUE, "Boolean.TRUE"));
        list.add(new SearchValue<>(Boolean.FALSE, "Boolean.FALSE"));
        return Model.ofList(list);
    }

    protected AutoCompleteTextPanel createAutoCompetePanel(String id, IModel<String> model, LookupTableType lookupTable) {
        AutoCompleteTextPanel<String> autoCompletePanel = new AutoCompleteTextPanel<String>(id, model, String.class,
                true, lookupTable) {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<String> getIterator(String input) {
                return WebComponentUtil.prepareAutoCompleteList(lookupTable, input).iterator();
            }
        };

        (autoCompletePanel).getBaseFormComponent().add(new Behavior() {

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
        return autoCompletePanel;
    }

    protected IModel<List<DisplayableValue<?>>> createEnumChoices(Class<? extends Enum> inputClass) {
        Enum[] enumConstants = inputClass.getEnumConstants();
        List<DisplayableValue<?>> list = new ArrayList<>();
        for(int i = 0; i < enumConstants.length; i++){
            list.add(new SearchValue<>(enumConstants[i], getString(enumConstants[i])));
        }
        return Model.ofList(list);

    }

}
