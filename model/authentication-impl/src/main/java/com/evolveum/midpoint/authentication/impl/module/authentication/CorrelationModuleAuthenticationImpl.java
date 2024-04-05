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
import com.evolveum.midpoint.model.api.correlator.CandidateOwners;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class CorrelationModuleAuthenticationImpl extends ModuleAuthenticationImpl implements CorrelationModuleAuthentication {

    private List<CorrelationModuleConfigurationType> correlators;
    private int currentProcessingCorrelator;

    private CandidateOwners candidateOwners = new CandidateOwners();
    private final List<ObjectType> owners = new ArrayList<>();

    private FocusType preFocus;
    private Map<ItemPath, String> processedAttributes = new HashMap<>();

    private Integer correlationMaxUsersNumber;

    public CorrelationModuleAuthenticationImpl(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.CORRELATION, sequenceModule);
        setType(ModuleType.LOCAL);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
    }

    public ModuleAuthenticationImpl clone() {
        CorrelationModuleAuthenticationImpl module = new CorrelationModuleAuthenticationImpl(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        module.setCorrelators(this.correlators);
        module.setCorrelationMaxUsersNumber(this.correlationMaxUsersNumber);
        super.clone(module);
        return module;
    }

    public void setCorrelators(List<CorrelationModuleConfigurationType> correlators) {
        this.correlators = new ArrayList<>(correlators);
        sortCorrelators();
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

    public boolean currentCorrelatorIndexEquals(int expectedValue) {
        return currentProcessingCorrelator == expectedValue;
    }

    public void setNextCorrelator() {
        currentProcessingCorrelator++;
    }

    public boolean isEmptyCorrelatorsList() {
        return CollectionUtils.isEmpty(correlators);
    }

    public void rewriteCandidateOwners(CandidateOwners newOwners) {
        candidateOwners.replaceWith(newOwners);
    }

    public Set<String> getCandidateOids() {
        return candidateOwners.getCandidateOids();
    }

    public void rewriteOwner(ObjectType owner) {
        rewriteOwners(Collections.singletonList(owner));
    }

   public void rewriteOwners(List<ObjectType> owners) {
       clearOwners();
       if (owners != null) {
           owners.forEach(this::addOwnerIfNotExist);
       }
    }

    public void clearOwners() {
        owners.clear();
    }

    public void addOwnerIfNotExist(ObjectType owner) {
        if (owner == null) {
            return;
        }
        if (!ownerAlreadyExist(owner)) {
            owners.add(owner);
        }
    }

    private boolean ownerAlreadyExist(ObjectType owner) {
        return owners.stream()
                .anyMatch(o -> owner.getOid().equals(o.getOid()));
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

    public int getCurrentCorrelatorIndex() {
        return currentProcessingCorrelator;
    }

    public boolean isCorrelationMaxUsersNumberSet() {
        return correlationMaxUsersNumber != null;
    }

    public Integer getCorrelationMaxUsersNumber() {
        return correlationMaxUsersNumber;
    }

    public void setCorrelationMaxUsersNumber(Integer correlationMaxUsersNumber) {
        this.correlationMaxUsersNumber = correlationMaxUsersNumber;
    }

    private void sortCorrelators() {
        if (correlators != null && correlators.size() > 2) {
            correlators.sort(this::compareCorrelatorsByOrder);
        }
    }

    private int compareCorrelatorsByOrder(CorrelationModuleConfigurationType module1, CorrelationModuleConfigurationType module2) {
        int order1 = module1.getOrder();
        int order2 = module2.getOrder();

        return Integer.compare(order1, order2);
    }
}
