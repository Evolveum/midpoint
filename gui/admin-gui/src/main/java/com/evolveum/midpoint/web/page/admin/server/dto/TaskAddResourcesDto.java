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

	@Override
    public TaskAddResourcesDto clone() {
		return new TaskAddResourcesDto(oid, name);
	}
}
