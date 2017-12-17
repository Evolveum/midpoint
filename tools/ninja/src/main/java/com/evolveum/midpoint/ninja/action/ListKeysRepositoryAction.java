package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.ListKeysOptions;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.crypto.ProtectorImpl;
import org.apache.xml.security.utils.Base64;
import org.springframework.context.ApplicationContext;

import javax.crypto.SecretKey;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Enumeration;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListKeysRepositoryAction extends RepositoryAction<ListKeysOptions> {

    private static final String KEY_DIGEST_TYPE = "SHA1";

    @Override
    public void execute() throws Exception {
        ApplicationContext appContext = context.getApplicationContext();
        Protector protector = appContext.getBean(Protector.class);

        if (protector instanceof ProtectorImpl) {
            ProtectorImpl impl = (ProtectorImpl) protector;
            logInfo("Location: {}", impl.getKeyStorePath());
        }

        KeyStore keyStore = protector.getKeyStore();

        logInfo("Type: {}", keyStore.getType());

        Provider provider = keyStore.getProvider();
        logInfo("Provider: {}", provider.getName());

        Enumeration<String> aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();

            logInfo("======");

            describeAlias(keyStore, alias, protector);

            if (aliases.hasMoreElements()) {
                logInfo("======");
            }
        }

        // todo implement dump other keys from keystore
    }

    private void describeAlias(KeyStore keyStore, String alias, Protector protector)
            throws KeyStoreException, UnrecoverableEntryException, NoSuchAlgorithmException, EncryptionException {

        logInfo("Alias: {}", alias);
        logInfo("Creation date: {}", keyStore.getCreationDate(alias));

        Certificate cert = keyStore.getCertificate(alias);
        if (cert != null) {
            logInfo("Certificate: {}", cert);
        }

        Certificate[] chain = keyStore.getCertificateChain(alias);
        if (chain != null) {
            logInfo("Certificate chain: {}", chain);
        }

        char[] password = getPassword();

        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(password);
        KeyStore.Entry entry = keyStore.getEntry(alias, protParam);

        if (!(entry instanceof KeyStore.SecretKeyEntry)) {
            return;
        }

        KeyStore.SecretKeyEntry sEntry = (KeyStore.SecretKeyEntry) entry;
        SecretKey key = sEntry.getSecretKey();
        logInfo("Secret key entry");

        logInfo("  Algorithm: {}", key.getAlgorithm());
        logInfo("  Format: {}", key.getFormat());
        logInfo("  Key length: {}", key.getEncoded().length * 8);
        logInfo("  SHA1 digest: {}", getSecretKeyDigest(key));

        if (protector instanceof ProtectorImpl) {
            ProtectorImpl impl = (ProtectorImpl) protector;

            String name = impl.getSecretKeyDigest(key);
            logInfo("  Key name: {}", name);
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

        return Base64.encode(sha1.digest(key.getEncoded()));
    }
}
