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

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class OrgTableDto extends Selectable implements OrgDto, InlineMenuable {

    public static final String F_NAME = "name";
    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_IDENTIFIER = "identifier";
    public static final String F_DESCRIPTION = "description";
    public static final String F_OBJECT = "object";

    private String oid;
    private Class<? extends ObjectType> type;

    private String name;
    private String displayName;
    private QName relation;
    private String identifier;
    private String description;
    private ObjectType object;

    public OrgTableDto(String oid, Class<? extends ObjectType> type) {
        this.oid = oid;
        this.type = type;
    }

    public static OrgTableDto createDto(PrismObject<? extends ObjectType> object) {
        OrgTableDto dto = new OrgTableDto(object.getOid(), object.getCompileTimeClass());
        dto.name = WebComponentUtil.getName(object);
        dto.description = object.getPropertyRealValue(OrgType.F_DESCRIPTION, String.class);
        dto.displayName = WebComponentUtil.getOrigStringFromPoly(
                object.getPropertyRealValue(OrgType.F_DISPLAY_NAME, PolyString.class));
        dto.identifier = object.getPropertyRealValue(OrgType.F_IDENTIFIER, String.class);

        dto.relation = WebComponentUtil.isObjectOrgManager(object) ? SchemaConstants.ORG_MANAGER : null;
        dto.object = object.asObjectable();

        return dto;
    }

    @Override
    public String getOid() {
        return oid;
    }

    @Override
    public Class<? extends ObjectType> getType() {
        return type;
    }

    @Override
    public QName getRelation() {
        return relation;
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

    @Override
    public String getName() {
        return name;
    }

    public ObjectType getObject() {
        return object;
    }

    public void setObject(ObjectType object) {
        this.object = object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrgTableDto)) return false;

        OrgTableDto that = (OrgTableDto) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (object != null ? !object.equals(that.object) : that.object != null) return false;
        if (oid != null ? !oid.equals(that.oid) : that.oid != null) return false;
        if (relation != null ? !relation.equals(that.relation) : that.relation != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = oid != null ? oid.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (relation != null ? relation.hashCode() : 0);
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (object != null ? object.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "OrgTableDto{oid='" + oid + '\'' + ",name='" + name + '\''
                + ", type=" + (type != null ? type.getSimpleName() : null) + '}';
    }

    @Override
    public List<InlineMenuItem> getMenuItems() {
        return null;
    }
}
