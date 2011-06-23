package com.evolveum.midpoint.model.handling;

import java.util.List;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

public interface SchemaHandler {

	public ObjectModificationType processInboundHandling(UserType user,
			ResourceObjectShadowType resourceObjectShadow, OperationResult result);

	public List<ObjectModificationType> processOutboundHandling(UserType user,
			List<ResourceObjectShadowType> resourceObjectShadows, OperationResult result);

	public void processUserTemplate(UserType user, String userTemplateOid, OperationResult result);
}
