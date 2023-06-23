/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.security.*;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.List;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.KeyStoreBasedProtector;
import com.evolveum.midpoint.prism.crypto.Protector;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListKeysRepositoryAction extends Action<ListKeysOptions, Void> {

    private static final String KEY_DIGEST_TYPE = "SHA1";

    @Override
    public String getOperationName() {
        return "list keys";
    }

    @Override
    public LogTarget getLogTarget() {
        return LogTarget.SYSTEM_ERR;
    }

    @Override
    public @NotNull NinjaApplicationContextLevel getApplicationContextLevel(List<Object> allOptions) {
        return NinjaApplicationContextLevel.NO_REPOSITORY;
    }

    @Override
    public Void execute() throws Exception {
        ApplicationContext appContext = context.getApplicationContext();
        Protector protector = appContext.getBean(Protector.class);

        if (protector instanceof KeyStoreBasedProtector) {
            KeyStoreBasedProtector p = (KeyStoreBasedProtector) protector;
            context.out.println("Location: " + p.getKeyStorePath());
        }

        KeyStore keyStore = protector.getKeyStore();

        context.out.println("Type: " + keyStore.getType());

        Provider provider = keyStore.getProvider();
        context.out.println("Provider: " + provider.getName());

        Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();

            context.out.println("======");

            describeAlias(keyStore, alias, protector);

            if (aliases.hasMoreElements()) {
                context.out.println("======");
            }
        }

        // todo implement dump other keys from keystore
        return null;
    }

    private void describeAlias(KeyStore keyStore, String alias, Protector protector)
            throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException, EncryptionException {

        context.out.println("Alias: " + alias);
        context.out.println("Creation date: " + keyStore.getCreationDate(alias));

        Certificate cert = keyStore.getCertificate(alias);
        if (cert != null) {
            context.out.println("Certificate: " + cert);
        }

        Certificate[] chain = keyStore.getCertificateChain(alias);
        if (chain != null) {
            context.out.println("Certificate chain: " + chain);
        }

        char[] password = getPassword();

        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(password);
        KeyStore.Entry entry = keyStore.getEntry(alias, protParam);

        if (!(entry instanceof KeyStore.SecretKeyEntry)) {
            return;
        }

        KeyStore.SecretKeyEntry sEntry = (KeyStore.SecretKeyEntry) entry;
        SecretKey key = sEntry.getSecretKey();
        context.out.println("Secret key entry");

        context.out.println("  Algorithm: " + key.getAlgorithm());
        context.out.println("  Format: " + key.getFormat());
        context.out.println("  Key length: " + key.getEncoded().length * 8);
        context.out.println("  SHA1 digest: " + getSecretKeyDigest(key));

        if (protector instanceof KeyStoreBasedProtector) {
            KeyStoreBasedProtector impl = (KeyStoreBasedProtector) protector;

            String name = impl.getSecretKeyDigest(key);
            context.out.println("  Key name: " + name);
        }
    }

    private char[] getPassword() {
        String password = options.getKeyPassword();
        if (password == null) {
            password = options.getAskKeyPassword();
        }

        if (password == null) {
            password = "";
        }

        return password.toCharArray();
    }

    private String getSecretKeyDigest(SecretKey key) throws NinjaException {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance(KEY_DIGEST_TYPE);
        } catch (NoSuchAlgorithmException ex) {
            throw new NinjaException(ex.getMessage(), ex);
        }

        return Base64.encodeBase64String(sha1.digest(key.getEncoded()));
    }
}
