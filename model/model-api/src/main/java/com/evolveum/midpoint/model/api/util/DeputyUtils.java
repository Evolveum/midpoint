/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Experimental. TODO implement correctly
 *
 * @author mederly
 */
public class DeputyUtils {

	public static boolean isDelegationRelation(QName relation) {
		return QNameUtil.match(relation, SchemaConstants.ORG_DEPUTY);
	}

	// temporary implementation
	@NotNull
	public static Collection<PrismReferenceValue> getDelegatorReferences(@NotNull UserType user) {
		List<PrismReferenceValue> rv = new ArrayList<>();
		for (AssignmentType assignment : user.getAssignment()) {
			if (assignment.getTargetRef() != null
					&& isDelegationRelation(assignment.getTargetRef().getRelation())
					&& (assignment.getActivation() == null
					|| assignment.getActivation().getEffectiveStatus() == null
					|| assignment.getActivation().getEffectiveStatus() == ActivationStatusType.ENABLED)) {
				rv.add(assignment.getTargetRef().asReferenceValue().clone());
			}
		}
		return rv;
	}

	@NotNull
	public static Collection<String> getDelegatorOids(@NotNull UserType user) {
		return getDelegatorReferences(user).stream()
				.map(PrismReferenceValue::getOid)
				.collect(Collectors.toList());
	}

	public static boolean isDelegationPresent(@NotNull UserType deputy, @NotNull String delegatorOid) {
		return getDelegatorOids(deputy).contains(delegatorOid);
	}
}
