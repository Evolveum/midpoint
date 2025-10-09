/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
