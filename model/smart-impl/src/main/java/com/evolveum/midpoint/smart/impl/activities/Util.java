/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionWorkStateType;

class Util {

    static ActivityState getParentState(AbstractActivityRun<?, ?, ?> run, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return ActivityState.getActivityStateUpwards(
                run.getActivity().getPath().allExceptLast(),
                run.getRunningTask(),
                ObjectTypesSuggestionWorkStateType.COMPLEX_TYPE,
                result);
    }
}
