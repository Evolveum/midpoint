/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.mel.value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.evolveum.midpoint.model.common.expression.script.mel.MelComparable;
import com.evolveum.midpoint.model.common.expression.script.mel.MelException;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.QNameUtil;

import com.google.common.collect.ImmutableSet;
import dev.cel.common.types.*;
import dev.cel.common.values.NullValue;

import javax.xml.namespace.QName;

/**
 * @author Radovan Semancik
 */
public class ItemPathCelValue extends AbstractStructuredCelValue<Object> implements MidPointValueProducer<ItemPath>, MelComparable {

    public static final String ITEM_PATH_PACKAGE_NAME = ItemPath.class.getTypeName();
    private static final String F_SEGMENTS = "segments";
    public static final CelType CEL_TYPE = createCelType();

    private final ItemPath itemPath;

    ItemPathCelValue(ItemPath itemPath) {
        this.itemPath = itemPath;
    }

    public static ItemPathCelValue create(ItemPath itemPath) {
        return new ItemPathCelValue(itemPath);
    }

    protected Map<String, Object> createMapValue() {
        Map<String, Object> value = new HashMap<>();
        value.put(F_SEGMENTS, wrapSegments(itemPath.getSegments()));
        return value;
    }

    private Object wrapSegments(List<?> segments) {
        return segments == null ? NullValue.NULL_VALUE : segments.stream().map(this::wrapSegment).toList();
    }

    private Object wrapSegment(Object segment) {
        if (segment == null) {
            return NullValue.NULL_VALUE;
        }
        if (segment instanceof QName q) { // ItemName gets processed here
            return QNameCelValue.create(q);
        }
        if (segment instanceof IdItemPathSegment id) {
            return id.getId();
        }
        throw new MelException("Unknown segment in item path: " + segment + " ("+segment.getClass().getTypeName()+")");
    }

    @Override
    public ItemPath getJavaValue() {
        return itemPath;
    }

    @Override
    public CelType celType() {
        return CEL_TYPE;
    }

    private static CelType createCelType() {
        final ImmutableSet<String> fieldNames = ImmutableSet.of(F_SEGMENTS);
        StructType.FieldResolver fieldResolver = fieldName -> {
            if (F_SEGMENTS.equals(fieldName)) {
                return Optional.of(ListType.create(SimpleType.ANY));
            } else {
                throw new IllegalStateException("Illegal request for ItemPath field " + fieldName);
            }
        };
        return StructType.create(ITEM_PATH_PACKAGE_NAME, fieldNames, fieldResolver);
    }

    @Override
    public boolean melEquals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof String s) {
            return s.equals(itemPath.toString());
        }
        if (other instanceof ItemPathCelValue p) {
            return itemPath.equivalent(p.getJavaValue());
        }
        if (other instanceof QNameCelValue q) {
            return itemPath.equivalent(ItemPath.create(q.getJavaValue()));
        }
        return false;
    }

}
