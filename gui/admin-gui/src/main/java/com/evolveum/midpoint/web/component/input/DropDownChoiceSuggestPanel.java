/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class DropDownChoiceSuggestPanel<T> extends InputPanel implements Serializable {

    @Serial private static final long serialVersionUID = 1L;
    private static final String ID_INPUT = "input";
    private static final String ID_SUGGEST = "suggest";
    private static final String ID_SUGGEST_CONTAINER = "suggestContainer";

    private final IModel<T> model;
    private final IModel<? extends List<? extends T>> choices;
    private final IChoiceRenderer<T> renderer;
    private final boolean allowNull;

    public DropDownChoiceSuggestPanel(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<T> renderer,
            boolean allowNull) {
        super(id);
        this.model = model;
        this.choices = choices;
        this.renderer = renderer;
        this.allowNull = allowNull;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout(model, choices, renderer, allowNull);
    }

    private void initLayout(IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<T> renderer, boolean allowNull) {
        DropDownChoice<T> input = new DropDownChoice<T>(ID_INPUT, model,
                choices, renderer) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                if (allowNull) {
                    return super.getDefaultChoice(selectedValue);
                } else {
                    return getString("DropDownChoicePanel.notDefined");
                }
            }

            @Override
            protected String getNullValidDisplayValue() {
                return DropDownChoiceSuggestPanel.this.getNullValidDisplayValue();
            }

            @Override
            public String getModelValue() {
                T object = this.getModelObject();
                if (object != null) {
                    if (QName.class.isAssignableFrom(object.getClass())
                            && !getChoices().isEmpty()
                            && QName.class.isAssignableFrom(getChoices().iterator().next().getClass())) {
                        for (int i = 0; i < getChoices().size(); i++) {
                            if (QNameUtil.match((QName) getChoices().get(i), (QName) object)) {
                                return this.getChoiceRenderer().getIdValue(object, i);
                            }
                        }
                    }
                }

                return super.getModelValue();
            }

            @Override
            public IModel<? extends List<? extends T>> getChoicesModel() {
                return super.getChoicesModel();
            }
        };

        input.setNullValid(allowNull);
        input.setOutputMarkupId(true);
        add(input);

        WebMarkupContainer suggestContainer = new WebMarkupContainer(ID_SUGGEST_CONTAINER);
        suggestContainer.setOutputMarkupId(true);
        suggestContainer.add(new VisibleBehaviour(this::isSuggestContainerVisible));
        add(suggestContainer);

        AjaxIconButton suggestButton = new AjaxIconButton(
                ID_SUGGEST,
                () -> getSuggestButtonIcon().getObject(),
                () -> getSuggestButtonLabel().getObject()) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                onSuggestAction(ajaxRequestTarget);
                ajaxRequestTarget.add(this);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);

                String id = getMarkupId(true);
                String beforeIconCss = getSuggestButtonIcon().getObject();
                String afterIconCss = getSuggestProcessingStateButtonIcon().getObject();
                String loadingText = getSuggestProcessingStateButtonLabel().getObject();

                attributes.getAjaxCallListeners().add(new PreAjaxLoadingStateListener(id, beforeIconCss, afterIconCss, loadingText));
            }

            @Override
            public AjaxIconButton showTitleAsLabel(boolean show) {
                return super.showTitleAsLabel(show);
            }
        };

        suggestButton.setOutputMarkupId(true);
        suggestButton.showTitleAsLabel(true);
        suggestContainer.add(suggestButton);
    }

    @Override
    public DropDownChoice<T> getBaseFormComponent() {
        //noinspection unchecked
        return (DropDownChoice<T>) get("input");
    }

    protected void onSuggestAction(AjaxRequestTarget target) {

    }

    protected boolean isSuggestContainerVisible() {
        return true;
    }

    protected IModel<String> getSuggestButtonLabel() {
        return createStringResource("DropDownChoicePanel.suggest.label");
    }

    protected IModel<String> getSuggestButtonIcon() {
        return Model.of(GuiStyleConstants.CLASS_MAGIC_WAND);
    }

    protected IModel<String> getSuggestProcessingStateButtonIcon() {
        return Model.of(GuiStyleConstants.CLASS_SPINNER);
    }

    protected IModel<String> getSuggestProcessingStateButtonLabel() {
        return createStringResource("DropDownChoicePanel.suggesting.label");
    }

    public IModel<T> getModel() {
        return getBaseFormComponent().getModel();
    }

    protected String getNullValidDisplayValue() {
        return getString("DropDownChoicePanel.notDefined");
    }

    public T getFirstChoice() {
        DropDownChoice<T> baseComponent = getBaseFormComponent();
        if (!baseComponent.getChoices().isEmpty()) {
            return baseComponent.getChoices().get(0);
        }
        return null;
    }
}
