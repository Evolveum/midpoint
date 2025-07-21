/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import com.evolveum.midpoint.gui.impl.component.input.DateTimePickerPanel;

import jakarta.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.markup.html.form.Form;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author katka
 */
@Component
public class DatePanelFactory extends AbstractInputGuiComponentFactory<XMLGregorianCalendar> {

    @Autowired private GuiComponentRegistry registry;

    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return DOMUtil.XSD_DATETIME.equals(wrapper.getTypeName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<XMLGregorianCalendar> panelCtx) {
        DateTimePickerPanel panel = DateTimePickerPanel.createByXMLGregorianCalendarModel(panelCtx.getComponentId(), panelCtx.getRealValueModel());

        Form<?> form = Form.findForm(panelCtx.getParentContainer());
        DateValidator validator;
        String validatorErrorMessageKey;
        if (ScheduleType.F_EARLIEST_START_TIME.equals(panelCtx.getDefinitionName()) || ScheduleType.F_LATEST_START_TIME.equals(panelCtx.getDefinitionName())) {
            validator = WebComponentUtil.getRangeValidator(form, TaskType.F_SCHEDULE);
            validatorErrorMessageKey = "ScheduleType.dateValidator.errorMessage";
        } else {
            validator = WebComponentUtil.getRangeValidator(form, SchemaConstants.PATH_ACTIVATION);
            validatorErrorMessageKey = "DateValidator.message.fromAfterTo";
        }
        if (ActivationType.F_VALID_FROM.equals(panelCtx.getDefinitionName()) || ScheduleType.F_EARLIEST_START_TIME.equals(panelCtx.getDefinitionName())) {
            validator.setDateFrom(panel.getBaseFormComponent());
        } else if (ActivationType.F_VALID_TO.equals(panelCtx.getDefinitionName()) || ScheduleType.F_LATEST_START_TIME.equals(panelCtx.getDefinitionName())) {
            validator.setDateTo(panel.getBaseFormComponent());
        }
        validator.setMessageKey(validatorErrorMessageKey);
        return panel;
    }
}
