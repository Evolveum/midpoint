/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * @author lazyman
 */
public class DebugSearchDto implements Serializable, DebugDumpable {
    private static final long serialVersionUID = 1L;

//    public static final String F_TYPE = "type";
    public static final String F_RESOURCE = "resource";
    public static final String F_OID_FILTER = "oidFilter";
    public static final String F_OBJECT_CLASS = "objectClass";
    public static final String F_SEARCH = "search";

//    private ObjectTypes type;
    private ObjectReferenceType resource;
    private Search search;
//    private String oidFilter;
    private QName objectClass;

    public DebugSearchDto(){
        resetResourceRef();
    }

    public ObjectReferenceType getResource() {
        return resource;
    }

    public void setResource(ObjectReferenceType resource) {
        this.resource = resource;
    }

    public Search getSearch() {
        return search;
    }

    public void setSearch(Search search) {
        this.search = search;
    }

    public QName getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(QName objectClass) {
        this.objectClass = objectClass;
    }

//    public String getOidFilter() {
//        return oidFilter;
//    }
//
//    public void setOidFilter(String oidFilter) {
//        this.oidFilter = oidFilter;
//    }

    public boolean isResourceEmpty() {
        return getResource() == null || getResource().getOid() == null || getResource().asReferenceValue().isEmpty();
    }

    public void resetResourceRef(){
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(ResourceType.COMPLEX_TYPE);
        this.resource = ref;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("DebugSearchDto\n");
//        DebugUtil.debugDumpWithLabelLn(sb, "type", type==null?null:type.toString(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "resource", resource==null?null:resource.toString(), indent+1);
        DebugUtil.debugDumpWithLabel(sb, "objectClass", objectClass==null? null : objectClass.toString(), indent+1);
        DebugUtil.debugDumpWithLabel(sb, "search", search, indent+1);
        return sb.toString();
    }
}
