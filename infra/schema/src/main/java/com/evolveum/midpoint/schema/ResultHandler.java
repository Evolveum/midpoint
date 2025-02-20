/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Classes implementing this interface are used to handle iterative results.
 *
 * It is only used to handle iterative search results now. It may be reused for
 * other purposes as well.
 *
 * Also see {@link ObjectHandler} which is not limited to {@link ObjectType} and handles
 * real values instead of prism values.
 *
 * TODO this should perhaps be named ObjectResultHandler
 *
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface ResultHandler<T extends ObjectType> extends ObjectHandler<PrismObject<T>> {

    /**
     * Returns the handler with the same functionality as this one, enriching it with providing its own child operation result
     * and taking care of limiting the growth of the operation result tree.
     *
     * The reason is that we need to introduce operation results when *emitting* the objects from the source, in order to
     * distinguish the processing time of handling the objects from the time to produce them. On the *receiving* end, though,
     * we must do the same: we must mark the processing as belonging to the receiving component (like provisioning or model).
     *
     * Said in other words, the receiving side has the primary obligation to provide its own operation result.
     * The emitting side, though, should do this to defend itself if the receiving side doesn't do that.
     * (To ascribe the processing time at least to "handle object found" method of the emitter, so it's discernible
     * there; at least if one looks closely enough.)
     *
     * An exception is when the processing on the receiving size is negligible. This also applies to some intermediaries
     * (like repository cache), which are not the primary processing component. They can safely defer creating the operation
     * result to the upstream processing component(s). (Which can, of course, avoid creating the result if their own processing
     * is small.) However, this exception may not apply when we're crossing the component (repo, provisioning, model, ...)
     * boundaries. At these places, it may make sense to create a separate result even for very small processing.
     *
     * See also the principle #4 in {@link OperationResult} javadoc.
     *
     * Open questions:
     *
     * . Do we always need to delete subresults and summarize the parent result?
     * Probably only if we are the first in the chain of handlers, i.e. we create directly the children
     * of the search-like operation result, one related to each object found.
     *
     * . Should the subresult be minor or not? Probably yes.
     */
    default ResultHandler<T> providingOwnOperationResult(String operationName) {
        return (object, parentResult) -> {
            var result = parentResult
                    .subresult(operationName)
                    .addParam(OperationResult.PARAM_OBJECT, object)
                    .setMinor()
                    .build();
            try {
                return handle(object, result);
            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                result.close();
                result.deleteSubresultsIfPossible();
                parentResult.summarize();
            }
        };
    }
}
