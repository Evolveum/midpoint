/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.web.security.util.KeyStoreKey;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.springframework.security.saml.SamlKeyException;
import org.springframework.security.saml.key.SimpleKey;
import org.springframework.security.saml.spi.SamlKeyStoreProvider;
import org.springframework.security.saml.util.X509Utilities;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import static java.util.Optional.ofNullable;
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
            try {
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(null, DEFAULT_KS_PASSWD);

                byte[] certbytes = X509Utilities.getDER(key.getCertificate());
                Certificate certificate = X509Utilities.getCertificate(certbytes);
                ks.setCertificateEntry(key.getName(), certificate);

                if (hasText(key.getPrivateKey())) {
                    PrivateKey pkey;
                    Object obj = null;
                    try {
                        PEMParser parser = new PEMParser(new CharArrayReader(key.getPrivateKey().toCharArray()));
                        obj = parser.readObject();
                        parser.close();
                        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
                        if (obj == null) {
                            throw new SamlKeyException("Unable to decode PEM key:" + key.getPrivateKey());
                        }
                        else if (obj instanceof PEMEncryptedKeyPair) {

                            // Encrypted key - we will use provided password
                            PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) obj;
                            char[] passarray = (ofNullable(key.getPassphrase()).orElse("")).toCharArray();
                            PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(passarray);
                            KeyPair kp = converter.getKeyPair(ckp.decryptKeyPair(decProv));
                            pkey = kp.getPrivate();
                        }
                        else if (obj instanceof PEMKeyPair) {
                            // Unencrypted key - no password needed
                            PEMKeyPair ukp = (PEMKeyPair) obj;
                            KeyPair kp = converter.getKeyPair(ukp);
                            pkey = kp.getPrivate();
                        }
                        else if (obj instanceof PrivateKeyInfo) {
                            // Encrypted key - we will use provided password
                            PrivateKeyInfo pk = (PrivateKeyInfo) obj;
                            pkey = converter.getPrivateKey(pk);
                        }
                        else if (obj instanceof PKCS8EncryptedPrivateKeyInfo) {
                            // Encrypted key - we will use provided password
                            PKCS8EncryptedPrivateKeyInfo cpk = (PKCS8EncryptedPrivateKeyInfo) obj;
                            char[] passarray = (ofNullable(key.getPassphrase()).orElse("")).toCharArray();
                            final InputDecryptorProvider provider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(passarray);
                            pkey = converter.getPrivateKey(cpk.decryptPrivateKeyInfo(provider));
                        }
                        else {
                            throw new SamlKeyException("Unable get private key from " + obj);
                        }
                    } catch (IOException e) {
                        throw new SamlKeyException("Unable get private key", e);
                    } catch (OperatorCreationException | PKCSException e) {
                        throw new SamlKeyException("Unable get private key from " + obj, e);
                    }

                    ks.setKeyEntry(key.getName(), pkey, key.getPassphrase().toCharArray(), new
                            Certificate[]{certificate});
                }

                return ks;
            } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
                throw new SamlKeyException(e);
            } catch (IOException e) {
                throw new SamlKeyException(e);
            }
        }
    }

    //this is hack for use bcpkix-jdk15on:1.64 library
    private void usedBcpkixDependency() {
        PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) new Object();
    }
}
