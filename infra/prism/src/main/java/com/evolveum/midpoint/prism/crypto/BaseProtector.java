/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.crypto;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.Validate;

/**
 * @author mederly
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

    @Override
    public boolean isEncrypted(ProtectedStringType ps) {
        Validate.notNull(ps, "Protected string must not be null.");
        return ps.isEncrypted();
    }
    
}
