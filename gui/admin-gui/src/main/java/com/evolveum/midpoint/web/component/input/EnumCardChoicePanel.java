/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;

/**
 * Card-based single selection for a set of enum values, mirroring the look of the
 * "Recommended Object Classes" selection in the connector development wizard.
 * Each option is rendered as a card with a radio button, a title and a short description;
 * clicking the card selects the option.
 *
 * <p>A read-only panel renders its selection as disabled and is excluded from form
 * processing, so a form submission can neither change nor clear the locked value.
 *
 * @param <T> enum type of the selected value
 */
public class EnumCardChoicePanel<T extends Enum<?>> extends InputPanel {

    private static final String ID_INPUT = "input";
    private static final String ID_PANEL = "panel";
    private static final String ID_CARD = "card";
    private static final String ID_RADIO = "radio";
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";

    private final IModel<T> valueModel;
    private final List<CardOption<T>> options;
    private final boolean required;
    private final boolean readOnly;

    public EnumCardChoicePanel(String id, IModel<T> valueModel, List<CardOption<T>> options, boolean required, boolean readOnly) {
        super(id);
        this.valueModel = valueModel;
        this.options = List.copyOf(options);
        this.required = required;
        this.readOnly = readOnly;
    }

    /**
     * Creates a card option whose texts are resolved from the standard enum display name keys
     * ({@code <EnumType>.<CONSTANT>}) and their {@code .description} counterparts, the same way
     * other enum editors localize enum values.
     */
    public static <T extends Enum<?>> CardOption<T> createLocalizedOption(T value, Component owner, String descriptionFallback) {
        String key = WebComponentUtil.createEnumResourceKey(value);
        return new CardOption<>(value,
                new StringResourceModel(key, owner).setDefaultValue(value.name()),
                new StringResourceModel(key + ".description", owner).setDefaultValue(descriptionFallback));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        RadioGroup<T> radioGroup = new RadioGroup<>(ID_INPUT, valueModel);
        radioGroup.setOutputMarkupId(true);
        if (readOnly) {
            // A read-only selection must survive form submissions: a disabled radio input is
            // never submitted, so an enabled group would convert the missing input to null and
            // wipe the value (e.g. the locked SQL integration type of a connector development)
            // whenever the surrounding form is submitted. Disabling the group makes Wicket skip
            // both its validation and its model update, so the value is only rendered.
            radioGroup.setEnabled(false);
        }

        if (required) {
            radioGroup.add(new INullAcceptingValidator<T>() {

                @Serial
                private static final long serialVersionUID = 1L;

                @Override
                public void validate(IValidatable<T> validatable) {
                    if (validatable.getValue() == null) {
                        ValidationError error = new ValidationError();
                        error.addKey("EnumCardChoicePanel.required");
                        validatable.error(error);
                    }
                }
            });
        }

        ListView<CardOption<T>> list = new ListView<>(ID_PANEL, Model.ofList(options)) {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<CardOption<T>> item) {
                CardOption<T> option = item.getModelObject();
                item.add(AttributeAppender.append("class", getCardCssClass()));

                WebMarkupContainer card = new WebMarkupContainer(ID_CARD);
                card.add(AttributeAppender.append("class",
                        () -> option.getValue() == valueModel.getObject() ? getSelectedCardCssClass() : null));
                item.add(card);

                Radio<T> radio = new Radio<>(ID_RADIO, Model.of(option.getValue()), radioGroup);
                radio.setOutputMarkupId(true);
                radio.setEnabled(!readOnly);
                card.add(radio);

                Label name = new Label(ID_NAME, option.getTitle());
                name.setOutputMarkupId(true);
                card.add(name);

                Label description = new Label(ID_DESCRIPTION, option.getDescription());
                description.setOutputMarkupId(true);
                card.add(description);

                item.add(AttributeAppender.append("style", "cursor: pointer;"));
                item.add(new AjaxEventBehavior("click") {

                    @Serial
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        if (readOnly) {
                            return;
                        }
                        radioGroup.clearInput();
                        valueModel.setObject(option.getValue());
                        target.add(radioGroup);
                        onSelectionChanged(target);
                    }
                });
            }
        };
        list.setOutputMarkupId(true);
        radioGroup.add(list);

        radioGroup.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(radioGroup);
                onSelectionChanged(target);
            }
        });

        add(radioGroup);
    }

    /**
     * Called after the user selects a card.
     */
    protected void onSelectionChanged(AjaxRequestTarget target) {
    }

    /**
     * CSS classes added to the card of the selected option. Allows to customize selection style. Default is null.
     */
    protected String getSelectedCardCssClass() {
        return null;
    }

    private String getCardCssClass() {
        if (options.size() == 1) {
            return "";
        }
        if (options.size() == 2) {
            return "col-6";
        }
        return "col-4";
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT);
    }

    /**
     * A single selectable card: the enum value together with its display title and description.
     *
     * @param <T> enum type of the value
     */
    public static final class CardOption<T> implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final T value;
        private final IModel<String> title;
        private final IModel<String> description;

        public CardOption(T value, IModel<String> title, IModel<String> description) {
            this.value = value;
            this.title = title;
            this.description = description;
        }

        public T getValue() {
            return value;
        }

        public IModel<String> getTitle() {
            return title;
        }

        public IModel<String> getDescription() {
            return description;
        }
    }
}
