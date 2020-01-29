/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.web.security.util.KeyStoreKey;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.springframework.security.saml.SamlKeyException;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.spi.SamlKeyStoreProvider;
import org.springframework.security.saml.util.X509Utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */

public class MidpointSamlKeyStoreProvider implements SamlKeyStoreProvider {

    @Override
    public KeyStore getKeyStore(SimpleKey key) {
        if (key == null) {
            throw new SamlKeyException(new IllegalArgumentException("Key is null, please define it"));
        }
        if (key instanceof KeyStoreKey) {
            try {
                KeyStore ks = KeyStore.getInstance("JKS");
                FileInputStream is = new FileInputStream(((KeyStoreKey)key).getKeyStorePath());
                ks.load(is, ((KeyStoreKey)key).getKeyStorePassword().toCharArray());
                return ks;
            } catch (KeyStoreException e) {
                throw new SamlKeyException(e);
            } catch (IOException e) {
                throw new SamlKeyException(e);
            } catch (CertificateException e) {
                throw new SamlKeyException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new SamlKeyException(e);
            }
        } else {
            return SamlKeyStoreProvider.super.getKeyStore(key);
        }
    }

    //this is hack for use bcpkix-jdk15on:1.64 library
    private void usedBcpkixDependency() {
        PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) new Object();
    }
}
