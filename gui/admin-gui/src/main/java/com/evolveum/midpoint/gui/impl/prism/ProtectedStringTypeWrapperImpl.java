/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.gui.impl.prism;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import java.util.Collection;

/**
 * Created by honchar
 */
public class ProtectedStringTypeWrapperImpl extends PrismPropertyWrapperImpl<ProtectedStringType>{
    private static final long serialVersionUID = 1L;

    private static final transient Trace LOGGER = TraceManager.getTrace(ProtectedStringTypeWrapperImpl.class);

    public ProtectedStringTypeWrapperImpl(PrismContainerValueWrapper<?> parent, PrismProperty<ProtectedStringType> item, ItemStatus status) {
        super(parent, item, status);

//        getItem().setRealValue(null);
    }

    @Override
    public <D extends ItemDelta<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>>> Collection<D> getDelta() throws SchemaException {
        PrismPropertyValueWrapper<ProtectedStringType> valueWrapper = getValue();
        if (valueWrapper != null && valueWrapper.getRealValue() == null && valueWrapper.getOldValue().getRealValue() != null){
            valueWrapper.setStatus(ValueStatus.DELETED);
        }
        return super.getDelta();
    }


}
