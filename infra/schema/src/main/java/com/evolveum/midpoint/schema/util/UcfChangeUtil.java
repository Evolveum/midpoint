/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.messaging.JsonAsyncProvisioningRequest;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UcfChangeType;
import com.evolveum.prism.xml.ns._public.types_3.*;

import org.apache.commons.collections4.CollectionUtils;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.evolveum.midpoint.util.QNameUtil.uriToQName;

import static com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType.*;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;

/**
 *
 */
@SuppressWarnings("unused")
public class UcfChangeUtil {

    public static UcfChangeType createForNewObject(QName objectClassName, Map<QName, Object> attributes,
            PrismContext prismContext) throws SchemaException {
        ShadowType shadow = new ShadowType(prismContext);
        copyAttributes(attributes, shadow.asPrismObject().findOrCreateContainer(ShadowType.F_ATTRIBUTES).getValue(), prismContext);
        UcfChangeType change = new UcfChangeType();
        ObjectDelta<ShadowType> addDelta = DeltaFactory.Object.createAddDelta(shadow.asPrismObject());
        change.setObjectClass(objectClassName);
        change.setObjectDelta(DeltaConvertor.toObjectDeltaType(addDelta));
        return change;
    }

    private static void copyAttributes(Map<QName, Object> attributes, PrismContainerValue<?> target, PrismContext prismContext)
            throws SchemaException {
        for (Map.Entry<QName, Object> entry : attributes.entrySet()) {
            PrismProperty<Object> attribute = prismContext.itemFactory().createProperty(entry.getKey());
            if (entry.getValue() instanceof Collection) {
                for (Object value : (Collection) entry.getValue()) {
                    attribute.addValue(prismContext.itemFactory().createPropertyValue(value));
                }
            } else {
                attribute.setValue(prismContext.itemFactory().createPropertyValue(entry.getValue()));
            }
            target.add(attribute);
        }
    }

    public static UcfChangeType create(QName objectClassName, Map<QName, Object> identifiers, ObjectDeltaType delta, PrismContext prismContext)
            throws SchemaException {
        UcfChangeType change = new UcfChangeType();
        change.setObjectClass(objectClassName);
        change.setIdentifiers(new ShadowAttributesType(prismContext));
        copyAttributes(identifiers, change.getIdentifiers().asPrismContainerValue(), prismContext);
        change.setObjectDelta(delta);
        return change;
    }

    /**
     * Creates {@link UcfChangeType} from {@link JsonAsyncProvisioningRequest}. Assumes standard change representation.
     * (I.e. not storing replaced values in attributes map.)
     */
    @Experimental
    public static UcfChangeType createFromAsyncProvisioningRequest(JsonAsyncProvisioningRequest request, String defaultNamespace,
            PrismContext prismContext) throws SchemaException {
        String operation = request.getOperation();
        QName objectClass = uriToQName(request.getObjectClass(), defaultNamespace);
        if (request.isAdd()) {
            return UcfChangeUtil.createForNewObject(objectClass, getAttributes(request, defaultNamespace), prismContext);
        } else if (request.isModify()) {
            return UcfChangeUtil.create(objectClass, getIdentifiers(request, defaultNamespace), createModifyDelta(request, defaultNamespace, prismContext), prismContext);
        } else if (request.isDelete()) {
            return UcfChangeUtil.create(objectClass, getIdentifiers(request, defaultNamespace), createDeleteDelta(), prismContext);
        } else {
            throw new IllegalArgumentException("Unsupported operation: " + request.getOperation());
        }
    }

    private static Map<QName, Object> getAttributes(JsonAsyncProvisioningRequest request, String defaultNamespace) {
        Map<QName, Object> rv = new HashMap<>();
        addFromMap(rv, request.getAttributes(), defaultNamespace);
        return rv;
    }

    private static Map<QName, Object> getIdentifiers(JsonAsyncProvisioningRequest request, String defaultNamespace) {
        Map<QName, Object> rv = new HashMap<>();
        addFromMap(rv, request.getPrimaryIdentifiers(), defaultNamespace);
        addFromMap(rv, request.getSecondaryIdentifiers(), defaultNamespace);
        return rv;
    }

    private static void addFromMap(Map<QName, Object> target, Map<String, Collection<?>> source, String defaultNamespace) {
        emptyIfNull(source).forEach((key, value) ->
                target.put(uriToQName(key, defaultNamespace), value));
    }

    private static ObjectDeltaType createDeleteDelta() {
        ObjectDeltaType delta = new ObjectDeltaType();
        delta.setChangeType(ChangeTypeType.DELETE);
        return delta;
    }

    private static ObjectDeltaType createModifyDelta(JsonAsyncProvisioningRequest request, String defaultNamespace, PrismContext prismContext) {
        ObjectDeltaType delta = new ObjectDeltaType();
        delta.setChangeType(ChangeTypeType.MODIFY);

        for (Map.Entry<String, JsonAsyncProvisioningRequest.DeltaValues> entry : emptyIfNull(request.getChanges()).entrySet()) {
            ItemPathType path = new ItemPathType(
                    ItemPath.create(ShadowType.F_ATTRIBUTES, uriToQName(entry.getKey(), defaultNamespace)));
            JsonAsyncProvisioningRequest.DeltaValues deltaValues = entry.getValue();
            if (deltaValues.getReplace() != null) {
                delta.getItemDelta().add(createItemDelta(REPLACE, path, deltaValues.getReplace(), prismContext));
            } else {
                if (CollectionUtils.isNotEmpty(deltaValues.getAdd())) {
                    delta.getItemDelta().add(createItemDelta(ADD, path, deltaValues.getAdd(), prismContext));
                }
                if (CollectionUtils.isNotEmpty(deltaValues.getDelete())) {
                    delta.getItemDelta().add(createItemDelta(DELETE, path, deltaValues.getDelete(), prismContext));
                }
            }
        }

        return delta;
    }

    private static ItemDeltaType createItemDelta(ModificationTypeType type, ItemPathType path, Collection<?> values, PrismContext prismContext) {
        ItemDeltaType itemDelta = new ItemDeltaType();
        itemDelta.setModificationType(type);
        itemDelta.setPath(path);
        for (Object value : values) {
            itemDelta.getValue().add(RawType.fromPropertyRealValue(value, null, prismContext));
        }
        return itemDelta;
    }
}
