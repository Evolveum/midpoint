package com.evolveum.midpoint.model.impl.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
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
                //.useOwnPartitionForProcessedObjects(true);
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
        SystemConfigurationSimulationType simulation = value != null ? value.getSimulation() : null;
        currentConfiguration = Objects.requireNonNullElse(simulation, new SystemConfigurationSimulationType());
    }

    @PostConstruct
    public void init() {
        systemConfigurationChangeDispatcher.registerListener(this);
    }

    @PreDestroy
    public void shutdown() {
        systemConfigurationChangeDispatcher.unregisterListener(this);
    }

    void storeProcessedObject(
            @NotNull String oid, @NotNull SimulationResultProcessedObjectType processedObject, @NotNull OperationResult result) {
        try {
            List<ItemDelta<?, ?>> modifications = PrismContext.get().deltaFor(SimulationResultType.class)
                    .item(SimulationResultType.F_PROCESSED_OBJECT)
                    .add(processedObject.asPrismContainerValue())
                    .asItemDeltas();
            repository.modifyObject(SimulationResultType.class, oid, modifications, result);
        } catch (Exception e) {
            // FIXME: Handle properly
            throw new SystemException(e);
        }
    }

    /** TEMPORARY. Retrieves stored deltas. May be replaced by something more general in the future. */
    @NotNull Collection<ProcessedObject<?>> getStoredProcessedObjects(@NotNull String oid, OperationResult result)
            throws SchemaException {
        ObjectQuery query = PrismContext.get().queryFor(SimulationResultProcessedObjectType.class)
                .ownerId(oid)
                .build();
        List<SimulationResultProcessedObjectType> processedObjectBeans =
                repository.searchContainers(SimulationResultProcessedObjectType.class, query, null, result);
        Collection<ProcessedObject<?>> processedObjects = new ArrayList<>();
        for (SimulationResultProcessedObjectType processedObjectBean : processedObjectBeans) {
            processedObjects.add(
                    ProcessedObject.parse(processedObjectBean));
        }
        return processedObjects;
    }

    @Override
    public SimulationResultContext newSimulationContext(@NotNull String resultOid) {
        return new SimulationResultContextImpl(this, resultOid, new SimulationResultType()); // FIXME (config)
    }
}
