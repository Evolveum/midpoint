/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.security;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsible to cleanup spring authentication object after we finished WS method call
 */
public class SpringAuthenticationJanitorInterceptor implements PhaseInterceptor<SoapMessage> {

    private String phase;
    private Set<String> before = new HashSet<>();
    private Set<String> after = new HashSet<>();
    private String id;

    public SpringAuthenticationJanitorInterceptor() {
        super();
        id = getClass().getName();
        phase = Phase.POST_INVOKE;
    }

    @Override
    public Set<String> getAfter() {
        return after;
    }

    @Override
    public Set<String> getBefore() {
        return before;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPhase() {
        return phase;
    }

    @Override
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    public void handleFault(SoapMessage message) {
    }
}
