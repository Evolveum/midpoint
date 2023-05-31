/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface UpgradeObjectProcessor<T extends ObjectType> {

    UpgradePhase getPhase();

    <O extends ObjectType> boolean isApplicable(Class<O> type);

    /**
     * Updates object to correct form
     *
     * @param object
     * @param result
     * @return true if object needs to be updated (saved) in midpoint repository, false otherwise
     */
    boolean processObject(PrismObject<T> object, OperationResult result);
}
