/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.crypto;

import com.evolveum.midpoint.prism.PrismContext;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.TrustManager;
import java.util.List;

/**
 *  Builder for KeyStoreBasedProtector implementation.
 */
public final class KeyStoreBasedProtectorBuilder {
    String keyStorePath;
    String keyStorePassword;
    String encryptionKeyAlias;
    String requestedJceProviderName;
    String encryptionAlgorithm;
    String digestAlgorithm;
    List<TrustManager> trustManagers;

    @NotNull private final PrismContext prismContext;

    public static KeyStoreBasedProtectorBuilder create(@NotNull PrismContext prismContext) {
        return new KeyStoreBasedProtectorBuilder(prismContext);
    }

    private KeyStoreBasedProtectorBuilder(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public KeyStoreBasedProtectorBuilder keyStorePath(String val) {
        keyStorePath = val;
        return this;
    }

    public KeyStoreBasedProtectorBuilder keyStorePassword(String val) {
        keyStorePassword = val;
        return this;
    }

    public KeyStoreBasedProtectorBuilder encryptionKeyAlias(String val) {
        encryptionKeyAlias = val;
        return this;
    }

    public KeyStoreBasedProtectorBuilder requestedJceProviderName(String val) {
        requestedJceProviderName = val;
        return this;
    }

    public KeyStoreBasedProtectorBuilder encryptionAlgorithm(String val) {
        encryptionAlgorithm = val;
        return this;
    }

    public KeyStoreBasedProtectorBuilder digestAlgorithm(String val) {
        digestAlgorithm = val;
        return this;
    }

    public KeyStoreBasedProtectorBuilder trustManagers(List<TrustManager> val) {
        trustManagers = val;
        return this;
    }

    public Protector initialize() {
        return prismContext.createInitializedProtector(this);
    }
}
