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

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.ValueSerializationUtil;
import com.evolveum.midpoint.repo.sql.data.audit.RObjectDeltaOperation;
import com.evolveum.midpoint.repo.sql.data.common.OperationResult;
import com.evolveum.midpoint.repo.sql.data.common.OperationResultFull;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtDate;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtLong;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtPolyString;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtReference;
import com.evolveum.midpoint.repo.sql.data.common.any.RAExtString;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtDate;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtLong;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtPolyString;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtReference;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtString;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference;
import com.evolveum.midpoint.repo.sql.data.common.container.RExclusion;
import com.evolveum.midpoint.repo.sql.data.common.container.RTrigger;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceOwner;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author lazyman
 */
public final class RUtil {

	/**
	 * This constant is used for mapping type for {@link javax.persistence.Lob}
	 * fields. {@link org.hibernate.type.MaterializedClobType} was not working
	 * properly with PostgreSQL, causing TEXT types (clobs) to be saved not in
	 * table row but somewhere else and it always messed up UTF-8 encoding
	 */
	public static final String LOB_STRING_TYPE = "org.hibernate.type.StringClobType";

	public static final int COLUMN_LENGTH_QNAME = 157;

	public static final String QNAME_DELIMITER = "#";
	
	/**
	 * This constant is used for oid column size in database.
	 */
	public static final int COLUMN_LENGTH_OID = 36;

	/**
	 * This namespace is used for wrapping xml parts of objects during save to
	 * database.
	 */
	public static final String NS_SQL_REPO = "http://midpoint.evolveum.com/xml/ns/fake/sqlRepository-1.xsd";
	public static final String SQL_REPO_OBJECTS = "sqlRepoObjects";
	public static final String SQL_REPO_OBJECT = "sqlRepoObject";
	public static final QName CUSTOM_OBJECT = new QName(NS_SQL_REPO, SQL_REPO_OBJECT);
	public static final QName CUSTOM_OBJECTS = new QName(NS_SQL_REPO, SQL_REPO_OBJECTS);

	private static final Trace LOGGER = TraceManager.getTrace(RUtil.class);

	private RUtil() {
	}

	public static <T extends Objectable> void revive(Objectable object, PrismContext prismContext)
			throws DtoTranslationException {
		try {
			prismContext.adopt(object);
		} catch (SchemaException ex) {
			throw new DtoTranslationException(ex.getMessage(), ex);
		}
	}

	public static <T> T toJAXB(String value, Class<T> clazz, PrismContext prismContext)
			throws SchemaException, JAXBException {
		return toJAXB(value, clazz, null, prismContext);
	}

	public static <T> T toJAXB(String value, Class<T> clazz, QName type, PrismContext prismContext)
			throws SchemaException, JAXBException {
		return toJAXB(null, null, value, clazz, type, prismContext);
	}

	@Deprecated
	public static <T> T toJAXB(Class<?> parentClass, ItemPath path, String value, Class<T> clazz,
			PrismContext prismContext) throws SchemaException, JAXBException {
		return toJAXB(parentClass, path, value, clazz, null, prismContext);
	}

	public static <T> T toJAXB(Class parentClass, ItemPath path, String value, Class<T> clazz,
			QName complexType, PrismContext prismContext) throws SchemaException, JAXBException {
		if (StringUtils.isEmpty(value)) {
			return null;
		}

	
		if (Objectable.class.isAssignableFrom(clazz)) {
			// if (root == null) {
			// return null;
			// }
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Parsing:\n{}", value);
			}
			PrismObject object = prismContext.parseObject(value);
			return (T) object.asObjectable();
		}
		PrismContainerDefinition definition = null;
		if (Containerable.class.isAssignableFrom(clazz)) {
			SchemaRegistry registry = prismContext.getSchemaRegistry();
			PrismContainerDefinition parentDefinition = registry.findContainerDefinitionByCompileTimeClass(parentClass);

			if (parentDefinition != null) {
				definition = parentDefinition.findContainerDefinition(path);
			}
		}
		T jaxbValue = ValueSerializationUtil.deserializeValue(value, parentClass, complexType, definition,
				prismContext, PrismContext.LANG_XML);

		return jaxbValue;
	}
	
	public static <T> T toJAXB(Class parentClass, QName path, QName elementName, String value, Class<T> clazz, PrismContext prismContext) throws SchemaException, JAXBException{
		if (StringUtils.isEmpty(value)) {
			return null;
		}
		if (Objectable.class.isAssignableFrom(clazz)) {
			// if (root == null) {
			// return null;
			// }
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Parsing:\n{}", value);
			}
			System.out.println("value" + value);
			PrismObject object = prismContext.parseObject(value);
			return (T) object.asObjectable();
		}
		SchemaRegistry registry = prismContext.getSchemaRegistry();
		 
			PrismContainerDefinition cDefinition = registry.findContainerDefinitionByCompileTimeClass(parentClass);
			ItemDefinition definition = null;
			if (cDefinition != null){
				definition = cDefinition.findItemDefinition(path);
			} else {
//				definition = registry.findPropertyDefinitionByElementName(SchemaConstantsGenerated.C_OPERATION_RESULT);
				definition = registry.findPropertyDefinitionByElementName(path);
				
			}
			

		if (definition == null) {
			definition = prismContext.getSchemaRegistry().findItemDefinitionByElementName(path);
			
		}
		if (definition == null && elementName != null){
			definition = registry.findItemDefinitionByElementName(elementName);
		}
		
		
//		System.out.println("definition: " + definition);
//		System.out.println("value: " + value);

		T jaxbValue = ValueSerializationUtil.deserializeValue(value, parentClass, path, definition,
				prismContext, PrismContext.LANG_XML);
		
//		if (OperationResultType.F_PARTIAL_RESULTS.equals(path)){
//			System.out.println("op result: " + );
//			return (T) ((List)jaxbValue).get(0);
//		}
		
		System.out.println("jaxbValue " + jaxbValue);
		if (jaxbValue instanceof List && !List.class.isAssignableFrom(clazz)){
			return (T) ((List) jaxbValue).get(0);
		}

		return jaxbValue;
	}

	public static <T> T toJAXB(Class parentClass, QName path, String value, Class<T> clazz, PrismContext prismContext) throws SchemaException, JAXBException {
		return toJAXB(parentClass, path, null, value, clazz, prismContext);
//		if (StringUtils.isEmpty(value)) {
//			return null;
//		}
//		if (Objectable.class.isAssignableFrom(clazz)) {
//			// if (root == null) {
//			// return null;
//			// }
//			if (LOGGER.isTraceEnabled()) {
//				LOGGER.trace("Parsing:\n{}", value);
//			}
//			PrismObject object = prismContext.parseObject(value);
//			return (T) object.asObjectable();
//		}
//		SchemaRegistry registry = prismContext.getSchemaRegistry();
//		 
//			PrismContainerDefinition cDefinition = registry.findContainerDefinitionByCompileTimeClass(parentClass);
//			ItemDefinition definition = null;
//			if (cDefinition != null){
//				definition = cDefinition.findItemDefinition(path);
//			} else {
////				definition = registry.findPropertyDefinitionByElementName(SchemaConstantsGenerated.C_OPERATION_RESULT);
//				definition = registry.findPropertyDefinitionByElementName(path);
//			}
//			
//
//		if (definition == null) {
//			definition = prismContext.getSchemaRegistry().findItemDefinitionByElementName(path);
//			System.out.println("definition: " + definition);
//		}
//		System.out.println("value: " + value);
//
//		T jaxbValue = ValueSerializationUtil.deserializeValue(value, parentClass, path, definition,
//				prismContext, PrismContext.LANG_XML);
//
//		return jaxbValue;
	}
	
	

	private static PrismContainerDefinition findContainerDefinition(Class clazz, ItemPath path,
			QName complexType, PrismContext prismContext) throws SchemaException {
		SchemaRegistry registry = prismContext.getSchemaRegistry();
        PrismContainerDefinition definition = registry.findContainerDefinitionByCompileTimeClass(clazz);
        if (definition == null) {
            if (complexType != null) {
                // definition = registry.(path);
            }
        }

        return definition;
    }

    public static Item cloneValuesFromJaxb(AuthorizationType jaxb, QName itemName){
    	if (!jaxb.getItem().isEmpty()){
    		Item jaxbContainer = jaxb.asPrismContainerValue().findItem(itemName);
			if (jaxbContainer != null) {
//				Item newContainer = jaxbContainer.clone();
				return jaxbContainer.clone();
			}
    	}
    	return null;
    }
    
    public static <T extends PrismValue> Collection<T> cloneValuesToJaxb(AuthorizationType objectSpecification, QName itemName){
    	PrismContainerValue objSpecCont = objectSpecification.asPrismContainerValue();
    	Item itemContainer = objSpecCont.findItem(itemName);
		if (!objectSpecification.getItem().isEmpty() && itemContainer != null){
			return PrismValue.cloneCollection(itemContainer.getValues());
//		jaxbContainer.findOrCreateProperty(SchemaConstants.C_ITEM).addAll(cloned);
//			jaxb.getItem().addAll(objectSpecification.getItem());
		}
		return null;
    }

	private static Element getFirstSubElement(Element parent) {
		if (parent == null) {
			return null;
		}

		NodeList list = parent.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				return (Element) list.item(i);
			}
		}

		return null;
	}

	public static <T> String toRepo(ItemDefinition parentDefinition, QName itemName, T value,
			PrismContext prismContext) throws SchemaException, JAXBException {
		if (value == null) {
			return null;
		}

		if (value instanceof Objectable) {
			return prismContext.serializeObjectToString(((Objectable) value).asPrismObject(),
					PrismContext.LANG_XML);
		}

		ItemDefinition definition = null;
		if (parentDefinition instanceof PrismContainerDefinition) {

			definition = ((PrismContainerDefinition)parentDefinition).findItemDefinition(itemName);
			if (definition == null) {
				definition = parentDefinition;
			}
		} else{
			definition = parentDefinition;
		}

		return ValueSerializationUtil.serializeValue(value, definition, itemName, parentDefinition.getName(), prismContext, PrismContext.LANG_XML);
	}

	@Deprecated
	/**
	 * DEPRECATED use toRepo(definition, itemName, value, prismContext) instead
	 * @param value
	 * @param itemName
	 * @param prismContext
	 * @return
	 * @throws SchemaException
	 * @throws JAXBException
	 */
	public static <T> String toRepo(T value, QName itemName, PrismContext prismContext)
			throws SchemaException, JAXBException {
		if (value == null) {
			return null;
		}

		// PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
		if (value instanceof Objectable) {
			return prismContext.serializeObjectToString(((Objectable) value).asPrismObject(),
					PrismContext.LANG_XML);
		}

		if (value instanceof Containerable) {
			// TODO: createFakeParentElement??? why we don't use the real
			// name???
			return prismContext.serializeContainerValueToString(
					((Containerable) value).asPrismContainerValue(),
					QNameUtil.getNodeQName(createFakeParentElement()), prismContext.LANG_XML);
		}
		
		ItemDefinition def = prismContext.getSchemaRegistry().findItemDefinitionByElementName(itemName);
		if (def == null){
			ValueSerializationUtil.serializeValue(value, itemName, prismContext, PrismContext.LANG_XML);
		}
		
		return ValueSerializationUtil.serializeValue(value, def, def.getName(), prismContext, PrismContext.LANG_XML);
		
	}
	
	@Deprecated
	/**
	 * DEPRECATED use toRepo(definition, itemName, value, prismContext) instead
	 * @param value
	 * @param itemName
	 * @param prismContext
	 * @return
	 * @throws SchemaException
	 * @throws JAXBException
	 */
	public static <T> String toRepo(T value, PrismContext prismContext)
			throws SchemaException, JAXBException {
		if (value == null) {
			return null;
		}

		// PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
		if (value instanceof Objectable) {
			return prismContext.serializeObjectToString(((Objectable) value).asPrismObject(),
					PrismContext.LANG_XML);
		}

		if (value instanceof Containerable) {
			// TODO: createFakeParentElement??? why we don't use the real
			// name???
			return prismContext.serializeContainerValueToString(
					((Containerable) value).asPrismContainerValue(),
					QNameUtil.getNodeQName(createFakeParentElement()), prismContext.LANG_XML);
		}
		
		
		return ValueSerializationUtil.serializeValue(value, new QName("fake"), prismContext, PrismContext.LANG_XML);
		
	}

	private static Element createFakeParentElement() {
		return DOMUtil.createElement(DOMUtil.getDocument(), CUSTOM_OBJECT);
	}

	public static <T> Set<T> listToSet(List<T> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		return new HashSet<T>(list);
	}

	public static Set<RPolyString> listPolyToSet(List<PolyStringType> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}

		Set<RPolyString> set = new HashSet<RPolyString>();
		for (PolyStringType str : list) {
			set.add(RPolyString.copyFromJAXB(str));
		}
		return set;
	}

	public static <T> List<T> safeSetToList(Set<T> set) {
		if (set == null || set.isEmpty()) {
			return new ArrayList<T>();
		}

		List<T> list = new ArrayList<T>();
		list.addAll(set);

		return list;
	}

    public static List<ObjectReferenceType> safeSetReferencesToList(Set<? extends RObjectReference> set, PrismContext prismContext) {

		if (set == null || set.isEmpty()) {
			return new ArrayList<ObjectReferenceType>();
		}

		List<ObjectReferenceType> list = new ArrayList<ObjectReferenceType>();
		for (RObjectReference str : set) {
			ObjectReferenceType ort = new ObjectReferenceType();
			RObjectReference.copyToJAXB(str, ort, prismContext);
			list.add(ort);
		}
		return list;
	}
	
    public static Set safeListReferenceToSet(List<ObjectReferenceType> list, PrismContext prismContext,
                                             RObject owner, RReferenceOwner refOwner) {
		Set<RObjectReference> set = new HashSet<RObjectReference>();
		if (list == null || list.isEmpty()) {
			return set;
		}

		for (ObjectReferenceType ref : list) {
			RObjectReference rRef = RUtil.jaxbRefToRepo(ref, prismContext, owner, refOwner);
			if (rRef != null) {
				set.add(rRef);
			}
		}
		return set;
	}
	
	public static RObjectReference jaxbRefToRepo(ObjectReferenceType reference, PrismContext prismContext,
			RObject owner, RReferenceOwner refOwner) {
		if (reference == null) {
			return null;
		}
		Validate.notNull(owner, "Owner of reference must not be null.");
		Validate.notNull(refOwner, "Reference owner of reference must not be null.");
		Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

		RObjectReference repoRef = RReferenceOwner.createObjectReference(refOwner);
		repoRef.setOwner(owner);
		RObjectReference.copyFromJAXB(reference, repoRef, prismContext);

		return repoRef;
	}

//	public static Set<RObjectReference> safeListReferenceToSet(List<ObjectReferenceType> list,
//			PrismContext prismContext, RContainer owner, RReferenceOwner refOwner) {
//		Set<RObjectReference> set = new HashSet<RObjectReference>();
//		if (list == null || list.isEmpty()) {
//			return set;
//		}
//
//		for (ObjectReferenceType ref : list) {
//			RObjectReference rRef = RUtil.jaxbRefToRepo(ref, prismContext, owner, refOwner);
//			if (rRef != null) {
//				set.add(rRef);
//			}
//		}
//		return set;
//	}

//	public static RObjectReference jaxbRefToRepo(ObjectReferenceType reference, PrismContext prismContext,
//			RContainer owner, RReferenceOwner refOwner) {
//		if (reference == null) {
//			return null;
//		}
//		Validate.notNull(owner, "Owner of reference must not be null.");
//		Validate.notNull(refOwner, "Reference owner of reference must not be null.");
//		Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");
//
//		RObjectReference repoRef = RReferenceOwner.createObjectReference(refOwner);
//		repoRef.setOwner(owner);
//		RObjectReference.copyFromJAXB(reference, repoRef, prismContext);
//
//		return repoRef;
//	}

	public static REmbeddedReference jaxbRefToEmbeddedRepoRef(ObjectReferenceType jaxb,
			PrismContext prismContext) {
		if (jaxb == null) {
			return null;
		}
		REmbeddedReference ref = new REmbeddedReference();
		REmbeddedReference.copyFromJAXB(jaxb, ref, prismContext);

		return ref;
	}
	
	public static Integer getIntegerFromString(String val) {
        if (val == null || !val.matches("[0-9]+")) {
            return null;
        }

        return Integer.parseInt(val);
    }

//	public static Long getLongFromString(String val) {
//		if (val == null || !val.matches("[0-9]+")) {
//			return null;
//		}
//
//		return Long.parseLong(val);
//	}

	/**
	 * This method is used to override "hasIdentifierMapper" in EntityMetaModels
	 * of entities which have composite id and class defined for it. It's
	 * workaround for bug as found in forum
	 * https://forum.hibernate.org/viewtopic.php?t=978915&highlight=
	 * 
	 * @param sessionFactory
	 */
	public static void fixCompositeIDHandling(SessionFactory sessionFactory) {
		fixCompositeIdentifierInMetaModel(sessionFactory, RObjectDeltaOperation.class);

        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtDate.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtPolyString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtReference.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtLong.class);
        
        fixCompositeIdentifierInMetaModel(sessionFactory, RAssignmentExtension.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtDate.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtPolyString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtReference.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtLong.class);
        
        fixCompositeIdentifierInMetaModel(sessionFactory, RObjectReference.class);
        for (RReferenceOwner owner : RReferenceOwner.values()) {
            fixCompositeIdentifierInMetaModel(sessionFactory, owner.getClazz());
        }

        fixCompositeIdentifierInMetaModel(sessionFactory, RAssignmentReference.class);
        for (RCReferenceOwner owner : RCReferenceOwner.values()) {
            fixCompositeIdentifierInMetaModel(sessionFactory, owner.getClazz());
        }

        fixCompositeIdentifierInMetaModel(sessionFactory, RAssignment.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RExclusion.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RTrigger.class);
        for (RObjectType type : ClassMapper.getKnownTypes()) {
            fixCompositeIdentifierInMetaModel(sessionFactory, type.getClazz());
        }
//		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyContainer.class);
//		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyClob.class);
//		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyDate.class);
//		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyString.class);
//		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyPolyString.class);
//		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyReference.class);
//		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyLong.class);
//
//		fixCompositeIdentifierInMetaModel(sessionFactory, RObjectReference.class);
//		for (RReferenceOwner owner : RReferenceOwner.values()) {
//			fixCompositeIdentifierInMetaModel(sessionFactory, owner.getClazz());
//		}
//
//		fixCompositeIdentifierInMetaModel(sessionFactory, RContainer.class);
//		fixCompositeIdentifierInMetaModel(sessionFactory, RAssignment.class);
//		fixCompositeIdentifierInMetaModel(sessionFactory, RAuthorization.class);
//		fixCompositeIdentifierInMetaModel(sessionFactory, RExclusion.class);
//		fixCompositeIdentifierInMetaModel(sessionFactory, RTrigger.class);
//		for (RContainerType type : ClassMapper.getKnownTypes()) {
//			fixCompositeIdentifierInMetaModel(sessionFactory, type.getClazz());
//		}
	}

	private static void fixCompositeIdentifierInMetaModel(SessionFactory sessionFactory, Class clazz) {
		ClassMetadata classMetadata = sessionFactory.getClassMetadata(clazz);
		if (classMetadata instanceof AbstractEntityPersister) {
			AbstractEntityPersister persister = (AbstractEntityPersister) classMetadata;
			EntityMetamodel model = persister.getEntityMetamodel();
			IdentifierProperty identifier = model.getIdentifierProperty();

			try {
				Field field = IdentifierProperty.class.getDeclaredField("hasIdentifierMapper");
				field.setAccessible(true);
				field.set(identifier, true);
				field.setAccessible(false);
			} catch (Exception ex) {
				throw new SystemException("Attempt to fix entity meta model with hack failed, reason: "
						+ ex.getMessage(), ex);
			}
		}
	}




	public static void copyResultFromJAXB(ItemDefinition parentDef, QName itemName, OperationResultType jaxb, OperationResult repo,
			PrismContext prismContext) throws DtoTranslationException {
	Validate.notNull(repo, "Repo object must not be null.");

        if (jaxb == null) {
            return;
        }

	repo.setStatus(getRepoEnumValue(jaxb.getStatus(), ROperationResultStatus.class));

		
		
        if (repo instanceof OperationResultFull) {
            try {
                ((OperationResultFull) repo).setFullResult(RUtil.toRepo(parentDef, itemName, jaxb, prismContext));


            } catch (Exception ex) {
                throw new DtoTranslationException(ex.getMessage(), ex);
            }
        }
    }

	public static String computeChecksum(Object... objects) {
		StringBuilder builder = new StringBuilder();
		for (Object object : objects) {
			if (object == null) {
				continue;
			}

			builder.append(object.toString());
		}

		return DigestUtils.md5Hex(builder.toString());
	}

	public static <T extends SchemaEnum> T getRepoEnumValue(Object object, Class<T> type) {
		if (object == null) {
			return null;
		}
		Object[] values = type.getEnumConstants();
		for (Object value : values) {
			T schemaEnum = (T) value;
			if (schemaEnum.getSchemaValue().equals(object)) {
				return schemaEnum;
			}
		}

		throw new IllegalArgumentException("Unknown value '" + object + "' of type '" + object.getClass()
				+ "', can't translate to '" + type + "'.");
	}


	public static String qnameToString(QName qname) {
        StringBuilder sb = new StringBuilder();
        if (qname != null) {
            sb.append(qname.getNamespaceURI());
        }
        sb.append(QNAME_DELIMITER);
        if (qname != null) {
            sb.append(qname.getLocalPart());
        }

        return sb.toString();
    }

    public static QName stringToQName(String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        int index = text.lastIndexOf(QNAME_DELIMITER);
        String namespace = StringUtils.left(text, index);
        String localPart = StringUtils.right(text, text.length() - index - 1);

        if (StringUtils.isEmpty(localPart)) {
            return null;
        }

        return new QName(namespace, localPart);
    }

    public static Long toLong(Short s) {
        if (s == null) {
            return null;
        }

        return s.longValue();
    }

    public static Short toShort(Long l) {
        if (l == null) {
            return null;
        }

        if (l > Short.MAX_VALUE || l < Short.MIN_VALUE) {
            throw new IllegalArgumentException("Couldn't cast value to short " + l);
        }

        return l.shortValue();
    }

    public static String getDebugString(RObject object) {
        StringBuilder sb = new StringBuilder();
        if (object.getName() != null) {
            sb.append(object.getName().getOrig());
        } else {
            sb.append("null");
        }
        sb.append('(').append(object.getOid()).append(')');

        return sb.toString();
    }

    public static String getTableName(Class hqlType) {
        MidPointNamingStrategy namingStrategy = new MidPointNamingStrategy();
        return namingStrategy.classToTableName(hqlType.getSimpleName());
    }

    public static byte[] getByteArrayFromXml(String xml, boolean compress) {
        byte[] array;

        GZIPOutputStream gzip = null;
        try {
            if (compress) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                gzip = new GZIPOutputStream(out);
                gzip.write(xml.getBytes("utf-8"));
                gzip.close();
                out.close();

                array = out.toByteArray();
            } else {
                array = xml.getBytes("utf-8");
            }
        } catch (Exception ex) {
            throw new SystemException("Couldn't save full xml object, reason: " + ex.getMessage(), ex);
        } finally {
            IOUtils.closeQuietly(gzip);
        }

        return array;
    }

    public static String getXmlFromByteArray(byte[] array, boolean compressed) {
        String xml;

        GZIPInputStream gzip = null;
        try {
            if (compressed) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                gzip = new GZIPInputStream(new ByteArrayInputStream(array));
                IOUtils.copy(gzip, out);
                xml = new String(out.toByteArray(), "utf-8");
            } else {
                xml = new String(array, "utf-8");
            }
        } catch (Exception ex) {
            throw new SystemException("Couldn't read data from full object column, reason: " + ex.getMessage(), ex);
        } finally {
            IOUtils.closeQuietly(gzip);
        }

        return xml;
    }
}
