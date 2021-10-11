/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MultiButtonPanel<T> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_BUTTONS = "buttons";

    private int numberOfButtons;

    public MultiButtonPanel(String id, IModel<T> model, int numberOfButtons) {
        super(id, model);

        this.numberOfButtons = numberOfButtons;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    public int getNumberOfButtons() {
        return numberOfButtons;
    }

    private void initLayout() {
        RepeatingView buttons = new RepeatingView(ID_BUTTONS);
        add(buttons);

        for (int id = 0; id < numberOfButtons; id++) {
            AjaxIconButton button = createButton(id, buttons.newChildId(), getModel());
            if (button != null) {
                buttons.add(button);
            }
        }
    }

    protected AjaxIconButton createButton(int index, String componentId, IModel<T> model) {
        return null;
    }

    protected AjaxIconButton buildDefaultButton(String componentId, IModel<String> icon, IModel<String> title,
                                                IModel<String> cssClass, final AjaxEventProcessor onClickProcessor) {
        AjaxIconButton btn = new AjaxIconButton(componentId, icon, title) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (onClickProcessor != null) {
                    onClickProcessor.onEventPerformed(target);
                }
            }
        };

        btn.showTitleAsLabel(true);
        if (cssClass != null) {
            btn.add(AttributeAppender.append("class", cssClass));
        }

        return btn;
    }
}
