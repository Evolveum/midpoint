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
package com.evolveum.midpoint.model.migrator;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType.Reaction;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectSynchronizationType.Reaction.Action;
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
	
	public <I extends ObjectType, O extends ObjectType> PrismObject<O> migrate(PrismObject<I> original) {
		Class<I> origType = original.getCompileTimeClass();
		if (ObjectTemplateType.class.isAssignableFrom(origType)) {
			PrismObject<ObjectTemplateType> out = migrateObjectTemplate((PrismObject<ObjectTemplateType>) original);
			return (PrismObject<O>) out;
		}
		if (ResourceType.class.isAssignableFrom(origType)) {
			PrismObject<ResourceType> out = migrateResource((PrismObject<ResourceType>) original);
			return (PrismObject<O>) out;
		}
		if (FocusType.class.isAssignableFrom(origType)) {
			PrismObject<FocusType> out = migrateFocus((PrismObject<FocusType>) original);
			original = (PrismObject<I>) out;
		}
		if (UserType.class.isAssignableFrom(origType)) {
			PrismObject<UserType> out = migrateUser((PrismObject<UserType>) original);
			return (PrismObject<O>) out;
		}
		return (PrismObject<O>) original;
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
		migratedSchemaHandling.getAccountType().clear();
		
		SynchronizationType synchronization = migratedResourceType.getSynchronization();
		if (synchronization != null) {
			for (ObjectSynchronizationType objectSynchronization: synchronization.getObjectSynchronization()) {
				migrateObjectSynchronization(objectSynchronization);
			}
		}
		
		return migrated;
	}
	
	private void migrateObjectSynchronization(ObjectSynchronizationType sync) {
		if (sync == null || sync.getReaction() == null){
			return;
		}
		
		List<Reaction> migratedReactions = new ArrayList<Reaction>();
		for (Reaction reaction : sync.getReaction()){
			if (reaction.getAction() == null){
				continue;
			}
			List<Action> migratedAction = new ArrayList<Action>();
			for (Action action : reaction.getAction()){
				migratedAction.add(migrateAction(action));
			}
			Reaction migratedReaction = reaction.clone();
			migratedReaction.getAction().clear();
			migratedReaction.getAction().addAll(migratedAction);
			migratedReactions.add(migratedReaction);
		}
		
		sync.getReaction().clear();
		sync.getReaction().addAll(migratedReactions);
	}

	private Action migrateAction(Action action){
		List<Object> migrated = new ArrayList<Object>();
		if (action.getAny() == null){
			return action;
		}
		
		for (Object object : action.getAny()){
			if (object == null){
				continue;
			}
			if (!(object instanceof Element)) {
				continue;
			}
			Element parameter = (Element) object;
			if (QNameUtil.compareQName(new QName(SchemaConstants.NS_C, "userTemplateRef"), (Node) parameter)){
				Element e = DOMUtil.createElement(parameter.getOwnerDocument(), ObjectSynchronizationType.F_OBJECT_TEMPLATE_REF);
				Element source = (Element) parameter.cloneNode(true);
				DOMUtil.copyContent(source, e);
				migrated.add(e);
			} else{
				migrated.add(parameter);
			}
		}
		
		action.getAny().clear();
		action.getAny().addAll(migrated);
		return action;
	}
	
	private PrismObject<FocusType> migrateFocus(PrismObject<FocusType> orig) {
		FocusType origFocusType = orig.asObjectable();
		List<AssignmentType> origAssignments = origFocusType.getAssignment();
		if (origAssignments.isEmpty()) {
			return orig;
		}
		boolean hasAccountConstruction = false;
		for (AssignmentType origAssignment: origFocusType.getAssignment()) {
			if (origAssignment.getAccountConstruction() != null) {
				hasAccountConstruction = true;
			}
		}
		
		if (!hasAccountConstruction) {
			return orig;
		}
		
		PrismObject<FocusType> migrated = orig.clone();
		FocusType migratedFocusType = migrated.asObjectable();
		
		for (AssignmentType migAssignment: migratedFocusType.getAssignment()) {
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
		
		PrismObject<UserType> migrated = orig.clone();

		
		UserType migratedUserType = migrated.asObjectable();
		
		//Credentials
		if (origUserType.getCredentials() != null && origUserType.getCredentials().isAllowedIdmAdminGuiAccess() != null && origUserType.getCredentials().isAllowedIdmAdminGuiAccess()){
			AssignmentType superUserRole = new AssignmentType();
			superUserRole.setTargetRef(ObjectTypeUtil.createObjectRef(SystemObjectsType.ROLE_SUPERUSER.value(), ObjectTypes.ROLE));
			migratedUserType.getAssignment().add(superUserRole);
			
			if (origUserType.getCredentials().getPassword() == null){
				migratedUserType.setCredentials(null);
			} else{
				CredentialsType migratedCredentials = origUserType.getCredentials().clone();
				migratedCredentials.setAllowedIdmAdminGuiAccess(null);
				
				migratedUserType.setCredentials(migratedCredentials);
			}
		}
		
		// Activation
		ActivationType origActivation = origUserType.getActivation();
		if ((origActivation == null || origActivation.isEnabled() == null) && 
				(origUserType.getAccountRef().isEmpty())) {
			return migrated;
		}
		
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
