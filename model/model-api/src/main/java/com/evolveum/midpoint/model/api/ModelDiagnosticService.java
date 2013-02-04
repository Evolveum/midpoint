/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

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
	
	/**
	 * Provide repository run-time configuration and diagnostic information.
	 */
	public RepositoryDiag getRepositoryDiag(Task task, OperationResult parentResult);
	
	/**
	 * Runs a short, non-descructive repository self test.
	 * This methods should never throw a (checked) exception. All the results
	 * should be in the returned result structure (including fatal errors).
	 */
	public OperationResult repositorySelfTest(Task task);

}
