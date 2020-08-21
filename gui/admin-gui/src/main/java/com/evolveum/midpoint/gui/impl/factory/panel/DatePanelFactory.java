/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.Form;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

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
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return DOMUtil.XSD_DATETIME.equals(wrapper.getTypeName());
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<XMLGregorianCalendar> panelCtx) {
        DatePanel panel = new DatePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel());

        Form<?> form = Form.findForm(panelCtx.getForm());
        DateValidator validator = WebComponentUtil.getRangeValidator(form, SchemaConstants.PATH_ACTIVATION);
        if (ActivationType.F_VALID_FROM.equals(panelCtx.getDefinitionName())) {
            validator.setDateFrom((DateTimeField) panel.getBaseFormComponent());
        } else if (ActivationType.F_VALID_TO.equals(panelCtx.getDefinitionName())) {
            validator.setDateTo((DateTimeField) panel.getBaseFormComponent());
        }

        return panel;
    }
}
