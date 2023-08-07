/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.search;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityRun.NS_EXT;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class SearchIterativeMockWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

    private static final ItemName OBJECT_SET_NAME = new ItemName(NS_EXT, "objectSet");
    private static final ItemName MESSAGE_NAME = new ItemName(NS_EXT, "message");
    private static final ItemName FAIL_ON_NAME = new ItemName(NS_EXT, "failOn");
    private static final ItemName FREEZE_IF_SCAVENGER = new ItemName(NS_EXT, "freezeIfScavenger");

    static final QName WORK_DEFINITION_TYPE_QNAME = new QName(NS_EXT, "SearchIterativeMockDefinitionType");
    static final QName WORK_DEFINITION_ITEM_QNAME = new QName(NS_EXT, "searchIterativeMock");

    @NotNull private final ObjectSetType objectSet;
    @Nullable private final String message;
    @Nullable private final SearchFilterType failOn;
    private final boolean freezeIfScavenger;

    SearchIterativeMockWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);
        PrismContainerValue<?> pcv = info.source().getValue();
        this.objectSet = getObjectSet(pcv);
        this.message = pcv.getPropertyRealValue(MESSAGE_NAME, String.class);
        this.failOn = pcv.getPropertyRealValue(FAIL_ON_NAME, SearchFilterType.class);
        this.freezeIfScavenger = Boolean.TRUE.equals(
                pcv.getPropertyRealValue(FREEZE_IF_SCAVENGER, Boolean.class));
    }

    private @NotNull ObjectSetType getObjectSet(PrismContainerValue<?> pcv) {
        ObjectSetType specified = pcv.getItemRealValue(OBJECT_SET_NAME, ObjectSetType.class);
        ObjectSetType resulting = specified != null ? specified : new ObjectSetType();
        ObjectSetUtil.assumeObjectType(resulting, ObjectType.COMPLEX_TYPE);
        return resulting;
    }

    public @NotNull ObjectSetType getObjectSet() {
        return objectSet;
    }

    public @Nullable String getMessage() {
        return message;
    }

    @Nullable SearchFilterType getFailOn() {
        return failOn;
    }

    boolean isFreezeIfScavenger() {
        return freezeIfScavenger;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "objectSet", objectSet, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "message", message, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "failOn", failOn, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "freezeIfScavenger", freezeIfScavenger, indent+1);
    }

    @Override
    public @NotNull ObjectSetType getObjectSetSpecification() {
        return objectSet;
    }
}
