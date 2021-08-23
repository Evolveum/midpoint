/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.Serializable;

public class PageObjectModel<O extends ObjectType> implements Serializable {

    private PrismObjectWrapper<O> wrapper;
    private GuiObjectDetailsPageType detailsPageConfiguration;

    public PageObjectModel() {

    }

    public void setWrapper(PrismObjectWrapper<O> wrapper) {
        this.wrapper = wrapper;
    }

    public PrismObjectWrapper<O> getWrapper() {
        return wrapper;
    }

    public void setDetailsPageConfiguration(GuiObjectDetailsPageType detailsPageConfiguration) {
        this.detailsPageConfiguration = detailsPageConfiguration;
    }
}
