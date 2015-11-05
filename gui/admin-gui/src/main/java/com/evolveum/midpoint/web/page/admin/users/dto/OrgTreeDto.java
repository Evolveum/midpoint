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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import javax.xml.namespace.QName;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class OrgTreeDto implements Serializable, Comparable<OrgTreeDto>, OrgDto {

    private OrgTreeDto parent;
    private String oid;
    private String name;
    private String description;
    private String displayName;
    private String identifier;
    private QName relation;
    
    private ObjectType object;

//    public OrgTreeDto(OrgTreeDto parent, String oid, QName relation, String name, String description,
//                      String displayName, String identifier) {
//        this.parent = parent;
//        this.oid = oid;
//        this.relation = relation;
//        this.name = name;
//        this.description = description;
//        this.displayName = displayName;
//        this.identifier = identifier;
//    }
    
    public OrgTreeDto(OrgTreeDto parent, PrismObject<OrgType> object) {
    	OrgType org = object.asObjectable();
    	this.object = org;
this.parent = parent;
this.oid = org.getOid();
//this.relation = relation;
this.name = WebMiscUtil.getOrigStringFromPoly(org.getName());
this.description = org.getDescription();
this.displayName = WebMiscUtil.getOrigStringFromPoly(org.getDisplayName());
this.identifier = org.getIdentifier();
}

    public OrgTreeDto getParent() {
        return parent;
    }

    @Override
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public QName getRelation() {
        return relation;
    }

    public void setRelation(QName relation) {
        this.relation = relation;
    }
    
    public ObjectType getObject() {
		return object;
	}
    
    public void setObject(ObjectType object) {
		this.object = object;
	}

    @Override
    public Class<? extends ObjectType> getType() {
        return OrgType.class;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrgTreeDto that = (OrgTreeDto) o;

        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (oid != null ? !oid.equals(that.oid) : that.oid != null) return false;
        if (relation != null ? !relation.equals(that.relation) : that.relation != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = oid != null ? oid.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        result = 31 * result + (relation != null ? relation.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "OrgTreeDto{oid='" + oid + '\'' + ",name='" + name + '\'' + '}';
    }

    @Override
    public int compareTo(OrgTreeDto o) {
        //todo implement [lazyman]
        return 0;
    }

    public void setParent(OrgTreeDto parent) {
        this.parent = parent;
    }
}
