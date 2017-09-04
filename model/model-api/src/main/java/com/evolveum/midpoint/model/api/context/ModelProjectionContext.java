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
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public interface ModelProjectionContext extends ModelElementContext<ShadowType> {

	/**
	 * Returns synchronization delta.
	 *
	 * Synchronization delta describes changes that have recently happened. MidPoint reacts to these
	 * changes by "pulling them in" (e.g. using them in inbound mappings).
	 */
	public ObjectDelta<ShadowType> getSyncDelta();

	public void setSyncDelta(ObjectDelta<ShadowType> syncDelta);

    ResourceShadowDiscriminator getResourceShadowDiscriminator();

    /**
	 * Decision regarding the account. It describes the overall situation of the account e.g. whether account
	 * is added, is to be deleted, unliked, etc.
	 *
	 * If set to null no decision was made yet. Null is also a typical value when the context is created.
	 *
	 * @see SynchronizationPolicyDecision
	 */
	public SynchronizationPolicyDecision getSynchronizationPolicyDecision();

	ObjectDelta<ShadowType> getExecutableDelta() throws SchemaException;
}
