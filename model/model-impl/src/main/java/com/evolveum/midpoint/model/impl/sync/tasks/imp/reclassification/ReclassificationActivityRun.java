/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp.reclassification;

import java.util.Collection;

import com.evolveum.midpoint.model.impl.sync.tasks.imp.AbstractImportActivityRun;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

public final class ReclassificationActivityRun
        extends AbstractImportActivityRun<ReclassificationWorkDefinition, ReclassificationActivityHandler> {

    ReclassificationActivityRun(ActivityRunInstantiationContext<ReclassificationWorkDefinition, ReclassificationActivityHandler> context) {
        super(context, "Shadow reclassification");
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (getRunningTask().isPersistentAtShadowLevelButNotFully()) {
            throw new SchemaException("Execution mode " + ExecutionModeType.PREVIEW + " is unsupported, please use "
                    + ExecutionModeType.FULL + " for full processing or " + ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW + " for simulation.");
        }

        return super.beforeRun(result);
    }

    @Override
    protected QName getSourceChannel() {
        return SchemaConstants.CHANNEL_SHADOW_RECLASSIFICATION;
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> mergeSearchOptions(Collection<SelectorOptions<GetOperationOptions>> defaultOptions, Collection<SelectorOptions<GetOperationOptions>> searchOptions) {
        Collection<SelectorOptions<GetOperationOptions>> mergedOptions = super.mergeSearchOptions(defaultOptions, searchOptions);

        return GetOperationOptions.merge(mergedOptions,
                SchemaService.get().getOperationOptionsBuilder()
                        .shadowClassificationMode(ShadowClassificationModeType.FORCED)
                        .build());
    }

    @Override
    protected String getChannelOverride() {
        return SchemaConstants.CHANNEL_SHADOW_RECLASSIFICATION_URI;
    }
}
