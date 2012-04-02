package com.evolveum.midpoint.web.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.SystemManager;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.SystemConfigurationDto;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;

public class SystemManagerImpl extends ObjectManagerImpl<SystemConfigurationType, SystemConfigurationDto>
        implements SystemManager {

    private static final long serialVersionUID = 7510934216789096238L;
    private static final Trace LOGGER = TraceManager.getTrace(SystemManager.class);
    
    @Autowired(required = true)
    TaskManager taskManager;

    @Override
    public Collection<SystemConfigurationDto> list(PagingType paging) {
        SystemConfigurationType config;
        try {
            PrismObject<SystemConfigurationType> object = get(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), new PropertyReferenceListType());
            config = object.asObjectable();
        } catch (ObjectNotFoundException ex) {
            // TODO: error handling
            throw new SystemException(ex);
        }

        Collection<SystemConfigurationDto> collection = new ArrayList<SystemConfigurationDto>();
        if (config != null) {
            collection.add(createObject(config));
        }

        return collection;
    }

    @Override
    protected Class<? extends ObjectType> getSupportedObjectClass() {
        return SystemConfigurationType.class;
    }

    @Override
    protected SystemConfigurationDto createObject(SystemConfigurationType objectType) {
        return new SystemConfigurationDto(objectType);
    }

    @Override
    public Set<PropertyChange> submit(SystemConfigurationDto changedObject, Task task, OperationResult parentResult) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public boolean updateLoggingConfiguration(LoggingConfigurationType configuration) {
        boolean updated = false;
        Task task = taskManager.createTaskInstance(UPDATE_LOGGING_CONFIGURATION);
        OperationResult result = task.getResult();
        try {
        	String oid = SystemObjectsType.SYSTEM_CONFIGURATION.value();
        	SystemConfigurationType oldConfig = getModel().getObject(SystemConfigurationType.class, oid, null, result).asObjectable();
        	PrismObject<SystemConfigurationType> oldPrism = oldConfig.asPrismObject();
        	PrismObject<SystemConfigurationType> newPrism = oldPrism.clone();
        	newPrism.asObjectable().setLogging(configuration);
        	
        	ObjectDelta<SystemConfigurationType> delta = DiffUtil.diff(oldPrism,newPrism);
        	getModel().modifyObject(SystemConfigurationType.class, delta.getOid(), delta.getModifications(), task, result);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return updated;
    }

    @Override
    public LoggingConfigurationType getLoggingConfiguration(OperationResult result) {
    	SystemConfigurationType system = getSystemConfiguration(result);
    	return system.getLogging();
    }
    
    private SystemConfigurationType getSystemConfiguration(OperationResult result) {
		final OperationResult getSystemConfigResult = result.createSubresult("Get System Configuration");

		SystemConfigurationType config = null;
		try {
			PrismObject<SystemConfigurationType> object = getModel().getObject(SystemConfigurationType.class,
					SystemObjectsType.SYSTEM_CONFIGURATION.value(), new PropertyReferenceListType(),
					getSystemConfigResult);
			config = object.asObjectable();

			getSystemConfigResult.recordSuccess();
		} catch (Exception ex) {
			String message = "Couldn't get system configuration";
			LoggingUtils.logException(LOGGER, message, ex);

			getSystemConfigResult.recordFatalError(message, ex);
		} finally {
			getSystemConfigResult.computeStatus();
		}

		if (config == null) {
			config = new SystemConfigurationType();
		}

		return config;
	}
}
