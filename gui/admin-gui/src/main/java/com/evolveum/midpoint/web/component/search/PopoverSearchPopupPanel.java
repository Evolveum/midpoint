/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.util.DateValidator;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.XMLGregorianCalendar;

public class PopoverSearchPopupPanel<T> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_POPOVER_FORM = "popoverForm";
    private static final String ID_CONFIRM_BUTTON = "confirmButton";


    public PopoverSearchPopupPanel(String id) {
        super(id);
    }

    public PopoverSearchPopupPanel(String id, IModel<T> model) {
        super(id,model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        MidpointForm midpointForm = new MidpointForm(ID_POPOVER_FORM);
        midpointForm.setOutputMarkupId(true);
        add(midpointForm);

        customizationPopoverForm(midpointForm);

        AjaxButton confirm = new AjaxButton(ID_CONFIRM_BUTTON, createStringResource("Button.ok")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (isItemPanelEnabled()) {
                    confirmPerformed(target);
                }
            }
        };
        midpointForm.add(confirm);

    }

    protected void customizationPopoverForm(MidpointForm midpointForm){

    }

    protected void confirmPerformed(AjaxRequestTarget target){

    }

    protected Boolean isItemPanelEnabled() {
        return true;
    }

    public MidpointForm getPopoverForm() {
        return (MidpointForm) get(ID_POPOVER_FORM);
    }
}
