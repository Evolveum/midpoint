package com.evolveum.midpoint.model.impl.simulation;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationSimulationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;


@Component
public class SimulationResultManagerImpl implements SimulationResultManager, SystemConfigurationChangeListener {

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repository;

    @Autowired
    private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    private SystemConfigurationSimulationType currentConfiguration;

    public PrismObject<SimulationResultType> newWithDefaults() {
        var ret = new SimulationResultType();
        ret.name("Simulation Result: " + System.currentTimeMillis());
                //.useOwnPartitionForProcessedObjects(currentConfiguration.isUseOwnPartitionForProcessedObjects());
        return ret.asPrismObject();
    }

    @Override
    public SimulationResultType newConfiguration() {
        return newWithDefaults().asObjectable();
    }

    @Override
    public SimulationResultContext newSimulationResult(@Nullable SimulationResultType configuration, @NotNull OperationResult parentResult) {
        // TODO: Check if this is configuration
        configuration = mergeDefaults(configuration);

        @NotNull
        String storedOid;
        try {
            storedOid = repository.addObject(configuration.asPrismObject(), null, parentResult);
        } catch (ObjectAlreadyExistsException | SchemaException e) {
            throw new SystemException(e);
        }

        return new SimulationResultContextImpl(this, storedOid, configuration);
    }


    private @Nullable SimulationResultType mergeDefaults(@Nullable SimulationResultType configuration) {
        // FIXME: Merge somehow
        return configuration;
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        if (value == null) {
            currentConfiguration  = new SystemConfigurationSimulationType();
            return;
        }

        // Get current simulations configuration
        currentConfiguration = value.getSimulation() != null ? value.getSimulation() : new SystemConfigurationSimulationType();
    }

    @PostConstruct
    public void init() {
        systemConfigurationChangeDispatcher.registerListener(this);
    }

    @PreDestroy
    public void shutdown() {
        systemConfigurationChangeDispatcher.unregisterListener(this);
    }

    public void storeProcessedObject(@NotNull String oid, SimulationResultProcessedObjectType processedObject, @NotNull OperationResult parentResult) {
        try {
        List<ItemDelta<?, ?>> modifications = PrismContext.get().deltaFor(SimulationResultType.class)
            .item(SimulationResultType.F_PROCESSED_OBJECT)
            .add(processedObject.asPrismContainerValue())
            .asItemDeltas()
            ;
            repository.modifyObject(SimulationResultType.class, oid, modifications, parentResult);
        } catch (Exception e) {
            // FIXME: Handle properly
            throw new SystemException(e);
        }
    }


}
