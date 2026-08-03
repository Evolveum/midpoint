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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of {@link ConnectorSignatureVerifier} used in place of
 * the production verifier during testing.
 *
 * It overrides the production environment check to always return {@code true},
 * enabling testing of production-only functionality. Instead of using the
 * Integration Catalog signing keys, it generates a dedicated key pair for
 * signing and verification.
 */
public class MockConnectorSignatureVerifier extends ConnectorSignatureVerifier {

    private final static String KEY_ID = "test-key-id";

    private KeyPair keyPair;

    public MockConnectorSignatureVerifier() {
        loadKeyPair();
    }

    private void loadKeyPair() {
        try {
            KeyPairGenerator generator =
                    KeyPairGenerator.getInstance("Ed25519");

            this.keyPair = generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void refreshKeyPair() {
        loadKeyPair();
    }

    public String sign(ConnectorIdentifierType connector) throws JsonProcessingException, GeneralSecurityException {
        byte[] payload = toJson(connector);

        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keyPair.getPrivate());
        signer.update(payload);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(signer.sign());
    }

    @Override
    public boolean isVerificationNeeded(ConnectorType connectorBean) {
        return isConnIdConnector(connectorBean);
    }

    @Override
    protected Map<String, PublicKey> getPublicKeys() {
        Map<String, PublicKey> keys = new HashMap<>();
        keys.put(KEY_ID, keyPair.getPublic());
        return keys;
    }

    public String getKeyId() {
        return KEY_ID;
    }
}
