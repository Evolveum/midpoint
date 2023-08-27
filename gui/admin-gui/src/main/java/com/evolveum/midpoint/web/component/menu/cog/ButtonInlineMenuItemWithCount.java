/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu.cog;

import org.apache.wicket.model.IModel;

public abstract class ButtonInlineMenuItemWithCount extends ButtonInlineMenuItem {

    public ButtonInlineMenuItemWithCount(IModel<String> labelModel){
        super(labelModel);
    }

    public ButtonInlineMenuItemWithCount(IModel<String> labelModel, boolean isSubmit){
        super(labelModel, isSubmit);
    }

    protected abstract int getCount();

}
