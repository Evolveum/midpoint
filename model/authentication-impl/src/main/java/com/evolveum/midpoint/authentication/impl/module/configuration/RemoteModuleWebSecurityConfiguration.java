/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
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

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */

public class RemoteModuleWebSecurityConfiguration extends ModuleWebSecurityConfigurationImpl {

    static Protector protector = PrismContext.get().getDefaultProtector();

    protected static Certificate getCertificate(AbstractSimpleKeyType key, Protector protector)
            throws EncryptionException, CertificateException, Base64Exception {
        if (key == null || key.getCertificate() == null) {
            return null;
        }

        return getCertificate(key.getCertificate(), protector);
    }

    protected static Certificate getCertificate(ProtectedStringType certficate, Protector protector) throws EncryptionException, CertificateException, Base64Exception {
        String clearValue = protector.decryptString(certficate);
        byte[] certBytes;

        if (StringUtils.isNotEmpty(clearValue) && clearValue.startsWith("-----")) {
            //remove header on start
            clearValue = clearValue.replaceFirst("-----", "");
            clearValue = clearValue.substring(clearValue.indexOf("-----"));
            clearValue = clearValue.replaceFirst("-----", "");

            //remove header on end
            clearValue = clearValue.substring(0, clearValue.indexOf("-----"));
            clearValue = clearValue.replaceFirst("^\\s*", "");
            clearValue = clearValue.replaceFirst("\\s++$", "");
        }

        if (Base64.isBase64(clearValue)) {
            boolean isBase64Url = clearValue.contains("-") || clearValue.contains("_");
            certBytes = Base64Utility.decode(clearValue, isBase64Url);
        } else {
            certBytes = clearValue.getBytes();
        }

        return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certBytes));
    }

    protected static Certificate getCertificate(AbstractKeyStoreKeyType key, Protector protector)
            throws EncryptionException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        if (key == null) {
            return null;
        }
        KeyStore ks = KeyStore.getInstance("JKS");
        FileInputStream is = new FileInputStream(key.getKeyStorePath());
        ks.load(is, protector.decryptString(key.getKeyStorePassword()).toCharArray());

        return ks.getCertificate(key.getKeyAlias());
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
                throw new EncryptionException("Unable to decode PEM key");
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
