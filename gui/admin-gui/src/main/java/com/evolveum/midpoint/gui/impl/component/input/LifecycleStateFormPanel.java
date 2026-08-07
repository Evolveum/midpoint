/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import org.apache.wicket.model.IModel;

public class LifecycleStateFormPanel extends LifecycleStatePanel {

    public LifecycleStateFormPanel(String id, IModel<PrismPropertyWrapper<String>> model) {
        super(id, model);
    }

    @Override
    protected String customCssClassForInputField() {
        return "w-100";
    }
}
