package com.evolveum.midpoint.model.impl.simulation;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
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
        var ret = new SimulationResultType()
                .useOwnPartitionForProcessedObjects(currentConfiguration.isUseOwnPartitionForProcessedObjects());
        return ret.asPrismObject();
    }


    @Override
    public void update(@Nullable SystemConfigurationType value) {
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

}
