/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public interface PrismValueWrapper<T, V extends PrismValue> extends Serializable, DebugDumpable {

    T getRealValue();
    void setRealValue(T realValue);

    ValueStatus getStatus();
    void setStatus(ValueStatus status);

    V getNewValue();
    V getOldValue();
    <IW extends ItemWrapper> IW getParent();

    <D extends ItemDelta<V,? extends ItemDefinition>> void addToDelta(D delta) throws SchemaException;

//    <ID extends ItemDelta> void applyDelta(ID delta) throws SchemaException;
}
