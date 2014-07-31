package com.evolveum.midpoint.prism.parser.util;

import java.lang.reflect.Field;
import java.security.CryptoPrimitive;

import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.AESProtector;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Transformer;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.EncryptedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class XNodeProcessorUtil {
	
	public static <T> String findEnumFieldValue(Class classType, Object bean){
        String name = bean.toString();
        for (Field field: classType.getDeclaredFields()) {
            XmlEnumValue xmlEnumValue = field.getAnnotation(XmlEnumValue.class);
            if (xmlEnumValue != null && field.getName().equals(name)) {
                return xmlEnumValue.value();
            }
        }
        return null;
    }
	
	public static <T> void parseProtectedType(ProtectedDataType<T> protectedType, MapXNode xmap, PrismContext prismContext) throws SchemaException {
		XNode xEncryptedData = xmap.get(ProtectedDataType.F_ENCRYPTED_DATA);
        if (xEncryptedData != null) {
            if (!(xEncryptedData instanceof MapXNode)) {
                throw new SchemaException("Cannot parse encryptedData from "+xEncryptedData);
            }
            EncryptedDataType encryptedDataType = prismContext.getBeanConverter().unmarshall((MapXNode)xEncryptedData, EncryptedDataType.class);
            protectedType.setEncryptedData(encryptedDataType);
        } else {
            // Check for legacy EncryptedData
            XNode xLegacyEncryptedData = xmap.get(ProtectedDataType.F_XML_ENC_ENCRYPTED_DATA);
            if (xLegacyEncryptedData != null) {
                if (!(xLegacyEncryptedData instanceof MapXNode)) {
                    throw new SchemaException("Cannot parse EncryptedData from "+xEncryptedData);
                }
                
                 
//                xmlCipherData.getSingleSubEntry(ProtectedDataType.F_XML_ENC_CIPHER_VALUE.getLocalPart());
                
                MapXNode xConvertedEncryptedData = (MapXNode) xLegacyEncryptedData.cloneTransformKeys(new Transformer<QName,QName>() {
                    @Override
                    public QName transform(QName in) {
                        String elementName = StringUtils.uncapitalize(in.getLocalPart());
                        if (elementName.equals("type")) {
                            // this is rubbish, we don't need it, we don't want it
                            return null;
                        }
                        return new QName(null, elementName);
                    }
                });
                
                EncryptedDataType encryptedDataType = prismContext.getBeanConverter().unmarshall(xConvertedEncryptedData, EncryptedDataType.class);
                protectedType.setEncryptedData(encryptedDataType);
       
                if (protectedType instanceof ProtectedStringType){
                	transformEncryptedValue((ProtectedStringType) protectedType, prismContext);
                }
            }
        }
        // protected data empty..check for clear value
        if (protectedType.isEmpty()){
            XNode xClearValue = xmap.get(ProtectedDataType.F_CLEAR_VALUE);
            if (xClearValue == null){
            	//TODO: try to use common namespace (only to be compatible with previous versions)
            	//FIXME maybe add some warning, info...
            	xClearValue = xmap.get(new QName(ProtectedDataType.F_CLEAR_VALUE.getLocalPart()));
            }
            if (xClearValue == null){
            	return;
            }
            if (!(xClearValue instanceof PrimitiveXNode)){
                //this is maybe not good..
                throw new SchemaException("Cannot parse clear value from " + xClearValue);
            }
            // TODO: clearValue
            T clearValue = (T) ((PrimitiveXNode)xClearValue).getParsedValue(DOMUtil.XSD_STRING);
            protectedType.setClearValue(clearValue);
        }

    }
	
	private static void transformEncryptedValue(ProtectedDataType protectedType, PrismContext prismContext) throws SchemaException{
		Protector protector = prismContext.getDefaultProtector();
		if (protector == null){
			return;
		}
//		AESProtector protector = new AESProtector();
//		protector.init();
        try {
        	protector.decrypt(protectedType);
        	Object clearValue = protectedType.getClearValue();
        	if (clearValue instanceof String){
        		String clear = (String) clearValue;
        		if (clear.startsWith("<value>") && clear.endsWith("</value>")){
        			clear = clear.replace("<value>","").replace("</value>", "");
        			clearValue = (String) clear;
        		}
        		protectedType.setClearValue(clearValue);
        		protector.encrypt(protectedType);
        	}
        } catch (EncryptionException ex){
        	//System.out.println("failed to encrypt..");
        	throw new IllegalArgumentException("failed to encrypt. " + ex);
        }
	}

}
