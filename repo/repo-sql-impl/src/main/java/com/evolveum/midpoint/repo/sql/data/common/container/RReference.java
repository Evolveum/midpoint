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

package com.evolveum.midpoint.repo.sql.data.common.container;

import com.evolveum.midpoint.repo.sql.data.common.ObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author lazyman
 * @author mederly
 *
 * TODO
 *
 */
public abstract class RReference implements ObjectReference {

    //other primary key fields
    private String targetOid;
    private String relation;
    private RObjectType type;

	public RObject getTarget() {        // for HQL use only
        return null;
    }

    @Override
    public String getTargetOid() {
        return targetOid;
    }

    @Override
    public String getRelation() {
        return relation;
    }

    @Override
    public RObjectType getType() {
        return type;
    }

    @Override
    public void setRelation(String relation) {
        this.relation = relation;
    }

    public void setTarget(RObject target) {     // shouldn't be called
    }

    @Override
    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    @Override
    public void setType(RObjectType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RReference ref = (RReference) o;

        if (targetOid != null ? !targetOid.equals(ref.targetOid) : ref.targetOid != null) return false;
        if (type != ref.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = targetOid != null ? targetOid.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (relation != null ? relation.hashCode() : 0);

        return result;
    }

    public static void copyFromJAXB(ObjectReferenceType jaxb, RReference repo) {
        RObjectReference.copyFromJAXB(jaxb, repo);
    }
}
