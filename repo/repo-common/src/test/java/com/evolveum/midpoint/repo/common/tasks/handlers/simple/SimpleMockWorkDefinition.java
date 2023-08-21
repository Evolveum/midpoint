/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.simple;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityRun.NS_EXT;
import static com.evolveum.midpoint.util.MiscUtil.or0;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.DebugUtil;

public class SimpleMockWorkDefinition extends AbstractWorkDefinition {

    private static final ItemName MESSAGE_NAME = new ItemName(NS_EXT, "message");
    private static final ItemName INITIAL_FAILURES_NAME = new ItemName(NS_EXT, "initialFailures");

    static final QName WORK_DEFINITION_TYPE_QNAME = new QName(NS_EXT, "SimpleMockDefinitionType");
    static final QName WORK_DEFINITION_ITEM_QNAME = new QName(NS_EXT, "simpleMock");

    private final String message;
    private final int initialFailures;

    SimpleMockWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);
        PrismContainerValue<?> pcv = info.source().getValue();
        this.message = pcv.getPropertyRealValue(MESSAGE_NAME, String.class);
        this.initialFailures = or0(pcv.getPropertyRealValue(INITIAL_FAILURES_NAME, Integer.class));
    }

    public String getMessage() {
        return message;
    }

    int getInitialFailures() {
        return initialFailures;
    }

    @Override
    public @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation() {
        return AffectedObjectsInformation.ObjectSet.notSupported(); // not relevant here
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "message", message, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "initialFailures", initialFailures, indent+1);
    }
}
