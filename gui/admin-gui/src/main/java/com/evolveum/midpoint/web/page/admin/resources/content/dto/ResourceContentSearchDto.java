/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources.content.dto;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ResourceContentSearchDto implements Serializable, DebugDumpable {
    private static final long serialVersionUID = 1L;

    private Boolean resourceSearch = Boolean.FALSE;
    private ShadowKindType kind;
    private String intent;
    private QName objectClass;
    private String resourceOid;

    private boolean isUseObjectClass = false;

    public ResourceContentSearchDto(ShadowKindType kind) {
        this.kind = kind;
        if (kind == null) {
            this.isUseObjectClass = true;
        }
    }

    public Boolean isResourceSearch() {
        return resourceSearch;
    }
    public void setResourceSearch(Boolean resourceSearch) {
        this.resourceSearch = resourceSearch;
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

    public QName getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(QName objectClass) {
        this.objectClass = objectClass;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public boolean isUseObjectClass() {
        return isUseObjectClass;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ResourceContentSearchDto\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceSearch", resourceSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "kind", kind==null?null:kind.toString(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "intent", intent, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectClass", objectClass, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "isUseObjectClass", isUseObjectClass, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "resourceOid", resourceOid, indent+1);
        return sb.toString();
    }

}
