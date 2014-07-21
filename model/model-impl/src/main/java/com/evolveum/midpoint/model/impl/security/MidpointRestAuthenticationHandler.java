package com.evolveum.midpoint.model.impl.security;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.Response.StatusType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class MidpointRestAuthenticationHandler implements ContainerRequestFilter, ContainerResponseFilter {
	 
	@Autowired(required =true)
	private UserProfileService userDetails;
	
	@Autowired(required = true)
	private SecurityEnforcer securityEnforcer;
	
	@Autowired(required = true)
	private Protector protector;
	
    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
        AuthorizationPolicy policy = (AuthorizationPolicy)m.get(AuthorizationPolicy.class);
        
        OperationResourceInfo ori = m.getExchange().get(OperationResourceInfo.class);
        String methodName = ori.getMethodToInvoke().getName();
        
        if (policy == null){
        	return Response.status(401).header("WWW-Authenticate", "Basic").build();
        }
        
        String username = policy.getUserName();
        
        if (username == null){
        	return Response.status(401).header("WWW-Authenticate", "Basic").build();
        }
        
        
        MidPointPrincipal principal;
		try {
			principal = userDetails.getPrincipal(username);
		} catch (ObjectNotFoundException e) {
			return Response.status(401).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user.").build();
		}
        
        if (principal == null ){
        	return Response.status(401).header("WWW-Authenticate", "Basic").build();
        }
        
        UserType userToAuthenticate = principal.getUser();
        
        String password = policy.getPassword();
        
        if (password == null){
        	return Response.status(401).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user without password").build();
        }
        
        if (userToAuthenticate.getCredentials() == null){
        	return Response.status(401).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user.").build();
        }
        
        PasswordType pass = userToAuthenticate.getCredentials().getPassword();
        
        if (pass == null){
        	return Response.status(401).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user.").build();
        }
        
        ProtectedStringType protectedPass = pass.getValue();
        if (protectedPass.getClearValue() != null){
        	if (!password.equals(protectedPass.getClearValue())){
               	return Response.status(401).header("WWW-Authenticate", "Basic").build();
            }
        } else if (protectedPass.getEncryptedDataType() != null){
        	try{
        		String decrypted = protector.decryptString(protectedPass);
        		if (!password.equals(decrypted)){
        			return Response.status(401).header("WWW-Authenticate", "Basic").build();
        		}
        	} catch (EncryptionException ex){
        		return Response.status(401).header("WWW-Authenticate", "Basic").build();
        	}
        	
        } else {
        	return Response.status(401).header("WWW-Authenticate", "Basic authentication fialed. Cannot obtain password value.").build();
        }
        
        m.put("authenticatedUser", userToAuthenticate);
        securityEnforcer.setupPreAuthenticatedSecurityContext(userToAuthenticate.asPrismObject());
                
           
        return null;
        
        
        
//        authorizationEvaluator.isAuthorized(principal, action);
        
        
//        if (isAuthenticated(username, password)) {
//            // let request to continue
//            return null;
//        } else {
//            // authentication failed, request the authetication, add the realm name if needed to the value of WWW-Authenticate 
//            return Response.status(401).header("WWW-Authenticate", "Basic").build();
//        }
    }
//
//	@Override
//	public Response handleResponse(Message m, OperationResourceInfo ori, Response response) {
//		securityEnforcer.setupPreAuthenticatedSecurityContext((PrismObject) null);
//		return null;
//	}

	@Override
	public void filter(ContainerRequestContext arg0, ContainerResponseContext arg1) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void filter(ContainerRequestContext arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}
 
}
