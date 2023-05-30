/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.handler;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;

public interface UpgradeObjectHandler {

    UpgradePhase getPhase();

    boolean isApplicable(String version);

    void handleObject(PrismObject<?> object, OperationResult result);
}
