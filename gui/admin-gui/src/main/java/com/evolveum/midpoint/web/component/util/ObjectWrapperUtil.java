package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ObjectWrapperUtil {

	
	public static <O extends ObjectType> ObjectWrapper createObjectWrapper(String displayName, String description, PrismObject<O> object, ContainerStatus status, PageBase pageBase) {
		try {
		PrismObjectDefinition editedDefinition = pageBase.getModelInteractionService().getEditObjectDefinition(object);
		ObjectWrapper wrapper = new ObjectWrapper(displayName, description, object, editedDefinition, status);
		return wrapper;
		} catch (SchemaException ex){
			throw new SystemException(ex);
		}
	}
}
