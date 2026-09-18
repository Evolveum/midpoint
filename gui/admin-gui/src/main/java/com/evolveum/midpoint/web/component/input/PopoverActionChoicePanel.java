/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import java.io.Serial;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.gui.impl.component.search.panel.Popover;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;
import org.jetbrains.annotations.NotNull;

/**
 * An input field with a popover containing selectable choices and a trailing action.
 *
 * <p>The popover is shown when the field receives focus, similarly to a browser's
 * saved-value suggestions. Selecting a choice updates the input model without
 * submitting the surrounding form. Subclasses provide the behavior for the action row.</p>
 */
public abstract class PopoverActionChoicePanel<T> extends InputPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_INPUT = "input";
    private static final String ID_POPOVER = "popover";
    private static final String ID_CHOICES_CONTAINER = "choicesContainer";
    private static final String ID_CHOICES = "choices";
    private static final String ID_CHOICE = "choice";
    private static final String ID_CHOICE_LABEL = "choiceLabel";
    private static final String ID_ACTION = "action";
    private static final String ID_ICON = "icon";

    private final IModel<T> model;
    private final IModel<? extends List<? extends T>> choices;
    private final IChoiceRenderer<T> renderer;
    private final boolean allowNull;

    private PopoverActionChoicePanel(
            String id,
            IModel<T> model,
            IModel<? extends List<? extends T>> choices,
            IChoiceRenderer<T> renderer,
            boolean allowNull) {
        super(id);
        this.model = model;
        this.choices = choices;
        this.renderer = renderer;
        this.allowNull = allowNull;
    }

    public PopoverActionChoicePanel(
            String id,
            IModel<T> model,
            IModel<? extends List<? extends T>> choices,
            boolean allowNull) {
        this(id, model, choices, new IChoiceRenderer<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(T object) {
                return object.toString();
            }

            @Override
            public String getIdValue(T object, int index) {
                return Integer.toString(index);
            }
        }, allowNull);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        IModel<String> filterModel = Model.of("");
        TextField<T> input = buildTextField();
        add(input);

        Popover popover = new Popover(ID_POPOVER) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public Component getPopoverReferenceComponent() {
                return input;
            }
        };
        add(popover);

        input.add(AttributeModifier.replace("aria-controls", popover.getMarkupId()));
        input.add(AttributeModifier.replace("aria-expanded", "false"));

        WebMarkupContainer choicesContainer = new WebMarkupContainer(ID_CHOICES_CONTAINER);
        choicesContainer.setOutputMarkupId(true);
        popover.add(choicesContainer);
        choicesContainer.add(buildListView(filterModel, input, popover));

        input.add(new AjaxEventBehavior("focus") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getDynamicExtraParameters().add("var input = Wicket.$('" + input.getMarkupId()
                        + "'); return input ? { filter: input.value } : {};");
            }

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                filterModel.setObject(RequestCycle.get().getRequest().getRequestParameters()
                        .getParameterValue("filter").toString());
                target.add(choicesContainer);
                showPopover(target, input, popover);
            }
        });

        input.add(new AjaxEventBehavior("keyup") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(Duration.ofMillis(200), true));
                attributes.getDynamicExtraParameters().add("var input = Wicket.$('" + input.getMarkupId()
                        + "'); return input ? { filter: input.value } : {};");
            }

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                filterModel.setObject(RequestCycle.get().getRequest().getRequestParameters()
                        .getParameterValue("filter").toString());
                target.add(choicesContainer);
                showPopover(target, input, popover);
            }
        });

        input.add(new AjaxEventBehavior("blur") {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.getDynamicExtraParameters().add("var input = Wicket.$('" + input.getMarkupId()
                        + "'), popover = Wicket.$('" + popover.getMarkupId() + "'), target = attrs.event.relatedTarget;"
                        + " return { filter: input ? input.value : '', insidePopover: !!(target && popover"
                        + " && (target === popover || popover.contains(target))) };");
            }

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                var parameters = RequestCycle.get().getRequest().getRequestParameters();
                if (parameters.getParameterValue("insidePopover").toBoolean(false)) {
                    return;
                }

                String inputValue = parameters.getParameterValue("filter").toString();
                if (!isSelectedChoiceValue(inputValue)) {
                    model.setObject(null);
                    input.clearInput();
                    target.add(input);
                    onChoiceSelected(target, PopoverActionChoicePanel.this);
                }
                filterModel.setObject("");
                closePopover(target, input, popover);
            }
        });

        AjaxLink<Void> action = new AjaxLink<>(ID_ACTION) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                closePopover(target, input, popover);
                onActionClick(target, PopoverActionChoicePanel.this);
            }
        };
        action.add(new Label(ID_CHOICE_LABEL, getActionLabel()));
        action.add(new IconComponent(ID_ICON, getActionIconCssModel()));
        popover.add(action);
    }

    protected IModel<String> getChoicesIconCssModel() {
        return Model.of();
    }

    protected IModel<String> getActionIconCssModel() {
        return Model.of();
    }

    private @NotNull ListView<T> buildListView(IModel<String> filterModel, TextField<T> input, Popover popover) {
        return new ListView<>(ID_CHOICES, listModel()) {

            @Override
            protected void populateItem(ListItem<T> item) {
                AjaxLink<Void> choice = new AjaxLink<>(ID_CHOICE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PopoverActionChoicePanel.this.model.setObject(item.getModelObject());
                        filterModel.setObject("");
                        target.add(input);
                        closePopover(target, input, popover);
                        onChoiceSelected(target, PopoverActionChoicePanel.this);
                    }
                };
                choice.add(new Label(ID_CHOICE_LABEL,
                        Model.of(String.valueOf(PopoverActionChoicePanel.this.renderer.getDisplayValue(item.getModelObject())))));
                choice.add(new IconComponent(ID_ICON, getChoicesIconCssModel()));
                item.add(choice);
                item.add(new VisibleBehaviour(() -> {
                    String filter = filterModel.getObject();
                    return StringUtils.isBlank(filter) || String.valueOf(
                                    PopoverActionChoicePanel.this.renderer.getDisplayValue(item.getModelObject()))
                            .toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
                }));
            }
        };
    }

    private boolean isSelectedChoiceValue(String value) {
        T selectedChoice = model.getObject();
        return selectedChoice == null
                ? StringUtils.isBlank(value)
                : value.equals(String.valueOf(renderer.getDisplayValue(selectedChoice)));
    }

    private @NotNull TextField<T> buildTextField() {
        TextField<T> input = new TextField<>(ID_INPUT, model) {
            @Override
            @SuppressWarnings("unchecked")
            public <C> IConverter<C> getConverter(Class<C> type) {
                return (IConverter<C>) new IConverter<T>() {
                    @Override
                    public T convertToObject(String value, Locale locale) throws ConversionException {
                        if (StringUtils.isBlank(value)) {
                            return null;
                        }

                        List<? extends T> availableChoices = choices.getObject();
                        if (availableChoices != null) {
                            for (T choice : availableChoices) {
                                if (value.equals(String.valueOf(renderer.getDisplayValue(choice)))) {
                                    return choice;
                                }
                            }
                        }

                        throw new ConversionException("Unknown choice: " + value);
                    }

                    @Override
                    public String convertToString(T value, Locale locale) {
                        return value != null ? String.valueOf(renderer.getDisplayValue(value)) : "";
                    }
                };
            }
        };
        input.setOutputMarkupId(true);
        input.setRequired(!allowNull);
        return input;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private IModel<? extends List<T>> listModel() {
        return (IModel) choices;
    }

    private void closePopover(AjaxRequestTarget target, TextField<T> input, Popover popover) {
        target.appendJavaScript("$('#" + popover.getMarkupId() + "').fadeOut(200); $('#"
                + input.getMarkupId() + "').attr('aria-expanded', 'false');");
    }

    private void showPopover(AjaxRequestTarget target, TextField<T> input, Popover popover) {
        target.appendJavaScript("MidPointTheme.showPopover('#" + input.getMarkupId()
                + "', '#" + popover.getMarkupId() + "', true);");
    }

    protected IModel<String> getActionLabel() {
        return createStringResource("PopoverActionChoicePanel.define.new");
    }

    /** Invoked after a choice has been copied into the input model. */
    protected void onChoiceSelected(AjaxRequestTarget target, PopoverActionChoicePanel<T> panel) {
        // Subclasses may refresh dependent components.
    }

    protected abstract void onActionClick(AjaxRequestTarget target, PopoverActionChoicePanel<T> panel);

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull FormComponent<T> getBaseFormComponent() {
        return (FormComponent<T>) get(ID_INPUT);
    }

    public IModel<T> getModel() {
        return model;
    }

    public IModel<? extends List<? extends T>> getChoicesModel() {
        return choices;
    }

    public void togglePopover(AjaxRequestTarget target) {
        ((Popover) get(ID_POPOVER)).toggle(target);
    }
}

