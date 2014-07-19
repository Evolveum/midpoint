/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.parser.XNodeSerializer;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.RawTypeUtil;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 *
 */
public class DeltaConvertor {
	
	public static final QName PATH_ELEMENT_NAME = new QName(PrismConstants.NS_TYPES, "path");

	public static <T extends Objectable> ObjectDelta<T> createObjectDelta(ObjectModificationType objectModification,
			Class<T> type, PrismContext prismContext) throws SchemaException {
        Validate.notNull(prismContext, "No prismContext in DeltaConvertor.createObjectDelta call");
		PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		if (objectDefinition == null) {
			throw new SchemaException("No object definition for class "+type);
		}
        return createObjectDelta(objectModification, objectDefinition);
    }

    public static <T extends Objectable> ObjectDelta<T> createObjectDelta(ObjectModificationType objectModification,
            PrismObjectDefinition<T> objDef) throws SchemaException {
        ObjectDelta<T> objectDelta = new ObjectDelta<T>(objDef.getCompileTimeClass(), ChangeType.MODIFY, objDef.getPrismContext());
        objectDelta.setOid(objectModification.getOid());

        for (ItemDeltaType propMod : objectModification.getItemDelta()) {
            ItemDelta itemDelta = createItemDelta(propMod, objDef);
            objectDelta.addModification(itemDelta);
        }

        return objectDelta;
    }
    
    public static <T extends Objectable> ObjectDelta<T> createObjectDelta(ObjectDeltaType objectDeltaType,
            PrismContext prismContext) throws SchemaException {

        Validate.notNull(prismContext, "No prismContext in DeltaConvertor.createObjectDelta call");
        QName objectType = objectDeltaType.getObjectType();
        if (objectType == null) {
            throw new SchemaException("No objectType specified");
        }
    	PrismObjectDefinition<T> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(objectType);
    	Class<T> type = objDef.getCompileTimeClass();

        if (objectDeltaType.getChangeType() == ChangeTypeType.ADD) {
        	ObjectDelta<T> objectDelta = new ObjectDelta<T>(type, ChangeType.ADD, prismContext);
            objectDelta.setOid(objectDeltaType.getOid());
            ObjectType objectToAddElement = objectDeltaType.getObjectToAdd();
//            PrismObject<T> objectToAdd = prismContext.getXnodeProcessor().parseObject(objectToAddElement.getXnode());
//            PrismObject<T> objectToAdd = prismContext.getJaxbDomHack().parseObjectFromJaxb(objectToAddElement);
            objectDelta.setObjectToAdd(objectToAddElement.asPrismObject());
            return objectDelta;
        } else if (objectDeltaType.getChangeType() == ChangeTypeType.MODIFY) {
        	ObjectDelta<T> objectDelta = new ObjectDelta<T>(type, ChangeType.MODIFY, prismContext);
            objectDelta.setOid(objectDeltaType.getOid());
	        for (ItemDeltaType propMod : objectDeltaType.getItemDelta()) {
	            ItemDelta itemDelta = createItemDelta(propMod, objDef);
	            objectDelta.addModification(itemDelta);
	        }
	        return objectDelta;
        } else if (objectDeltaType.getChangeType() == ChangeTypeType.DELETE) {
        	ObjectDelta<T> objectDelta = new ObjectDelta<T>(type, ChangeType.DELETE, prismContext);
            objectDelta.setOid(objectDeltaType.getOid());
            return objectDelta;
        } else {
        	throw new SchemaException("Unknown change type "+objectDeltaType.getChangeType());
        }

    }

    public static ObjectDeltaOperation createObjectDeltaOperation(ObjectDeltaOperationType objectDeltaOperationType,
                                                                          PrismContext prismContext) throws SchemaException {
        ObjectDeltaOperation retval = new ObjectDeltaOperation(createObjectDelta(objectDeltaOperationType.getObjectDelta(), prismContext));
        if (objectDeltaOperationType.getExecutionResult() != null) {
            retval.setExecutionResult(OperationResult.createOperationResult(objectDeltaOperationType.getExecutionResult()));
        }
        return retval;
    }

    public static <T extends Objectable> Collection<? extends ItemDelta> toModifications(ObjectModificationType objectModification,
			Class<T> type, PrismContext prismContext) throws SchemaException {
        Validate.notNull(prismContext, "No prismContext in DeltaConvertor.toModifications call");
        PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
		if (objectDefinition == null) {
			throw new SchemaException("No object definition for class "+type);
		}
        return toModifications(objectModification, objectDefinition);
    }
    
    public static <T extends Objectable> Collection<? extends ItemDelta> toModifications(ObjectModificationType objectModification, 
    		PrismObjectDefinition<T> objDef) throws SchemaException {
    	return toModifications(objectModification.getItemDelta(), objDef);
    }
    	
    public static <T extends Objectable> Collection<? extends ItemDelta> toModifications(Collection<ItemDeltaType> itemDeltaTypes, 
    		PrismObjectDefinition<T> objDef) throws SchemaException {
    	Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
    	for (ItemDeltaType propMod : itemDeltaTypes) {
            ItemDelta itemDelta = createItemDelta(propMod, objDef);
            modifications.add(itemDelta);
        }
    	return modifications;
	}
    
    /**
     * Converts this delta to ObjectModificationType (XML).
     */
    public static <T extends Objectable> ObjectModificationType toObjectModificationType(ObjectDelta<T> delta) throws SchemaException {
        if (delta.getChangeType() != ChangeType.MODIFY) {
            throw new IllegalStateException("Cannot produce ObjectModificationType from delta of type " + delta.getChangeType());
        }
        ObjectModificationType modType = new ObjectModificationType();
        modType.setOid(delta.getOid());
        List<ItemDeltaType> propModTypes = modType.getItemDelta();
        for (ItemDelta<?> propDelta : delta.getModifications()) {
            Collection<ItemDeltaType> propPropModTypes;
            try {
                propPropModTypes = toPropertyModificationTypes(propDelta);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " in " + delta.toString(), e);
            }
            propModTypes.addAll(propPropModTypes);
        }
        return modType;
    }
    
	public static ObjectDeltaType toObjectDeltaType(ObjectDelta<? extends ObjectType> objectDelta) throws SchemaException {
        Validate.notNull(objectDelta.getPrismContext(), "ObjectDelta without prismContext cannot be converted to ObjectDeltaType");
		ObjectDeltaType objectDeltaType = new ObjectDeltaType();
		objectDeltaType.setChangeType(convertChangeType(objectDelta.getChangeType()));
		Class<? extends Objectable> type = objectDelta.getObjectTypeClass();
		PrismObjectDefinition<? extends Objectable> objDef = objectDelta.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        if (objDef == null) {
            throw new SchemaException("Unknown compile time class: " + type);
        }
		objectDeltaType.setObjectType(objDef.getTypeName());
		objectDeltaType.setOid(objectDelta.getOid());
		
		if (objectDelta.getChangeType() == ChangeType.ADD) {
			PrismObject<? extends ObjectType> prismObject = objectDelta.getObjectToAdd();
			if (prismObject != null) {
//				Element objectElement = prismObject.getPrismContext().getJaxbDomHack().serializeObjectToJaxb(prismObject);
//				ObjectDeltaType.ObjectToAdd objectToAdd = new ObjectDeltaType.ObjectToAdd();
//				objectToAdd.setAny(new JAXBElement(new QName("any"), prismObject.getCompileTimeClass(), prismObject.asObjectable()));
//				XNode node = prismObject.getPrismContext().getXnodeProcessor().serializeObject(prismObject);
//				MapXNode objToAdd = null;
//				if (node instanceof RootXNode){
//					objToAdd = (MapXNode) ((RootXNode) node).getSubnode();
//				} else if (node instanceof MapXNode){
//					objToAdd = (MapXNode) node;
//				} else {
//					throw new IllegalArgumentException("cannot process delta with object to add: " + node);
//				}
				objectDeltaType.setObjectToAdd(prismObject.asObjectable());
			}
		} else if (objectDelta.getChangeType() == ChangeType.MODIFY) {
		    ObjectModificationType modType = new ObjectModificationType();
		    modType.setOid(objectDelta.getOid());
		    for (ItemDelta<?> propDelta : objectDelta.getModifications()) {
		        Collection<ItemDeltaType> propPropModTypes;
		        try {
		            propPropModTypes = toPropertyModificationTypes(propDelta);
		        } catch (SchemaException e) {
		            throw new SchemaException(e.getMessage() + " in " + objectDelta.toString(), e);
		        }
		        objectDeltaType.getItemDelta().addAll(propPropModTypes);
		    }
        } else if (objectDelta.getChangeType() == ChangeType.DELETE) {
        	// Nothing to do
        } else {
        	throw new SystemException("Unknown changetype "+objectDelta.getChangeType());
        }
		return objectDeltaType;
	}

    public static String toObjectDeltaTypeXml(ObjectDelta<? extends ObjectType> delta) throws SchemaException, JAXBException {
        Validate.notNull(delta.getPrismContext(), "ObjectDelta without prismContext cannot be converted to XML");
        ObjectDeltaType objectDeltaType = toObjectDeltaType(delta);
        return delta.getPrismContext().serializeAtomicValue(objectDeltaType, SchemaConstants.T_OBJECT_DELTA, PrismContext.LANG_XML);
    }


    public static ObjectDeltaOperationType toObjectDeltaOperationType(ObjectDeltaOperation objectDeltaOperation) throws SchemaException {
        ObjectDeltaOperationType retval = new ObjectDeltaOperationType();
        if (objectDeltaOperation.getObjectDelta() != null) {
            retval.setObjectDelta(toObjectDeltaType(objectDeltaOperation.getObjectDelta()));
        }
        if (objectDeltaOperation.getExecutionResult() != null) {
            retval.setExecutionResult(objectDeltaOperation.getExecutionResult().createOperationResultType());
        }
        return retval;
    }
    
	private static ChangeTypeType convertChangeType(ChangeType changeType) {
		if (changeType == ChangeType.ADD) {
			return ChangeTypeType.ADD;
		}
		if (changeType == ChangeType.MODIFY) {
			return ChangeTypeType.MODIFY;
		}
		if (changeType == ChangeType.DELETE) {
			return ChangeTypeType.DELETE;
		}
		throw new SystemException("Unknown changetype "+changeType);
	}

	/**
     * Creates delta from PropertyModificationType (XML). The values inside the PropertyModificationType are converted to java.
     * That's the reason this method needs schema and objectType (to locate the appropriate definitions).
     */
    public static <V extends PrismValue> ItemDelta<V> createItemDelta(ItemDeltaType propMod,
            Class<? extends Objectable> objectType, PrismContext prismContext) throws SchemaException {
        Validate.notNull("No prismContext in DeltaConvertor.createItemDelta call");
        PrismObjectDefinition<? extends Objectable> objectDefinition = prismContext.getSchemaRegistry().
        		findObjectDefinitionByCompileTimeClass(objectType);
        return createItemDelta(propMod, objectDefinition);
    }

    public static <V extends PrismValue> ItemDelta<V> createItemDelta(ItemDeltaType propMod, PrismContainerDefinition<?> pcDef) throws
            SchemaException {
    	ItemPathType parentPathType = propMod.getPath();
    	ItemPath parentPath = null;
    	if (parentPathType != null){
    		parentPath = parentPathType.getItemPath();
    	} else {
    		throw new IllegalStateException("Path argument in the itemDelta HAVE TO BE specified.");
    	}
        if (propMod.getValue() == null) {
            throw new IllegalArgumentException("No value in item delta (path: " + parentPath + ") while creating a property delta");
        }
       
        ItemDefinition containingPcd = pcDef.findItemDefinition(parentPath);
        PrismContainerDefinition containerDef = null;
        if (containingPcd == null) {
        	containerDef = pcDef.findContainerDefinition(parentPath.allUpToLastNamed());
        	if (containerDef == null){
        		throw new SchemaException("No definition for " + parentPath.allUpToLastNamed().lastNamed().getName() + " (while creating delta for " + pcDef + ")");
        	} 
        }
        QName elementName = parentPath.lastNamed().getName();
        Item item = RawTypeUtil.getParsedItem(containingPcd, propMod.getValue(), elementName, containerDef);//propMod.getValue().getParsedValue(containingPcd);
        ItemDelta<V> itemDelta = item.createDelta(parentPath);
        if (propMod.getModificationType() == ModificationTypeType.ADD) {
        	itemDelta.addValuesToAdd(PrismValue.resetParentCollection(PrismValue.cloneCollection(item.getValues())));
        } else if (propMod.getModificationType() == ModificationTypeType.DELETE) {
        	itemDelta.addValuesToDelete(PrismValue.resetParentCollection(PrismValue.cloneCollection(item.getValues())));
        } else if (propMod.getModificationType() == ModificationTypeType.REPLACE) {
        	itemDelta.setValuesToReplace(PrismValue.resetParentCollection(PrismValue.cloneCollection(item.getValues())));
        }

        return itemDelta;
    }

    /**
     * Converts this delta to PropertyModificationType (XML).
     */
    public static Collection<ItemDeltaType> toPropertyModificationTypes(ItemDelta delta) throws SchemaException {
    	delta.checkConsistence();
        if (delta.isEmpty() && delta.getPrismContext() == null) {
            throw new IllegalStateException("Non-empty ItemDelta with no prismContext cannot be converted to ItemDeltaType.");
        }
        Collection<ItemDeltaType> mods = new ArrayList<>();
        ItemPathType path = new ItemPathType(delta.getPath());
        if (delta.getValuesToReplace() != null) {
            ItemDeltaType mod = new ItemDeltaType();
            mod.setPath(path);
            mod.setModificationType(ModificationTypeType.REPLACE);
            try {
                addModValues(delta, mod, delta.getValuesToReplace());
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + delta.getElementName(), e);
            }
            mods.add(mod);
        }
        if (delta.getValuesToAdd() != null) {
            ItemDeltaType mod = new ItemDeltaType();
            mod.setPath(path);
            mod.setModificationType(ModificationTypeType.ADD);
            try {
                addModValues(delta, mod, delta.getValuesToAdd());
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + delta.getElementName(), e);
            }
            mods.add(mod);
        }
        if (delta.getValuesToDelete() != null) {
            ItemDeltaType mod = new ItemDeltaType();
            mod.setPath(path);
            mod.setModificationType(ModificationTypeType.DELETE);
            try {
                addModValues(delta, mod, delta.getValuesToDelete());
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + delta.getElementName(), e);
            }
            mods.add(mod);
        }
        return mods;
    }

    // requires delta.prismContext to be set
    private static void addModValues(ItemDelta delta, ItemDeltaType mod, Collection<PrismValue> values) throws SchemaException {
        if (values == null || values.isEmpty()) {
            RawType modValue = new RawType(delta.getPrismContext());
            mod.getValue().add(modValue);
        } else {
	        for (PrismValue value : values) {
	        	XNode xnode = toXNode(delta, value);
	        	RawType modValue = new RawType(xnode, value.getPrismContext());
                mod.getValue().add(modValue);
	        }
        }
    }
    
    private static XNode toXNode(ItemDelta delta, PrismValue value) throws SchemaException{
		XNodeSerializer serializer = delta.getPrismContext().getXnodeProcessor().createSerializer();
		XNode node = serializer.serializeItemValue(value, delta.getDefinition());
		return node;
    }

//	private static Object toAny(ItemDelta delta, PrismValue value, Document document) throws SchemaException {
//		PrismContext prismContext = delta.getPrismContext();
//		if (prismContext != null) {
//			return RawTypeUtil.toAny(value, document, prismContext);
//		}
//		if (value instanceof PrismPropertyValue<?>) {
//			PrismPropertyValue<?> pval = (PrismPropertyValue<?>)value;
//			return pval.getRawElement();
//		} else {
//			throw new SystemException("Null prism context in "+value+" in "+delta);
//		}
//	}

    public static Collection<ObjectDelta> createObjectDeltas(ObjectDeltaListType deltaList, PrismContext prismContext) throws SchemaException {
        List<ObjectDelta> retval = new ArrayList<>();
        for (ObjectDeltaType deltaType : deltaList.getDelta()) {
            retval.add(createObjectDelta(deltaType, prismContext));
        }
        return retval;
    }
}
