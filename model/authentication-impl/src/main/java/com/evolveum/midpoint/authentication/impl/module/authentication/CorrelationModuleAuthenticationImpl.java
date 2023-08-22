/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.CorrelationModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.util.ModuleType;
import com.evolveum.midpoint.model.api.correlator.CandidateOwner;
import com.evolveum.midpoint.model.api.correlator.CandidateOwnersMap;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class CorrelationModuleAuthenticationImpl extends ModuleAuthenticationImpl implements CorrelationModuleAuthentication {

    private List<CorrelationModuleConfigurationType> correlators;
    private int currentProcessingCorrelator;

    private CandidateOwnersMap candidateOwners = new CandidateOwnersMap();
    private final List<ObjectType> owners = new ArrayList<>();

    private FocusType preFocus;
    private Map<ItemPath, String> processedAttributes = new HashMap<>();

    public CorrelationModuleAuthenticationImpl(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.CORRELATION, sequenceModule);
        setType(ModuleType.LOCAL);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
    }

    public ModuleAuthenticationImpl clone() {
        CorrelationModuleAuthenticationImpl module = new CorrelationModuleAuthenticationImpl(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        module.setCorrelators(this.correlators);
        super.clone(module);
        return module;
    }

    public void setCorrelators(List<CorrelationModuleConfigurationType> correlators) {
        //todo sort by order
        this.correlators = correlators;
    }

    @Override
    public String getCurrentCorrelatorIdentifier() {
        if (isEmptyCorrelatorsList()) {
            return null;    //todo what if correlator identifier isn't specified?
        }
        return correlators.get(currentProcessingCorrelator).getCorrelatorIdentifier();
    }

    public boolean isLastCorrelator() {
        return currentProcessingCorrelator == correlators.size() - 1;
    }

    public void setNextCorrelator() {
        currentProcessingCorrelator++;
    }

    public boolean isEmptyCorrelatorsList() {
        return CollectionUtils.isEmpty(correlators);
    }

    public void addCandidateOwners(CandidateOwnersMap map) {
        candidateOwners.mergeWith(map);
    }

    public Set<String> getCandidateOids() {
        return candidateOwners.values()
                .stream()
                .map(CandidateOwner::getOid)
                .collect(Collectors.toSet());
    }

    public void addOwner(ObjectType owner) {
        owners.add(owner);
    }

    public List<ObjectType> getOwners() {
        return Collections.unmodifiableList(owners);
    }

    public void addAttributes(Map<ItemPath, String> attributes) {
        attributes.forEach((k, v) -> this.processedAttributes.putIfAbsent(k, v));
    }

    public Map<ItemPath, String> getProcessedAttributes() {
        return processedAttributes;
    }

    public void setPreFocus(FocusType preFocus) {
        this.preFocus = preFocus;
    }

    public FocusType getPreFocus() {
        return preFocus;
    }

}
