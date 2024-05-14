/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.delta;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ObjectTreeDeltaValue<O extends ObjectType> extends ContainerTreeDeltaValue<O> {

    private String oid;

    public ObjectTreeDeltaValue() {
    }

    public ObjectTreeDeltaValue(PrismContainerValue<O> value, ModificationType modificationType) {
        super(value, modificationType);
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    @Override
    protected String debugDumpShortName() {
        return "OTDV";
    }

    @Override
    public ItemPath getPath() {
        return ItemPath.EMPTY_PATH;
    }
}
