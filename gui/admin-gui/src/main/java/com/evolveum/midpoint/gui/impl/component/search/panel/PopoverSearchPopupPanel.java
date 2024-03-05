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
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;

public class PopoverSearchPopupPanel<T> extends BasePanel<T> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_POPOVER_FORM = "popoverForm";
    private static final String ID_CONFIRM_BUTTON = "confirmButton";
    private static final String ID_REMOVE = "remove";
    private static final String ID_CANCEL_BUTTON = "cancelButton";

    private final Popover popover;

    public PopoverSearchPopupPanel(String id, Popover popover) {
        super(id);

        this.popover = popover;
    }

    public PopoverSearchPopupPanel(String id, Popover popover, IModel<T> model) {
        super(id,model);

        this.popover = popover;
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

        AjaxIconButton confirm = new AjaxIconButton(ID_CONFIRM_BUTTON, new Model<>("fa fa-check"),
                createStringResource("PopoverSearchPanel.applyButton"))
        {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (isItemPanelEnabled()) {
                    confirmPerformed(target);
                }
            }
        };
        confirm.showTitleAsLabel(true);
        midpointForm.add(confirm);

        AjaxButton remove = new AjaxButton(ID_REMOVE, createStringResource("PopoverSearchPanel.clearButton")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeSearchValue(target);
            }
        };
        midpointForm.add(remove);

        AjaxButton cancel = new AjaxButton(ID_CANCEL_BUTTON, createStringResource("PopoverSearchPanel.cancelButton")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                popover.toggle(target);
            }
        };
        midpointForm.add(cancel);

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

    protected final MidpointForm getPopoverForm() {
        return (MidpointForm) get(ID_POPOVER_FORM);
    }
}
