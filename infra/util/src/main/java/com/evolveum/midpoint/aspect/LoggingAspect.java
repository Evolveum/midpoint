package com.evolveum.midpoint.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.core.annotation.Order;

@Aspect
@Order(value=2)
public class LoggingAspect
{
        @Around("repositoryService() || provisioningService() || modelService()")
        public Object invokeAround(final ProceedingJoinPoint pjp) throws Throwable
        {
                final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(pjp.getSignature().getDeclaringType().getName());

                final Object[] args = pjp.getArgs();
                final String[] names = ((CodeSignature) pjp.getSignature()).getParameterNames();
                @SuppressWarnings("unchecked")
                final Class<CodeSignature>[] types = ((CodeSignature) pjp.getSignature()).getParameterTypes();
                final String name = ((CodeSignature) pjp.getSignature()).getName();
                final StringBuffer methodCallInfo = new StringBuffer();
                methodCallInfo.append("Entering method: " + name + "(");

                for (int i = 0; i < args.length; i++)
                {
                        methodCallInfo.append(types[i].getName() + " : " + names[i] + " = '" + args[i] + "'");
                        if (args.length == i + 1)
                        {
                                methodCallInfo.append(" )");
                        }
                        else
                        {
                                methodCallInfo.append(",\n");
                        }
                }
                if (args.length == 0)
                {
                        methodCallInfo.append(")");
                }

                logger.trace(methodCallInfo.toString());

                final Object tmp = pjp.proceed();
                logger.trace("Successfully executed method: " + name + "()");
                return tmp;
        }

        @Pointcut("execution(public * com.evolveum.midpoint.repo.api.RepositoryService.*(..))")
        public void repositoryService() {}

        @Pointcut("execution(public * com.evolveum.midpoint.provisioning.api.ProvisioningService.*(..))")
        public void provisioningService() {}
        
        @Pointcut("execution(public * com.evolveum.midpoint.model.api.ModelService.*(..))")
        public void modelService() {}

        
}
