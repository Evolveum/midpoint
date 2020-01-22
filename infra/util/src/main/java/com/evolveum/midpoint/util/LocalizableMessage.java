/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import java.io.Serializable;

/**
 * @author semancik
 * @author mederly
 *
 */
public interface LocalizableMessage extends Serializable, ShortDumpable {

    String getFallbackMessage();

    boolean isEmpty();

    static boolean isEmpty(LocalizableMessage msg) {
        return msg == null || msg.isEmpty();
    }
}
