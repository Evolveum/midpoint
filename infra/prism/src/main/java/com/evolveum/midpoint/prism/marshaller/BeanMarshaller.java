/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.prism.marshaller;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.lang.reflect.*;
import java.util.*;

public class BeanMarshaller {

    private static final Trace LOGGER = TraceManager.getTrace(BeanMarshaller.class);

    public static final String DEFAULT_PLACEHOLDER = "##default";

    @NotNull private final PrismBeanInspector inspector;
	@NotNull private final PrismContext prismContext;
	@NotNull private final Map<Class,Marshaller> specialMarshallers = new HashMap<>();

	@FunctionalInterface
	private interface Marshaller {
		XNode marshal(Object bean, SerializationContext sc) throws SchemaException;
	}

	private void createSpecialMarshallerMap() {
		specialMarshallers.put(XmlAsStringType.class, this::marshalXmlAsStringType);
		specialMarshallers.put(SchemaDefinitionType.class, this::marshalSchemaDefinition);
		specialMarshallers.put(ProtectedByteArrayType.class, this::marshalProtectedDataType);
		specialMarshallers.put(ProtectedStringType.class, this::marshalProtectedDataType);
		specialMarshallers.put(ItemPathType.class, this::marshalItemPathType);
		specialMarshallers.put(RawType.class, this::marshalRawType);
//		add(PolyString.class, this::unmarshalPolyStringFromPrimitive, this::unmarshalPolyStringFromMap);
//		add(PolyStringType.class, this::unmarshalPolyStringFromPrimitive, this::unmarshalPolyStringFromMap);
	}

	public BeanMarshaller(@NotNull PrismContext prismContext, @NotNull PrismBeanInspector inspector) {
		this.prismContext = prismContext;
		this.inspector = inspector;
		createSpecialMarshallerMap();
	}

	@Nullable
	public <T> XNode marshall(@Nullable T bean) throws SchemaException {
		return marshall(bean, null);
	}

	@Nullable
	public <T> XNode marshall(@Nullable T bean, @Nullable SerializationContext ctx) throws SchemaException {
		if (bean == null) {
			return null;
		}
		Marshaller marshaller = specialMarshallers.get(bean.getClass());
		if (marshaller != null) {
			return marshaller.marshal(bean, ctx);
		} else if (bean instanceof Containerable) {
			return prismContext.xnodeSerializer().serializeRealValue(bean, new QName("dummy")).getSubnode();
		} else if (bean instanceof Enum) {
			return marshalEnum((Enum) bean, ctx);
		} else if (bean.getClass().getAnnotation(XmlType.class) != null) {
			return marshalXmlType(bean, ctx);
		} else {
			return marshalToPrimitive(bean, ctx);
		}
	}

	private XNode marshalToPrimitive(Object bean, SerializationContext ctx) {
		return createPrimitiveXNode(bean, null, false);
	}

	private XNode marshalXmlType(Object bean, SerializationContext ctx) throws SchemaException {

        Class<?> beanClass = bean.getClass();

        MapXNode xmap;
        if (bean instanceof SearchFilterType) {
            // this hack is here because of c:ConditionalSearchFilterType - it is analogous to situation when unmarshalling this type (TODO: rework this in a nicer way)
            xmap = marshalSearchFilterType((SearchFilterType) bean);
            if (SearchFilterType.class.equals(bean.getClass())) {
                return xmap;        // nothing more to serialize; otherwise we continue, because in that case we deal with a subclass of SearchFilterType
            }
        } else {
            xmap = new MapXNode();
        }

		String namespace = inspector.determineNamespace(beanClass);
		if (namespace == null) {
			throw new IllegalArgumentException("Cannot determine namespace of "+beanClass);
		}
		
		List<String> propOrder = inspector.getPropOrder(beanClass);
		for (String fieldName: propOrder) {
			QName elementName = inspector.findFieldElementQName(fieldName, beanClass, namespace);
			Method getter = inspector.findPropertyGetter(beanClass, fieldName);
			if (getter == null) {
				throw new IllegalStateException("No getter for field "+fieldName+" in "+beanClass);
			}
			Object getterResult;
			try {
				getterResult = getter.invoke(bean);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Cannot invoke method for field "+fieldName+" in "+beanClass+": "+e.getMessage(), e);
			}
			
			if (getterResult == null) {
				continue;
			}
			
			Field field = inspector.findPropertyField(beanClass, fieldName);
			boolean isAttribute = inspector.isAttribute(field, getter);
			
			if (getterResult instanceof Collection<?>) {
				Collection col = (Collection) getterResult;
				if (col.isEmpty()) {
					continue;
				}
				Iterator i = col.iterator();
//				if (i == null) {
//					// huh?!? .. but it really happens
//					throw new IllegalArgumentException("Iterator of collection returned from "+getter+" is null");
//				}
				Object getterResultValue = i.next();
				if (getterResultValue == null) {
					continue;
				}
				
				ListXNode xlist = new ListXNode();

                // elementName will be determined from the first item on the list
                // TODO make sure it will be correct with respect to other items as well!
                if (getterResultValue instanceof JAXBElement && ((JAXBElement) getterResultValue).getName() != null) {
                    elementName = ((JAXBElement) getterResultValue).getName();
                }

                for (Object element: col) {
                	if (element == null){
                		continue;
                	}
                    QName fieldTypeName = inspector.findFieldTypeName(field, element.getClass(), namespace);
					Object elementToMarshall = element;
					if (element instanceof JAXBElement){
						elementToMarshall = ((JAXBElement) element).getValue();
					} 
					XNode marshalled = marshallValue(elementToMarshall, fieldTypeName, isAttribute, ctx);

                    // Brutal hack - made here just to make scripts (bulk actions) functional while not breaking anything else
                    // Fix it in 3.1. [med]
                    if (fieldTypeName == null && element instanceof JAXBElement && marshalled != null) {
                        QName typeName = inspector.determineTypeForClass(elementToMarshall.getClass());
                        if (typeName != null && !getSchemaRegistry().hasImplicitTypeDefinition(elementName, typeName)
								&& getSchemaRegistry().findTypeDefinitionByType(typeName, TypeDefinition.class) != null) {
                            marshalled.setExplicitTypeDeclaration(true);
                            marshalled.setTypeQName(typeName);
                        }
                    }
                    else {
                    // end of hack

                        setExplicitTypeDeclarationIfNeeded(getter, getterResultValue, marshalled, fieldTypeName);
                    }
					xlist.add(marshalled);
				}
                xmap.put(elementName, xlist);
			} else {
				QName fieldTypeName = inspector.findFieldTypeName(field, getterResult.getClass(), namespace);
				Object valueToMarshall = null;
				if (getterResult instanceof JAXBElement){
					valueToMarshall = ((JAXBElement) getterResult).getValue();
					elementName = ((JAXBElement) getterResult).getName();
				} else{
					valueToMarshall = getterResult;
				}
				XNode marshelled = marshallValue(valueToMarshall, fieldTypeName, isAttribute, ctx);
				if (!getter.getReturnType().equals(valueToMarshall.getClass()) && getter.getReturnType().isAssignableFrom(valueToMarshall.getClass()) && !(valueToMarshall instanceof Enum)) {
					PrismObjectDefinition def = prismContext.getSchemaRegistry().determineDefinitionFromClass(valueToMarshall.getClass());
					if (def != null){
						QName type = def.getTypeName();
						marshelled.setTypeQName(type);
						marshelled.setExplicitTypeDeclaration(true);
					}
				}
				xmap.put(elementName, marshelled);
				
//				setExplicitTypeDeclarationIfNeeded(getter, valueToMarshall, xmap, fieldTypeName);
			}
		}
		
		return xmap;
	}

	private XNode marshalEnum(Enum bean, SerializationContext ctx) {
		Class<? extends Enum> beanClass = bean.getClass();
		String enumValue = inspector.findEnumFieldValue(beanClass, bean.toString());
		if (StringUtils.isEmpty(enumValue)){
			enumValue = bean.toString();
		}
		QName fieldTypeName = inspector.findFieldTypeName(null, beanClass, DEFAULT_PLACEHOLDER);
		return createPrimitiveXNode(enumValue, fieldTypeName, false);

	}

	private XNode marshalXmlAsStringType(Object bean, SerializationContext sc) {
        PrimitiveXNode xprim = new PrimitiveXNode<>();
        xprim.setValue(((XmlAsStringType) bean).getContentAsString(), DOMUtil.XSD_STRING);
        return xprim;
    }

    public void revive(Object bean, final PrismContext prismContext) throws SchemaException {
		Handler<Object> visitor = o -> {
			if (o instanceof Revivable) {
				try {
					((Revivable)o).revive(prismContext);
				} catch (SchemaException e) {
					throw new TunnelException(e);
				}
			}
			return true;
		};
		try {
			visit(bean,visitor);
		} catch (TunnelException te) {
			SchemaException e = (SchemaException) te.getCause();
			throw e;
		}
	}
	
	public void visit(Object bean, Handler<Object> handler) {
		if (bean == null) {
			return;
		}
		
		Class<? extends Object> beanClass = bean.getClass();
						
		handler.handle(bean);
		
		if (beanClass.isEnum() || beanClass.isPrimitive()){
			//nothing more to do
			return;
		}

        // TODO: implement special handling for RawType, if necessary (it has no XmlType annotation any more)

		XmlType xmlType = beanClass.getAnnotation(XmlType.class);
		if (xmlType == null) {
			// no @XmlType annotation, we are not interested to go any deeper
			return;
		}
		
		List<String> propOrder = inspector.getPropOrder(beanClass);
		for (String fieldName: propOrder) {
			Method getter = inspector.findPropertyGetter(beanClass, fieldName);
			if (getter == null) {
				throw new IllegalStateException("No getter for field "+fieldName+" in "+beanClass);
			}
			Object getterResult;
			try {
				getterResult = getter.invoke(bean);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SystemException("Cannot invoke method for field "+fieldName+" in "+beanClass+": "+e.getMessage(), e);
			}
			
			if (getterResult == null) {
				continue;
			}
			
			if (getterResult instanceof Collection<?>) {
				Collection col = (Collection)getterResult;
				if (col.isEmpty()) {
					continue;
				}
				
				for (Object element: col) {
					visitValue(element, handler);
					
				}
			} else {
				visitValue(getterResult, handler);
			}
		}
	}
	
	private void visitValue(Object element, Handler<Object> handler) {
		Object elementToMarshall = element;
		if (element instanceof JAXBElement){
			elementToMarshall = ((JAXBElement) element).getValue();
		} 
		visit(elementToMarshall, handler);
	}

	private void setExplicitTypeDeclarationIfNeeded(Method getter, Object getterResult, XNode xmap, QName fieldTypeName){
		Class getterReturnType = getter.getReturnType();
		Class getterType = null;
		if (Collection.class.isAssignableFrom(getterReturnType)){
			Type genericReturnType = getter.getGenericReturnType();
			if (genericReturnType instanceof ParameterizedType){
				Type actualType = inspector.getTypeArgument(genericReturnType, "explicit type declaration");

				if (actualType instanceof Class){
					getterType = (Class) actualType;
				}
			}
		}
		if (getterType == null){
			getterType = getterReturnType;
		}
		Class getterResultReturnType = getterResult.getClass();
		if (getterType != getterResultReturnType && getterType.isAssignableFrom(getterResultReturnType)) {
			xmap.setExplicitTypeDeclaration(true);
			xmap.setTypeQName(fieldTypeName);
		}
	}
	
	private <T> XNode marshallValue(T value, QName fieldTypeName, boolean isAttribute, SerializationContext ctx) throws SchemaException {
		if (value == null) {
			return null;
		}
		if (isAttribute) {
			// hoping the value fits into primitive!
			return createPrimitiveXNode(value, fieldTypeName, isAttribute);
		} else {
			return marshall(value, ctx);
		}
	}
	
	private <T> PrimitiveXNode<T> createPrimitiveXNode(T value, QName fieldTypeName, boolean isAttribute) {
		PrimitiveXNode<T> xprim = new PrimitiveXNode<T>();
		xprim.setValue(value, fieldTypeName);
		xprim.setAttribute(isAttribute);
		return xprim;
	}

    private <T> PrimitiveXNode<T> createPrimitiveXNode(T val, QName type) {
        return createPrimitiveXNode(val, type, false);
    }

    private XNode marshalRawType(Object value, SerializationContext sc) throws SchemaException {
        return ((RawType) value).serializeToXNode();
	}

    private XNode marshalItemPathType(Object o, SerializationContext sc) {
		ItemPathType itemPath = (ItemPathType) o;
        PrimitiveXNode<ItemPathType> xprim = new PrimitiveXNode<>();
        if (itemPath != null) {
            xprim.setValue(itemPath, ItemPathType.COMPLEX_TYPE);
        }
        return xprim;
    }

    private XNode marshalSchemaDefinition(Object o, SerializationContext ctx) {
		SchemaDefinitionType schemaDefinitionType = (SchemaDefinitionType) o;
        SchemaXNode xschema = new SchemaXNode();
        xschema.setSchemaElement(schemaDefinitionType.getSchema());
        MapXNode xmap = new MapXNode();
        xmap.put(DOMUtil.XSD_SCHEMA_ELEMENT, xschema);
        return xmap;
    }

    // TODO create more appropriate interface to be able to simply serialize ProtectedStringType instances
    public <T> MapXNode marshalProtectedDataType(Object o, SerializationContext sc) throws SchemaException {
		ProtectedDataType<T> protectedType = (ProtectedDataType<T>) o;
        MapXNode xmap = new MapXNode();
        if (protectedType.getEncryptedDataType() != null) {
            EncryptedDataType encryptedDataType = protectedType.getEncryptedDataType();
            MapXNode xEncryptedDataType = (MapXNode) marshall(encryptedDataType);
            xmap.put(ProtectedDataType.F_ENCRYPTED_DATA, xEncryptedDataType);
        } else if (protectedType.getClearValue() != null){
            QName type = XsdTypeMapper.toXsdType(protectedType.getClearValue().getClass());
            PrimitiveXNode xClearValue = createPrimitiveXNode(protectedType.getClearValue(), type);
            xmap.put(ProtectedDataType.F_CLEAR_VALUE, xClearValue);
        }
        // TODO: clearValue
        return xmap;
    }


    //region Specific marshallers ==============================================================
	private MapXNode marshalSearchFilterType(SearchFilterType value) throws SchemaException {
		if (value == null) {
			return null;
		}
		return value.serializeToXNode();
	}

	//endregion

	@NotNull
	public PrismContext getPrismContext() {
		return prismContext;
	}

	private SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}

	public boolean canProcess(QName typeName) {
		Class<Object> clazz = getSchemaRegistry().determineClassForType(typeName);
		if (clazz != null && canProcess(clazz)) {
			return true;
		}
		TypeDefinition td = getSchemaRegistry().findTypeDefinitionByType(typeName);
		if (td instanceof SimpleTypeDefinition) {
			return true;			// most probably dynamic enum, at this point
		}
		return false;
	}

	public boolean canProcess(@NotNull Class<?> clazz) {
		return !Containerable.class.isAssignableFrom(clazz) &&
				(RawType.class.equals(clazz) || clazz.getAnnotation(XmlType.class) != null || XsdTypeMapper.getTypeFromClass(clazz) != null);
	}

	public QName determineTypeForClass(Class<?> clazz) {
		return inspector.determineTypeForClass(clazz);
	}

}


// TODO hacked, for now
//    private <T> String findEnumFieldValue(Class classType, Object bean){
//        String name = bean.toString();
//        for (Field field: classType.getDeclaredFields()) {
//            XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
//            if (xmlEnumValue != null && field.getName().equals(name)) {
//                return xmlEnumValue.value();
//            }
//        }
//        return null;
//    }
