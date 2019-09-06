/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.util.Choiceable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.io.Serializable;

/**
 * TODO replace with ObjectReferenceType
 *
 * @author mserbak
 */
public class TaskAddResourcesDto implements Serializable, Choiceable, Cloneable {
	private String name;
	private String oid;

	public TaskAddResourcesDto(String oid, String name) {
		this.oid = oid;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskAddResourcesDto)) return false;

        TaskAddResourcesDto that = (TaskAddResourcesDto) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (oid != null ? !oid.equals(that.oid) : that.oid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (oid != null ? oid.hashCode() : 0);
        return result;
    }

	public ObjectReferenceType asObjectReferenceType() {
		return ObjectTypeUtil.createObjectRef(oid, PolyStringType.fromOrig(name), ObjectTypes.RESOURCE);
	}

	public TaskAddResourcesDto clone() {
		return new TaskAddResourcesDto(oid, name);
	}
}
