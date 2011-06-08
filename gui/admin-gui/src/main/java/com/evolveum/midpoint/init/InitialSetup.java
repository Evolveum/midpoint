package com.evolveum.midpoint.init;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

public class InitialSetup {

	private static final Trace TRACE = TraceManager.getTrace(InitialSetup.class);

	ModelPortType modelService;

	public InitialSetup(ModelPortType modelService) {
		super();
		this.modelService = modelService;
	}

	public void init() {
		try {
			JAXBElement<UserType> adminUser = (JAXBElement<UserType>) JAXBUtil.unmarshal(InitialSetup.class
					.getClassLoader().getResourceAsStream("admin.xml"));
			modelService.addObject(adminUser.getValue(), new Holder(new OperationalResultType()));
		} catch (JAXBException e) {
			TRACE.error("Failed to process initial configuration for user administrator", e);
		} catch (FaultMessage e) {
			TRACE.error("Failed to import initial configuration for user administrator", e);
		} catch (RuntimeException e) {
			TRACE.error("Unexpected exception while processing initial configuration", e);
		}

	}

}
