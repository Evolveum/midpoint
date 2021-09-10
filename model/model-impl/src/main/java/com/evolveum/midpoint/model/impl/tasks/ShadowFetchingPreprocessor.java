package com.evolveum.midpoint.model.impl.tasks;

import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.task.ObjectPreprocessor;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * A preprocessor that fetched incoming object (presumably resolved using noFetch option)
 * in order to obtain full attributes.
 *
 * It is expected that it throws an exception if the object cannot be fetched fully.
 */
public class ShadowFetchingPreprocessor implements ObjectPreprocessor<ShadowType> {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowFetchingPreprocessor.class);

    @NotNull private final SearchBasedActivityExecution<?, ?, ?, ?> activityExecution;
    @NotNull private final ModelObjectResolver modelObjectResolver;

    ShadowFetchingPreprocessor(@NotNull SearchBasedActivityExecution<?, ?, ?, ?> activityExecution,
            @NotNull ModelObjectResolver modelObjectResolver) {
        this.activityExecution = activityExecution;
        this.modelObjectResolver = modelObjectResolver;
    }

    @Override
    public PrismObject<ShadowType> preprocess(PrismObject<ShadowType> originalObject, Task task, OperationResult result)
            throws CommonException {
        String oid = originalObject.getOid();
        stateCheck(oid != null, "Original object has no OID");

        Collection<SelectorOptions<GetOperationOptions>> options = adaptSearchOptions(activityExecution.getSearchOptions());

        LOGGER.trace("Fetching {} with options: {}", originalObject, options);
        return modelObjectResolver
                .getObject(ShadowType.class, oid, options, task, result)
                .asPrismObject();
    }

    private Collection<SelectorOptions<GetOperationOptions>> adaptSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> originalOptions) {

        Collection<SelectorOptions<GetOperationOptions>> optionsToSet =
                activityExecution.getSchemaService().getOperationOptionsBuilder()
                        .noFetch(false)
                        .errorReportingMethod(FetchErrorReportingMethodType.FORCED_EXCEPTION) // we need exceptions!
                        .build();
        return GetOperationOptions.merge(activityExecution.getPrismContext(), originalOptions, optionsToSet);
    }
}
