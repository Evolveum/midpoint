/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public interface RefinedResourceSchema extends ResourceSchema, DebugDumpable {
	List<? extends RefinedObjectClassDefinition> getRefinedDefinitions();

	List<? extends RefinedObjectClassDefinition> getRefinedDefinitions(ShadowKindType kind);

	ResourceSchema getOriginalResourceSchema();

	default RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, ShadowType shadow) {
		return getRefinedDefinition(kind, ShadowUtil.getIntent(shadow));
	}

	/**
	 * if null accountType is provided, default account definition is returned.
	 */
	RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, String intent);

	CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(ResourceShadowDiscriminator discriminator);

	CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(PrismObject<ShadowType> shadow) throws
			SchemaException;

	CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(PrismObject<ShadowType> shadow,
			Collection<QName> additionalAuxiliaryObjectClassQNames) throws SchemaException;

	CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(QName structuralObjectClassQName,
			ShadowKindType kind, String intent);

	/**
	 * If no intents are provided, default account definition is returned.
	 * We check whether there is only one relevant rOCD.
	 */
	RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, Collection<String> intents) throws SchemaException;

	RefinedObjectClassDefinition getRefinedDefinition(QName objectClassName);

	default RefinedObjectClassDefinition getDefaultRefinedDefinition(ShadowKindType kind) {
		return getRefinedDefinition(kind, (String)null);
	}

	default PrismObjectDefinition<ShadowType> getObjectDefinition(ShadowKindType kind, String intent) {
		return getRefinedDefinition(kind, intent).getObjectDefinition();
	}

	default PrismObjectDefinition<ShadowType> getObjectDefinition(ShadowKindType kind, ShadowType shadow) {
		return getObjectDefinition(kind, ShadowUtil.getIntent(shadow));
	}

	RefinedObjectClassDefinition findRefinedDefinitionByObjectClassQName(ShadowKindType kind, QName objectClass);

	@Override
    ObjectClassComplexTypeDefinition findObjectClassDefinition(QName objectClassQName);

	LayerRefinedResourceSchema forLayer(LayerType layer);

	static RefinedResourceSchema getRefinedSchema(PrismObject<ResourceType> resource) throws SchemaException {
		return RefinedResourceSchemaImpl.getRefinedSchema(resource);
	}

	static ResourceSchema getResourceSchema(PrismObject<ResourceType> resource, PrismContext prismContext)
			throws SchemaException {
		return RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
	}
}
