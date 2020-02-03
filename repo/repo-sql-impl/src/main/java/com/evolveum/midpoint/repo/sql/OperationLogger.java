/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class OperationLogger {

    private static final Trace LOGGER_OP = TraceManager.getTrace("com.evolveum.midpoint.repo.operation");
    private static final String PREFIX = "Repository operation";

    public static <O extends ObjectType> void logAdd(PrismObject<O> object, RepoAddOptions options, OperationResult subResult) {
        if (!LOGGER_OP.isDebugEnabled()) {
            return;
        }
        LOGGER_OP.debug("{} add {}{}: {}\n{}", PREFIX, object, shortDumpOptions(options), getStatus(subResult),
                object.debugDump(1));
    }

    public static <O extends ObjectType> void logModify(Class<O> type, String oid, Collection<? extends ItemDelta> modifications,
            ModificationPrecondition<O> precondition, RepoModifyOptions options, OperationResult subResult) {
        if (!LOGGER_OP.isDebugEnabled()) {
            return;
        }
        LOGGER_OP.debug("{} modify {} {}{}{}: {}\n{}", PREFIX, type.getSimpleName(), oid,
                shortDumpOptions(options),
                precondition == null ? "" : " precondition="+precondition+", ",
                getStatus(subResult),
                DebugUtil.debugDump(modifications, 1, false));
    }

    public static <O extends ObjectType> void logDelete(Class<O> type, String oid, OperationResult subResult) {
        if (!LOGGER_OP.isDebugEnabled()) {
            return;
        }
        LOGGER_OP.debug("{}-{} delete {}: {}", PREFIX, type.getSimpleName(), oid, getStatus(subResult));
    }

    public static <O extends ObjectType> void logGetObject(Class<O> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, PrismObject<O> object, OperationResult subResult) {
        if (!LOGGER_OP.isTraceEnabled()) {
            return;
        }
        LOGGER_OP.trace("{} get {} {}{}: {}\n{}", PREFIX, type.getSimpleName(), oid,
                shortDumpOptions(options), getStatus(subResult),
                DebugUtil.debugDump(object, 1));
    }

    private static Object shortDumpOptions(ShortDumpable options) {
        if (options == null) {
            return "";
        } else {
            return " ["+options.shortDump()+"]";
        }
    }

    private static Object shortDumpOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        if (options == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder(" ");
            DebugUtil.shortDump(sb, options);
            return sb.toString();
        }
    }

    private static String getStatus(OperationResult subResult) {
        if (subResult == null) {
            return null;
        }
        String message = subResult.getMessage();
        if (message == null) {
            return subResult.getStatus().toString();
        } else {
            return subResult.getStatus().toString() + ": " + message;
        }
    }

}
