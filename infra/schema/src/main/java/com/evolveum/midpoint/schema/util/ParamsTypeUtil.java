package com.evolveum.midpoint.schema.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.SerializationContext;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParamsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UnknownJavaObjectType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

public class ParamsTypeUtil {

	public static ParamsType toParamsType(Map<String, Serializable> paramsMap){
		Set<Entry<String, Serializable>> params = paramsMap.entrySet();
		if (!params.isEmpty()) {
			ParamsType paramsType = new ParamsType();
			
			for (Entry<String, Serializable> entry : params) {
				paramsType.getEntry().add(createEntryElement(entry.getKey(),entry.getValue()));
			}
			return paramsType;
		}
		return null;
	}
	
	public static ParamsType toParamsType(Map<String, Serializable> paramsMap, PrismContext prismContext) throws SchemaException{
		Set<Entry<String, Serializable>> params = paramsMap.entrySet();
		if (!params.isEmpty()) {
			ParamsType paramsType = new ParamsType();
			
			for (Entry<String, Serializable> entry : params) {
				paramsType.getEntry().add(createEntryElement(entry, prismContext));
			}
			return paramsType;
		}
		return null;
	}

	private static EntryType createEntryElement(Entry<String, Serializable> entry, PrismContext prismContext) throws SchemaException {
		EntryType result = new EntryType();
		result.setKey(entry.getKey());
		if (XmlTypeConverter.canConvert(entry.getValue().getClass())){
			return createEntryElement(entry.getKey(), entry.getValue());
		} else {
			RootXNode xnode = prismContext.xnodeSerializer()
					.options(SerializationOptions.createSerializeReferenceNames())
					.serializeAnyData(entry.getValue(), SchemaConstants.C_PARAM_VALUE);
			return createEntryElement(entry.getKey(), new RawType(xnode, prismContext));
		}
	}

	public static Map<String, Serializable> fromParamsType(ParamsType paramsType, PrismContext prismContext) throws SchemaException{
		if (paramsType != null) {
			Map<String, Serializable> params = new HashMap<String, Serializable>();
			Serializable realValue = null;
			for (EntryType entry : paramsType.getEntry()) {
				if (entry.getEntryValue() != null){
					
					Serializable value = (Serializable) entry.getEntryValue().getValue();
					if (value instanceof RawType){
						XNode xnode = ((RawType) value).getXnode();
						if (xnode instanceof PrimitiveXNode){
							realValue = ((PrimitiveXNode) xnode).getGuessedFormattedValue();
						}
					} else {
						realValue = value;
					}
				}
				params.put(entry.getKey(), (Serializable) (realValue));
			}
			
			return params;
		}
		return null;
	}
	
	public static Map<String, Collection<String>> fromParamsType(ParamsType paramsType) {
		if (paramsType != null) {
			Map<String, Collection<String>> params = new HashMap<>();
			Serializable realValue = null;
			for (EntryType entry : paramsType.getEntry()) {
				if (entry.getEntryValue() != null) {
					
				params.put(entry.getKey(), entry.getEntryValue());
				}
			}
			
			return params;
		}
		return null;
	}
	
	/**
	 * Temporary workaround, brutally hacked -- so that the conversion 
	 * of OperationResult into OperationResultType 'somehow' works, at least to the point
	 * where when we:
	 * - have OR1
	 * - serialize it into ORT1
	 * - then deserialize into OR2
	 * - serialize again into ORT2
	 * so we get ORT1.equals(ORT2) - at least in our simple test case :)
	 * 
	 * FIXME: this should be definitely reworked
	 * 
	 * @param entry
	 * @return
	 */
	private static EntryType createEntryElement(String key, Serializable value) {
		EntryType entryType = new EntryType();
		entryType.setKey(key);
		if (value != null) {
			Document doc = DOMUtil.getDocument();
			if (value instanceof ObjectType && ((ObjectType)value).getOid() != null) {
				// Store only reference on the OID. This is faster and getObject can be used to retrieve
				// the object if needed. Although is does not provide 100% accuracy, it is a good tradeoff.
				setObjectReferenceEntry(entryType, ((ObjectType)value));
			// these values should be put 'as they are', in order to be deserialized into themselves
			} else if (value instanceof String || value instanceof Integer || value instanceof Long) {
				entryType.setEntryValue(new JAXBElement<Serializable>(SchemaConstants.C_PARAM_VALUE, Serializable.class, value));
			} else if (XmlTypeConverter.canConvert(value.getClass())) {
//				try {
//					entryType.setEntryValue(new JXmlTypeConverter.toXsdElement(value, SchemaConstants.C_PARAM_VALUE, doc, true));
//				} catch (SchemaException e) {
//					LOGGER.error("Cannot convert value {} to XML: {}",value,e.getMessage());
//					setUnknownJavaObjectEntry(entryType, value);
//				}
			} else if (value instanceof Element || value instanceof JAXBElement<?>) {
				entryType.setEntryValue((JAXBElement<?>) value);
			// FIXME: this is really bad code ... it means that 'our' JAXB object should be put as is
			} else if ("com.evolveum.midpoint.xml.ns._public.common.common_3".equals(value.getClass().getPackage().getName())) {
				JAXBElement<Object> o = new JAXBElement<Object>(SchemaConstants.C_PARAM_VALUE, Object.class, value);
				entryType.setEntryValue(o);
			} else {
				setUnknownJavaObjectEntry(entryType, value);
			}
		}
		return entryType;
	}

	private static void setObjectReferenceEntry(EntryType entryType, ObjectType objectType) {
		ObjectReferenceType objRefType = new ObjectReferenceType();
		objRefType.setOid(objectType.getOid());
		ObjectTypes type = ObjectTypes.getObjectType(objectType.getClass());
		if (type != null) {
			objRefType.setType(type.getTypeQName());
		}
		JAXBElement<ObjectReferenceType> element = new JAXBElement<ObjectReferenceType>(
				SchemaConstants.C_OBJECT_REF, ObjectReferenceType.class, objRefType);
//		entryType.setAny(element);
	}

	private static void setUnknownJavaObjectEntry(EntryType entryType, Serializable value) {
		UnknownJavaObjectType ujo = new UnknownJavaObjectType();
		ujo.setClazz(value.getClass().getName());
		ujo.setToString(value.toString());
		entryType.setEntryValue(new ObjectFactory().createUnknownJavaObject(ujo));
	}

}
