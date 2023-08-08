/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.query;


import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractTypedQuery<T, F extends AbstractTypedQuery<T,F>> {

    protected Class<T> type;
    private final GetOperationOptionsBuilder optionsBuilder;

    private List<ObjectOrdering> orderBy = new ArrayList<>();

    private Integer offset;
    private Integer maxSize;

    private Collection<SelectorOptions<GetOperationOptions>> options;

    public AbstractTypedQuery(Class<T> type) {
        this.type = type;
        this.optionsBuilder = SchemaService.get().getOperationOptionsBuilder();
    }


    abstract protected F self();


    public GetOperationOptionsBuilder operationOptionsBuilder() {
        return optionsBuilder;
    }

    public Integer getOffset() {
        return offset;
    }

    public F offset(Integer offset) {
        this.offset = offset;
        return self();
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public F maxSize(Integer maxSize) {
        this.maxSize = maxSize;
        return self();
    }

    public F orderBy(ItemPath path, OrderDirection direction) {
        orderBy.add(PrismContext.get().queryFactory().createOrdering(path, direction));
        return self();
    }

    protected boolean pagingRequired() {
        return maxSize != null || offset != null || !orderBy.isEmpty();
    }

    @Nullable
    protected ObjectPaging buildObjectPaging() {
        if (pagingRequired()) {
            var paging = PrismContext.get().queryFactory().createPaging(offset, maxSize);
            paging.setOrdering(new ArrayList<>(orderBy));
            return paging;
        }
        return null;
    }

    protected void fillFrom(AbstractTypedQuery<?,?> other) {
        offset = other.offset;
        maxSize = other.maxSize;
        orderBy = new ArrayList<>(other.orderBy);
    }
}

