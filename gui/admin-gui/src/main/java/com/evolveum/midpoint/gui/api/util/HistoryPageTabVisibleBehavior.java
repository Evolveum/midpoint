/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
public class HistoryPageTabVisibleBehavior<O extends ObjectType> extends ObjectTabVisibleBehavior<O> {
    private static final long serialVersionUID = 1L;

    private boolean visibleOnHistoryPage = false;
    private boolean isHistoryPage = false;

    public HistoryPageTabVisibleBehavior(IModel<PrismObject<O>> objectModel, String uiAuthorizationUrl, boolean visibleOnHistoryPage, PageBase pageBase) {
        super(objectModel, uiAuthorizationUrl, pageBase);
        this.visibleOnHistoryPage = visibleOnHistoryPage;
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && visibleOnHistoryPage;
    }
}
