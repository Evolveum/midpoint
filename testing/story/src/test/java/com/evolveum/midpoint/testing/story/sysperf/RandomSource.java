/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import java.util.Random;
import java.util.UUID;

class RandomSource {

    /** In order to provide more deterministic results, we use fixed seed for random generator. */
    static final Random FIXED_RANDOM = new Random(0L);

    /** Also let's use not-so-much random UUIDs. */
    static UUID randomUUID() {
        byte[] randomBytes = new byte[16];
        FIXED_RANDOM.nextBytes(randomBytes);
        long mostSigBits = 0;
        for (int i = 0; i < 8; i++) {
            mostSigBits = (mostSigBits << 8) | (randomBytes[i] & 0xff);
        }
        long leastSigBits = 0;
        for (int i = 8; i < 16; i++) {
            leastSigBits = (leastSigBits << 8) | (randomBytes[i] & 0xff);
        }
        return new UUID(mostSigBits, leastSigBits);
    }
}
