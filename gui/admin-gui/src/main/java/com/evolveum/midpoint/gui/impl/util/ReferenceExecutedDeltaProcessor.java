/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Post processor for reference attributes. Create ObjectReferenceType for saved object from executed deltas.
 */
public class ReferenceExecutedDeltaProcessor implements Serializable, ExecutedDeltaPostProcessor {

    private final Collection<ObjectDelta<? extends ObjectType>> objectDeltas;
    private final PrismReferenceValueWrapperImpl referenceWrapper;

    public ReferenceExecutedDeltaProcessor(
            Collection<ObjectDelta<? extends ObjectType>> objectDeltas,
            PrismReferenceValueWrapperImpl referenceWrapper) {
        this.objectDeltas = objectDeltas;
        this.referenceWrapper = referenceWrapper;
    }

    public Collection<ObjectDelta<? extends ObjectType>> getObjectDeltas() {
        return objectDeltas;
    }

    @Override
    public void processExecutedDelta(Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, PageBase pageBase) {
        String resourceOid = ObjectDeltaOperation.findFocusDeltaOidInCollection(executedDeltas);
        ObjectDeltaOperation<? extends ObjectType> objectDelta = ObjectDeltaOperation.findFocusDeltaInCollection(executedDeltas);
        var ref = new ObjectReferenceType()
                .oid(resourceOid)
                .relation(RelationUtil.getDefaultRelation())
                .targetName("")
                .type(getType());
        String displayName = WebComponentUtil.getReferencedObjectDisplayNameAndName(ref, true, pageBase);
        ref.targetName(displayName);
        referenceWrapper.setRealValue(ref);
        referenceWrapper.resetNewObjectModel();
    }

    private QName getType() {
        List<QName> types = ((PrismReferenceWrapper)referenceWrapper.getParent()).getTargetTypes();
        if (types.size() == 1) {
            QName type = types.get(0);
            if (type != null && StringUtils.isEmpty(type.getNamespaceURI())) {
                type = ObjectTypes.canonizeObjectType(type);
            }
            return type;
        }
        return ObjectType.COMPLEX_TYPE;
    }
}
