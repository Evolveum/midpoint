/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorIdentifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.security.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mock implementation of {@link ConnectorSignatureVerifier} used in place of
 * the production verifier during testing.
 *
 * Main differences of the regular verifier:
 *
 * - It overrides the check for being in production environment (by overriding {@link #isVerificationNeeded(ConnectorType)}),
 * in order to enable signature-checking functionality during tests.
 *
 * - Instead of using the Integration Catalog signing keys, it generates a single dedicated key pair for signing and verification.
 */
public class MockConnectorSignatureVerifier extends ConnectorSignatureVerifier {

    /** Used to create and check signatures. */
    private final static String MAIN_KEY_ID = "test-key-id";

    /** This is to test scenarios with multiple known public keys. The key itself is not used in any signature operations. */
    private final static String UNUSED_KEY_ID = "unused-key-id";

    private KeyPair keyPair;
    private KeyPair unusedKeyPair;

    public MockConnectorSignatureVerifier() {
        generateKeyPairs();
    }

    private void generateKeyPairs() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
            this.keyPair = generator.generateKeyPair();
            this.unusedKeyPair = generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    void refreshKeyPair() {
        generateKeyPairs();
    }

    byte[] sign(ConnectorIdentifierType connector) throws JsonProcessingException, GeneralSecurityException {
        byte[] payload = toJson(connector);

        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keyPair.getPrivate());
        signer.update(payload);

        return signer.sign();
    }

    @Override
    public boolean isVerificationNeeded(ConnectorType connectorBean) {
        return isConnIdConnector(connectorBean); // intentionally not checking whether we are in production
    }

    @Override
    protected Map<String, PublicKey> getPublicKeys() {
        Map<String, PublicKey> keys = new LinkedHashMap<>();
        keys.put(UNUSED_KEY_ID, unusedKeyPair.getPublic()); // putting this first to confuse the checker even more
        keys.put(MAIN_KEY_ID, keyPair.getPublic());
        return keys;
    }

    String getKeyId() {
        return MAIN_KEY_ID;
    }
}
