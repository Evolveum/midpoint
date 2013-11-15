/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users.dto;

import com.evolveum.midpoint.web.component.util.Selectable;

/**
 * @author lazyman
 */
public class OrgTreeDto extends Selectable implements Comparable<OrgTreeDto> {

    private OrgTreeDto parent;
    private String oid;
    private String name;
    private String description;
    private String displayName;
    private String identifier;

    public OrgTreeDto(OrgTreeDto parent, String oid, String name, String description,
                      String displayName, String identifier) {
        this.parent = parent;
        this.oid = oid;
        this.name = name;
        this.description = description;
        this.displayName = displayName;
        this.identifier = identifier;
    }

    public OrgTreeDto getParent() {
        return parent;
    }

    public String getOid() {
        return oid;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "OrgTreeDto{" + ", oid='" + oid + '\'' + ", name='" + name + '\'' + '}';
    }

    @Override
    public int compareTo(OrgTreeDto o) {
        //todo implement [lazyman]
        return 0;
    }
}
