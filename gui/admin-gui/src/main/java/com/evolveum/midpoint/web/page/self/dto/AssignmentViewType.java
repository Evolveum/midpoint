/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar.
 */
public enum AssignmentViewType {
    ROLE_CATALOG_VIEW(SchemaConstants.OBJECT_COLLECTION_ROLE_CATALOG_URI, AbstractRoleType.class),
    ROLE_TYPE(SchemaConstants.OBJECT_COLLECTION_ALL_ROLES_URI, RoleType.class),
    ORG_TYPE(SchemaConstants.OBJECT_COLLECTION_ALL_ORGS_URI, OrgType.class),
    SERVICE_TYPE(SchemaConstants.OBJECT_COLLECTION_ALL_SERVICES_URI, ServiceType.class),
    USER_TYPE(SchemaConstants.OBJECT_COLLECTION_USER_ASSIGNMENTS_URI, AbstractRoleType.class);

    private String uri;
    private Class type;

    AssignmentViewType(String uri, Class type){
        this.uri = uri;
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public Class getType(){
        return type;
    }

}
