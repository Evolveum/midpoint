/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
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
    	QName propName = item.getName();
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
    	QName propName = item.getName();
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
