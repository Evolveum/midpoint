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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BeanMarshaller {

    private static final Trace LOGGER = TraceManager.getTrace(BeanMarshaller.class);

    public static final String DEFAULT_PLACEHOLDER = "##default";

    private PrismBeanInspector inspector;

	@NotNull private final PrismContext prismContext;

	public BeanMarshaller(@NotNull PrismContext prismContext, PrismBeanInspector inspector) {
		this.prismContext = prismContext;
		this.inspector = inspector;
	}

	@NotNull
	public PrismContext getPrismContext() {
		return prismContext;
	}

	private SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}

	public boolean canProcess(QName typeName) {
		return getSchemaRegistry().determineCompileTimeClass(typeName) != null; 
	}
	
	public boolean canProcess(@NotNull Class<?> clazz) {
		return RawType.class.equals(clazz) || clazz.getAnnotation(XmlType.class) != null;
	}
	
	public QName determineTypeForClass(Class<?> clazz) {
		return inspector.determineTypeForClass(clazz);
	}
	
	private MapXNode marshalSearchFilterType(SearchFilterType value) throws SchemaException {
		if (value == null) {
			return null;
		}
		return value.serializeToXNode();
	}

	private Type getTypeArgument(Type origType, String desc) {
		if (!(origType instanceof ParameterizedType)) {
			throw new IllegalArgumentException("No a parametrized type "+desc);
		}
		ParameterizedType parametrizedType = (ParameterizedType)origType;
		Type[] actualTypeArguments = parametrizedType.getActualTypeArguments();
		if (actualTypeArguments == null || actualTypeArguments.length == 0) {
			throw new IllegalArgumentException("No type arguments for getter "+desc);
		}
		if (actualTypeArguments.length > 1) {
			throw new IllegalArgumentException("Too many type arguments for getter for "+desc);
		}
		return actualTypeArguments[0];
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


	public <T> XNode marshall(T bean) throws SchemaException {
		return marshall(bean, null);
	}

	public <T> XNode marshall(T bean, SerializationContext ctx) throws SchemaException {
		if (bean == null) {
			return null;
		}
        if (bean instanceof SchemaDefinitionType) {
            return marshalSchemaDefinition((SchemaDefinitionType) bean);
        } else if (bean instanceof ProtectedDataType<?>) {
            MapXNode xProtected = marshalProtectedDataType((ProtectedDataType<?>) bean);
            return xProtected;
        } else if (bean instanceof ItemPathType){
            return marshalItemPathType((ItemPathType) bean);
        } else if (bean instanceof RawType) {
            return marshalRawValue((RawType) bean);
        } else if (bean instanceof XmlAsStringType) {
            return marshalXmlAsStringType((XmlAsStringType) bean);
        } else if (prismContext != null && prismContext.getSchemaRegistry().determineDefinitionFromClass(bean.getClass()) != null){
			// TODO change to marshalItemContent
        	return ((PrismContextImpl) prismContext).getPrismMarshaller().marshalItemAsRoot(((Objectable)bean).asPrismObject(),
					null, null, ctx).getSubnode();
        }
        // Note: SearchFilterType is treated below

        Class<? extends Object> beanClass = bean.getClass();

        if (beanClass == String.class) {
        	return createPrimitiveXNode((String)bean, DOMUtil.XSD_STRING, false);
        }
        
        //check for enums
        if (beanClass.isEnum()){
			String enumValue = inspector.findEnumFieldValue(beanClass, bean.toString());
            if (StringUtils.isEmpty(enumValue)){
                enumValue = bean.toString();
            }
            QName fieldTypeName = inspector.findFieldTypeName(null, beanClass, DEFAULT_PLACEHOLDER);
            return createPrimitiveXNode(enumValue, fieldTypeName, false);
        }

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

		XmlType xmlType = beanClass.getAnnotation(XmlType.class);
		if (xmlType == null) {
			throw new IllegalArgumentException("Cannot marshall "+beanClass+" it does not have @XmlType annotation");
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
				Collection col = (Collection)getterResult;
				if (col.isEmpty()) {
					continue;
				}
				Iterator i = col.iterator();
				if (i == null) {
					// huh?!? .. but it really happens
					throw new IllegalArgumentException("Iterator of collection returned from "+getter+" is null");
				}
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
                        if (typeName != null && !getSchemaRegistry().hasImplicitTypeDefinition(elementName, typeName)) {
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
				if (!getter.getReturnType().equals(valueToMarshall.getClass()) && getter.getReturnType().isAssignableFrom(valueToMarshall.getClass())){
					if (prismContext != null) {
                        PrismObjectDefinition def = prismContext.getSchemaRegistry().determineDefinitionFromClass(valueToMarshall.getClass());
                        if (def != null){
                            QName type = def.getTypeName();
                            marshelled.setTypeQName(type);
                            marshelled.setExplicitTypeDeclaration(true);
                        }
					}
				}
				xmap.put(elementName, marshelled);
				
//				setExplicitTypeDeclarationIfNeeded(getter, valueToMarshall, xmap, fieldTypeName);
			}
		}
		
		return xmap;
	}

    private XNode marshalXmlAsStringType(XmlAsStringType bean) {
        PrimitiveXNode xprim = new PrimitiveXNode<>();
        xprim.setValue(bean.getContentAsString(), DOMUtil.XSD_STRING);
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
				Type actualType = getTypeArgument(genericReturnType, "explicit type declaration");

				if (actualType instanceof Class){
					getterType = (Class) actualType;
				}
			}
		}
		if (getterType == null){
			getterType = getterReturnType;
		}
		Class getterResultReturnType = getterResult.getClass();
		if (getterType != getterResultReturnType && getterType.isAssignableFrom(getterResultReturnType)){
			xmap.setExplicitTypeDeclaration(true);
			xmap.setTypeQName(fieldTypeName);
		}
	}
	
	private <T> XNode marshallValue(T value, QName fieldTypeName, boolean isAttribute, SerializationContext ctx) throws SchemaException {
		if (value == null) {
			return null;
		}
		if (canProcess(value.getClass())) {
			// This must be a bean
			return marshall(value, ctx);
		} else {
			// primitive value
			return createPrimitiveXNode(value, fieldTypeName, isAttribute);
		}
	}
	
	private <T> PrimitiveXNode<T> createPrimitiveXNode(T value, QName fieldTypeName, boolean isAttribute){
		PrimitiveXNode<T> xprim = new PrimitiveXNode<T>();
		xprim.setValue(value, fieldTypeName);
		xprim.setAttribute(isAttribute);
		return xprim;
	}

    private <T> PrimitiveXNode<T> createPrimitiveXNode(T val, QName type) {
        return createPrimitiveXNode(val, type, false);
    }

    private XNode marshalRawValue(RawType value) throws SchemaException {
        return value.serializeToXNode();
	}

    private XNode marshalItemPathType(ItemPathType itemPath) {
        PrimitiveXNode<ItemPath> xprim = new PrimitiveXNode<ItemPath>();
        if (itemPath != null){
            ItemPath path = itemPath.getItemPath();
            xprim.setValue(path, ItemPathType.COMPLEX_TYPE);
        }
        return xprim;
    }

    private XNode marshalSchemaDefinition(SchemaDefinitionType schemaDefinitionType) {
        SchemaXNode xschema = new SchemaXNode();
        xschema.setSchemaElement(schemaDefinitionType.getSchema());
        MapXNode xmap = new MapXNode();
        xmap.put(DOMUtil.XSD_SCHEMA_ELEMENT, xschema);
        return xmap;
    }

    // TODO create more appropriate interface to be able to simply serialize ProtectedStringType instances
    public <T> MapXNode marshalProtectedDataType(ProtectedDataType<T> protectedType) throws SchemaException {
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
}
 