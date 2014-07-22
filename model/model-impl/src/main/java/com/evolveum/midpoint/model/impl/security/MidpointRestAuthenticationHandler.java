package com.evolveum.midpoint.model.impl.security;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.security.Principal;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.SecurityContext;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.UserProfileService;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class MidpointRestAuthenticationHandler implements ContainerRequestFilter, ContainerResponseFilter {
	 
	@Autowired(required =true)
	private UserProfileService userDetails;
	
	@Autowired(required = true)
	private SecurityEnforcer securityEnforcer;
	
	@Autowired(required = true)
	private Protector protector;
	
    public void handleRequest(Message m, ContainerRequestContext requestCtx) {
        AuthorizationPolicy policy = (AuthorizationPolicy)m.get(AuthorizationPolicy.class);
        
        if (policy == null){
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        }
        
        String username = policy.getUserName();
        
        if (username == null){
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        }
        
        
        MidPointPrincipal principal;
		try {
			principal = userDetails.getPrincipal(username);
		} catch (ObjectNotFoundException e) {
			requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user.").build());
			return;
		}
        
        if (principal == null ){
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        	return;
        }
        
        UserType userToAuthenticate = principal.getUser();
        
        String password = policy.getPassword();
        
        if (password == null){
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user without password").build());
        	return;
        }
        
        if (userToAuthenticate.getCredentials() == null){
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user.").build());
        	return;
        }
        
        PasswordType pass = userToAuthenticate.getCredentials().getPassword();
        
        if (pass == null){
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic authentication failed. Cannot authenticate user.").build());
        	return;
        }
        
        ProtectedStringType protectedPass = pass.getValue();
        if (protectedPass.getClearValue() != null){
        	if (!password.equals(protectedPass.getClearValue())){
        		requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        		return;
            }
        } else if (protectedPass.getEncryptedDataType() != null){
        	try{
        		String decrypted = protector.decryptString(protectedPass);
        		if (!password.equals(decrypted)){
        			requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        			return;
        		}
        	} catch (EncryptionException ex){
        		requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic").build());
        		return;
        	}
        	
        } else {
        	requestCtx.abortWith(Response.status(401).header("WWW-Authenticate", "Basic authentication fialed. Cannot obtain password value.").build());
        	return;
        }
        
        m.put("authenticatedUser", userToAuthenticate);
        securityEnforcer.setupPreAuthenticatedSecurityContext(userToAuthenticate.asPrismObject());
           
        OperationResult authorizeResult = new OperationResult("Rest authentication/authorization operation.");
        
        
        try {
			securityEnforcer.authorize(AuthorizationConstants.AUTZ_REST_URL, null, null, null, null, null, authorizeResult);
		} catch (SecurityViolationException e){
			requestCtx.abortWith(Response.status(403).header("WWW-Authenticate", "Basic").build());
			return;
		} catch (SchemaException e) {
			requestCtx.abortWith(Response.status(Status.BAD_REQUEST).build());
			return;
		}
           
        
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
//	public Response handleResponse(Message m, Response response) {
//		securityEnforcer.setupPreAuthenticatedSecurityContext((PrismObject) null);
//		return null;
//	}

	@Override
	public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
		
		
//		handleResponse(m, ori, response)
		
	}

	@Override
	public void filter(ContainerRequestContext requestCtx) throws IOException {
		Message m = JAXRSUtils.getCurrentMessage();
		handleRequest(m, requestCtx);
	}
 
}
