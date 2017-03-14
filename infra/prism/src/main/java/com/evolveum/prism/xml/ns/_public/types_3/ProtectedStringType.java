/**
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
package com.evolveum.prism.xml.ns._public.types_3;

import java.io.UnsupportedEncodingException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.SystemException;


/**
 * This class was originally generated. But it was heavily modified by hand.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProtectedStringType")
public class ProtectedStringType extends ProtectedDataType<String> implements Cloneable {

	public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "ProtectedStringType");
	
	private static final String CHARSET = "UTF-8";
	
	public ProtectedStringType() {
		content = new ContentList();
	}
	
	@Override
	public byte[] getClearBytes() {
		String clearValue = getClearValue();
		try {
			// We want fixed charset here, independent of locale. We want consistent and portable encryption/decryption.
			return clearValue.getBytes(CHARSET);
		} catch (UnsupportedEncodingException e) {
			throw new SystemException("Unsupported charset '"+CHARSET+"', is this system from 19th century?", e);
		}
	}

	@Override
	public void setClearBytes(byte[] bytes) {
        if (bytes != null) {
            setClearValue(bytesToString(bytes));
        }
	}

	@Override
	public boolean canSupportType(Class<?> type) {
		return String.class.isAssignableFrom(type);
	}

	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}

    @Override
    public ProtectedStringType clone() {
        ProtectedStringType cloned = new ProtectedStringType();
        cloneTo(cloned);
        return cloned;
    }

    public static String bytesToString(byte[] clearBytes) {
        try {
            // We want fixed charset here, independent of locale. We want consistent and portable encryption/decryption.
            return new String(clearBytes, CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new SystemException("Unsupported charset '"+CHARSET+"', is this system from 19th century?", e);
        }
    }

}
