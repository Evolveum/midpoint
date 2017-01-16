/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.menu.cog;

import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import org.apache.wicket.model.IModel;

/**
 * Created honchar.
 */
public class ButtonInlineMenuItem extends BasicInlineMenuItem {
    private int id = -1;
    private String buttonIconCssClass;
    private String buttonColorCssClass;

    public ButtonInlineMenuItem() {
        super();
    }

    public ButtonInlineMenuItem(IModel<String> label, boolean submit, InlineMenuItemAction action, int id) {
        super(label, submit, action);
        this.id = id;
        buttonColorCssClass = DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString();
    }

    public ButtonInlineMenuItem(IModel<String> label, boolean submit, InlineMenuItemAction action,
                                int id, String buttonIconCssClass) {
        super(label, submit, action);
        this.id = id;
        this.buttonIconCssClass = buttonIconCssClass;
        buttonColorCssClass = DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString();
    }

    public ButtonInlineMenuItem(IModel<String> label, boolean submit, InlineMenuItemAction action,
                                int id, String buttonIconCssClass, String buttonColorCssClass) {
        super(label, submit, action);
        this.id = id;
        this.buttonIconCssClass = buttonIconCssClass;
        this.buttonColorCssClass = buttonColorCssClass;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getButtonIconCssClass() {
        return buttonIconCssClass;
    }

    public void setButtonIconCssClass(String buttonIconCssClass) {
        this.buttonIconCssClass = buttonIconCssClass;
    }

    public String getButtonColorCssClass() {
        return buttonColorCssClass;
    }

    public void setButtonColorCssClass(String buttonColorCssClass) {
        this.buttonColorCssClass = buttonColorCssClass;
    }
}
