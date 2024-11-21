/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.gui.api.util.DisplayForLifecycleState;
import com.evolveum.midpoint.gui.api.util.DisplayableChoiceRenderer;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;
import com.evolveum.midpoint.prism.impl.binding.AbstractMutableContainerable;
import com.evolveum.midpoint.util.DisplayableValue;

import com.evolveum.midpoint.web.component.input.DisplayableValueChoiceRenderer;
import com.evolveum.midpoint.web.component.prism.InputPanel;
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
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.AppendingStringBuffer;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LifecycleStatePanel extends InputPanel {

    private static final Trace LOGGER = TraceManager.getTrace(LifecycleStatePanel.class);

    private static final String ID_PANEL = "panel";

    private final IModel<PrismPropertyWrapper<String>> model;

    public LifecycleStatePanel(String id, IModel<PrismPropertyWrapper<String>> model) {
        super(id);
        this.model = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

//    @Override
//    public void renderHead(IHeaderResponse response) {
//        super.renderHead(response);
//
//        callScript(response);
//    }

//    private void callScript(IHeaderResponse response) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("MidPointTheme.initDropdownResize('").append(getMarkupId()).append("');");
//
//        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
//    }

    private <T> void initLayout() {
        setOutputMarkupId(true);

        IModel<List> choicesModel = Model.ofList(getChoices());

        IModel<DisplayableValue<String>> model = new IModel<>() {
            @Override
            public DisplayableValue<String> getObject() {
                String value = null;
                try {
                    if (getModelObject() != null) {
                        value = getModelObject().getValue().getRealValue();
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't get value from " + getModelObject(), e);
                }
                if (value == null) {
                    value = SchemaConstants.LIFECYCLE_ACTIVE;
                }
                String finalValue = value;
                Optional<DisplayableValue<String>> displayValue = choicesModel.getObject()
                        .stream()
                        .filter(choice -> ((DisplayableValue<String>) choice).getValue().equals(finalValue))
                        .findFirst();

                if (displayValue.isPresent()) {
                    return displayValue.get();
                }
                return new DisplayableValueImpl<>(finalValue, finalValue);
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
                DisplayForLifecycleState display = DisplayForLifecycleState.valueOfOrDefault(displayValue.getValue());
                String label = new DisplayableValueChoiceRenderer<>(null).getDisplayValue(displayValue);
                buffer.append("\n<option ");
                setOptionAttributes(buffer, choice, index, selected);
                buffer.append(">");
                if (display.getLabel() == null) {
                    buffer.append(label);
                } else {
                    String advancedLabel = LocalizationUtil.translate(display.getLabel());
                    if (label.equals(advancedLabel)) {
                        buffer.append(label);
                    } else {
                        buffer.append(advancedLabel);
                    }
                }
                buffer.append("</option>");

            }
        };
        input.setNullValid(false);
        input.setOutputMarkupId(true);

//        input.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
//            @Override
//            protected void onUpdate(AjaxRequestTarget target) {
//                callScript(target.getHeaderResponse());
//                target.add(input);
//            }
//        });
//
//        input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour() {
//            @Override
//            protected void onUpdate(AjaxRequestTarget target) {
//                callScript(target.getHeaderResponse());
//                target.add(input);
//            }
//        });

        input.add(new EmptyOnChangeAjaxFormUpdatingBehavior(){
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(input);
            }
        });

        input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
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
            DisplayForLifecycleState display = DisplayForLifecycleState.valueOfOrDefault(name);
            return display.getCssClass() + " form-control form-control-sm resizing-select" + customCssClassForInputField();
        }));

        input.add(AttributeAppender.append("aria-label", createStringResource("ObjectType.lifecycleState")));

        add(input);
    }

    private PrismPropertyWrapper<String> getModelObject() {
        return this.model.getObject();
    }

    protected String customCssClassForInputField() {
        return "";
    }

    private List getChoices() {
        List choices = new ArrayList();

        if (getModelObject() == null) {
            return choices;
        }

        String lookupOid = getModelObject().getPredefinedValuesOid();
        if (StringUtils.isEmpty(lookupOid)) {
            return choices;
        }
        LookupTableType lookupTable = WebComponentUtil.loadLookupTable(lookupOid, getPageBase());

        if (lookupTable == null) {
            return choices;
        }

        List<LookupTableRowType> rows = new ArrayList(lookupTable.getRow());

        rows.sort(Comparator.comparingLong(AbstractMutableContainerable::getId));

        for (LookupTableRowType row : rows) {
            String value = com.evolveum.midpoint.gui.api.util.LocalizationUtil.translateLookupTableRowLabel(row);
            DisplayableValue<String> display = new SearchValue<>(row.getKey(), value);
            choices.add(display);
        }

        return choices;
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_PANEL);
    }
}
