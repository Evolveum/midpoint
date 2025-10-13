/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;

public abstract class AbstractMockActivityHandler<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
        implements ActivityHandler<WD, AH> {

}
