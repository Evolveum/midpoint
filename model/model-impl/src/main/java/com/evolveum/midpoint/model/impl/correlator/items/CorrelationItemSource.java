/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.SourceObjectType;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.util.MatchingUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * "Source side" of a {@link CorrelationItem}.
 *
 * TODO finish!
 */
public class CorrelationItemSource {

    /**
     * The complete path related to {@link #sourceObject}.
     * Does _not_ contain the variable reference ($focus, $projection, etc).
     */
    @NotNull private final ItemPath path;

    /** The source object from which the item(s) are selected by the {@link #path}. */
    @NotNull private final ObjectType sourceObject;

    /** Do we reference the focus or the projection? */
    @NotNull private final SourceObjectType sourceObjectType;

    private CorrelationItemSource(
            @NotNull ItemPath path,
            @NotNull ObjectType sourceObject,
            @NotNull SourceObjectType sourceObjectType) {
        this.path = path;
        this.sourceObject = sourceObject;
        this.sourceObjectType = sourceObjectType;
    }

    public static CorrelationItemSource create(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelationContext correlationContext) throws ConfigurationException {
        return forItem(
                getSourcePath(itemBean),
                correlationContext);
    }

    private static ItemPath getSourcePath(ItemCorrelationType itemBean) {
        if (itemBean.getSourcePath() != null) {
            return itemBean.getSourcePath().getItemPath();
        } else if (itemBean.getPath() != null) {
            return itemBean.getPath().getItemPath();
        } else {
            // TODO implement "ref" processing
            throw new UnsupportedOperationException("'ref' is not yet supported");
        }
    }

    private static CorrelationItemSource forItem(
            @NotNull ItemPath path,
            @NotNull CorrelationContext correlationContext) throws ConfigurationException {
        SourceObjectType sourceObjectType;
        ItemPath pathInObject;
        if (path.startsWithVariable()) {
            sourceObjectType = SourceObjectType.fromVariable(
                    Objects.requireNonNull(
                            path.firstToVariableNameOrNull()));
            pathInObject = path.rest();
        } else {
            sourceObjectType = SourceObjectType.FOCUS;
            pathInObject = path;
        }
        return new CorrelationItemSource(
                pathInObject,
                correlationContext.getSourceObject(sourceObjectType),
                sourceObjectType);
    }

    /**
     * Returns the source value that should be used for the correlation.
     * We assume there is a single one.
     */
    public Object getRealValue() {
        return MiscUtil.extractSingleton(
                MatchingUtil.getRealValuesForPath(sourceObject, path),
                () -> new UnsupportedOperationException("Multiple values in " + path + " are not supported"));
    }

    @Override
    public String toString() {
        return "CorrelationItemSource{" +
                "path=" + path +
                ", sourceObject=" + sourceObject +
                ", sourceObjectType=" + sourceObjectType +
                '}';
    }
}
