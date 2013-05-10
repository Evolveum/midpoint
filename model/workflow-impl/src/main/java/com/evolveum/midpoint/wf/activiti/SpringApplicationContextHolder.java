/*
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.activiti;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.wf.dao.MiscDataUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Currently unused. Necessary for "smart" workflow tasks.
 */

@Component
public class SpringApplicationContextHolder implements ApplicationContextAware {

	private static ApplicationContext context;

	public void setApplicationContext(ApplicationContext ctx) throws BeansException { 
		context = ctx;
    }  

	public static ApplicationContext getApplicationContext() {
        if (context == null) {
            throw new IllegalStateException("Spring application context could not be determined.");
        }
		return context;
	}

    public static ActivitiInterface getActivitiInterface() {
        return getBean("activitiInterface", ActivitiInterface.class);
    }

    private static<T> T getBean(String name, Class<T> aClass) {
        T bean = getApplicationContext().getBean(name, aClass);
        if (bean == null) {
            throw new IllegalStateException("Could not find " + name + " bean");
        }
        return bean;
    }

    public static MiscDataUtil getMiscDataUtil() {
        return getBean("miscDataUtil", MiscDataUtil.class);
    }

    public static RepositoryService getRepositoryService() {
        return getBean("repositoryService", RepositoryService.class);
    }

    public static PrismContext getPrismContext() {
        return getBean("prismContext", PrismContext.class);
    }
}

  
