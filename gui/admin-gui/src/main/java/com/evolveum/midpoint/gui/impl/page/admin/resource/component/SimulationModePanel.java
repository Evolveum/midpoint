/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.DisplayableChoiceRenderer;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.AutoCompleteDisplayableValueConverter;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.util.DisplayableValue;

import com.evolveum.midpoint.web.component.input.DisplayableValueChoiceRenderer;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.string.AppendingStringBuffer;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SimulationModePanel extends BasePanel<PrismPropertyWrapper<String>> {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationModePanel.class);

    private static final String ID_PANEL = "panel";

    private static final String ID_MODE = "mode";
    private static final String ID_SWITCH_TO_DEV = "switchToDev";
    private static final String ID_SWITCH_TO_PROD = "switchToProd";

    enum DisplayForOption {

        ACTIVE("SimulationModePanel.option.active", "colored-form-success"),
        DRAFT("SimulationModePanel.option.draft", "colored-form-danger"),
        PROPOSED("SimulationModePanel.option.proposed", "colored-form-warning"),
        DEFAULT(null, "colored-form-info");

        private final String label;
        private final String cssClass;
        DisplayForOption(String label, String cssClass) {
            this.label = label;
            this.cssClass = cssClass;
        }

        private static DisplayForOption valueOfOrDefault(String name) {
            if (StringUtils.isEmpty(name)) {
                return DisplayForOption.DEFAULT;
            }

            DisplayForOption value;
            try {
                value = valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                return DisplayForOption.DEFAULT;
            }

            if (value == null) {
                return DisplayForOption.DEFAULT;
            }

            return value;
        }
    }

    public SimulationModePanel(String id, IModel<PrismPropertyWrapper<String>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        callScript(response);
    }

    private void callScript(IHeaderResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("MidPointTheme.initDropdownResize('").append(getMarkupId()).append("');");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    private <T> void initLayout() {
        setOutputMarkupId(true);

        IModel<List> choicesModel = Model.ofList(getChoices());

        IModel<DisplayableValue<String>> model = new IModel<>() {
            @Override
            public DisplayableValue<String> getObject() {
                String value = null;
                try {
                    value = getModelObject().getValue().getRealValue();
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't get value from " + getModelObject(), e);
                }
                if (value == null) {
                    value = SchemaConstants.LIFECYCLE_ACTIVE;
                }
                String finalValue = value;
                return (DisplayableValue<String>) choicesModel.getObject()
                        .stream()
                        .filter(choice -> ((DisplayableValue<String>)choice).getValue().equals(finalValue))
                        .findFirst()
                        .get();
            }

            @Override
            public void setObject(DisplayableValue<String> object) {
                try {
                    String value = null;
                    if (object != null) {
                        value = object.getValue();
                    }
                    getModelObject().getValue().setRealValue(value);
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't get value from " + getModelObject(), e);
                }
            }
        };

        IChoiceRenderer renderer = new DisplayableChoiceRenderer();

        DropDownChoice input = new DropDownChoice<T>(
                ID_PANEL,
                (IModel<T>) model,
                (List<? extends T>) choicesModel.getObject(),
                renderer) {

            private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                Optional<? extends T> value = getChoices()
                        .stream()
                        .filter(displayableValue ->
                                ((DisplayableValue<String>) displayableValue).getValue().equals(SchemaConstants.LIFECYCLE_ACTIVE))
                        .findFirst();

                if (value.isPresent()) {
                    return (String) getChoiceRenderer().getDisplayValue(value.get());
                }
                return SchemaConstants.LIFECYCLE_ACTIVE;
            }

            @Override
            protected void appendOptionHtml(AppendingStringBuffer buffer, T choice, int index, String selected) {
                DisplayableValue<String> displayValue = (DisplayableValue<String>) choice;
                DisplayForOption display = DisplayForOption.valueOfOrDefault(displayValue.getValue());
                String label = new DisplayableValueChoiceRenderer<>(null).getDisplayValue(displayValue);
                if (display.label == null) {
                    buffer.append("\n<option ");
                    setOptionAttributes(buffer, choice, index, selected);
                    buffer.append(">");
                    buffer.append(label);
                    buffer.append("</option>");
                } else {
                    buffer.append("\n<option ");
                    setOptionAttributes(buffer, choice, index, selected);
                    buffer.append("style=\"display:none;\">");
                    buffer.append(label);
                    buffer.append("</option>");

                    String advancedLabel = LocalizationUtil.translate(display.label);
                    buffer.append("\n<option ");
                    setOptionAttributes(buffer, choice, index, null);
                    buffer.append(">");
                    buffer.append(advancedLabel);
                    buffer.append("</option>");
                }
            }
        };
        input.setNullValid(false);
        input.setOutputMarkupId(true);

        input.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                callScript(target.getHeaderResponse());
                target.add(input);
            }
        });

        input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                callScript(target.getHeaderResponse());
                target.add(input);
            }
        });

        input.add(AttributeAppender.replace("class", () -> {
            DisplayableValue<String> value = (DisplayableValue<String>) input.getModelObject();
            String name;
            if (value == null) {
                name = SchemaConstants.LIFECYCLE_ACTIVE;
            } else {
                name = value.getValue();
            }
            DisplayForOption display = DisplayForOption.valueOfOrDefault(name);
            return display.cssClass + " form-control form-control-sm resizing-select";
        }));

        add(input);

//        DropDownChoicePanel panel = new DropDownChoicePanel(
//                ID_PANEL, model, choices, new DisplayableChoiceRenderer(), false) {
//            @Override
//            protected String getNullValidDisplayValue() {
//                Optional<? extends DisplayableValue<String>> value = choices.getObject()
//                        .stream()
//                        .filter(displayableValue -> displayableValue.getValue().equals(SchemaConstants.LIFECYCLE_ACTIVE))
//                        .findFirst();
//
//                if (value.isPresent()) {
//                    return (String) getBaseFormComponent().getChoiceRenderer().getDisplayValue(value.get());
//                }
//                return SchemaConstants.LIFECYCLE_ACTIVE;
//            }
//        };
//        add(panel);

//        Label label = new Label(ID_MODE, createLabelModel());
//        label.setOutputMarkupId(true);
//        add(label);
//
//        AjaxIconButton toggleToProduction = new AjaxIconButton(ID_SWITCH_TO_PROD, Model.of(GuiStyleConstants.CLASS_ICON_TOOGLE),
//                createStringResource("OperationalButtonsPanel.button.toggleToProduction")) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                updateState(SchemaConstants.LIFECYCLE_ACTIVE);
//                target.add(SimulationModePanel.this);
//                target.add(label);
//            }
//        };
//
//        toggleToProduction.setOutputMarkupId(true);
//        toggleToProduction.showTitleAsLabel(true);
//        toggleToProduction.add(new VisibleBehaviour(() -> isToggleModeButtonVisible(SchemaConstants.LIFECYCLE_ACTIVE)));
//        add(toggleToProduction);
//
//        AjaxIconButton toggleToDevelopment = new AjaxIconButton(ID_SWITCH_TO_DEV, Model.of(GuiStyleConstants.CLASS_ICON_TOOGLE),
//                createStringResource("OperationalButtonsPanel.button.toggleToDevelopment")) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                updateState(SchemaConstants.LIFECYCLE_PROPOSED);
//                target.add(SimulationModePanel.this);
//                target.add(label);
//            }
//        };
//        toggleToDevelopment.setOutputMarkupId(true);
//        toggleToDevelopment.showTitleAsLabel(true);
//        toggleToDevelopment.add(new VisibleBehaviour(() -> isToggleModeButtonVisible(SchemaConstants.LIFECYCLE_PROPOSED)));
//        add(toggleToDevelopment);

    }

    private List getChoices() {
        List choices = new ArrayList();
        String lookupOid = getModelObject().getPredefinedValuesOid();
        if (StringUtils.isEmpty(lookupOid)) {
            return choices;
        }
        LookupTableType lookupTable = WebComponentUtil.loadLookupTable(lookupOid, getPageBase());

        if (lookupTable == null) {
            return choices;
        }

        List<LookupTableRowType> rows = lookupTable.getRow();

        for (LookupTableRowType row : rows) {
            String value = com.evolveum.midpoint.gui.api.util.LocalizationUtil.translateLookupTableRowLabel(row);
            DisplayableValue<String> display = new SearchValue<>(row.getKey(), value);
            choices.add(display);
        }

        return choices;
    }

    private IModel<String> createLabelModel() {
        return () -> {
            String key = "SimulationMode.undefined";
            try {
                String lifecycle = getModelObject().getValue().getRealValue();
                if (StringUtils.isEmpty(lifecycle)) {
                    lifecycle = SchemaConstants.LIFECYCLE_ACTIVE;
                }

                if (SchemaConstants.LIFECYCLE_ACTIVE.equals(lifecycle)
                        || SchemaConstants.LIFECYCLE_PROPOSED.equals(lifecycle)) {
                    key = "SimulationMode." + lifecycle;
                }
            } catch (SchemaException e) {
                LOGGER.error("Couldn't get value from " + getModelObject(), e);
            }
            return getString(key);
        };
    }

    private void updateState(String lifecycleActive) {
        try {
            getModelObject().getValue().setRealValue(lifecycleActive);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value from " + getModelObject(), e);
        }
    }

    private boolean isToggleModeButtonVisible(@NotNull String expectedLifecycleState) {
        try {
            String lifecycleState = getModelObject().getValue().getRealValue();
            if (StringUtils.isEmpty(lifecycleState)) {
                lifecycleState = SchemaConstants.LIFECYCLE_ACTIVE;
            }

            return !expectedLifecycleState.equals(lifecycleState);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value from " + getModelObject(), e);
        }
        return false;
    }
}
