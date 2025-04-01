/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchReadOnlyCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;

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
                // We need to know if delta represents type not present in system
                // If delta contains them, we need to preserve definitions from item delta.
                boolean isLegacyDelta = DeltaConvertor.isLegacyDelta(itemDelta);

                // If delta is legacy delta (type was removed), we want to keep "functional" runtime definition, so nobody will try
                // to convert it to any sensible value during visualisation, since we can not reason about it contents
                if (objectDefinition != null && !managedByProvisioning && !isLegacyDelta) {
                    ItemDefinition<?> def = objectDefinition.findItemDefinition(itemDelta.getPath());
                    if (def != null && !Objects.equals(def, itemDelta.getDefinition())) {
                        itemDelta.applyDefinition(def);
                    }
                }
                // Also  if we are dealing with legacy delta, we do not want to fetch estimated old value from system,
                // because it may have different definition (eg. property vs container)
                if (!isLegacyDelta && itemDelta.getEstimatedOldValues() == null) {
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
                            // We do cloning here, because the values will be modified during visualization process
                            // (e.g., references will get resolved). So the values cannot be immutable.
                            //noinspection unchecked
                            itemDelta.setEstimatedOldValues(
                                    PrismValueCollectionsUtil.cloneCollection(
                                            originalItem.getValues()));
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

    public String resolveReferenceDisplayName(ObjectReferenceType ref, boolean returnOidIfReferenceUnknown, Task task, OperationResult result) {
        if (ref == null) {
            return null;
        }

        PrismObject<?> object = ref.asReferenceValue().getObject();
        if (object == null) {
            object = resolveObject(ref, task, result);

            ref.asReferenceValue().setObject(object);
        }

        if (object == null) {
            return resolveReferenceName(ref, returnOidIfReferenceUnknown, task, result);
        }

        if (ShadowType.class.equals(object.getCompileTimeClass())) {
            ShadowSimpleAttribute<?> namingAttribute = ShadowUtil.getNamingAttribute((ShadowType) object.asObjectable());
            Object realName = namingAttribute != null ? namingAttribute.getRealValue() : null;
            if (realName != null) {
                return realName.toString();
            }

            return resolveReferenceName(ref, returnOidIfReferenceUnknown, task, result);
        }

        PolyStringType displayName = ObjectTypeUtil.getDisplayName(object);
        if (displayName != null) {
            return displayName.getOrig();
        }

        return resolveReferenceName(ref, returnOidIfReferenceUnknown, task, result);
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

        PrismObject<?> object = resolveObject(ref, task, result);
        if (object != null) {
            return object.getName().getOrig();
        }

        return returnOidIfReferenceUnknown ? ref.getOid() : null;
    }

    public PrismObject<?> resolveObject(ObjectReferenceType ref, Task task, OperationResult result) {
        try {
            ObjectTypes type = getTypeFromReference(ref);

            return modelService.getObject(type.getClassDefinition(), ref.getOid(), GetOperationOptions.createNoFetchCollection(), task, result);
        } catch (Exception ex) {
            return null;
        }
    }

    private ObjectTypes getTypeFromReference(ObjectReferenceType ref) {
        QName typeName = ref.getType() != null ? ref.getType() : ObjectType.COMPLEX_TYPE;
        return ObjectTypes.getObjectTypeFromTypeQName(typeName);
    }
}
