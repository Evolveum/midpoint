/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchReadOnlyCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;

import java.util.List;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Resolves definitions and old values.
 * Currently NOT references.
 */
@Component
public class Resolver {

    private static final Trace LOGGER = TraceManager.getTrace(Resolver.class);

    public static final String CLASS_DOT = Resolver.class.getName() + ".";
    private static final String OP_RESOLVE = CLASS_DOT + "resolve";

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ModelService modelService;

    @Autowired
    private ProvisioningService provisioningService;

    @Autowired
    private Visualizer visualizer;

    public <O extends ObjectType> void resolve(PrismObject<O> object, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        if (object == null) {
            return;
        }
        Class<O> clazz = object.getCompileTimeClass();
        if (clazz == null) {
            warn(result, "Compile time class for " + toShortString(object) + " is not known");
        } else {
            PrismObjectDefinition<O> def = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
            if (def != null) {
                if (ResourceType.class.isAssignableFrom(clazz) || ShadowType.class.isAssignableFrom(clazz)) {
                    try {
                        provisioningService.applyDefinition(object, task, result);
                    } catch (ObjectNotFoundException | CommunicationException | ConfigurationException e) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply definition on {} -- continuing with no definition", e,
                                ObjectTypeUtil.toShortString(object));
                    }
                } else {
                    object.applyDefinition(def);
                }
                if (object.getName() == null && object.getOid() != null) {
                    String oid = object.getOid();
                    try {
                        PrismObject<O> originalObject = modelService.getObject(clazz, oid, createNoFetchReadOnlyCollection(), task, result);
                        object.asObjectable().setName(new PolyStringType(originalObject.getName()));
                    } catch (ObjectNotFoundException e) {
                        //ignore when object doesn't exist
                    } catch (RuntimeException | SchemaException | ConfigurationException | CommunicationException |
                            SecurityViolationException e) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve object {}", e, oid);
                        warn(result, "Couldn't resolve object " + oid + ": " + e.getMessage(), e);
                    }
                }
            } else {
                warn(result, "Definition for " + toShortString(object) + " couldn't be found");
            }
        }
    }

    public <O extends ObjectType> void resolve(ObjectDelta<O> objectDelta, boolean includeOriginalObject, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        if (objectDelta == null) {
            return;
        }

        if (objectDelta.isAdd()) {
            resolve(objectDelta.getObjectToAdd(), task, result);
        } else if (objectDelta.isDelete()) {
            // nothing to do
        } else {
            PrismObject<O> originalObject = null;
            boolean originalObjectFetched = false;
            final Class<O> clazz = objectDelta.getObjectTypeClass();
            boolean managedByProvisioning = ResourceType.class.isAssignableFrom(clazz) || ShadowType.class.isAssignableFrom(clazz);
            PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
            if (objectDefinition == null) {
                warn(result, "Definition for " + clazz + " couldn't be found");
            } else {
                if (managedByProvisioning && canApplyDefinition(objectDelta)) {
                    try {
                        provisioningService.applyDefinition(objectDelta, task, result);
                    } catch (Throwable t) {
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply definition on {} -- continuing with no definition", t, objectDelta);
                    }
                }
            }
            for (ItemDelta itemDelta : objectDelta.getModifications()) {
                if (objectDefinition != null && !managedByProvisioning) {
                    ItemDefinition<?> def = objectDefinition.findItemDefinition(itemDelta.getPath());
                    if (def != null) {
                        itemDelta.applyDefinition(def);
                    }
                }
                if (itemDelta.getEstimatedOldValues() == null) {
                    final String oid = objectDelta.getOid();
                    if (!originalObjectFetched && oid != null && includeOriginalObject) {
                        try {
                            originalObject = modelService.getObject(clazz, oid, createNoFetchReadOnlyCollection(), task, result);
                        } catch (ObjectNotFoundException e) {
                            result.recordHandledError(e);
                            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Object {} does not exist", e, oid);
                        } catch (RuntimeException | SchemaException | ConfigurationException | CommunicationException |
                                SecurityViolationException e) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve object {}", e, oid);
                            warn(result, "Couldn't resolve object " + oid + ": " + e.getMessage(), e);
                        }
                        originalObjectFetched = true;
                    }
                    if (originalObject != null) {
                        Item<?, ?> originalItem = originalObject.findItem(itemDelta.getPath());
                        if (originalItem != null) {
                            itemDelta.setEstimatedOldValues(CloneUtil.cloneCollectionMembers(originalItem.getValues()));
                        }
                    }
                }
            }
        }
    }

    private <O extends ObjectType> boolean canApplyDefinition(ObjectDelta<O> objectDelta) {
        assert objectDelta.isModify();

        // This is currently the only precondition for applyDefinition method call.
        return objectDelta.getOid() != null;
    }

    private void warn(OperationResult result, String text, Exception e) {
        result.createSubresult(OP_RESOLVE).recordWarning(text, e);
    }

    private void warn(OperationResult result, String text) {
        result.createSubresult(OP_RESOLVE).recordWarning(text);
    }

    // TODO caching retrieved objects
    public void resolve(List<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException {
        for (ObjectDelta<? extends ObjectType> delta : deltas) {
            resolve(delta, true, task, result);
        }
    }

    public String resolveReferenceName(ObjectReferenceType ref, boolean returnOidIfReferenceUnknown, Task task, OperationResult result) {
        if (ref == null) {
            return null;
        }

        if (ref.getTargetName() != null) {
            return ref.getTargetName().getOrig();
        }

        if (ref.getObject() != null) {
            PrismObject<?> object = ref.getObject();
            if (object.getName() == null) {
                return returnOidIfReferenceUnknown ? ref.getOid() : null;
            }

            return object.getName().getOrig();
        }

        String oid = ref.getOid();
        if (oid == null) {
            return null;
        }

        try {
            ObjectTypes type = getTypeFromReference(ref);

            PrismObject<?> object = modelService.getObject(type.getClassDefinition(), ref.getOid(), GetOperationOptions.createRawCollection(), task, result);
            return object.getName().getOrig();
        } catch (Exception ex) {
            return returnOidIfReferenceUnknown ? ref.getOid() : null;
        }
    }

    private ObjectTypes getTypeFromReference(ObjectReferenceType ref) {
        QName typeName = ref.getType() != null ? ref.getType() : ObjectType.COMPLEX_TYPE;
        return ObjectTypes.getObjectTypeFromTypeQName(typeName);
    }
}
