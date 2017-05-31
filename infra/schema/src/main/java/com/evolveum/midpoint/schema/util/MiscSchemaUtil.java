/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.soap.Detail;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.marshaller.BeanMarshaller;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.*;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
public class MiscSchemaUtil {
	
	private static final Trace LOGGER = TraceManager.getTrace(MiscSchemaUtil.class);
	private static final Random RND = new Random();
	
	public static ObjectListType toObjectListType(List<PrismObject<? extends ObjectType>> list) {
		ObjectListType listType = new ObjectListType();
		for (PrismObject<? extends ObjectType> o : list) {
			listType.getObject().add(o.asObjectable());
		}
		return listType;
	}
	
	public static <T extends ObjectType> List<PrismObject<T>> toList(Class<T> type, ObjectListType listType) {
		List<PrismObject<T>> list = new ArrayList<>();
		for (ObjectType o : listType.getObject()) {
			list.add((PrismObject<T>) o.asPrismObject());
		}
		return list;
	}
	
	public static <T extends ObjectType> List<T> toObjectableList(List<PrismObject<T>> objectList) {
		if (objectList == null) {
			return null;
		}
		List<T> objectableList = new ArrayList<T>(objectList.size());
		for (PrismObject<T> object: objectList) {
			objectableList.add(object.asObjectable());
		}
		return objectableList;
	}
	
	public static ImportOptionsType getDefaultImportOptions() {
		ImportOptionsType options = new ImportOptionsType();
		options.setOverwrite(false);
		options.setValidateStaticSchema(false);
		options.setValidateDynamicSchema(false);
		options.setEncryptProtectedValues(true);
		options.setFetchResourceSchema(false);
		options.setSummarizeErrors(true);
		options.setSummarizeSucceses(true);
		return options;
	}

	public static CachingMetadataType generateCachingMetadata() {
		CachingMetadataType cmd = new CachingMetadataType();
		XMLGregorianCalendar xmlGregorianCalendarNow = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
		cmd.setRetrievalTimestamp(xmlGregorianCalendarNow);
		cmd.setSerialNumber(generateSerialNumber());
		return cmd;
	}

	private static String generateSerialNumber() {
		return Long.toHexString(RND.nextLong())+"-"+Long.toHexString(RND.nextLong());
	}

	public static boolean isNullOrEmpty(ProtectedStringType ps) {
		return (ps == null || ps.isEmpty());
	}

	public static void setPassword(CredentialsType credentials, ProtectedStringType password) {
		PasswordType credPass = credentials.getPassword();
		if (credPass == null) {
			credPass = new PasswordType();
			credentials.setPassword(credPass);
		}
		credPass.setValue(password);
	}

	public static Collection<String> toCollection(String entry) {
		List<String> list = new ArrayList<String>(1);
		list.add(entry);
		return list;
	}

	public static Collection<ItemPath> itemReferenceListTypeToItemPathList(PropertyReferenceListType resolve) {
		Collection<ItemPath> itemPathList = new ArrayList<ItemPath>(resolve.getProperty().size());
		for (ItemPathType itemXPathElement: resolve.getProperty()) {
			itemPathList.add(itemXPathElement.getItemPath());
		}
		return itemPathList;
	}
	
	public static SelectorQualifiedGetOptionsType optionsToOptionsType(Collection<SelectorOptions<GetOperationOptions>> options){
		SelectorQualifiedGetOptionsType optionsType = new SelectorQualifiedGetOptionsType();
		List<SelectorQualifiedGetOptionType> retval = new ArrayList<>();
		for (SelectorOptions<GetOperationOptions> option: options){
			retval.add(selectorOptionToSelectorQualifiedGetOptionType(option));
		}
		optionsType.getOption().addAll(retval);
		return optionsType;
	}
	
	private static SelectorQualifiedGetOptionType selectorOptionToSelectorQualifiedGetOptionType(SelectorOptions<GetOperationOptions> selectorOption){
		OptionObjectSelectorType selectorType = selectorToSelectorType(selectorOption.getSelector());
		GetOperationOptionsType getOptionsType = getOptionsToGetOptionsType(selectorOption.getOptions());
		SelectorQualifiedGetOptionType selectorOptionType = new SelectorQualifiedGetOptionType();
		selectorOptionType.setOptions(getOptionsType);
		selectorOptionType.setSelector(selectorType);
		return selectorOptionType;
	}

	 private static OptionObjectSelectorType selectorToSelectorType(ObjectSelector selector) {
			if (selector == null) {
				return null;
			}
			OptionObjectSelectorType selectorType = new OptionObjectSelectorType();
			selectorType.setPath(new ItemPathType(selector.getPath()));
			return selectorType;
		}
	 
	 private static GetOperationOptionsType getOptionsToGetOptionsType(GetOperationOptions options) {
		 GetOperationOptionsType optionsType = new GetOperationOptionsType();
		 optionsType.setRetrieve(RetrieveOption.toRetrieveOptionType(options.getRetrieve()));
		 optionsType.setResolve(options.getResolve());
		 optionsType.setResolveNames(options.getResolveNames());
		 optionsType.setNoFetch(options.getNoFetch());
		 optionsType.setRaw(options.getRaw());
		 optionsType.setTolerateRawData(options.getTolerateRawData());
		 optionsType.setNoDiscovery(options.getDoNotDiscovery());
		 // TODO relational value search query (but it might become obsolete)
		 optionsType.setAllowNotFound(options.getAllowNotFound());
		 optionsType.setPointInTimeType(PointInTimeType.toPointInTimeTypeType(options.getPointInTimeType()));
		 optionsType.setStaleness(options.getStaleness());
		 optionsType.setDistinct(options.getDistinct());
		 return optionsType;
	 }

	 public static List<SelectorOptions<GetOperationOptions>> optionsTypeToOptions(SelectorQualifiedGetOptionsType objectOptionsType) {
		if (objectOptionsType == null) {
			return null;
		}
		List<SelectorOptions<GetOperationOptions>> retval = new ArrayList<>();
		for (SelectorQualifiedGetOptionType optionType : objectOptionsType.getOption()) {
			retval.add(selectorQualifiedGetOptionTypeToSelectorOption(optionType));
		}
		return retval;
	}

	private static SelectorOptions<GetOperationOptions> selectorQualifiedGetOptionTypeToSelectorOption(SelectorQualifiedGetOptionType objectOptionsType) {
		ObjectSelector selector = selectorTypeToSelector(objectOptionsType.getSelector());
		GetOperationOptions options = getOptionsTypeToGetOptions(objectOptionsType.getOptions());
		return new SelectorOptions<>(selector, options);
	}

	private static GetOperationOptions getOptionsTypeToGetOptions(GetOperationOptionsType optionsType) {
		GetOperationOptions options = new GetOperationOptions();
        options.setRetrieve(RetrieveOption.fromRetrieveOptionType(optionsType.getRetrieve()));
        options.setResolve(optionsType.isResolve());
        options.setResolveNames(optionsType.isResolveNames());
        options.setNoFetch(optionsType.isNoFetch());
        options.setRaw(optionsType.isRaw());
		options.setTolerateRawData(optionsType.isTolerateRawData());
		options.setDoNotDiscovery(optionsType.isNoDiscovery());
		// TODO relational value search query (but it might become obsolete)
		options.setAllowNotFound(optionsType.isAllowNotFound());
		options.setPointInTimeType(PointInTimeType.toPointInTimeType(optionsType.getPointInTimeType()));
		options.setStaleness(optionsType.getStaleness());
		options.setDistinct(optionsType.isDistinct());
		return options;
	}

    private static ObjectSelector selectorTypeToSelector(OptionObjectSelectorType selectorType) {
		if (selectorType == null) {
			return null;
		}
		return new ObjectSelector(selectorType.getPath().getItemPath());
	}
	
    /**
     * Convenience method that helps avoid some compiler warnings.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Collection<ObjectDelta<? extends ObjectType>> createCollection(ObjectDelta<?>... deltas) {
    	return (Collection)MiscUtil.createCollection(deltas);
    }
    
    /**
     * Convenience method that helps avoid some compiler warnings.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Collection<? extends ItemDelta<?,?>> createCollection(ItemDelta<?,?>... deltas) {
    	return (Collection)MiscUtil.createCollection(deltas);
    }
    
	public static Collection<ObjectDelta<? extends ObjectType>> cloneObjectDeltaCollection(
			Collection<ObjectDelta<? extends ObjectType>> origCollection) {
		if (origCollection == null) {
			return null;
		}
		Collection<ObjectDelta<? extends ObjectType>> clonedCollection = new ArrayList<ObjectDelta<? extends ObjectType>>(origCollection.size());
		for (ObjectDelta<? extends ObjectType> origDelta: origCollection) {
			clonedCollection.add(origDelta.clone());
		}
		return clonedCollection;
	}
	
	public static Collection<ObjectDeltaOperation<? extends ObjectType>> cloneObjectDeltaOperationCollection(
			Collection<ObjectDeltaOperation<? extends ObjectType>> origCollection) {
		if (origCollection == null) {
			return null;
		}
		Collection<ObjectDeltaOperation<? extends ObjectType>> clonedCollection = new ArrayList<ObjectDeltaOperation<? extends ObjectType>>(origCollection.size());
		for (ObjectDeltaOperation<? extends ObjectType> origDelta: origCollection) {
			clonedCollection.add(origDelta.clone());
		}
		return clonedCollection;
	}

    public static ObjectReferenceType createObjectReference(String oid, QName type) {
    	ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(oid);
        ref.setType(type);
        return ref;
    }
    
    public static boolean equalsIntent(String intent1, String intent2) {
		if (intent1 == null) {
			intent1 = SchemaConstants.INTENT_DEFAULT;
		}
		if (intent2 == null) {
			intent2 = SchemaConstants.INTENT_DEFAULT;
		}
		return intent1.equals(intent2);
	}
    
	public static boolean matchesKind(ShadowKindType expectedKind, ShadowKindType actualKind) {
		if (expectedKind == null) {
			return true;
		}
		return expectedKind.equals(actualKind);
	}


	public static AssignmentPolicyEnforcementType getAssignmentPolicyEnforcementType(
			ProjectionPolicyType accountSynchronizationSettings) {
		if (accountSynchronizationSettings == null) {
			// default
			return AssignmentPolicyEnforcementType.RELATIVE;
		}
		AssignmentPolicyEnforcementType assignmentPolicyEnforcement = accountSynchronizationSettings.getAssignmentPolicyEnforcement();
		if (assignmentPolicyEnforcement == null) {
			return AssignmentPolicyEnforcementType.RELATIVE;
		}
		return assignmentPolicyEnforcement;
	}

	public static boolean compareRelation(QName query, QName refRelation) {
    	return ObjectTypeUtil.relationMatches(query, refRelation);
	}

	public static PrismReferenceValue objectReferenceTypeToReferenceValue(ObjectReferenceType refType) {
		if (refType == null) {
			return null;
		}
		PrismReferenceValue rval = new PrismReferenceValue();
		rval.setOid(refType.getOid());
		rval.setDescription(refType.getDescription());
		rval.setFilter(refType.getFilter());
		rval.setRelation(refType.getRelation());
		rval.setTargetType(refType.getType());
		return rval;
	}
	
	public static PropertyLimitationsType getLimitationsType(List<PropertyLimitationsType> limitationsTypes, LayerType layer) throws SchemaException {
		if (limitationsTypes == null) {
			return null;
		}
		PropertyLimitationsType found = null;
		for (PropertyLimitationsType limitType: limitationsTypes) {
			if (contains(limitType.getLayer(),layer)) {
				if (found == null) {
					found = limitType;
				} else {
					throw new SchemaException("Duplicate definition of limitations for layer '"+layer+"'");
				}
			}
		}
		return found;
	}
	
	private static boolean contains(List<LayerType> layers, LayerType layer) {
		if (layers == null || layers.isEmpty()) {
			if (layer == null) {
				return true;
			} else {
				return false;
			}
		}
		return layers.contains(layer);
	}

	// Some searches may return duplicate objects. This is an utility method to remove the duplicates.
	public static <O extends ObjectType> void reduceSearchResult(List<PrismObject<O>> results) {
		if (results == null || results.isEmpty()) {
			return;
		}
		Map<String,PrismObject<O>> map = new HashMap<>();
		Iterator<PrismObject<O>> iterator = results.iterator();
		while (iterator.hasNext()) {
			PrismObject<O> prismObject = iterator.next();
			if (map.containsKey(prismObject.getOid())) {
				iterator.remove();
			} else {
				map.put(prismObject.getOid(), prismObject);
			}
		}
	}

	/**
	 * Returns modification time or creation time (if there was no mo 
	 */
	public static XMLGregorianCalendar getChangeTimestamp(MetadataType metadata) {
		if (metadata == null) {
			return null;
		}
		XMLGregorianCalendar modifyTimestamp = metadata.getModifyTimestamp();
		if (modifyTimestamp != null) {
			return modifyTimestamp;
		} else {
			return metadata.getCreateTimestamp();
		}
	}

	// TODO some better place
	public static void serializeFaultMessage(Detail detail, FaultMessage faultMessage, PrismContext prismContext, Trace logger) {
        try {
			BeanMarshaller marshaller = ((PrismContextImpl) prismContext).getBeanMarshaller();
			XNode faultMessageXnode = marshaller.marshall(faultMessage.getFaultInfo());			// TODO
            RootXNode xroot = new RootXNode(SchemaConstants.FAULT_MESSAGE_ELEMENT_NAME, faultMessageXnode);
            xroot.setExplicitTypeDeclaration(true);
            QName faultType = prismContext.getSchemaRegistry().determineTypeForClass(faultMessage.getFaultInfo().getClass());
            xroot.setTypeQName(faultType);
			((PrismContextImpl) prismContext).getParserDom().serializeUnderElement(xroot, SchemaConstants.FAULT_MESSAGE_ELEMENT_NAME, detail);
        } catch (SchemaException e) {
            logger.error("Error serializing fault message (SOAP fault detail): {}", e.getMessage(), e);
        }
	}

	public static boolean referenceMatches(ObjectReferenceType refPattern, ObjectReferenceType ref) {
		if (refPattern.getOid() != null && !refPattern.getOid().equals(ref.getOid())) {
			return false;
		}
		if (refPattern.getType() != null && !QNameUtil.match(refPattern.getType(), ref.getType())) {
			return false;
		}
		if (!ObjectTypeUtil.relationMatches(refPattern.getRelation(), ref.getRelation())) {
			return false;
		}
		return true;
	}
	
	/**
	 * Make quick and reasonably reliable comparison. E.g. compare prism objects only by
	 * comparing OIDs. This is ideal for cases when the compare is called often and the
	 * objects are unlikely to change (e.g. user interface selectable beans).
	 */
	@SuppressWarnings("rawtypes")
	public static boolean quickEquals(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a instanceof PrismObject) {
			if (b instanceof PrismObject) {
				// In case both values are objects then compare only OIDs.
				// that should be enough. Comparing complete objects may be slow
				// (e.g. if the objects have many assignments)
				String aOid = ((PrismObject)a).getOid();
				String bOid = ((PrismObject)b).getOid();
				if (aOid != null && bOid != null) {
					return aOid.equals(bOid);
				}
			} else {
				return false;
			}
		}
		if (a instanceof ObjectType) {
			if (b instanceof ObjectType) {
				// In case both values are objects then compare only OIDs.
				// that should be enough. Comparing complete objects may be slow
				// (e.g. if the objects have many assignments)
				String aOid = ((ObjectType)a).getOid();
				String bOid = ((ObjectType)b).getOid();
				if (aOid != null && bOid != null) {
					return aOid.equals(bOid);
				}
			} else {
				return false;
			}
		}
		return a.equals(b);
	}
}
