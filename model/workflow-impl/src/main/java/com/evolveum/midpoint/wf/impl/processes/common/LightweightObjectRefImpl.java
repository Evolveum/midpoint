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

package com.evolveum.midpoint.wf.impl.processes.common;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import javax.xml.namespace.QName;
import java.io.Serializable;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.prism.xml.ns._public.types_3.PolyStringType.fromOrig;

/**
 * @author mederly
 */
public class LightweightObjectRefImpl implements LightweightObjectRef, Serializable {

    private static final long serialVersionUID = 8339882391823107167L;
    private String oid;
    private QName type;
    private String description;
    private String targetName;

    public LightweightObjectRefImpl(String oid, QName type, String description) {
        this.oid = oid;
        this.type = type;
        this.description = description;
    }

    public LightweightObjectRefImpl(ObjectReferenceType objectReferenceType) {
        this.oid = objectReferenceType.getOid();
        this.type = objectReferenceType.getType();
        this.description = objectReferenceType.getDescription();
        this.targetName = getOrig(objectReferenceType.getTargetName());
    }

    public LightweightObjectRefImpl(String value) {
        this(value, null, null);
    }

    @Override
    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    @Override
    public QName getType() {
        return type;
    }

    public void setType(QName type) {
        this.type = type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    @Override
    public ObjectReferenceType toObjectReferenceType() {
        ObjectReferenceType retval = new ObjectReferenceType();
        retval.setOid(oid);
        retval.setDescription(description);
        retval.setType(type);
        retval.setTargetName(fromOrig(targetName));
        return retval;
    }

    @Override
    public String toString() {
        return "LightweightObjectRefImpl{" +
                "oid='" + oid + '\'' +
                ", targetName=" + targetName +
                ", type=" + type +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpWithLabel(sb, "LightweightObjectRef", toDebugName(), indent);
		return sb.toString();
    }
}
