/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;

/**
 * @author lazyman
 */
public class PageAdmin extends PageBase {

    public PageAdmin() {
        this(null);
    }

    public PageAdmin(PageParameters parameters) {
        super(parameters);
    }
}
