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

import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.model.security.api.UserDetailsService;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

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
 * Responsible to inject Spring authentication object before we call WS method
 */
public class SpringAuthenticationInjectorInterceptor implements PhaseInterceptor<SoapMessage> {

    private String phase;
    private Set<String> before = new HashSet<String>();
    private Set<String> after = new HashSet<String>();
    private String id;

    private UserDetailsService userDetailsService;

    public SpringAuthenticationInjectorInterceptor(UserDetailsService userDetailsService) {
        super();
        this.userDetailsService = userDetailsService;
        id = getClass().getName();
        phase = Phase.PRE_PROTOCOL;
        getAfter().add(WSS4JInInterceptor.class.getName());
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


    private SOAPMessage getSOAPMessage(SoapMessage msg) {
        SAAJInInterceptor.INSTANCE.handleMessage(msg);
        return msg.getContent(SOAPMessage.class);
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        //Note: in constructor we have specified that we will be called after we have been successfully authenticated the user through WS-Security
        //Now we will only set the Spring Authentication object based on the user found in the header
        SOAPMessage doc = getSOAPMessage(message);
        try {
            String username = "";
            Element securityHeader = WSSecurityUtil.getSecurityHeader(doc.getSOAPPart(), "");
            username = getUsernameFromSecurityHeader(securityHeader);

            if (username != null && username.length() > 0) {
                PrincipalUser principal = userDetailsService.getUser(username);
            	UserType userType = principal.getUser();
            	if (userType.getActivation() == null || userType.getActivation().isEnabled() == null || 
            			!userType.getActivation().isEnabled()) {
            		throw new Fault(
            				new WSSecurityException("User is disabled"));
            	}
            	if (userType.getCredentials() == null || userType.getCredentials().isAllowedIdmAdminGuiAccess() == null || 
            			!userType.getCredentials().isAllowedIdmAdminGuiAccess()) {
            		throw new Fault(
            				new WSSecurityException("User does not have administration privilege, cannot access web service"));
            	}
                Authentication authentication = new UsernamePasswordAuthenticationToken(principal.getUser(), null);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (WSSecurityException e) {
            throw new Fault(e);
        }

    }

    private String getUsernameFromSecurityHeader(Element securityHeader) {
        String username = "";

        NodeList list = securityHeader.getChildNodes();
        int len = list.getLength();
        Node elem;
        for (int i = 0; i < len; i++) {
            elem = list.item(i);
            if (elem.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if ("UsernameToken".equals(elem.getLocalName())) {
                NodeList nodes = elem.getChildNodes();
                int len2 = nodes.getLength();
                for (int j = 0; j < len2; j++) {
                    Node elem2 = nodes.item(j);
                    if ("Username".equals(elem2.getLocalName())) {
                        username = elem2.getTextContent();
                    }
                }
            }
        }
        return username;
    }

    @Override
    public void handleFault(SoapMessage message) {
    }
}
