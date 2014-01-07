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
package com.evolveum.midpoint.common.crypto;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;

/**
 * @author semancik
 *
 */
public class CryptoUtil {

	/**
	 * Encrypts all encryptable values in the object.
	 */
	public static <T extends ObjectType> void encryptValues(final Protector protector, final PrismObject<T> object) throws EncryptionException{
	    Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable){
				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue<?> pval = (PrismPropertyValue<?>)visitable;
				try {
					encryptValue(protector, pval);
				} catch (EncryptionException e) {
					throw new TunnelException(e);
				}
			}
		};
		try {
			object.accept(visitor);
		} catch (TunnelException e) {
			EncryptionException origEx = (EncryptionException)e.getCause();
			throw origEx;
		}
	}

	/**
	 * Encrypts all encryptable values in delta.
	 */
	public static <T extends ObjectType> void encryptValues(final Protector protector, final ObjectDelta<T> delta) throws EncryptionException{
	    Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable){
				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue<?> pval = (PrismPropertyValue<?>)visitable;
				try {
					encryptValue(protector, pval);
				} catch (EncryptionException e) {
					throw new TunnelException(e);
				}
			}
		};
		try {
			delta.accept(visitor);
		} catch (TunnelException e) {
			EncryptionException origEx = (EncryptionException)e.getCause();
			throw origEx;
		}
	}

	private static <T extends ObjectType> void encryptValue(Protector protector, PrismPropertyValue<?> pval) throws EncryptionException{
    	Itemable item = pval.getParent();
    	if (item == null) {
    		return;
    	}
    	ItemDefinition itemDef = item.getDefinition();
    	if (itemDef == null || itemDef.getTypeName() == null) {
    		return;
    	}
    	if (!itemDef.getTypeName().equals(ProtectedStringType.COMPLEX_TYPE)) {
    		return;
    	}
    	QName propName = item.getElementName();
    	PrismPropertyValue<ProtectedStringType> psPval = (PrismPropertyValue<ProtectedStringType>)pval;
    	ProtectedStringType ps = psPval.getValue();
    	if (ps.getClearValue() != null) {
            try {
//                LOGGER.trace("Encrypting cleartext value for field " + propName + " while importing " + object);
                protector.encrypt(ps);
            } catch (EncryptionException e) {
//                LOGGER.error("Faild to encrypt cleartext value for field " + propName + " while importing " + object);
            	// add some context to the exception message
                throw new EncryptionException("Failed to encrypt value for field " + propName + ": " + e.getMessage(), e);
            }
        }
    	
    	if (pval.getParent() == null){
			pval.setParent(item);
		}
    }
	
	// Checks that everything is encrypted
	public static <T extends ObjectType> void checkEncrypted(final PrismObject<T> object) {
	    Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable){
				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue<?> pval = (PrismPropertyValue<?>)visitable;
				checkEncrypted(pval);
			}
		};
		try {
			object.accept(visitor);
		} catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage() + " in " + object, e);
		}

	}
	
	// Checks that everything is encrypted
	public static <T extends ObjectType> void checkEncrypted(final ObjectDelta<T> delta) {
	    Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable){
				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue<?> pval = (PrismPropertyValue<?>)visitable;
				checkEncrypted(pval);
			}
		};
		try {
			delta.accept(visitor);
		} catch (IllegalStateException e) {
			throw new IllegalStateException(e.getMessage() + " in delta " + delta, e);
		}
	}
	
	private static <T extends ObjectType> void checkEncrypted(PrismPropertyValue<?> pval) {
    	Itemable item = pval.getParent();
    	if (item == null) {
    		return;
    	}
    	ItemDefinition itemDef = item.getDefinition();
    	if (itemDef == null || itemDef.getTypeName() == null) {
    		return;
    	}
    	if (!itemDef.getTypeName().equals(ProtectedStringType.COMPLEX_TYPE)) {
    		return;
    	}
    	QName propName = item.getElementName();
    	PrismPropertyValue<ProtectedStringType> psPval = (PrismPropertyValue<ProtectedStringType>)pval;
    	ProtectedStringType ps = psPval.getValue();
    	if (ps.getClearValue() != null) {
    		throw new IllegalStateException("Unencrypted value in field " + propName);
        }
    }

	public static void checkEncrypted(Collection<? extends ItemDelta> modifications) {
		Visitor visitor = new Visitor() {
			@Override
			public void visit(Visitable visitable){
				if (!(visitable instanceof PrismPropertyValue)) {
					return;
				}
				PrismPropertyValue<?> pval = (PrismPropertyValue<?>)visitable;
				checkEncrypted(pval);
			}
		};
		for (ItemDelta<?> delta: modifications) {
			try {
				delta.accept(visitor);
			} catch (IllegalStateException e) {
				throw new IllegalStateException(e.getMessage() + " in modification " + delta, e);
			}

		}
	}

}
