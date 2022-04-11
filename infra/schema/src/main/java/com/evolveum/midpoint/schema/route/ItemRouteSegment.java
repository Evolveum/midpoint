/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.route;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRouteSegmentType;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

@Experimental
public class ItemRouteSegment {

    @NotNull final ItemPath path;

    @Nullable private final SearchFilterType selectorBean;

    // TODO check for compatible parsing
    private QName parsedFor;

    private ObjectFilter parsedSelector;

    private ItemRouteSegment(@NotNull ItemPath path, @Nullable SearchFilterType selectorBean) {
        this.path = path;
        this.selectorBean = selectorBean;
    }

    public static @NotNull ItemRouteSegment fromBean(@NotNull ItemRouteSegmentType bean) {
        return new ItemRouteSegment(
                bean.getPath() != null ? bean.getPath().getItemPath() : ItemPath.EMPTY_PATH,
                bean.getSelector());
    }

    public static @NotNull ItemRouteSegment fromPath(ItemPath path) {
        return new ItemRouteSegment(path, null);
    }

    public List<PrismValue> filter(List<PrismValue> prismValues) throws SchemaException {
        if (prismValues.isEmpty()) {
            return List.of();
        }
        if (selectorBean == null) {
            return prismValues;
        }
        if (parsedSelector == null) {
            parsedSelector = parseSelector(prismValues);
        }
        ObjectFilter filter = parseSelector(prismValues);
        List<PrismValue> matching = new ArrayList<>();
        for (PrismValue prismValue : prismValues) {
            if (prismValue instanceof PrismContainerValue) {
                if (filter.match((PrismContainerValue<?>) prismValue, SchemaService.get().matchingRuleRegistry())) {
                    matching.add(prismValue);
                }
            }
        }
        return matching;
    }

    private ObjectFilter parseSelector(List<PrismValue> prismValues) throws SchemaException {
        assert selectorBean != null;
        ItemDefinition<?> itemDefinition = null;
        for (PrismValue prismValue : prismValues) {
            if (prismValue.getParent() != null && prismValue.getParent().getDefinition() != null) {
                itemDefinition = prismValue.getParent().getDefinition();
            }
        }
        if (itemDefinition == null) {
            throw new IllegalStateException("Cannot filter on '" + selectorBean
                    + "' as there's no definition for " + prismValues);
        } else if (!(itemDefinition instanceof PrismContainerDefinition)) {
            throw new IllegalStateException("Cannot filter on '" + selectorBean
                    + "' as the definition is not a container one: " + itemDefinition);
        }
        PrismContainerDefinition<?> containerDefinition = (PrismContainerDefinition<?>) itemDefinition;

        return PrismContext.get().getQueryConverter().parseFilter(selectorBean, containerDefinition);
    }

    @NotNull ItemRouteSegment replacePath(ItemPath newPath) {
        return new ItemRouteSegment(newPath, selectorBean);
    }

    public @NotNull ItemPath getPath() {
        return path;
    }

    public ObjectFilter getParsedSelector() {
        return parsedSelector;
    }

    @Override
    public String toString() {
        return path +
                (parsedSelector != null ?
                        "[" + parsedSelector + "]" :
                        selectorBean != null ? "[" + selectorBean + "]" : "");
    }
}
