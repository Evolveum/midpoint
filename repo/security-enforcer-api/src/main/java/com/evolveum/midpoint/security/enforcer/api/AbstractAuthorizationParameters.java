/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.enforcer.api;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.spec.SelectorClause;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * Parameters describing the details of the situation we want to check authorization for.
 *
 * Currently, there are two kinds of parameters, which may be unified in the future.
 *
 * . Traditional, object-based ones ({@link AuthorizationParameters}) assume we talk about a prism object and optionally
 * a delta over it.
 *
 * . New, experimental, value-based ones ({@link ValueAuthorizationParameters}) assume we talk about a prism value.
 * There is now way of specifying a delta there, as delta-like operations are currently carried out on the object level only.
 */
public interface AbstractAuthorizationParameters extends ShortDumpable {

    /** The value (for value-based params) or "any object" - for traditional params. */
    PrismValue getValue();

    default boolean hasValue() {
        return getValue() != null;
    }

    /** Is the value fully known? See {@link SelectorClause#requiresFullInformation()}. */
    boolean isFullInformationAvailable();
}
