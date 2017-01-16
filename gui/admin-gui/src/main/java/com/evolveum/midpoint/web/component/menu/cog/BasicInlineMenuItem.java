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

import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
public class BasicInlineMenuItem {
    protected IModel<String> label;
    protected InlineMenuItemAction action;
    protected boolean submit;

    BasicInlineMenuItem(){
    }

    BasicInlineMenuItem(IModel<String> label, boolean submit, InlineMenuItemAction action){
        this.label = label;
        this.action = action;
        this.submit = submit;
    }

    public IModel<String> getLabel() {
        return label;
    }

    public void setLabel(IModel<String> label) {
        this.label = label;
    }

    public InlineMenuItemAction getAction() {
        return action;
    }

    public void setAction(InlineMenuItemAction action) {
        this.action = action;
    }

    /**
     * if true, link must be rendered as submit link button, otherwise normal ajax link
     */
    public boolean isSubmit() {
        return submit;
    }

    public void setSubmit(boolean submit) {
        this.submit = submit;
    }

    public boolean isDivider() {
        return label == null && action == null;
    }

    public boolean isMenuHeader() {
        return label != null && action == null;
    }
}
