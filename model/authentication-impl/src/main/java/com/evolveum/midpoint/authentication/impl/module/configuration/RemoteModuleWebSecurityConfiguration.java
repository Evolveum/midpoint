/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.Saml2X509Credential;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */

public class RemoteModuleWebSecurityConfiguration extends ModuleWebSecurityConfigurationImpl {

    private static final Trace LOGGER = TraceManager.getTrace(RemoteModuleWebSecurityConfiguration.class);

    protected static Certificate getCertificate(AbstractSimpleKeyType key, Protector protector)
            throws EncryptionException, CertificateException {
        if (key == null || key.getCertificate() == null) {
            return null;
        }
        byte[] certbytes = protector.decryptString(key.getCertificate()).getBytes();
        return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certbytes));
    }

    protected static Certificate getCertificate(AbstractKeyStoreKeyType key, Protector protector)
            throws EncryptionException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        if (key == null) {
            return null;
        }
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream is = new FileInputStream(key.getKeyStorePath());
        ks.load(is, protector.decryptString(key.getKeyStorePassword()).toCharArray());

        Certificate certificate = ks.getCertificate(key.getKeyAlias());
        return certificate;
    }

    protected static PrivateKey getPrivateKey(AbstractSimpleKeyType key, Protector protector)
            throws EncryptionException, IOException, PKCSException, OperatorCreationException {
        if (key == null) {
            return null;
        }
        PrivateKey pkey = null;

        String stringPrivateKey = protector.decryptString(key.getPrivateKey());
        String stringPassphrase = protector.decryptString(key.getPassphrase());
        if (hasText(stringPrivateKey)) {
            Object obj;
            PEMParser parser = new PEMParser(new CharArrayReader(stringPrivateKey.toCharArray()));
            obj = parser.readObject();
            parser.close();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            if (obj == null) {
                throw new EncryptionException("Unable to decode PEM key:" + key.getPrivateKey());
            } else if (obj instanceof PEMEncryptedKeyPair) {

                // Encrypted key - we will use provided password
                PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) obj;
                char[] passarray = (ofNullable(stringPassphrase).orElse("")).toCharArray();
                PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(passarray);
                KeyPair kp = converter.getKeyPair(ckp.decryptKeyPair(decProv));
                pkey = kp.getPrivate();
            } else if (obj instanceof PEMKeyPair) {
                // Unencrypted key - no password needed
                PEMKeyPair ukp = (PEMKeyPair) obj;
                KeyPair kp = converter.getKeyPair(ukp);
                pkey = kp.getPrivate();
            } else if (obj instanceof PrivateKeyInfo) {
                // Encrypted key - we will use provided password
                PrivateKeyInfo pk = (PrivateKeyInfo) obj;
                pkey = converter.getPrivateKey(pk);
            } else if (obj instanceof PKCS8EncryptedPrivateKeyInfo) {
                // Encrypted key - we will use provided password
                PKCS8EncryptedPrivateKeyInfo cpk = (PKCS8EncryptedPrivateKeyInfo) obj;
                char[] passarray = (ofNullable(stringPassphrase).orElse("")).toCharArray();
                final InputDecryptorProvider provider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(passarray);
                pkey = converter.getPrivateKey(cpk.decryptPrivateKeyInfo(provider));
            } else {
                throw new EncryptionException("Unable get private key from " + obj);
            }
        }

        return pkey;
    }

    public static PrivateKey getPrivateKey(AbstractKeyStoreKeyType key, Protector protector)
            throws KeyStoreException, IOException, EncryptionException, NoSuchAlgorithmException,
            UnrecoverableKeyException, CertificateException {
        if (key == null) {
            return null;
        }
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream is = new FileInputStream(key.getKeyStorePath());
        ks.load(is, protector.decryptString(key.getKeyStorePassword()).toCharArray());

        Key pkey = ks.getKey(key.getKeyAlias(), protector.decryptString(key.getKeyPassword()).toCharArray());
        if (!(pkey instanceof PrivateKey)) {
            throw new EncryptionException("Alias " + key.getKeyAlias() + " don't return key of PrivateKey type.");
        }
        return (PrivateKey) pkey;
    }
}
