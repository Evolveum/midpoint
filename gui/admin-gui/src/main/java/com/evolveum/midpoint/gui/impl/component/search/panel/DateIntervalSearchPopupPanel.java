/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.util.List;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.DateTimePickerPanel;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.util.DateValidator;

public class DateIntervalSearchPopupPanel extends PopoverSearchPopupPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_FROM_DATE_CONTAINER = "fromDateContainer";
    private static final String ID_FROM_LABEL = "fromLabel";
    private static final String ID_DATE_FROM_VALUE = "dateFromValue";
    private static final String ID_TO_LABEL = "toLabel";
    private static final String ID_DATE_TO_VALUE = "dateToValue";
    private static final String ID_DATE_TO_CONTAINER = "toDateContainer";
    private static final String ID_INTERVAL_PRESETS_CONTAINER = "intervalPresetsContainer";
    private static final String ID_INTERVAL_PRESETS = "intervalPresets";
    private static final String ID_INTERVAL_PRESET = "intervalPreset";

    private IModel<XMLGregorianCalendar> fromDateModel;

    private IModel<XMLGregorianCalendar> toDateModel;

    private IModel<List<NamedIntervalPreset>> intervalPresetsModel = Model.ofList(NamedIntervalPreset.DEFAULT_PRESETS);

    public DateIntervalSearchPopupPanel(String id, Popover popover, IModel<XMLGregorianCalendar> fromDateModel, IModel<XMLGregorianCalendar> toDateModel) {
        super(id, popover);
        this.fromDateModel = fromDateModel;
        this.toDateModel = toDateModel;
    }

    @Override
    protected void customizationPopoverForm(MidpointForm popoverForm) {
        WebMarkupContainer fromDateContainer = new WebMarkupContainer(ID_FROM_DATE_CONTAINER);
        fromDateContainer.add(createDateContainerClassBehavior());
        popoverForm.add(fromDateContainer);

        WebMarkupContainer fromLabel = new WebMarkupContainer(ID_FROM_LABEL);
        fromLabel.add(createLabelClassBehavior());
        fromLabel.add(new VisibleBehaviour(() -> isInterval()));
        fromDateContainer.add(fromLabel);

        DateTimePickerPanel fromDatePanel = DateTimePickerPanel.createByXMLGregorianCalendarModel(ID_DATE_FROM_VALUE, fromDateModel);
        fromDatePanel.add(createDateClassBehavior());
        fromDatePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        fromDatePanel.getBaseFormComponent().add(AttributeAppender.append("aria-label", LocalizationUtil.translate("UserReportConfigPanel.dateFrom")));
        fromDateContainer.add(fromDatePanel);

        WebMarkupContainer toContainer = new WebMarkupContainer(ID_DATE_TO_CONTAINER);
        toContainer.add(createDateContainerClassBehavior());
        toContainer.add(new VisibleBehaviour(() -> isInterval()));
        popoverForm.add(toContainer);

        WebMarkupContainer toLabel = new WebMarkupContainer(ID_TO_LABEL);
        toLabel.add(createLabelClassBehavior());
        toContainer.add(toLabel);

        DateTimePickerPanel toDatePanel = DateTimePickerPanel.createByXMLGregorianCalendarModel(ID_DATE_TO_VALUE, toDateModel);
        toDatePanel.add(createDateClassBehavior());
        toDatePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        toDatePanel.getBaseFormComponent().add(AttributeAppender.append("aria-label", LocalizationUtil.translate("UserReportConfigPanel.dateTo")));
        toContainer.add(toDatePanel);

        WebMarkupContainer intervalPresetsContainer = new WebMarkupContainer(ID_INTERVAL_PRESETS_CONTAINER);
        intervalPresetsContainer.add(new VisibleBehaviour(() -> !intervalPresetsModel.getObject().isEmpty()));
        popoverForm.add(intervalPresetsContainer);

        ListView<NamedIntervalPreset> intervalPresets = new ListView<>(ID_INTERVAL_PRESETS, intervalPresetsModel) {

            @Override
            protected void populateItem(ListItem<NamedIntervalPreset> item) {
                IModel<String> presetLabelModel = new LoadableDetachableModel<>() {

                    @Override
                    protected String load() {
                        NamedIntervalPreset preset = item.getModelObject();
                        return LocalizationUtil.translateMessage(preset.text());
                    }
                };

                AjaxButton intervalPreset = new AjaxButton(ID_INTERVAL_PRESET, presetLabelModel) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        intervalPressedClicked(target, item.getModelObject());
                    }
                };
                intervalPreset.add(createSelectedBehavior(item.getModel()));
                item.add(intervalPreset);
            }
        };
        intervalPresetsContainer.add(intervalPresets);

        DateValidator validator = WebComponentUtil.getRangeValidator(popoverForm, SchemaConstants.PATH_ACTIVATION);
        validator.setDateFrom(toDatePanel.getBaseFormComponent());
        validator.setDateFrom(fromDatePanel.getBaseFormComponent());
    }

    private Behavior createSelectedBehavior(IModel<NamedIntervalPreset> model) {
        return AttributeAppender.append("class", () -> {
            XMLGregorianCalendar from = fromDateModel.getObject();
            XMLGregorianCalendar to = toDateModel.getObject();

            if (from == null || to == null) {
                return null;
            }

            Duration duration = model.getObject().duration();
            XMLGregorianCalendar fromCloned = XmlTypeConverter.createXMLGregorianCalendar(from);
            fromCloned.add(duration);

            if (fromCloned.compare(to) == 0) {
                return "bg-primary";
            }

            return null;
        });
    }

    private void intervalPressedClicked(AjaxRequestTarget target, NamedIntervalPreset preset) {
        long currentTime = System.currentTimeMillis();

        XMLGregorianCalendar from = XmlTypeConverter.createXMLGregorianCalendar(currentTime);
        from.add(preset.duration().negate());

        XMLGregorianCalendar to = XmlTypeConverter.createXMLGregorianCalendar(currentTime);

        fromDateModel.setObject(from);
        toDateModel.setObject(to);

        target.add(getPopoverForm());
    }

    private boolean isIntervalAndPresetsAvailable() {
        return isInterval() && intervalPresetsModel.getObject().isEmpty();
    }

    private Behavior createDateContainerClassBehavior() {
        return AttributeAppender.append(
                "class",
                () -> isIntervalAndPresetsAvailable() ? "row" : null);
    }

    private Behavior createDateClassBehavior() {
        return AttributeAppender.append(
                "class",
                () -> isIntervalAndPresetsAvailable() ? "col-10" : null);
    }

    private Behavior createLabelClassBehavior() {
        return AttributeAppender.append(
                "class",
                () -> isIntervalAndPresetsAvailable() ? "col-2" : null);
    }

    protected boolean isInterval() {
        return true;
    }

    public IModel<List<NamedIntervalPreset>> getIntervalPresetsModel() {
        return intervalPresetsModel;
    }

    public void setIntervalPresetsModel(IModel<List<NamedIntervalPreset>> intervalPresetsModel) {
        this.intervalPresetsModel = intervalPresetsModel;
    }
}
