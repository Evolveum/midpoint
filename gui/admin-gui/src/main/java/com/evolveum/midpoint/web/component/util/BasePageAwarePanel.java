/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;

/**
 * @author semancik
 */
public class BasePageAwarePanel<T> extends BasePanel<T> {

    public BasePageAwarePanel(String id, PageBase parentPage) {
        this(id, null, parentPage);
    }

    public BasePageAwarePanel(String id, IModel<T> model, PageBase parentPage) {
        super(id, model);

        initLayout(parentPage);
    }

    protected void initLayout(PageBase parentPage) {

    }
}
