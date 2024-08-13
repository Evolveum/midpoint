/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import com.evolveum.midpoint.util.LocalizableMessage;

public class NativeOnlySupportedException extends QueryException {

    public NativeOnlySupportedException(LocalizableMessage localizableMessage) {
        super(localizableMessage);
    }


}
