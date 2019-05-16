/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
public class HistoryPageTabVisibleBehavior<O extends ObjectType> extends FocusTabVisibleBehavior<O>{
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
