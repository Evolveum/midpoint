/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.*;
import java.util.function.Consumer;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationTransformer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationLimitationsType;

/**
 * Collects authorization limitations from a set of authorizations (see {@link #accept(Authorization)}) and then
 * applies them (as a filter) to a set of authorizations (see {@link #transform(Authorization)}).
 *
 * Used for creating a donor principal with limited set of authorizations.
 *
 * @author semancik
 */
public class AuthorizationLimitationsCollector implements Consumer<Authorization>, AuthorizationTransformer {

    private boolean unlimited = false;
    private final List<String> limitActions = new ArrayList<>();

    /**
     * Parsing limitation from the authorization.
     */
    @Override
    public void accept(Authorization autz) {
        if (unlimited) {
            return;
        }
        AuthorizationLimitationsType limitations = autz.getLimitations();
        if (limitations == null) {
            unlimited = true;
            return;
        }
        List<String> actions = limitations.getAction();
        if (actions.isEmpty()) {
            unlimited = true;
            return;
        }
        limitActions.addAll(actions);
    }

    /**
     * Deciding whether authorization is acceptable
     * (based on a value parsed before)
     */
    @Override
    public Collection<Authorization> transform(Authorization autz) {
        if (unlimited || allActionsAllowed(autz)) {
            return Collections.singletonList(autz);
        }
        Authorization limitedAutz = autz.clone();
        Iterator<String> actionIterator = limitedAutz.getAction().iterator();
        //noinspection Java8CollectionRemoveIf: Not all operations are supported on prism collections
        while (actionIterator.hasNext()) {
            String autzAction = actionIterator.next();
            if (!limitActions.contains(autzAction)) {
                actionIterator.remove();
            }
        }
        if (limitedAutz.getAction().isEmpty()) {
            return List.of();
        }
        return List.of(limitedAutz);
    }

    private boolean allActionsAllowed(Authorization autz) {
        for (String autzAction: autz.getAction()) {
            if (!limitActions.contains(autzAction)) {
                return false;
            }
        }
        return true;
    }
}
