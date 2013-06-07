package com.evolveum.midpoint.web.security;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.DefaultFilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.util.AntPathRequestMatcher;

import com.evolveum.midpoint.common.security.AuthorizationEvaluator;
import com.evolveum.midpoint.util.QNameUtil;

public class MidPointGuiAuthorizationEvaluator extends AuthorizationEvaluator {

	
	@Override
	public void decide(Authentication authentication, Object object,
			Collection<ConfigAttribute> configAttributes) throws AccessDeniedException,
			InsufficientAuthenticationException {
	
		if (object instanceof FilterInvocation) {
			FilterInvocation filterInvocation = (FilterInvocation) object;
			Collection<ConfigAttribute> guiConfigAttr = new ArrayList<ConfigAttribute>();
			
			for (PageUrlMapping urlMapping : PageUrlMapping.values()) {
				AntPathRequestMatcher matcher = new AntPathRequestMatcher(urlMapping.getUrl());
				if (matcher.matches(filterInvocation.getRequest()) && urlMapping.getAction() != null){
					for (String action : urlMapping.getAction()){
						if (StringUtils.isNotBlank(action)) {
							guiConfigAttr.add(new SecurityConfig(action));
						}
					}
				}
			}
			
			if (configAttributes == null && guiConfigAttr.isEmpty()){
				return;
			}
			try {
				super.decide(authentication, object,
						guiConfigAttr.isEmpty() ? configAttributes : guiConfigAttr);
			} catch (InsufficientAuthenticationException ex) {
				throw ex;
			} catch (AccessDeniedException ex) {
				throw ex;
			}
		}
	}
	
}
