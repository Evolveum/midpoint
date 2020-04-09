/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugSearchDto;

/**
 * @author lazyman
 */
public class ConfigurationStorage implements PageStorage {

    private static final long serialVersionUID = 1L;

    private DebugSearchDto debugSearchDto;

    private ObjectPaging debugSearchPaging;

    @Override
    public Search getSearch() {
        return debugSearchDto.getSearch();
    }

    @Override
    public void setSearch(Search search) {
        debugSearchDto.setSearch(search);
    }

    public void setDebugSearchDto(DebugSearchDto debugSearchDto) {
        this.debugSearchDto = debugSearchDto;
    }

    public DebugSearchDto getDebugSearchDto() {
        return debugSearchDto;
    }

    @Override
    public ObjectPaging getPaging() {
        return debugSearchPaging;
    }

    @Override
    public void setPaging(ObjectPaging debugSearchPaging) {
        this.debugSearchPaging = debugSearchPaging;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ConfigurationStorage\n");
        DebugUtil.debugDumpWithLabelLn(sb, "debugSearchDto", debugSearchDto, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "debugSearchPaging", debugSearchPaging, indent+1);
        return sb.toString();
    }
}
