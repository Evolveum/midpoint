/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;

public class PopoverSearchPopupPanel<T> extends BasePanel<T> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_POPOVER_FORM = "popoverForm";
    private static final String ID_CONFIRM_BUTTON = "confirmButton";
    private static final String ID_REMOVE = "remove";

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

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (isItemPanelEnabled()) {
                    confirmPerformed(target);
                }
            }
        };
        midpointForm.add(confirm);

        AjaxButton remove = new AjaxButton(ID_REMOVE, createStringResource("Button.clear")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeSearchValue(target);
            }
        };
        midpointForm.add(remove);
    }

    protected void customizationPopoverForm(MidpointForm midpointForm){

    }

    protected void confirmPerformed(AjaxRequestTarget target){

    }

    protected Boolean isItemPanelEnabled() {
        return true;
    }


    protected void removeSearchValue(AjaxRequestTarget target) {

    }
}
