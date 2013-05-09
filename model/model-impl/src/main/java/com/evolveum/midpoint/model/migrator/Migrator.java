/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.model.migrator;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class Migrator {
	
	public <I extends ObjectType, O extends ObjectType> TypedPrismObject<O> migrate(TypedPrismObject<I> original) {
		Class<I> origType = original.getType();
		if (ObjectTemplateType.class.isAssignableFrom(origType)) {
			PrismObject<ObjectTemplateType> out = migrateObjectTemplate((PrismObject<ObjectTemplateType>) original.getObject());
			return (TypedPrismObject<O>) new TypedPrismObject<ObjectTemplateType>((Class<ObjectTemplateType>) origType, out);
		}
		if (ResourceType.class.isAssignableFrom(origType)) {
			PrismObject<ResourceType> out = migrateResource((PrismObject<ResourceType>) original.getObject());
			return (TypedPrismObject<O>) new TypedPrismObject<ResourceType>((Class<ResourceType>) origType, out);
		}
		if (AbstractRoleType.class.isAssignableFrom(origType)) {
			PrismObject<AbstractRoleType> out = migrateAbstractRole((PrismObject<AbstractRoleType>) original.getObject());
			return (TypedPrismObject<O>) new TypedPrismObject<AbstractRoleType>((Class<AbstractRoleType>) origType, out);
		}
		if (UserType.class.isAssignableFrom(origType)) {
			PrismObject<UserType> out = migrateUser((PrismObject<UserType>) original.getObject());
			return (TypedPrismObject<O>) new TypedPrismObject<UserType>((Class<UserType>) origType, out);
		}
		return (TypedPrismObject<O>) original;
	}
	
	private PrismObject<ObjectTemplateType> migrateObjectTemplate(PrismObject<ObjectTemplateType> orig) {
		QName elementName = orig.getName();
		if (elementName.equals(SchemaConstants.C_OBJECT_TEMPLATE)) {
			return orig;
		}
		PrismObject<ObjectTemplateType> migrated = orig.clone();
		migrated.setName(SchemaConstants.C_OBJECT_TEMPLATE);
		return migrated;
	}

	private PrismObject<ResourceType> migrateResource(PrismObject<ResourceType> orig) {
		ResourceType origResourceType = orig.asObjectable();
		SchemaHandlingType origSchemaHandling = origResourceType.getSchemaHandling();
		if (origSchemaHandling == null) {
			return orig;
		}
		if (origSchemaHandling.getAccountType().isEmpty()) {
			return orig;
		}
		
		PrismObject<ResourceType> migrated = orig.clone();
		ResourceType migratedResourceType = migrated.asObjectable();
		SchemaHandlingType migratedSchemaHandling = migratedResourceType.getSchemaHandling();
		for (ResourceObjectTypeDefinitionType accountType: origSchemaHandling.getAccountType()) {
			accountType.setKind(ShadowKindType.ACCOUNT);
			String intent = accountType.getName();
			if (intent != null) {
				accountType.setIntent(intent);
				accountType.setName(null);
			}
			migratedSchemaHandling.getObjectType().add(accountType);
		}
		origSchemaHandling.getAccountType().clear();
		
		return migrated;
	}
	
	private PrismObject<AbstractRoleType> migrateAbstractRole(PrismObject<AbstractRoleType> orig) {
		AbstractRoleType origARoleType = orig.asObjectable();
		List<AssignmentType> origAssignments = origARoleType.getAssignment();
		if (origAssignments.isEmpty()) {
			return orig;
		}
		boolean hasAccountConstruction = false;
		for (AssignmentType origAssignment: origARoleType.getAssignment()) {
			if (origAssignment.getAccountConstruction() != null) {
				hasAccountConstruction = true;
			}
		}
		
		if (!hasAccountConstruction) {
			return orig;
		}
		
		PrismObject<AbstractRoleType> migrated = orig.clone();
		AbstractRoleType migratedARoleType = migrated.asObjectable();
		
		for (AssignmentType migAssignment: migratedARoleType.getAssignment()) {
			if (migAssignment.getAccountConstruction() != null) {
				ConstructionType accountConstruction = migAssignment.getAccountConstruction();
				accountConstruction.setKind(ShadowKindType.ACCOUNT);
				migAssignment.setConstruction(accountConstruction);
				migAssignment.setAccountConstruction(null);
			}
		}
		
		return migrated;
	}
	
	private PrismObject<UserType> migrateUser(PrismObject<UserType> orig) {
		UserType origUserType = orig.asObjectable();
		ActivationType origActivation = origUserType.getActivation();
		if ((origActivation == null || origActivation.isEnabled() == null) && 
				(origUserType.getAccountRef().isEmpty())) {
			return orig;
		}
		
		PrismObject<UserType> migrated = orig.clone();
		
		// Activation
		UserType migratedUserType = migrated.asObjectable();
		ActivationType activationType = migratedUserType.getActivation();
		if (activationType.getAdministrativeStatus() == null) {
			if (activationType.isEnabled()) {
				activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
			} else {
				activationType.setAdministrativeStatus(ActivationStatusType.DISABLED);
			}
		}
		activationType.setEnabled(null);
		
		// accountRef
		for (ObjectReferenceType accountRef: migratedUserType.getAccountRef()) {
			ObjectReferenceType linkRef = new ObjectReferenceType();
			linkRef.setOid(accountRef.getOid());
			migratedUserType.getLinkRef().add(linkRef);
		}
		migratedUserType.getAccountRef().clear();
		
		return migrated;
	}

}
