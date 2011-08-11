package com.evolveum.midpoint.init;

import java.io.InputStream;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.Holder;

import org.apache.commons.io.IOUtils;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;

public class InitialSetup {

	private static final Trace TRACE = TraceManager.getTrace(InitialSetup.class);

	private final String[] FILES_FOR_IMPORT = new String[] { "systemConfiguration.xml", "admin.xml" };

	private ModelPortType modelService;

	public InitialSetup(ModelPortType modelService) {
		super();
		this.modelService = modelService;
	}

	@SuppressWarnings("unchecked")
	public void init() {
		TRACE.info("Starting initial object import.");

		OperationResult mainResult = new OperationResult("Initial Import");
		for (String file : FILES_FOR_IMPORT) {
			OperationResult result = new OperationResult("Import Object");

			InputStream stream = null;
			try {
				stream = getResource(file);
				JAXBElement<ObjectType> element = (JAXBElement<ObjectType>) JAXBUtil.unmarshal(stream);
				ObjectType object = element.getValue();
				Holder<OperationResultType> holder = new Holder<OperationResultType>(
						result.createOperationResultType());

				boolean importObject = true;
				try {
					modelService.getObject(object.getOid(), new PropertyReferenceListType(), holder);
					importObject = false;
					result = OperationResult.createOperationResult(holder.value);
					result.recordSuccess();
				} catch (FaultMessage ex) {
					if (ex.getFaultInfo() instanceof ObjectNotFoundFaultType) {
						importObject = true;
					} else {
						LoggingUtils.logException(TRACE, "Couldn't get object with oid {} from model", ex,
								object.getOid());

						OperationResultType resultType = (ex.getFaultInfo() != null && ex.getFaultInfo()
								.getOperationResult() == null) ? holder.value : ex.getFaultInfo()
								.getOperationResult();
						result = OperationResult.createOperationResult(resultType);
						result.recordWarning("Couldn't get object with oid '" + object.getOid()
								+ "' from model", ex);
					}
				}

				if (!importObject) {
					continue;
				}

				holder = new Holder<OperationResultType>(result.createOperationResultType());
				modelService.addObject(object, holder);
				result = OperationResult.createOperationResult(holder.value);
				result.recordSuccess();
			} catch (Exception ex) {
				LoggingUtils.logException(TRACE, "Couldn't import file {}", ex, file);
				result.recordFatalError("Couldn't import file '" + file + "'", ex);
			} finally {
				if (stream != null) {
					IOUtils.closeQuietly(stream);
				}
				mainResult.addSubresult(result);
			}
		}
		mainResult.recordSuccess();

		TRACE.info("Import status:\n" + mainResult.dump());
		TRACE.info("Initial object import finished.");
	}

	private InputStream getResource(String name) {
		return InitialSetup.class.getClassLoader().getResourceAsStream(name);
	}
}
