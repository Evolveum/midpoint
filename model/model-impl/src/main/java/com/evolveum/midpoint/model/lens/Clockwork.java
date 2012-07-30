/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.ChangeExecutor;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * @author semancik
 *
 */
@Component
public class Clockwork {
	
	@Autowired(required = true)
	Projector projector;
	
	@Autowired(required = true)
	private ChangeExecutor changeExecutor;
	
	private LensDebugListener debugListener;
	
	private boolean consistenceChecks = true;
	
	public LensDebugListener getDebugListener() {
		return debugListener;
	}

	public void setDebugListener(LensDebugListener debugListener) {
		this.debugListener = debugListener;
	}

	public void run(LensContext context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		while (context.getState() != ModelState.FINAL) {
			click(context, task, result);
		}
	}
	
	public void click(LensContext context, Task task, OperationResult result) throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		
		projector.project(context, result);
		
		ModelState state = context.getState();
		switch (state) {
			case INITIAL:
				processInitialToPrimary(context, task, result);
				break;
			case PRIMARY:
				processPrimaryToSecondary(context, task, result);
				break;
			case SECONDARY:
				processSecondary(context, task, result);
				break;
			case FINAL:
				return;
		}
		
		// Invoke hook
	}

	private void processInitialToPrimary(LensContext context, Task task, OperationResult result) {
		// Context loaded, nothing to do. Bump state to PRIMARY
		context.setState(ModelState.PRIMARY);
	}
	
	private void processPrimaryToSecondary(LensContext context, Task task, OperationResult result) {
		// Nothing to do now. The context is already recomputed.
		context.setState(ModelState.SECONDARY);
	}
	
	private void processSecondary(LensContext context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (context.getWave() > context.getMaxWave() + 1) {
			context.setState(ModelState.FINAL);
			return;
		}
		// execute current wave and go to the next wave
		changeExecutor.executeChanges(context, result);
		// TODO: attempts
		context.incrementWave();
	}

}
