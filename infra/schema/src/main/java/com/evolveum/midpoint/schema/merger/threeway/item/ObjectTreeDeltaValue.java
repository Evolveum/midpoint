/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ObjectTreeDeltaValue<O extends ObjectType> extends ContainerTreeDeltaValue<O> {

    public ObjectTreeDeltaValue() {
    }

    public ObjectTreeDeltaValue(PrismContainerValue<O> value, ModificationType modificationType) {
        super(value, modificationType);
    }

    @Override
    protected String debugDumpShortName() {
        return "OTDV";
    }

    @Override
    protected void debugDumpTitle(StringBuilder sb, int indent) {
        // todo implement - whole title/content doesn't work correctly
    }
}
