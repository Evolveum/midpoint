/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DurationWithOneElementPanel extends InputPanel {

    protected static final String ID_INPUT = "input";
    protected static final String ID_UNIT = "unit";

    private final boolean isPositive;

    private List<ChoiceWrapper> choices = List.of(
            new ChoiceWrapper(DatatypeConstants.YEARS.toString(), "Y", "P"),
            new ChoiceWrapper(DatatypeConstants.MONTHS.toString(), "M", "P"),
            new ChoiceWrapper(DatatypeConstants.DAYS.toString(), "D", "P"),
            new ChoiceWrapper(DatatypeConstants.HOURS.toString(), "H", "PT"),
            new ChoiceWrapper(DatatypeConstants.MINUTES.toString(), "M", "PT"),
            new ChoiceWrapper(DatatypeConstants.SECONDS.toString(), "S", "PT")
    );

    public DurationWithOneElementPanel(String id, IModel<Duration> model, boolean isPositive) {
        super(id);
        this.isPositive = isPositive;

        final TextField<Duration> text = new TextField<>(ID_INPUT, model) {

            @Override
            protected boolean shouldTrimInput() {
                return false;
            }

            @Override
            public void convertInput() {
                String durationStr = getBaseFormComponent().getRawInput();

                if (durationStr == null) {
                    setConvertedInput(null);
                }

                try {
                    setConvertedInput(new DurationConverter().convertToObject(durationStr, getPageBase().getLocale()));
                } catch (Exception ex) {
                    this.error(getPageBase().getString("DurationPanel.incorrectValueError", getLabel().getObject()));
                }
            }

            @Override
            protected IConverter<?> createConverter(Class<?> type) {
                return new DurationConverter();
            }

        };
        text.setType(Integer.class);
        add(text);

        IModel<String> unitModel = new LoadableModel<>(false) {
            @Override
            protected String load() {
                DatatypeConstants.Field actualUnit = parseUnitFiled(model);
                return actualUnit.toString();
            }
        };

        DropDownChoice<String> unit = new DropDownChoice<>(
                ID_UNIT,
                unitModel,
                choices.stream().map(choice -> choice.name).toList(),
                StringChoiceRenderer.prefixed("DurationWithOneElementPanel.unit."));
        unit.setNullValid(false);
        unit.setOutputMarkupId(true);
        unit.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                DurationConverter converter = new DurationConverter();
                Duration duration = converter.convertToObject(
                        converter.convertToString(
                                model.getObject(),
                                parseUnitFiled(model)),
                        getPageBase().getLocale());
                model.setObject(duration);
            }
        });
        add(unit);
    }

    private DatatypeConstants.Field parseUnitFiled(IModel<Duration> model) {
        if (model.getObject() == null) {
            return getChoicesAsField().get(0);
        }

        List<DatatypeConstants.Field> choices = getChoicesAsField();

        Optional<DatatypeConstants.Field> unit =
                choices.stream().filter(choice -> model.getObject().isSet(choice)).findFirst();
        if (unit.isPresent()) {
            return unit.get();
        }
        return getChoicesAsField().get(0);
    }

    private List<DatatypeConstants.Field> getChoicesAsField() {
        return List.of(
                DatatypeConstants.YEARS,
                DatatypeConstants.MONTHS,
                DatatypeConstants.DAYS,
                DatatypeConstants.HOURS,
                DatatypeConstants.MINUTES,
                DatatypeConstants.SECONDS
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public FormComponent<Duration> getBaseFormComponent() {
        return (FormComponent<Duration>) get(ID_INPUT);
    }

    private ChoiceWrapper getUnit(){
        String value = ((DropDownChoice<String>) get(ID_UNIT)).getModelObject();
        Optional<ChoiceWrapper> unit = choices.stream().filter(choice -> value.equals(choice.name)).findFirst();
        if (unit.isPresent()) {
            return unit.get();
        }
        return null;
    }

    private class ChoiceWrapper implements Serializable {
        private final String name;

        private final String unit;

        private final String unitPrefix;

        private ChoiceWrapper(String name, String unit, String unitPrefix) {
            this.name = name;
            this.unit = unit;
            this.unitPrefix = unitPrefix;
        }
    }

    private class DurationConverter implements IConverter<Duration> {

        @Override
        public Duration convertToObject(String value, Locale locale) throws ConversionException {

            if (StringUtils.isEmpty(value)) {
                return null;
            }

            ChoiceWrapper unit = getUnit();

            if (unit == null) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            if (!isPositive) {
                sb.append("-");
            }
            sb.append(unit.unitPrefix).append(Integer.valueOf(value)).append(unit.unit);

            return XmlTypeConverter.createDuration(sb.toString());
        }

        @Override
        public String convertToString(Duration duration, Locale locale) {
            List<DatatypeConstants.Field> choices = getChoicesAsField();

            ChoiceWrapper unit = getUnit();
            Optional<DatatypeConstants.Field> field = choices
                    .stream()
                    .filter(choice -> choice.toString().equals(unit.name))
                    .findFirst();
            return String.valueOf(duration.getField(field.get()).intValue());
        }

        private String convertToString(Duration duration, DatatypeConstants.Field unit) {
            List<DatatypeConstants.Field> choices = getChoicesAsField();

            Optional<DatatypeConstants.Field> field = choices
                    .stream()
                    .filter(choice -> choice.equals(unit))
                    .findFirst();
            return String.valueOf(duration.getField(field.get()).intValue());
        }
    }
}
