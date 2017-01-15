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

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mederly
 */
public interface LightweightObjectRef extends DebugDumpable {
    String getOid();

    QName getType();

    String getDescription();

    String getTargetName();

    ObjectReferenceType toObjectReferenceType();

    static List<String> toDebugNames(Collection<LightweightObjectRef> refs) {
        return refs.stream()
                .map(LightweightObjectRef::toDebugName)
				.collect(Collectors.toList());
    }

    default String toDebugName() {
    	return QNameUtil.getLocalPart(getType()) + ":" + getOid() + " (" + getTargetName() + ")";
	}
}
