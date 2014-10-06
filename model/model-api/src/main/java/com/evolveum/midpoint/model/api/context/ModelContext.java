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
package com.evolveum.midpoint.model.api.context;

import java.io.Serializable;
import java.util.Collection;

import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public interface ModelContext<F extends ObjectType> extends Serializable, DebugDumpable {

	public ModelState getState();
	
	public ModelElementContext<F> getFocusContext();
	
	public Collection<? extends ModelProjectionContext> getProjectionContexts();
	
	public ModelProjectionContext findProjectionContext(ResourceShadowDiscriminator rat);

    Class<F> getFocusClass();

    void reportProgress(ProgressInformation progress);

    PrismContext getPrismContext();       // use with care
}
