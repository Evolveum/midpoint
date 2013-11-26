package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ResourceEventDescription;
import com.evolveum.midpoint.provisioning.api.ResourceEventListener;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

public class ResourceEventListenerImpl implements ResourceEventListener{

	private ShadowCache shadowCache;
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void notifyEvent(ResourceEventDescription eventDescription, Task task, OperationResult parentResult) {
		
		Validate.notNull(eventDescription, "Event description must not be null.");
		Validate.notNull(task, "Task must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null");
		
		PrismObject<ShadowType> shadow = null;
		
		if (eventDescription.getCurrentShadow() != null){
			shadow = eventDescription.getCurrentShadow();
		} else if (eventDescription.getOldShadow() != null){
			shadow = eventDescription.getOldShadow();
		} else if (eventDescription.getDelta() != null && eventDescription.getDelta().isAdd()){
			if (eventDescription.getDelta().getObjectToAdd() == null){
				throw new IllegalStateException("Found ADD delta, but no object to add was specified.");
			}
			shadow = eventDescription.getDelta().getObjectToAdd();
		} else{
			
			throw new IllegalStateException("Resource event description does not contain neither old shadow, nor current shadow, nor shadow in delta");
		}
		
		ShadowType shadowType = shadow.asObjectable();
	
		
		try {
			ResourceType resource = shadowCache.getResource(shadow, parentResult);
		} catch (ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(shadow);
		
		if (eventDescription.getCurrentShadow() == null && eventDescription.getDelta() == null){
			throw new IllegalStateException("Neither current shadow, nor delta specified. It is required to have at least one of them specified.");
		}
	
	}

}
