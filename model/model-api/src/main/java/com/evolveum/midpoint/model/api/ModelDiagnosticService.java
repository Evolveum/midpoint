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
package com.evolveum.midpoint.model.api;

import java.util.Collection;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * A service provided by the IDM Model focused on system diagnostic. It allows to retrieve diagnostic data
 * that are not exactly part of system configuration (such as repository configuration). It can also be used
 * to initiate self-tests and similar diagnostic routines. 
 * 
 * UNSTABLE: This is likely to change
 * PRIVATE: This interface is not supposed to be used outside of midPoint
 * 
 * @author Radovan Semancik
 *
 */
public interface ModelDiagnosticService {
	
	String CLASS_NAME_WITH_DOT = ModelDiagnosticService.class.getName() + ".";
	String REPOSITORY_SELF_TEST = CLASS_NAME_WITH_DOT + "repositorySelfTest";
	String PROVISIONING_SELF_TEST = CLASS_NAME_WITH_DOT + "provisioningSelfTest";
	
	/**
	 * Provide repository run-time configuration and diagnostic information.
	 */
	public RepositoryDiag getRepositoryDiag(Task task, OperationResult parentResult);
	
	/**
	 * Runs a short, non-destructive repository self test.
	 * This methods should never throw a (checked) exception. All the results
	 * should be in the returned result structure (including fatal errors).
	 */
	public OperationResult repositorySelfTest(Task task);
	
	/**
	 * Runs a short, non-destructive internal provisioning test. It tests provisioning framework and
	 * general setup. Use ModelService.testResource for testing individual resource configurations.
	 */
	public OperationResult provisioningSelfTest(Task task);

}
