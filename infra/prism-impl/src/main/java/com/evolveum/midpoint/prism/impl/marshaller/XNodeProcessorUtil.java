/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.prism.impl.marshaller;

import java.lang.reflect.Field;

import javax.xml.bind.annotation.XmlValue;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.EncryptedDataType;
import com.evolveum.prism.xml.ns._public.types_3.HashedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class XNodeProcessorUtil {

	public static <T> void parseProtectedType(ProtectedDataType<T> protectedType, MapXNodeImpl xmap, PrismContext prismContext, ParsingContext pc) throws SchemaException {
		RootXNodeImpl xEncryptedData = xmap.getEntryAsRoot(ProtectedDataType.F_ENCRYPTED_DATA);
        if (xEncryptedData != null) {
            if (!(xEncryptedData.getSubnode() instanceof MapXNodeImpl)) {
                throw new SchemaException("Cannot parse encryptedData from "+xEncryptedData);
            }
			EncryptedDataType encryptedDataType = prismContext.parserFor(xEncryptedData).context(pc).parseRealValue(EncryptedDataType.class);
            protectedType.setEncryptedData(encryptedDataType);
        } else {
            // Check for legacy EncryptedData
            RootXNodeImpl xLegacyEncryptedData = xmap.getEntryAsRoot(ProtectedDataType.F_XML_ENC_ENCRYPTED_DATA);
            if (xLegacyEncryptedData != null) {
                if (!(xLegacyEncryptedData.getSubnode() instanceof MapXNodeImpl)) {
                    throw new SchemaException("Cannot parse EncryptedData from "+xEncryptedData);
                }
                RootXNodeImpl xConvertedEncryptedData = (RootXNodeImpl) xLegacyEncryptedData.cloneTransformKeys(in -> {
					String elementName = StringUtils.uncapitalize(in.getLocalPart());
					if (elementName.equals("type")) {
						// this is rubbish, we don't need it, we don't want it
						return null;
					}
					return new QName(null, elementName);
				});

                EncryptedDataType encryptedDataType = prismContext.parserFor(xConvertedEncryptedData).context(pc).parseRealValue(EncryptedDataType.class);
                protectedType.setEncryptedData(encryptedDataType);

                if (protectedType instanceof ProtectedStringType){
                	transformEncryptedValue(protectedType, prismContext);
                }
            }
        }
        RootXNodeImpl xHashedData = xmap.getEntryAsRoot(ProtectedDataType.F_HASHED_DATA);
        if (xHashedData != null) {
            if (!(xHashedData.getSubnode() instanceof MapXNodeImpl)) {
                throw new SchemaException("Cannot parse hashedData from "+xHashedData);
            }
            HashedDataType hashedDataType = prismContext.parserFor(xHashedData).context(pc).parseRealValue(HashedDataType.class);
            protectedType.setHashedData(hashedDataType);
        }
        // protected data empty..check for clear value
        if (protectedType.isEmpty()){
            XNodeImpl xClearValue = xmap.get(ProtectedDataType.F_CLEAR_VALUE);
            if (xClearValue == null){
            	//TODO: try to use common namespace (only to be compatible with previous versions)
            	//FIXME maybe add some warning, info...
            	xClearValue = xmap.get(new QName(ProtectedDataType.F_CLEAR_VALUE.getLocalPart()));
            }
            if (xClearValue == null){
            	return;
            }
            if (!(xClearValue instanceof PrimitiveXNodeImpl)){
                //this is maybe not good..
                throw new SchemaException("Cannot parse clear value from " + xClearValue);
            }
            // TODO: clearValue
            T clearValue = (T) ((PrimitiveXNodeImpl)xClearValue).getParsedValue(DOMUtil.XSD_STRING, String.class);
            protectedType.setClearValue(clearValue);
        }

    }

	private static void transformEncryptedValue(ProtectedDataType protectedType, PrismContext prismContext) throws SchemaException{
		Protector protector = prismContext.getDefaultProtector();
		if (protector == null) {
			return;
		}
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

	public static <T> Field findXmlValueField(Class<T> beanClass) {
		for (Field field: beanClass.getDeclaredFields()) {
			XmlValue xmlValue = field.getAnnotation(XmlValue.class);
			if (xmlValue != null) {
				return field;
			}
		}
		return null;
	}
}
