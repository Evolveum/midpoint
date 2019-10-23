/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * TODO clean this up as it is part of prism-api!
 *
 * @author semancik
 */
public class PrismUtil {

    public static <T> void recomputeRealValue(T realValue, PrismContext prismContext) {
        if (realValue == null) {
            return;
        }
        // TODO: switch to Recomputable interface instead of PolyString
        if (realValue instanceof PolyString && prismContext != null) {
            PolyString polyStringVal = (PolyString)realValue;
            // Always recompute. Recompute is cheap operation and this avoids a lot of bugs
            polyStringVal.recompute(prismContext.getDefaultPolyStringNormalizer());
        }
    }

    public static <T> void recomputePrismPropertyValue(PrismPropertyValue<T> pValue, PrismContext prismContext) {
        if (pValue == null) {
            return;
        }
        recomputeRealValue(pValue.getValue(), prismContext);
    }

//    /**
//     * Super-mega-giga-ultra hack. This is used to "fortify" XML namespace declaration in a non-standard way.
//     * It is useful in case that someone will try some stupid kind of schema-less XML normalization that removes
//     * "unused" XML namespace declaration. The declarations are usually used, but they are used inside QName values
//     * that the dumb normalization cannot see. Therefore this fortification places XML namespace declaration in
//     * a explicit XML elements. That can be reconstructed later by using unfortification method below.
//     */
//    public static void fortifyNamespaceDeclarations(Element definitionElement) {
//        for(Element childElement: DOMUtil.listChildElements(definitionElement)) {
//            fortifyNamespaceDeclarations(definitionElement, childElement);
//        }
//    }
//
//    private static void fortifyNamespaceDeclarations(Element definitionElement, Element childElement) {
//        Document doc = definitionElement.getOwnerDocument();
//        NamedNodeMap attributes = childElement.getAttributes();
//        for(int i=0; i<attributes.getLength(); i++) {
//            Attr attr = (Attr)attributes.item(i);
//            if (DOMUtil.isNamespaceDefinition(attr)) {
//                String prefix = DOMUtil.getNamespaceDeclarationPrefix(attr);
//                String namespace = DOMUtil.getNamespaceDeclarationNamespace(attr);
//                Element namespaceElement = doc.createElementNS(PrismConstants.A_NAMESPACE.getNamespaceURI(), PrismConstants.A_NAMESPACE.getLocalPart());
//                namespaceElement.setAttribute(PrismConstants.A_NAMESPACE_PREFIX, prefix);
//                namespaceElement.setAttribute(PrismConstants.A_NAMESPACE_URL, namespace);
//                definitionElement.insertBefore(namespaceElement, childElement);
//            }
//        }
//    }
//
//    public static void unfortifyNamespaceDeclarations(Element definitionElement) {
//        Map<String,String> namespaces = new HashMap<>();
//        for(Element childElement: DOMUtil.listChildElements(definitionElement)) {
//            if (PrismConstants.A_NAMESPACE.equals(DOMUtil.getQName(childElement))) {
//                String prefix = childElement.getAttribute(PrismConstants.A_NAMESPACE_PREFIX);
//                String namespace = childElement.getAttribute(PrismConstants.A_NAMESPACE_URL);
//                namespaces.put(prefix, namespace);
//                definitionElement.removeChild(childElement);
//            } else {
//                unfortifyNamespaceDeclarations(definitionElement, childElement, namespaces);
//                namespaces = new HashMap<>();
//            }
//        }
//    }
//
//    private static void unfortifyNamespaceDeclarations(Element definitionElement, Element childElement,
//            Map<String, String> namespaces) {
//        for (Entry<String, String> entry: namespaces.entrySet()) {
//            String prefix = entry.getKey();
//            String namespace = entry.getValue();
//            String declaredNamespace = DOMUtil.getNamespaceDeclarationForPrefix(childElement, prefix);
//            if (declaredNamespace == null) {
//                DOMUtil.setNamespaceDeclaration(childElement, prefix, namespace);
//            } else if (!namespace.equals(declaredNamespace)) {
//                throw new IllegalStateException("Namespace declaration with prefix '"+prefix+"' that was used to declare "+
//                        "namespace '"+namespace+"' is now used for namespace '"+declaredNamespace+"', cannot unfortify.");
//            }
//        }
//    }

    public static boolean isEmpty(PolyStringType value) {
        return value == null || StringUtils.isEmpty(value.getOrig()) && StringUtils.isEmpty(value.getNorm());
    }

//    public static PrismUnmarshaller getXnodeProcessor(@NotNull PrismContext prismContext) {
//        return ((PrismContextImpl) prismContext).getPrismUnmarshaller();
//    }

    public static <T,X> PrismPropertyValue<X> convertPropertyValue(PrismPropertyValue<T> srcVal,
            PrismPropertyDefinition<T> srcDef, PrismPropertyDefinition<X> targetDef,
            PrismContext prismContext) {
        if (targetDef.getTypeName().equals(srcDef.getTypeName())) {
            return (PrismPropertyValue<X>) srcVal;
        } else {
            Class<X> expectedJavaType = XsdTypeMapper.toJavaType(targetDef.getTypeName());
            X convertedRealValue = JavaTypeConverter.convert(expectedJavaType, srcVal.getValue());
            return prismContext.itemFactory().createPropertyValue(convertedRealValue);
        }
    }

    public static <T,X> PrismProperty<X> convertProperty(PrismProperty<T> srcProp, PrismPropertyDefinition<X> targetDef,
            PrismContext prismContext) throws SchemaException {
        if (targetDef.getTypeName().equals(srcProp.getDefinition().getTypeName())) {
            return (PrismProperty<X>) srcProp;
        } else {
            PrismProperty<X> targetProp = targetDef.instantiate();
            Class<X> expectedJavaType = XsdTypeMapper.toJavaType(targetDef.getTypeName());
            for (PrismPropertyValue<T> srcPVal: srcProp.getValues()) {
                X convertedRealValue = JavaTypeConverter.convert(expectedJavaType, srcPVal.getValue());
                targetProp.add(prismContext.itemFactory().createPropertyValue(convertedRealValue));
            }
            return targetProp;
        }
    }

    public static <O extends Objectable> void setDeltaOldValue(PrismObject<O> oldObject, ItemDelta<?,?> itemDelta) {
        if (oldObject == null) {
            return;
        }
        Item<PrismValue, ItemDefinition> itemOld = oldObject.findItem(itemDelta.getPath());
        if (itemOld != null) {
            itemDelta.setEstimatedOldValues((Collection) PrismValueCollectionsUtil.cloneCollection(itemOld.getValues()));
        }
    }

    public static <O extends Objectable> void setDeltaOldValue(PrismObject<O> oldObject, Collection<? extends ItemDelta> itemDeltas) {
        for(ItemDelta itemDelta: itemDeltas) {
            setDeltaOldValue(oldObject, itemDelta);
        }
    }

    public static <T> boolean equals(T a, T b, MatchingRule<T> matchingRule) throws SchemaException {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (matchingRule == null) {
            if (a instanceof byte[]) {
                if (b instanceof byte[]) {
                    return Arrays.equals((byte[])a, (byte[])b);
                } else {
                    return false;
                }
            } else {
                return a.equals(b);
            }
        } else {
            return matchingRule.match(a, b);
        }
    }

    // for diagnostic purposes
    public static String serializeQuietly(PrismContext prismContext, Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Collection) {
            return ((Collection<?>) object).stream()
                    .map(o -> serializeQuietly(prismContext, o))
                    .collect(Collectors.joining("; "));
        }
        try {
            PrismSerializer<String> serializer = prismContext.xmlSerializer();
            if (object instanceof Item) {
                return serializer.serialize((Item) object);
            } else {
                return serializer.serializeRealValue(object, new QName("value"));
            }
        } catch (Throwable t) {
            return "Couldn't serialize (" + t.getMessage() + "): " + object;
        }
    }

    // for diagnostic purposes
    public static Object serializeQuietlyLazily(PrismContext prismContext, Object object) {
        if (object == null) {
            return null;
        }
        return new Object() {
            @Override
            public String toString() {
                return serializeQuietly(prismContext, object);
            }
        };
    }

    public static void debugDumpWithLabel(StringBuilder sb, String label, Containerable cc, int indent) {
        if (cc == null) {
            DebugUtil.debugDumpWithLabel(sb, label, (DebugDumpable)null, indent);
        } else {
            DebugUtil.debugDumpWithLabel(sb, label, cc.asPrismContainerValue(), indent);
        }
    }

    public static void debugDumpWithLabelLn(StringBuilder sb, String label, Containerable cc, int indent) {
        debugDumpWithLabel(sb, label, cc, indent);
        sb.append("\n");
    }

    public static boolean isStructuredType(QName typeName) {
        return QNameUtil.match(PolyStringType.COMPLEX_TYPE, typeName);
    }
}
