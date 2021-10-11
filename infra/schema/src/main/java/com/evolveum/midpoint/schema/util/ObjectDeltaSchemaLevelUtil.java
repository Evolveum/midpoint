/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.HashMap;
import java.util.Map;

/**
 *  The name is a bit ridiculous but we need to distinguish it from ObjectDeltaUtil in prism module.
 */
public class ObjectDeltaSchemaLevelUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDeltaSchemaLevelUtil.class);

    @FunctionalInterface
    public interface NameResolver {
        PolyString getName(Class<? extends ObjectType> objectClass, String oid) throws ObjectNotFoundException, SchemaException;
    }

    public static void resolveNames(
            ObjectDelta<? extends ObjectType> delta, NameResolver nameResolver, PrismContext prismContext) {
        Map<String, PolyString> resolvedOids = new HashMap<>();
        Visitor namesResolver = visitable -> {
            if (visitable instanceof PrismReferenceValue) {
                PrismReferenceValue refVal = ((PrismReferenceValue) visitable);
                String oid = refVal.getOid();
                if (oid == null) {
                    // sanity check; should not happen
                } else if (refVal.getTargetName() != null) {
                    resolvedOids.put(oid, refVal.getTargetName());
                } else if (resolvedOids.containsKey(oid)) {
                    PolyString resolvedName = resolvedOids.get(oid); // may be null
                    refVal.setTargetName(resolvedName);
                } else if (refVal.getObject() != null) {
                    PolyString name = refVal.getObject().getName();
                    refVal.setTargetName(name);
                    resolvedOids.put(oid, name);
                } else {
                    PrismObjectDefinition<? extends ObjectType> objectDefinition = null;
                    if (refVal.getTargetType() != null) {
                        objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(refVal.getTargetType());
                    }
                    Class<? extends ObjectType> objectClass = null;
                    if (objectDefinition != null) {
                        objectClass = objectDefinition.getCompileTimeClass();
                    }
                    if (objectClass == null) {
                        objectClass = ObjectType.class; // the default (shouldn't be needed)
                    }
                    try {
                        PolyString name = nameResolver.getName(objectClass, oid);
                        refVal.setTargetName(name);
                        resolvedOids.put(oid, name);
                        LOGGER.trace("Resolved {}: {} to {}", objectClass, oid, name);
                    } catch (ObjectNotFoundException e) {
                        LOGGER.trace("Couldn't determine the name for {}: {} as it does not exist", objectClass, oid, e);
                        resolvedOids.put(oid, null);
                    } catch (SchemaException | RuntimeException e) {
                        LOGGER.trace("Couldn't determine the name for {}: {} because of unexpected exception", objectClass, oid, e);
                        resolvedOids.put(oid, null);
                    }
                }
            }
        };
        //noinspection unchecked
        delta.accept(namesResolver);
    }

}
