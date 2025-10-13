/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqlbase;

import com.evolveum.midpoint.util.LocalizableMessage;

public class NativeOnlySupportedException extends QueryException {

    public NativeOnlySupportedException(LocalizableMessage localizableMessage) {
        super(localizableMessage);
    }


}
