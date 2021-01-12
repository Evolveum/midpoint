/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

public class OperationResultUtil {

    public static OperationResultType shallowClone(OperationResultType result, boolean subresults, boolean traces, boolean log) {
        OperationResultType clone = new OperationResultType();
        clone.setOperation(result.getOperation());
        clone.getQualifier().addAll(result.getQualifier());
        clone.setOperationKind(result.getOperationKind());
        clone.setStatus(result.getStatus());
        clone.setImportance(result.getImportance());
        clone.setMinor(result.isMinor());
        clone.setAsynchronousOperationReference(result.getAsynchronousOperationReference());
        clone.setStart(CloneUtil.clone(result.getStart()));
        clone.setEnd(CloneUtil.clone(result.getEnd()));
        clone.setMicroseconds(result.getMicroseconds());
        clone.setInvocationId(result.getInvocationId());
        clone.setTraced(result.isTraced());
        if (traces) {
            clone.getTrace().addAll(result.getTrace());
        }
        clone.setCount(result.getCount());
        clone.setHiddenRecordsCount(result.getHiddenRecordsCount());
        clone.setParams(result.getParams());
        clone.setContext(result.getContext());
        clone.setReturns(result.getReturns());
        clone.setToken(result.getToken());
        clone.setMessageCode(result.getMessageCode());
        clone.setMessage(result.getMessage());
        clone.setUserFriendlyMessage(result.getUserFriendlyMessage());
        clone.setDetails(result.getDetails());
        if (log) {
            clone.getLog().addAll(result.getLog());
        }
        if (subresults) {
            clone.getPartialResults().addAll(result.getPartialResults());
        }
        return clone;
    }

    public static boolean isSuccessful(OperationResultStatusType result) {
        return OperationResultStatusType.SUCCESS.equals(result) ||
                OperationResultStatusType.HANDLED_ERROR.equals(result) ||
                OperationResultStatusType.WARNING.equals(result);
    }

    public static boolean isError(OperationResultStatusType status) {
        return status == OperationResultStatusType.FATAL_ERROR || status == OperationResultStatusType.PARTIAL_ERROR;
    }
}
