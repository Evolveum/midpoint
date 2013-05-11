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
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensContextType;

/**
 * A service provided by the IDM Model that allows to improve the (user) interaction with the model.
 * It is supposed to provide services such as preview of changes, diagnostics and other informational
 * services. It should only provide access to read-only data or provide a temporary (throw-away) previews
 * of data. It should not change the state of IDM repository, resources or tasks. 
 * 
 * UNSTABLE: This is likely to change
 * PRIVATE: This interface is not supposed to be used outside of midPoint
 * 
 * @author Radovan Semancik
 *
 */
public interface ModelInteractionService {
	
	String CLASS_NAME_WITH_DOT = ModelInteractionService.class.getName() + ".";
	String PREVIEW_CHANGES = CLASS_NAME_WITH_DOT + "previewChanges";
	
	/**
	 * Computes the most likely changes triggered by the provided delta. The delta may be any change of any object, e.g.
	 * add of a user or change of a shadow. The resulting context will sort that out to "focus" and "projection" as needed.
	 * The supplied delta will be used as a primary change. The resulting context will reflect both this primary change and
	 * any resulting secondary changes.
	 * 
	 * The changes are only computed, NOT EXECUTED. It also does not change any state of any repository object or task. Therefore 
	 * this method is safe to use anytime. However it is reading the data from the repository and possibly also from the resources
	 * therefore there is still potential for communication (and other) errors and invocation of this method may not be cheap.
	 * However, as no operations are really executed there may be issues with resource dependencies. E.g. identifier that are generated
	 * by the resource are not taken into account while recomputing the values. This may also cause errors if some expressions depend
	 * on the generated values. 
	 */
	public <F extends ObjectType, P extends ObjectType> ModelContext<F, P> previewChanges(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult result) 
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException;

    public <F extends ObjectType, P extends ObjectType> ModelContext<F, P> unwrapModelContext(LensContextType wrappedContext, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException;

}
