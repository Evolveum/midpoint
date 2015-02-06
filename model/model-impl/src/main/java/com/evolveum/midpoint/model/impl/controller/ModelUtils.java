/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * 
 * @author lazyman
 * 
 */
public class ModelUtils {

	private static final Trace LOGGER = TraceManager.getTrace(ModelUtils.class);

	public static void validatePaging(ObjectPaging paging) {
		if (paging == null) {
			return;
		}

		if (paging.getMaxSize() != null && paging.getMaxSize().longValue() < 0) {
			throw new IllegalArgumentException("Paging max size must be more than 0.");
		}
		if (paging.getOffset() != null && paging.getOffset().longValue() < 0) {
			throw new IllegalArgumentException("Paging offset index must be more than 0.");
		}
	}
	
	public static void recordFatalError(OperationResult result, Throwable e) {
		recordFatalError(result, e.getMessage(), e);
	}

	public static void recordFatalError(OperationResult result, String message, Throwable e) {
		// Do not log at ERROR level. This is too harsh. Especially in object not found case.
		// What model considers an error may be just a normal situation for the code is using model API.
		// If this is really an error then it should be logged by the invoking code.
		LoggingUtils.logExceptionOnDebugLevel(LOGGER, message, e);
		result.recordFatalError(message, e);
		result.cleanupResult(e);
	}
	
	public static void recordPartialError(OperationResult result, Throwable e) {
		recordPartialError(result, e.getMessage(), e);
	}

	public static void recordPartialError(OperationResult result, String message, Throwable e) {
		// Do not log at ERROR level. This is too harsh. Especially in object not found case.
		// What model considers an error may be just a normal situation for the code is using model API.
		// If this is really an error then it should be logged by the invoking code.
		LoggingUtils.logExceptionOnDebugLevel(LOGGER, message, e);
		result.recordPartialError(message, e);
		result.cleanupResult(e);
	}
	
	public static <O extends ObjectType> String getOperationUrlFromDelta(ObjectDelta<O> delta) {
		if (delta == null) {
			return null;
		}
		if (delta.isAdd()) {
			return ModelAuthorizationAction.ADD.getUrl();
		}
		if (delta.isModify()) {
			return ModelAuthorizationAction.MODIFY.getUrl();
		}
		if (delta.isDelete()) {
			return ModelAuthorizationAction.DELETE.getUrl();
		}
		throw new IllegalArgumentException("Unknown delta type "+delta);
	}

}
