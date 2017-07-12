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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

/**
 *  @author shood
 * */
public class AccountDetailsSearchDto implements Serializable, DebugDumpable {
	private static final long serialVersionUID = 1L;

    public static final String F_SEARCH_TEXT = "text";
    public static final String F_KIND = "kind";
    public static final String F_INTENT = "intent";
    public static final String F_OBJECT_CLASS = "objectClass";
    public static final String F_FAILED_OPERATION_TYPE = "failedOperationType";

    private String text;
    private ShadowKindType kind;
    private String intent;
    private String objectClass;
    private List<QName> objectClassList;
    private FailedOperationTypeType failedOperationType;

    public List<QName> getObjectClassList() {
        return objectClassList;
    }

    public void setObjectClassList(List<QName> objectClassList) {
        this.objectClassList = objectClassList;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ShadowKindType getKind() {
        return kind;
    }

    public void setKind(ShadowKindType kind) {
        this.kind = kind;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }
    
    
    public FailedOperationTypeType getFailedOperationType() {
		return failedOperationType;
	}
    
    public void setFailedOperationType(FailedOperationTypeType failedOperationType) {
		this.failedOperationType = failedOperationType;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("AccountDetailsSearchDto\n");
		DebugUtil.debugDumpWithLabelLn(sb, "text", text, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "kind", kind==null?null:kind.toString(), indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "intent", intent, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "objectClass", objectClass, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "objectClassList", objectClassList, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "failedOperationType", failedOperationType==null?null:failedOperationType.toString(), indent+1);
		return sb.toString();
	}
}
