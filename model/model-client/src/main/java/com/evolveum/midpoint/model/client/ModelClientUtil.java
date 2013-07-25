/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.model.client;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * @author Radovan Semancik
 *
 */
public class ModelClientUtil {
	
	public static JAXBContext instantiateJaxbContext() throws JAXBException {
		return JAXBContext.newInstance("com.evolveum.midpoint.xml.ns._public.common.api_types_2:" +
				"com.evolveum.midpoint.xml.ns._public.common.common_2a:" +
				"com.evolveum.midpoint.xml.ns._public.common.fault_1:" +
				"com.evolveum.midpoint.xml.ns._public.communication.workflow_1:" +
				"com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_2:" +
				"com.evolveum.midpoint.xml.ns._public.connector.icf_1.resource_schema_2:" +
				"com.evolveum.midpoint.xml.ns._public.resource.capabilities_2:" +
				"com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2:" +
				"com.evolveum.prism.xml.ns._public.annotation_2:" +
				"com.evolveum.prism.xml.ns._public.query_2:" +
				"com.evolveum.prism.xml.ns._public.types_2:" +
				"org.w3._2000._09.xmldsig:" +
				"org.w3._2001._04.xmlenc");
	}

}
