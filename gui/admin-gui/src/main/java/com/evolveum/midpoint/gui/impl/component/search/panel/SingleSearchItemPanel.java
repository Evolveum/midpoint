/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.FilterableSearchItemWrapper;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.input.CheckPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import org.jetbrains.annotations.NotNull;

public abstract class SingleSearchItemPanel<S extends AbstractSearchItemWrapper> extends AbstractSearchItemPanel<S> {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_ITEM_CONTAINER = "searchItemContainer";
    private static final String ID_SEARCH_ITEM_FIELD = "searchItemField";
    private static final String ID_SEARCH_ITEM_LABEL = "searchItemLabel";
    private static final String ID_HELP = "help";
    private static final String ID_REMOVE_BUTTON = "removeButton";
    private static final String ID_CHECK_DISABLE_FIELD = "checkDisable";

    public SingleSearchItemPanel(String id, IModel<S> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        Label searchItemLabel = (Label) get(createComponentPath(ID_SEARCH_ITEM_CONTAINER, ID_SEARCH_ITEM_LABEL));
        Component searchItemField = getSearchItemFieldPanel();
        if (searchItemField instanceof InputPanel inputPanel) {
            inputPanel.getBaseFormComponent().add(AttributeAppender.append(
                    "aria-labelledby",
                    searchItemLabel.getMarkupId()));
        }
    }

    protected Component getSearchItemFieldPanel() {
        return get(createComponentPath(ID_SEARCH_ITEM_CONTAINER, ID_SEARCH_ITEM_FIELD));
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
        help.add(new InfoTooltipBehavior() {
            @Override
            public String getDataPlacement() {
                return "left";
            }
        });
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
        help.add(AttributeAppender.append(
                "aria-label",
                getParentPage().createStringResource("SingleSearchItemPanel.helpTooltip", createLabelModel().getObject())));
        searchItemContainer.add(help);

        Component searchItemField = initSearchItemField(ID_SEARCH_ITEM_FIELD);
        if (searchItemField instanceof InputPanel inputPanel && !(searchItemField instanceof AutoCompleteTextPanel)) {
            FormComponent<?> baseFormComponent = inputPanel.getBaseFormComponent();
            baseFormComponent.add(AttributeAppender.append("style", "max-width: 400px !important;"));
            baseFormComponent.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            baseFormComponent.add(AttributeAppender.append("readonly", () -> isFieldEnabled() ? null : "readonly"));
        }
        searchItemField.add(new VisibleBehaviour(this::isSearchItemFieldVisible));
        searchItemField.setOutputMarkupId(true);
        searchItemContainer.add(searchItemField);

        CheckPanel checkPanel = new CheckPanel(ID_CHECK_DISABLE_FIELD, new PropertyModel<>(getModel(), FilterableSearchItemWrapper.F_APPLY_FILTER));
        (checkPanel).getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                searchPerformed(ajaxRequestTarget);
            }
        });
        checkPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        checkPanel.add(new VisibleBehaviour(this::isCheckPanelVisible));

        checkPanel.setOutputMarkupId(true);
        searchItemContainer.add(checkPanel);

        AjaxSubmitButton removeButton = new AjaxSubmitButton(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                deletePerformed(target);
            }
        };
        removeButton.add(new VisibleBehaviour(() -> canRemoveSearchItem()));
        removeButton.setOutputMarkupId(true);
        removeButton.add(AttributeModifier.replace("title",
                getParentPage().createStringResource("SingleSearchItemPanel.removeProperty", createLabelModel().getObject())));
        searchItemContainer.add(removeButton);
    }

    protected boolean isFieldEnabled() {
        return getModelObject().isEnabled();
    }

    protected WebMarkupContainer getSearchItemContainer() {
        return (WebMarkupContainer) get(ID_SEARCH_ITEM_CONTAINER);
    }

    private IModel<String> createHelpModel() {
        if (getModelObject() == null) {
            return () -> "";
        }
        return getModelObject().getHelp();
    }

    protected abstract Component initSearchItemField(String id);

    protected boolean canRemoveSearchItem() {
        return getModelObject() != null && getModelObject().canRemoveSearchItem();
    }

    private boolean isSearchItemFieldVisible() {
        return getModelObject() != null && getModelObject().isVisible() && noFilterOrHasParameters();
    }

    private boolean noFilterOrHasParameters() {
        return getModelObject().getPredefinedFilter() == null ||
                StringUtils.isNotEmpty(getModelObject().getParameterName());
    }

    protected IModel<String> createLabelModel() {
        if (getModelObject() == null) {
            return () -> "";
        }
        @NotNull IModel<String> nameModel = getModelObject().getName();
        return StringUtils.isNotEmpty(nameModel.getObject()) ? nameModel : Model.of("");
    }

    private IModel<String> createTitleModel() {
        if (getModelObject() == null) {
            return () -> "";
        }
        return getModelObject().getTitle();
    }

    protected void searchPerformed(AjaxRequestTarget target) {
        SearchPanel panel = findParent(SearchPanel.class);
        panel.searchPerformed(target);
    }

    private void deletePerformed(AjaxRequestTarget target) {
        AbstractSearchItemWrapper wrapper = getModelObject();
        wrapper.setVisible(false);
        wrapper.clearValue();

        SearchPanel panel = findParent(SearchPanel.class);
        target.add(panel);
        panel.searchPerformed(target);
    }

    protected AutoCompleteTextPanel createAutoCompetePanel(String id, IModel<String> model, String lookupTableOid) {
        AutoCompleteTextPanel<String> autoCompletePanel = new AutoCompleteTextPanel<>(id, model, String.class,
                true, lookupTableOid) {

            private static final long serialVersionUID = 1L;

            @Override
            public Iterator<String> getIterator(String input) {
                return WebComponentUtil.prepareAutoCompleteList(getLookupTable(), input).iterator();
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
        for (int i = 0; i < enumConstants.length; i++) {
            list.add(new SearchValue<>(enumConstants[i], getString(enumConstants[i])));
        }
        return Model.ofList(list);

    }

    private boolean isCheckPanelVisible() {
        return getModelObject() != null && getModelObject().getPredefinedFilter() != null;
    }
}
