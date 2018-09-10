/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * TODO think about precise place of this interface
 *
 * @author mederly
 */
public interface RelationRegistry {

	List<RelationDefinitionType> getRelationDefinitions();

	RelationDefinitionType getRelationDefinition(QName relation);

	// SchemaException is thrown as SystemException as it really should not occur

	boolean isOfKind(QName relation, RelationKindType kind);

	boolean isManager(QName relation);

	boolean isDelegation(QName relation);

	boolean isMembership(QName relation);

	boolean isOwner(QName relation);

	boolean isApprover(QName relation);

	boolean processRelationOnLogin(QName relation);

	boolean processRelationOnRecompute(QName relation);

	boolean includeIntoParentOrgRef(QName relation);

	QName getDefaultRelation();

	@NotNull
	Collection<QName> getAllRelationsFor(RelationKindType kind);

	QName getDefaultRelationFor(RelationKindType kind);

	@NotNull
	QName normalizeRelation(QName relation);

	void applyRelationConfiguration(SystemConfigurationType relationsDefinition);

	boolean isDefault(QName relation);

	/**
	 * Returns aliases of a relation. Currently these are:
	 *  - unnormalized version of the relation QNme
	 *  - null if the relation is the default one
	 *
	 * In the future we might return some other values (e.g. explicitly configured aliases) as well.
	 * But we would need to adapt prismContext.relationsEquivalent method and other comparison methods as well.
	 * It is perhaps not worth the effort.
	 */
	@NotNull
	Collection<QName> getAliases(QName relation);
}
