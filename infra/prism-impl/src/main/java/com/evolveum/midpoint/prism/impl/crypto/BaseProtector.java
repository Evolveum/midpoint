/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.crypto;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.ProtectedData;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 *
 */
public abstract class BaseProtector implements Protector {

    @Override
    public <T> void decrypt(ProtectedData<T> protectedData) throws EncryptionException, SchemaException {
        if (!protectedData.isEncrypted()) {
            return;
            //TODO: is this exception really needed?? isn't it better just return the same protected data??
//            throw new IllegalArgumentException("Attempt to decrypt protected data that are not encrypted");
        } else {
            byte[] decryptedData = decryptBytes(protectedData);
            protectedData.setClearBytes(decryptedData);
            protectedData.setEncryptedData(null);
        }
    }

    protected abstract <T> byte[] decryptBytes(ProtectedData<T> protectedData) throws SchemaException, EncryptionException;

    @Override
    public String decryptString(ProtectedData<String> protectedString) throws EncryptionException {
        try {
            if (!protectedString.isEncrypted()) {
                return protectedString.getClearValue();
            } else {
                byte[] clearBytes = decryptBytes(protectedString);
                return ProtectedStringType.bytesToString(clearBytes);
            }
        } catch (SchemaException ex){
            throw new EncryptionException(ex);
        }
    }

    @Override
    public ProtectedStringType encryptString(String text) throws EncryptionException {
        ProtectedStringType protectedString = new ProtectedStringType();
        protectedString.setClearValue(text);
        encrypt(protectedString);
        return protectedString;
    }

}
