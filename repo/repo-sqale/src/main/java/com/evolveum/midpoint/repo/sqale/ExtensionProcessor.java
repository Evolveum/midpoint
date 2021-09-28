/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItem;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemCardinality;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.util.exception.SystemException;

public class ExtensionProcessor {

    private final SqaleRepoContext repositoryContext;

    public ExtensionProcessor(SqaleRepoContext repositoryContext) {
        this.repositoryContext = repositoryContext;
    }

    public Jsonb processExtensions(
            @NotNull Containerable extContainer, MExtItemHolderType holderType) {
        Map<String, Object> extMap = new LinkedHashMap<>();
        PrismContainerValue<?> prismContainerValue = extContainer.asPrismContainerValue();
        for (Item<?, ?> item : prismContainerValue.getItems()) {
            try {
                Objects.requireNonNull(item, "Object for converting must not be null.");
                ItemDefinition extDef = item.getDefinition();
                if (extDef == null) {
                    continue; // item does not have definition, skipping
                }
                ExtItemInfo extItemInfo = findExtensionItem(extDef, holderType);
                if (extItemInfo == null) {
                    continue; // not-indexed, skipping this item
                }

                Object value = extItemValue(item, extItemInfo);
                extMap.put(extItemInfo.getId(), value);
            } catch (RuntimeException e) {
                // If anything happens (like NPE in Map.of) we want to capture the "bad" item.
                throw new SystemException(
                        "Exception when translating extension item " + item, e);
            }
        }

        return Jsonb.fromMap(extMap);
    }

    /** Returns ext item definition or null if the item is not indexed and should be skipped. */
    @Nullable
    public ExtensionProcessor.ExtItemInfo findExtensionItem(
            ItemDefinition<?> definition, MExtItemHolderType holderType) {
        MExtItem extItem = resolveExtensionItem(definition, holderType);
        if (extItem == null) {
            return null; // not-indexed, returning null
        }

        // TODO review any need for shadow attributes, now they are stored fine, but the code here
        //  is way too simple compared to the old repo.

        ExtItemInfo info = new ExtItemInfo();
        info.item = extItem;
        if (definition instanceof PrismReferenceDefinition) {
            info.defaultRefTargetType = ((PrismReferenceDefinition) definition).getTargetTypeName();
        }

        return info;
    }

    public Object extItemValue(Item<?, ?> item, ExtItemInfo extItemInfo) {
        MExtItem extItem = extItemInfo.item;
        if (extItem.cardinality == MExtItemCardinality.ARRAY) {
            List<Object> vals = new ArrayList<>();
            for (Object realValue : item.getRealValues()) {
                vals.add(convertExtItemValue(realValue, extItemInfo));
            }
            return vals;
        } else {
            return convertExtItemValue(item.getRealValue(), extItemInfo);
        }
    }

    private Object convertExtItemValue(Object realValue, ExtItemInfo extItemInfo) {
        checkRealValueType(realValue, extItemInfo.item);
        if (realValue instanceof String
                || realValue instanceof Number
                || realValue instanceof Boolean) {
            return realValue;
        }

        if (realValue instanceof PolyString) {
            PolyString poly = (PolyString) realValue;
            return JsonbUtils.polyStringToMap(poly);
        }

        if (realValue instanceof Referencable) {
            Referencable ref = (Referencable) realValue;
            // we always want to store the type for consistent search results
            QName targetType = ref.getType();
            if (targetType == null) {
                targetType = extItemInfo.defaultRefTargetType;
            }
            if (targetType == null) {
                throw new IllegalArgumentException(
                        "Reference without target type can't be stored: " + ref);
            }
            return Map.of(JsonbUtils.JSONB_REF_TARGET_OID_KEY, ref.getOid(),
                    JsonbUtils.JSONB_REF_TARGET_TYPE_KEY, MObjectType.fromTypeQName(targetType),
                    JsonbUtils.JSONB_REF_RELATION_KEY, repositoryContext.processCacheableRelation(ref.getRelation()));
        }

        if (realValue instanceof Enum) {
            return realValue.toString();
        }

        if (realValue instanceof XMLGregorianCalendar) {
            // XMLGregorianCalendar stores only millis, but we cut it to 3 fraction digits
            // to make the behavior explicit and consistent.
            return ExtUtils.extensionDateTime((XMLGregorianCalendar) realValue);
        }

        throw new IllegalArgumentException(
                "Unsupported type '" + realValue.getClass().getName() + "' for value '" + realValue + "'.");
    }

    private void checkRealValueType(Object realValue, MExtItem extItemInfo) {
        Class<?> realValueType = ExtUtils.getRealValueClass(extItemInfo.valueType);
        if (realValueType != null && !realValueType.isAssignableFrom(realValue.getClass())) {
            throw new IllegalArgumentException("Incorrect real value type '"
                    + realValue.getClass().getName() + "' for item " + extItemInfo.itemName);
        }
    }

    /**
     * Finds extension item for the provided definition and holder type.
     * Returns null if the item is not indexed.
     */
    public MExtItem resolveExtensionItem(
            ItemDefinition<?> definition, MExtItemHolderType holderType) {
        Objects.requireNonNull(definition,
                "Item '" + definition.getItemName() + "' without definition can't be saved.");
        if (definition instanceof PrismContainerDefinition<?>) {
            // Skip containers for now
            return null;
        }
        if (definition instanceof PrismPropertyDefinition) {
            Boolean indexed = ((PrismPropertyDefinition<?>) definition).isIndexed();
            // null is default which is "indexed"
            if (indexed != null && !indexed) {
                return null;
            }
            // enum is recognized by having allowed values
            if (!ExtUtils.isRegisteredType(definition.getTypeName())
                    && !ExtUtils.isEnumDefinition(((PrismPropertyDefinition<?>) definition))) {
                return null;
            }
        } else if (!(definition instanceof PrismReferenceDefinition)) {
            throw new UnsupportedOperationException("Unknown definition type '" + definition
                    + "', can't say if '" + definition.getItemName() + "' is indexed or not.");
        } // else it's reference which is indexed implicitly

        return repositoryContext.resolveExtensionItem(MExtItem.keyFrom(definition, holderType));
    }

    /** Contains ext item from catalog and additional info needed for processing. */
    public static class ExtItemInfo {
        public MExtItem item;
        public QName defaultRefTargetType;

        public String getId() {
            return item.id.toString();
        }
    }
}
