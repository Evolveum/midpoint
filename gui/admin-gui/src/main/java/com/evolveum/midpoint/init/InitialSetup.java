package com.evolveum.midpoint.init;

import java.io.InputStream;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;

import org.apache.commons.io.IOUtils;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.util.Utils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

public class InitialSetup {

	private static final Trace TRACE = TraceManager.getTrace(InitialSetup.class);

	private ModelPortType modelService;

	public InitialSetup(ModelPortType modelService) {
		super();
		this.modelService = modelService;
	}

	@SuppressWarnings("unchecked")
	public void init() {
		TRACE.info("Starting initial object import.");

		OperationResultType resultType = new OperationResultType();
		resultType.setOperation("Initial Import");

		InputStream stream = null;
		try {
			stream = getResource("admin.xml");
			JAXBElement<UserType> adminUser = (JAXBElement<UserType>) JAXBUtil.unmarshal(stream);
			modelService.addObject(adminUser.getValue(), new Holder<OperationResultType>(resultType));
		} catch (JAXBException e) {
			TRACE.error("Failed to process initial configuration for user administrator", e);
		} catch (FaultMessage e) {
			TRACE.error("Failed to import initial configuration for user administrator", e);
		} catch (RuntimeException e) {
			TRACE.error("Unexpected exception while processing initial configuration", e);
		} finally {
			if (stream != null) {
				IOUtils.closeQuietly(stream);
			}
		}

		try {
			stream = getResource("systemConfiguration.xml");
			JAXBElement<SystemConfigurationType> config = (JAXBElement<SystemConfigurationType>) JAXBUtil
					.unmarshal(stream);
			modelService.addObject(config.getValue(), new Holder<OperationResultType>(resultType));
		} catch (Exception ex) {
			Utils.logException(TRACE, "Couldn't import system configuration.", ex);
		} finally {
			if (stream != null) {
				IOUtils.closeQuietly(stream);
			}
		}

		OperationResult result = OperationResult.createOperationResult(resultType);
		TRACE.info("Import status:\n" + result.debugDump());
		TRACE.info("Initial object import finished.");
	}

	private InputStream getResource(String name) {
		return InitialSetup.class.getClassLoader().getResourceAsStream(name);
	}
}
