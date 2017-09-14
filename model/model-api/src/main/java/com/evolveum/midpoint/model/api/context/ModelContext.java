/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.model.api.context;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author semancik
 *
 */
public interface ModelContext<F extends ObjectType> extends Serializable, DebugDumpable {

	ModelState getState();

	ModelElementContext<F> getFocusContext();

	Collection<? extends ModelProjectionContext> getProjectionContexts();

	ModelProjectionContext findProjectionContext(ResourceShadowDiscriminator rat);

	ModelExecuteOptions getOptions();

	@NotNull
	PartialProcessingOptionsType getPartialProcessingOptions();

	Class<F> getFocusClass();

	void reportProgress(ProgressInformation progress);

    DeltaSetTriple<? extends EvaluatedAssignment<?>> getEvaluatedAssignmentTriple();

    PrismContext getPrismContext();       // use with care

    PrismObject<SystemConfigurationType> getSystemConfiguration();  // beware, may be null - use only as a performance optimization

    String getChannel();

	// For diagnostic purposes (this is more detailed than rule-related part of LensContext debugDump,
	// while less detailed than that part of detailed LensContext debugDump).
	default String dumpAssignmentPolicyRules(int indent) {
		return dumpAssignmentPolicyRules(indent, false);
	}

	String dumpAssignmentPolicyRules(int indent, boolean alsoMessages);

	default String dumpFocusPolicyRules(int indent) {
		return dumpFocusPolicyRules(indent, false);
	}

	String dumpFocusPolicyRules(int indent, boolean alsoMessages);

	Map<String, Collection<Containerable>> getHookPreviewResultsMap();

	@NotNull
	<T> List<T> getHookPreviewResults(@NotNull Class<T> clazz);

	@Nullable
	<T> T getHookPreviewResult(@NotNull Class<T> clazz);
}
