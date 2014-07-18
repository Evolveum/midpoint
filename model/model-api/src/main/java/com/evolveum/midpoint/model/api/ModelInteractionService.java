/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_3.LensContextType;

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
	<F extends ObjectType> ModelContext<F> previewChanges(
			Collection<ObjectDelta<? extends ObjectType>> deltas, ModelExecuteOptions options, Task task, OperationResult result) 
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException;

    <F extends ObjectType> ModelContext<F> unwrapModelContext(LensContextType wrappedContext, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException;

    /**
     * <p>
     * Returns a schema that reflects editability of the object in terms of midPoint schema limitations and security. This method
     * merges together all the applicable limitations that midPoint knows of (schema, security, other constratints). It may be required
     * to pre-populate new object before calling this method, e.g. to put the object in a correct org in case that delegated administration
     * is used.
     * </p>
     * <p>
     * If null is returned then the access to the entire object is denied. It cannot be created or edited at all.
     * </p>
     * <p>
     * The returned definition contains all parts of static schema and run-time extensions. It does not contain parts of resource
     * "refined" schemas. Therefore for shadows it is only applicable to static parts of the shadow (not attributes).
     * </p>
     * <p>
     * This is <b>not</b> security-sensitive function. It provides data about security constraints but it does <b>not</b> enforce it and
     * it does not modify anything or reveal any data. The purpose of this method is to enable convenient display of GUI form fields,
     * e.g. to hide non-accessible fields from the form. The actual enforcement of the security is executed regardless of this
     * method.
     * </p>
     * 
     * @param object object to edit
     * @return schema with correctly set constraint parts or null
     * @throws SchemaException 
     */
    <O extends ObjectType> PrismObjectDefinition<O> getEditObjectDefinition(PrismObject<O> object, AuthorizationPhaseType phase) throws SchemaException;

    RefinedObjectClassDefinition getEditObjectClassDefinition(PrismObject<ShadowType> shadow, PrismObject<ResourceType> resource, AuthorizationPhaseType phase) throws SchemaException;

    /**
     * <p>
     * Returns a collection of all authorization actions known to the system. The format of returned data is designed for displaying
     * purposes.
     * </p>
     * <p>
     * Note: this method returns only the list of authorization actions that are known to the IDM Model component and the components
     * below. It does <b>not</b> return a GUI-specific authorization actions.
     * </p>
     * 
     * @return
     */
    Collection<? extends DisplayableValue<String>> getActionUrls();
}
