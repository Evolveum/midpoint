/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.crypto;

/**
 *  Creates protectors based on corresponding builder objects.
 *  Usually implemented by the PrismContext.
 */
public interface ProtectorCreator {

    /**
     * Creates initialized KeyStoreBasedProtector according to configured KeyStoreBasedProtectorBuilder object.
     */
    KeyStoreBasedProtector createInitializedProtector(KeyStoreBasedProtectorBuilder builder);

    /**
     * Creates uninitialized KeyStoreBasedProtector according to configured KeyStoreBasedProtectorBuilder object.
     */
    KeyStoreBasedProtector createProtector(KeyStoreBasedProtectorBuilder builder);

}
