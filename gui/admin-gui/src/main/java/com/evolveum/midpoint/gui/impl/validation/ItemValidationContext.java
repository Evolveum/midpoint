/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ItemValidationContext {

    private Class<? extends ObjectType> type;

    private PageBase page;

    public PageBase page() {
        return page;
    }

    public ItemValidationContext page(PageBase page) {
        this.page = page;
        return this;
    }

    public Class<? extends ObjectType> type() {
        return type;
    }

    public ItemValidationContext type(Class<? extends ObjectType> type) {
        this.type = type;
        return this;
    }
}
