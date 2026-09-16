/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.mappings.tasks;

import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Definition of mapping simulation (either inbound or outbound).
 *
 * In case of simulation, both inbound and outbound simulations have some common ground, which is described by this
 * interface. E.g. Each of them can provide mappings which should be simulated, or oid of the relevant resource.
 *
 * Even though this interface in the context of activities (in which the simulation is implemented) is not
 * necessarily needed, it seems to be useful, and it quite nicely groups definitions with similar purpose (mappings
 * simulation)
 *
 * @param <T> The parameter of the actual mapping type (i.e. inbound or outbound mapping).
 */
public interface MappingSimulationWorkDef<T extends MappingType> extends WorkDefinition {

    /**
     * Provide explicitly defined mappings which should be simulated.
     *
     * This method does not enforce to return all mappings which should be simulated. I.e. if the {@link
     * #excludeExistingMappings()} returns {@code false}, then the provided mappings should be merged with mappings
     * already present on the resource. This will most likely change in the future.
     *
     * @return The list of explicitly defined mappings.
     */
    Map<ItemPath, List<T>> provideMappings();

    /**
     * Tells if the applicable mappings already present in the resource should be excluded or not.
     *
     * If this method returns {@code false}, then the mappings provided by {@link #provideMappings()} should be
     * merged with the applicable mappings already present on the resource.
     *
     * @return Whether the existing applicable mappings should be excluded.
     */
    boolean excludeExistingMappings();

    /**
     * Return OID of the resource on which we want to simulate the mappings.
     *
     * @return The OID of the resource to simulate on.
     */
    String resourceOid();

    /**
     * Resolves the identification of the resource object type which should be used for the simulation.
     *
     * @return The ID of the object type to simulate on.
     */
    ResourceObjectTypeIdentification resolveObjectTypeId();

    /**
     * Factory method which returns correct implementation of this interface.
     *
     * @param info The work definition info which contains work definition bean.
     * @return The instance of a particular implementation of this interface.
     */
    static MappingSimulationWorkDef<? extends MappingType> of(WorkDefinitionFactory.WorkDefinitionInfo info) {
        final AbstractWorkDefinitionType workDefBean = info.getBean();
        if (workDefBean instanceof InboundMappingsSimulationWorkDefType inboundSimulationDef) {
            return new InboundMappingSimulationWorkDef(info, inboundSimulationDef);
        } else if (workDefBean instanceof OutboundMappingsSimulationWorkDefType outboundSimulationDef) {
            return new OutboundMappingSimulationWorkDef(info, outboundSimulationDef);
        }

        throw new IllegalArgumentException("Unexpected type of the work definition type: " + workDefBean.getClass());
    }
}
