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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.codec.digest.DigestUtils;
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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.ValueSerializationUtil;
import com.evolveum.midpoint.repo.sql.data.audit.RObjectDeltaOperation;
import com.evolveum.midpoint.repo.sql.data.common.OperationResult;
import com.evolveum.midpoint.repo.sql.data.common.RAnyContainer;
import com.evolveum.midpoint.repo.sql.data.common.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.RAuthorization;
import com.evolveum.midpoint.repo.sql.data.common.RContainer;
import com.evolveum.midpoint.repo.sql.data.common.RExclusion;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.RShadow;
import com.evolveum.midpoint.repo.sql.data.common.RSynchronizationSituationDescription;
import com.evolveum.midpoint.repo.sql.data.common.RTrigger;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyClob;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyDate;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyLong;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyPolyString;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyReference;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.data.common.other.RContainerType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LocalizedMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ParamsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationDescriptionType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

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

	/**
	 * This constant is used for {@link QName#localPart} column size in
	 * database.
	 */
	public static final int COLUMN_LENGTH_LOCALPART = 100;

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

	public static <T> T toJAXB(Class parentClass, QName path, String value, Class<T> clazz, PrismContext prismContext) throws SchemaException, JAXBException {
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
		SchemaRegistry registry = prismContext.getSchemaRegistry();
		 
			PrismContainerDefinition cDefinition = registry.findContainerDefinitionByCompileTimeClass(parentClass);
			ItemDefinition definition = null;
			if (cDefinition != null){
				definition = cDefinition.findItemDefinition(path);
			} else {
				definition = registry.findPropertyDefinitionByElementName(SchemaConstantsGenerated.C_OPERATION_RESULT);
			}
			

		if (definition == null) {
			definition = prismContext.getSchemaRegistry().findItemDefinitionByElementName(path);
//			System.out.println("definition: " + definition);
		}

		T jaxbValue = ValueSerializationUtil.deserializeValue(value, parentClass, path, definition,
				prismContext, PrismContext.LANG_XML);

		return jaxbValue;
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

		return ValueSerializationUtil.serializeValue(value, definition, parentDefinition.getName(), prismContext, PrismContext.LANG_XML);
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

	public static List<PolyStringType> safeSetPolyToList(Set<RPolyString> set) {
		if (set == null || set.isEmpty()) {
			return new ArrayList<PolyStringType>();
		}

		List<PolyStringType> list = new ArrayList<PolyStringType>();
		for (RPolyString str : set) {
			list.add(RPolyString.copyToJAXB(str));
		}
		return list;
	}

	public static Set<RSynchronizationSituationDescription> listSyncSituationToSet(RShadow owner,
			List<SynchronizationSituationDescriptionType> list) {
		Set<RSynchronizationSituationDescription> set = new HashSet<RSynchronizationSituationDescription>();
		if (list != null) {
			for (SynchronizationSituationDescriptionType str : list) {
				if (str == null) {
					continue;
				}
				set.add(RSynchronizationSituationDescription.copyFromJAXB(owner, str));
			}
		}

		return set;
	}

	public static List<SynchronizationSituationDescriptionType> safeSetSyncSituationToList(
			Set<RSynchronizationSituationDescription> set) {
		List<SynchronizationSituationDescriptionType> list = new ArrayList<SynchronizationSituationDescriptionType>();
		for (RSynchronizationSituationDescription str : set) {
			if (str == null) {
				continue;
			}
			list.add(RSynchronizationSituationDescription.copyToJAXB(str));
		}
		return list;
	}

	public static <T> List<T> safeSetToList(Set<T> set) {
		if (set == null || set.isEmpty()) {
			return new ArrayList<T>();
		}

		List<T> list = new ArrayList<T>();
		list.addAll(set);

		return list;
	}

	public static List<ObjectReferenceType> safeSetReferencesToList(Set<RObjectReference> set,
			PrismContext prismContext) {
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

	public static Set<RObjectReference> safeListReferenceToSet(List<ObjectReferenceType> list,
			PrismContext prismContext, RContainer owner, RReferenceOwner refOwner) {
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
			RContainer owner, RReferenceOwner refOwner) {
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

	public static REmbeddedReference jaxbRefToEmbeddedRepoRef(ObjectReferenceType jaxb,
			PrismContext prismContext) {
		if (jaxb == null) {
			return null;
		}
		REmbeddedReference ref = new REmbeddedReference();
		REmbeddedReference.copyFromJAXB(jaxb, ref, prismContext);

		return ref;
	}

	public static Long getLongFromString(String val) {
		if (val == null || !val.matches("[0-9]+")) {
			return null;
		}

		return Long.parseLong(val);
	}

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
		fixCompositeIdentifierInMetaModel(sessionFactory, RSynchronizationSituationDescription.class);

		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyContainer.class);
		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyClob.class);
		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyDate.class);
		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyString.class);
		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyPolyString.class);
		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyReference.class);
		fixCompositeIdentifierInMetaModel(sessionFactory, RAnyLong.class);

		fixCompositeIdentifierInMetaModel(sessionFactory, RObjectReference.class);
		for (RReferenceOwner owner : RReferenceOwner.values()) {
			fixCompositeIdentifierInMetaModel(sessionFactory, owner.getClazz());
		}

		fixCompositeIdentifierInMetaModel(sessionFactory, RContainer.class);
		fixCompositeIdentifierInMetaModel(sessionFactory, RAssignment.class);
		fixCompositeIdentifierInMetaModel(sessionFactory, RAuthorization.class);
		fixCompositeIdentifierInMetaModel(sessionFactory, RExclusion.class);
		fixCompositeIdentifierInMetaModel(sessionFactory, RTrigger.class);
		for (RContainerType type : ClassMapper.getKnownTypes()) {
			fixCompositeIdentifierInMetaModel(sessionFactory, type.getClazz());
		}
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

	public static void copyResultToJAXB(OperationResult repo, OperationResultType jaxb,
			PrismContext prismContext) throws DtoTranslationException {
		Validate.notNull(jaxb, "JAXB object must not be null.");
		Validate.notNull(repo, "Repo object must not be null.");

		jaxb.setDetails(repo.getDetails());
		jaxb.setMessage(repo.getMessage());
		jaxb.setMessageCode(repo.getMessageCode());
		jaxb.setOperation(repo.getOperation());
		if (repo.getStatus() != null) {
			jaxb.setStatus(repo.getStatus().getSchemaValue());
		}
		jaxb.setToken(repo.getToken());

		try {
			jaxb.setLocalizedMessage(RUtil.toJAXB(OperationResultType.class,
					OperationResultType.F_LOCALIZED_MESSAGE, repo.getLocalizedMessage(),
					LocalizedMessageType.class, prismContext));
			jaxb.setParams(RUtil.toJAXB(OperationResultType.class,
					OperationResultType.F_PARAMS, repo.getParams(), ParamsType.class,
					prismContext));

			jaxb.setContext(RUtil.toJAXB(OperationResultType.class, 
					OperationResultType.F_CONTEXT, repo.getContext(), ParamsType.class, prismContext));

			jaxb.setReturns(RUtil.toJAXB(OperationResultType.class, 
					OperationResultType.F_RETURNS, repo.getReturns(), ParamsType.class, prismContext));

			if (StringUtils.isNotEmpty(repo.getPartialResults())) {
				List result = (List) RUtil.toJAXB(OperationResultType.class, OperationResultType.F_PARTIAL_RESULTS, repo.getPartialResults(), OperationResultType.class, prismContext);
				jaxb.getPartialResults().addAll(result);
			}
		} catch (Exception ex) {
			throw new DtoTranslationException(ex.getMessage(), ex);
		}
	}

	public static void copyResultFromJAXB(OperationResultType jaxb, OperationResult repo,
			PrismContext prismContext) throws DtoTranslationException {
		Validate.notNull(jaxb, "JAXB object must not be null.");
		Validate.notNull(repo, "Repo object must not be null.");

		repo.setDetails(jaxb.getDetails());
		repo.setMessage(jaxb.getMessage());
		repo.setMessageCode(jaxb.getMessageCode());
		repo.setOperation(jaxb.getOperation());
		repo.setStatus(getRepoEnumValue(jaxb.getStatus(), ROperationResultStatus.class));
		repo.setToken(jaxb.getToken());
		PrismPropertyDefinition def = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstantsGenerated.C_OPERATION_RESULT);
		
		
		
		try {
//			String s = ValueSerializationUtil.serializeValue(jaxb, def, OperationResultType.F_PARTIAL_RESULTS, prismContext, PrismContext.LANG_XML);
//			System.out.println("serialized-===========: " + s);
			repo.setLocalizedMessage(RUtil.toRepo(def, OperationResultType.F_LOCALIZED_MESSAGE, jaxb.getLocalizedMessage(), prismContext));
			repo.setParams(RUtil.toRepo(def, OperationResultType.F_PARAMS, jaxb.getParams(), prismContext));
			repo.setContext(RUtil.toRepo(def, OperationResultType.F_CONTEXT, jaxb.getContext(), prismContext));
			repo.setReturns(RUtil.toRepo(def, OperationResultType.F_RETURNS, jaxb.getReturns(), prismContext));

			if (!jaxb.getPartialResults().isEmpty()) {
				OperationResultType result = new OperationResultType();
				result.getPartialResults().addAll(jaxb.getPartialResults());
				repo.setPartialResults(RUtil.toRepo(def, OperationResultType.F_PARTIAL_RESULTS, jaxb.getPartialResults(), prismContext));
			}
		} catch (Exception ex) {
			throw new DtoTranslationException(ex.getMessage(), ex);
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

	/**
	 * This method creates full {@link ItemPath} from
	 * {@link com.evolveum.midpoint.prism.query.ValueFilter} created from main
	 * item path and last element, which is now definition.
	 * <p/>
	 * Will be deleted after query api update
	 * 
	 * @param filter
	 * @return
	 */
	@Deprecated
	public static ItemPath createFullPath(ValueFilter filter) {
		ItemDefinition def = filter.getDefinition();
		ItemPath parentPath = filter.getParentPath();

		List<ItemPathSegment> segments = new ArrayList<ItemPathSegment>();
		if (parentPath != null) {
			for (ItemPathSegment segment : parentPath.getSegments()) {
				if (!(segment instanceof NameItemPathSegment)) {
					continue;
				}

				NameItemPathSegment named = (NameItemPathSegment) segment;
				segments.add(new NameItemPathSegment(named.getName()));
			}
		}
		segments.add(new NameItemPathSegment(def.getName()));

		return new ItemPath(segments);
	}
}
