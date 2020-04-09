/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu.cog;

import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public abstract class ButtonInlineMenuItem extends InlineMenuItem {

    public ButtonInlineMenuItem(IModel<String> labelModel){
        super(labelModel);
    }

    public ButtonInlineMenuItem(IModel<String> labelModel, boolean isSubmit){
        super(labelModel, isSubmit);
    }

    public abstract String getButtonIconCssClass();
}
