/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.marshaller;

import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.impl.lex.dom.DomLexicalProcessor;

import com.evolveum.midpoint.prism.marshaller.JaxbDomHack;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.impl.xml.XmlTypeConverterInternal;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * A set of ugly hacks that are needed for prism and "real" JAXB to coexist. We hate it be we need it.
 * This is a mix of DOM and JAXB code that allows the use of "any" methods on JAXB-generated objects.
 * Prism normally does not use of of that. But JAXB code (such as JAX-WS) can invoke it and therefore
 * it has to return correct DOM/JAXB elements as expected.
 *
 * @author Radovan Semancik
 */
public class JaxbDomHackImpl implements JaxbDomHack {

    private static final Trace LOGGER = TraceManager.getTrace(JaxbDomHack.class);

    private PrismContext prismContext;
    private DomLexicalProcessor domParser;

    public JaxbDomHackImpl(DomLexicalProcessor domParser, PrismContext prismContext) {
        super();
        this.domParser = domParser;
        this.prismContext = prismContext;
    }

    private <T extends Containerable> ItemDefinition locateItemDefinition(
            PrismContainerDefinition<T> containerDefinition, QName elementQName, Object valueElements)
            throws SchemaException {
        ItemDefinition def = containerDefinition.findItemDefinition(ItemName.fromQName(elementQName));
        if (def != null) {
            return def;
        }

        if (valueElements instanceof Element) {
            // Try to locate xsi:type definition in the element
            def = resolveDynamicItemDefinition(elementQName, (Element) valueElements,
                    prismContext);
        }

        if (valueElements instanceof List){
            List elements = (List) valueElements;
            if (elements.size() == 1){
                Object element = elements.get(0);
                if (element instanceof JAXBElement){
                    Object val = ((JAXBElement) element).getValue();
                    if (val.getClass().isPrimitive()){
                        QName typeName = XsdTypeMapper.toXsdType(val.getClass());
                        PrismPropertyDefinitionImpl propDef = new PrismPropertyDefinitionImpl(elementQName, typeName, prismContext);
//                        propDef.setMaxOccurs(maxOccurs);
                        propDef.setDynamic(true);
                        return propDef;
                    }
                }
            }
        }
        if (def != null) {
            return def;
        } else if (containerDefinition.isRuntimeSchema()) {
            // Try to locate global definition in any of the schemas
            return prismContext.getSchemaRegistry().resolveGlobalItemDefinition(elementQName, containerDefinition.getComplexTypeDefinition());
        } else {
            return null;
        }
    }

    private ItemDefinition resolveDynamicItemDefinition(QName elementName,
            Element element, PrismContext prismContext) throws SchemaException {
        QName typeName = null;
        // QName elementName = null;
        // Set it to multi-value to be on the safe side
        int maxOccurs = -1;
//        for (Object element : valueElements) {
            // if (elementName == null) {
            // elementName = JAXBUtil.getElementQName(element);
            // }
            // TODO: try JAXB types
            if (element instanceof Element) {
                Element domElement = (Element) element;
                if (DOMUtil.hasXsiType(domElement)) {
                    typeName = DOMUtil.resolveXsiType(domElement);
                    if (typeName != null) {
                        String maxOccursString = domElement.getAttributeNS(
                                PrismConstants.A_MAX_OCCURS.getNamespaceURI(),
                                PrismConstants.A_MAX_OCCURS.getLocalPart());
                        if (!StringUtils.isBlank(maxOccursString)) {
                            // TODO
//                            maxOccurs = parseMultiplicity(maxOccursString, elementName);
                        }
//                        break;
                    }
                }
            }
//        }
        // FIXME: now the definition assumes property, may also be property
        // container?
        if (typeName == null) {
            return null;
        }
        PrismPropertyDefinitionImpl propDef = new PrismPropertyDefinitionImpl(elementName, typeName, prismContext);
        propDef.setMaxOccurs(maxOccurs);
        propDef.setDynamic(true);
        return propDef;
    }

    /**
     * This is used in a form of "fromAny" to parse elements from a JAXB getAny method to prism.
     */
    @Override
    public <IV extends PrismValue,ID extends ItemDefinition,C extends Containerable> Item<IV,ID> parseRawElement(Object element,
            PrismContainerDefinition<C> definition) throws SchemaException {
        Validate.notNull(definition, "Attempt to parse raw element in a container without definition");

        QName elementName = JAXBUtil.getElementQName(element);
        ItemDefinition itemDefinition = definition.findItemDefinition(ItemName.fromQName(elementName));

        if (itemDefinition == null) {
            itemDefinition = locateItemDefinition(definition, elementName, element);
            if (itemDefinition == null) {
                throw new SchemaException("No definition for item "+elementName);
            }
        }

        PrismContext prismContext = definition.getPrismContext();
        Item<IV,ID> subItem;
        if (element instanceof Element) {
            // DOM Element
            subItem = prismContext.parserFor((Element) element).name(elementName).definition(itemDefinition).parseItem();
        } else if (element instanceof JAXBElement<?>) {
            // JAXB Element
            JAXBElement<?> jaxbElement = (JAXBElement<?>)element;
            Object jaxbBean = jaxbElement.getValue();
            if (itemDefinition == null) {
                throw new SchemaException("No definition for item "+elementName+" in container "+definition+" (parsed from raw element)", elementName);
            }
            if (itemDefinition instanceof PrismPropertyDefinition<?>) {
                // property
                PrismProperty<Object> property = ((PrismPropertyDefinition<Object>)itemDefinition).instantiate();
                property.setRealValue(jaxbBean);
                subItem = (Item<IV,ID>) property;
            } else if (itemDefinition instanceof PrismContainerDefinition<?>) {
                if (jaxbBean instanceof Containerable) {
                    PrismContainer<?> container = ((PrismContainerDefinition<?>)itemDefinition).instantiate();
                    PrismContainerValue subValue = ((Containerable)jaxbBean).asPrismContainerValue();
                    container.add(subValue);
                    subItem = (Item<IV,ID>) container;
                } else {
                    throw new IllegalArgumentException("Unsupported JAXB bean "+jaxbBean.getClass());
                }
            } else if (itemDefinition instanceof PrismReferenceDefinition) {
                // TODO
                if (jaxbBean instanceof Referencable){
                    PrismReference reference = ((PrismReferenceDefinition)itemDefinition).instantiate();
                    PrismReferenceValue refValue = ((Referencable) jaxbBean).asReferenceValue();
                    reference.merge(refValue);
                    subItem = (Item<IV,ID>) reference;
                } else{
                    throw new IllegalArgumentException("Unsupported JAXB bean" + jaxbBean);
                }

            } else {
                throw new IllegalArgumentException("Unsupported definition type "+itemDefinition.getClass());
            }
        } else {
            throw new IllegalArgumentException("Unsupported element type "+element.getClass());
        }
        return subItem;
    }


    /**
     * Serializes prism value to JAXB "any" format as returned by JAXB getAny() methods.
     */
    @Override
    public Object toAny(PrismValue value) throws SchemaException {
        if (value == null) {
            return null;
        }
        Itemable parent = value.getParent();
        if (parent == null) {
            throw new IllegalStateException("Couldn't convert parent-less prism value to xsd:any: " + value);
        }
        QName elementName = parent.getElementName();
        if (value instanceof PrismPropertyValue) {
            PrismPropertyValue<Object> pval = (PrismPropertyValue)value;
            if (pval.isRaw() && parent.getDefinition() == null) {
                XNodeImpl rawElement = (XNodeImpl) pval.getRawElement();
                if (rawElement instanceof MapXNodeImpl) {
                    return domParser.serializeXMapToElement((MapXNodeImpl)rawElement, elementName);
                } else if (rawElement instanceof PrimitiveXNodeImpl<?>) {
                    PrimitiveXNodeImpl<?> xprim = (PrimitiveXNodeImpl<?>)rawElement;
                    String stringValue = xprim.getStringValue();
                    Element element = DOMUtil.createElement(DOMUtil.getDocument(), elementName);
                    element.setTextContent(stringValue);
                    DOMUtil.setNamespaceDeclarations(element, xprim.getRelevantNamespaceDeclarations());
                    return element;
                } else {
                    throw new IllegalArgumentException("Cannot convert raw element "+rawElement+" to xsd:any");
                }
            } else {
                Object realValue = pval.getValue();
                if (XmlTypeConverter.canConvert(realValue.getClass())) {
                    // Always record xsi:type. This is FIXME, but should work OK for now (until we put definition into deltas)
                    return XmlTypeConverterInternal.toXsdElement(realValue, elementName, DOMUtil.getDocument(), true);
                } else {
                    return wrapIfNeeded(realValue, elementName);
                }
            }
        } else if (value instanceof PrismReferenceValue) {
            return prismContext.domSerializer().serialize(value, elementName);
        } else if (value instanceof PrismContainerValue<?>) {
            PrismContainerValue<?> pval = (PrismContainerValue<?>)value;
            if (pval.getParent().getCompileTimeClass() == null) {
                // This has to be runtime schema without a compile-time representation.
                // We need to convert it to DOM
                return prismContext.domSerializer().serialize(pval, elementName);
            } else {
                return wrapIfNeeded(pval.asContainerable(), elementName);
            }
        } else {
            throw new IllegalArgumentException("Unknown type "+value);
        }

    }

    private Object wrapIfNeeded(Object value, QName elementName) {
        if (value instanceof Element || value instanceof JAXBElement) {
            return value;
        } else {
            return new JAXBElement(elementName, value.getClass(), value);
        }
    }

}
