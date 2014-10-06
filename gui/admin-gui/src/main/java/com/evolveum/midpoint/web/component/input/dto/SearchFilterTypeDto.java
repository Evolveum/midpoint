/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.component.input.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class SearchFilterTypeDto implements Serializable{

    private static final Trace LOGGER = TraceManager.getTrace(SearchFilterTypeDto.class);

    public static final String F_FILTER_CLAUSE = "filterClause";
    public static final String F_FILTER_OBJECT = "filterObject";

    private String filterClause;
    private SearchFilterType filterObject;

    public SearchFilterTypeDto(SearchFilterType filter, PrismContext prismContext){
        if(filter != null){
            filterObject = filter;
        } else {
            filterObject = new SearchFilterType();
        }

        if(filterObject.getFilterClauseXNode() != null){
            filterClause = loadFilterClause(prismContext);
        }
    }

    private String loadFilterClause(PrismContext prismContext){
        String fClause;

        try {
            RootXNode clause = filterObject.getFilterClauseAsRootXNode();

            fClause = prismContext.serializeXNodeToString(clause, PrismContext.LANG_XML);
        } catch (SchemaException e){
            LoggingUtils.logException(LOGGER, "Could not load filterClause from SearchFilterType object.", e);

            //TODO - find better solution to inform user about fail in filterClause loading
            fClause = e.getMessage();
        }

        return fClause;
    }

    public void updateFilterClause(PrismContext context) throws SchemaException{
        if(filterObject == null){
            filterObject = new SearchFilterType();
        }

        if(filterClause != null && StringUtils.isNotEmpty(filterClause)){

            if(LOGGER.isTraceEnabled()){
                LOGGER.trace("Filter Clause to serialize: " + filterClause);
            }

            RootXNode filterClauseNode = (RootXNode) context.parseToXNode(filterClause, PrismContext.LANG_XML);

            filterObject.setFilterClauseXNode(filterClauseNode);
        } else {
            String oldDescription = filterObject.getDescription();

            filterObject = new SearchFilterType();
            filterObject.setDescription(oldDescription);
        }
    }

    public String getFilterClause() {
        return filterClause;
    }

    public void setFilterClause(String filterClause) {
        this.filterClause = filterClause;
    }

    public SearchFilterType getFilterObject() {
        return filterObject;
    }

    public void setFilterObject(SearchFilterType filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchFilterTypeDto)) return false;

        SearchFilterTypeDto that = (SearchFilterTypeDto) o;

        if (filterClause != null ? !filterClause.equals(that.filterClause) : that.filterClause != null) return false;
        if (filterObject != null ? !filterObject.equals(that.filterObject) : that.filterObject != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = filterClause != null ? filterClause.hashCode() : 0;
        result = 31 * result + (filterObject != null ? filterObject.hashCode() : 0);
        return result;
    }
}
