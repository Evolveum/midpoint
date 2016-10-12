/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.self.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.session.SessionStorage;

/**
 * Created by honchar.
 */
public enum AssignmentViewType {
    ROLE_CATALOG_VIEW, ROLE_TYPE, ORG_TYPE, SERVICE_TYPE;

    public static AssignmentViewType getViewTypeFromSession(PageBase pageBase){
        SessionStorage storage = pageBase.getSessionStorage();
        return storage.getRoleCatalog().getViewType();
    }

    public static void saveViewTypeToSession(PageBase pageBase, AssignmentViewType viewType){
        SessionStorage storage = pageBase.getSessionStorage();
        storage.getRoleCatalog().setViewType(viewType);
    }
}
