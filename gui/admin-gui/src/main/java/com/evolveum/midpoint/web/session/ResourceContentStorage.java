/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.ResourceContentSearchDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ResourceContentStorage implements PageStorage {
    private static final long serialVersionUID = 1L;

    private ResourceContentSearchDto contentSearch;
    private Boolean resourceSearch = Boolean.FALSE;

    private Search attributeSearch;
    private ObjectPaging paging;

    private ShadowKindType kind;

    public ResourceContentStorage(ShadowKindType kind) {
        this.kind = kind;
    }

    @Override
    public Search getSearch() {
        return attributeSearch;
    }

    @Override
    public void setSearch(Search search) {
        this.attributeSearch = search;
    }

    public Boolean getResourceSearch() {
        return resourceSearch;
    }

    public void setResourceSearch(Boolean resourceSearch) {
        this.resourceSearch = resourceSearch;
    }

    @Override
    public void setPaging(ObjectPaging paging) {
        this.paging = paging;

    }

    @Override
    public ObjectPaging getPaging() {
        return paging;
    }

    public ResourceContentSearchDto getContentSearch() {
        if (contentSearch == null) {
            contentSearch = new ResourceContentSearchDto(kind);
        }
        return contentSearch;
    }

    public void setContentSearch(ResourceContentSearchDto contentSearch) {
        this.contentSearch = contentSearch;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ResourceContentStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceSearch", resourceSearch, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "contentSearch", contentSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "attributeSearch", attributeSearch, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "paging", paging, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "kind", kind==null?null:kind.toString(), indent+1);
        return sb.toString();
    }

}
