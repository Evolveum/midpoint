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

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_DEFAULT;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

/**
 * Temporary workaround: in TestRefinedSchema we don't have spring context.
 * @author mederly
 */
class RelationRegistryDummyImpl implements RelationRegistry {

	@Override
	public List<RelationDefinitionType> getRelationDefinitions() {
		return emptyList();
	}

	@Override
	public RelationDefinitionType getRelationDefinition(QName relation) {
		return null;
	}

	@Override
	public boolean isOfKind(QName relation, RelationKindType kind) {
		return kind == RelationKindType.MEMBERSHIP && (relation == null || QNameUtil.match(relation, ORG_DEFAULT));
	}

	@Override
	public boolean isManager(QName relation) {
		return false;
	}

	@Override
	public boolean isDelegation(QName relation) {
		return false;
	}

	@Override
	public boolean isMembership(QName relation) {
		return isOfKind(relation, RelationKindType.MEMBERSHIP);
	}

	@Override
	public boolean isOwner(QName relation) {
		return false;
	}

	@Override
	public boolean isApprover(QName relation) {
		return false;
	}

	@Override
	public boolean processRelationOnLogin(QName relation) {
		return false;
	}

	@Override
	public boolean processRelationOnRecompute(QName relation) {
		return false;
	}

	@Override
	public boolean includeIntoParentOrgRef(QName relation) {
		return false;
	}

	@Override
	public QName getDefaultRelation() {
		return ORG_DEFAULT;
	}

	@NotNull
	@Override
	public Collection<QName> getAllRelationsFor(RelationKindType kind) {
		return kind == RelationKindType.MEMBERSHIP ? singletonList(ORG_DEFAULT) : emptyList();
	}

	@Override
	public QName getDefaultRelationFor(RelationKindType kind) {
		return kind == RelationKindType.MEMBERSHIP ? ORG_DEFAULT : null;
	}

	@NotNull
	@Override
	public QName normalizeRelation(QName relation) {
		return relation == null ? ORG_DEFAULT : relation;
	}

	@Override
	public void applyRelationConfiguration(SystemConfigurationType relationsDefinition) {
	}

	@Override
	public boolean isDefault(QName relation) {
		return relation == null || QNameUtil.match(relation, ORG_DEFAULT);
	}

	@NotNull
	@Override
	public Collection<QName> getAliases(QName relation) {
		return singleton(relation);
	}
}
