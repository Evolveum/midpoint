/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class MultiButtonPanel2<T> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_BUTTONS = "buttons";

    private int numberOfButtons;

    public MultiButtonPanel2(String id, IModel<T> model, int numberOfButtons) {
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
