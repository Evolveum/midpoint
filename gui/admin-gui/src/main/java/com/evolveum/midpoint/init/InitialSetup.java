package com.evolveum.midpoint.init;

import java.io.InputStream;

import javax.xml.bind.JAXBElement;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;

public class InitialSetup {

	private static final Trace TRACE = TraceManager.getTrace(InitialSetup.class);

	private final String[] FILES_FOR_IMPORT = new String[] { "systemConfiguration.xml", "admin.xml" };

	private ModelService model;
	
	public void setModel(ModelService model) {
		Validate.notNull(model, "Model service must not be null.");
		this.model = model;
	}

	@SuppressWarnings("unchecked")
	public void init() {
		TRACE.info("Starting initial object import.");

		OperationResult mainResult = new OperationResult("Initialisation");
		for (String file : FILES_FOR_IMPORT) {
			OperationResult result = mainResult.createSubresult("Import Object");

			InputStream stream = null;
			try {
				stream = getResource(file);
				JAXBElement<ObjectType> element = (JAXBElement<ObjectType>) JAXBUtil.unmarshal(stream);
				ObjectType object = element.getValue();

				boolean importObject = true;
				try {
					model.getObject(object.getOid(), new PropertyReferenceListType(), object.getClass(),
							result);
					importObject = false;
					result.recordSuccess();
				} catch (ObjectNotFoundException ex) {
					importObject = true;
				} catch (Exception ex) {
					LoggingUtils.logException(TRACE, "Couldn't get object with oid {} from model", ex,
							object.getOid());
					result.recordWarning("Couldn't get object with oid '" + object.getOid() + "' from model",
							ex);
				}

				if (!importObject) {
					continue;
				}

				model.addObject(object, result);
				result.recordSuccess();
			} catch (Exception ex) {
				LoggingUtils.logException(TRACE, "Couldn't import file {}", ex, file);
				result.recordFatalError("Couldn't import file '" + file + "'", ex);
			} finally {
				if (stream != null) {
					IOUtils.closeQuietly(stream);
				}
				result.computeStatus();
			}
		}
		mainResult.recordSuccess();
		TRACE.info("Initial object import finished.");

		TRACE.info("Model post initialization.");
		try {
			model.postInit(mainResult);
			TRACE.info("Model post initialization finished successful.");
		} catch (Exception ex) {
			LoggingUtils.logException(TRACE, "Model post initialization failed", ex);
		}

		TRACE.info("Initialization status:\n" + mainResult.dump());
	}

	private InputStream getResource(String name) {
		return InitialSetup.class.getClassLoader().getResourceAsStream(name);
	}
}
