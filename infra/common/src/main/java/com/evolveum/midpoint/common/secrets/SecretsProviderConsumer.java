/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.secrets;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.SecretsProvider;

public interface SecretsProviderConsumer {

    void addSecretsProvider(@NotNull SecretsProvider provider);

    void removeSecretsProvider(@NotNull SecretsProvider provider);

    @NotNull List<SecretsProvider> getSecretsProviders();
}
