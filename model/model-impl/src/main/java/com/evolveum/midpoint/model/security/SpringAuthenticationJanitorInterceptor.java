/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2012 Igor Farinic"
 *
 */
package com.evolveum.midpoint.model.security;

import com.evolveum.midpoint.model.security.api.UserDetailsService;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.util.WSSecurityUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPMessage;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsible to cleanup spring authentication object after we finished WS method call
 */
public class SpringAuthenticationJanitorInterceptor implements PhaseInterceptor<SoapMessage> {

    private String phase;
    private Set<String> before = new HashSet<String>();
    private Set<String> after = new HashSet<String>();
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