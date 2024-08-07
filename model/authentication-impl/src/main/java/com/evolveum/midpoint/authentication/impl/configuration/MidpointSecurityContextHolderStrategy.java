package com.evolveum.midpoint.authentication.impl.configuration;

import com.evolveum.midpoint.authentication.impl.MidpointSecurityContext;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.Assert;

import java.util.function.Supplier;

public class MidpointSecurityContextHolderStrategy implements SecurityContextHolderStrategy {
    private static final ThreadLocal<Supplier<SecurityContext>> contextHolder = new ThreadLocal<>();

    public void clearContext() {
        contextHolder.remove();
    }

    public SecurityContext getContext() {
        return this.getDeferredContext().get();
    }

    public Supplier<SecurityContext> getDeferredContext() {
        Supplier<SecurityContext> result = contextHolder.get();
        if (result == null) {
            SecurityContext context = this.createEmptyContext();
            result = () -> context;
            contextHolder.set(result);
        }

        return result;
    }

    public void setContext(SecurityContext context) {
        Assert.notNull(context, "Only non-null SecurityContext instances are permitted");
        contextHolder.set(() -> context);
    }

    public void setDeferredContext(Supplier<SecurityContext> deferredContext) {
        Assert.notNull(deferredContext, "Only non-null Supplier instances are permitted");
        Supplier<SecurityContext> notNullDeferredContext = () -> {
            SecurityContext result = deferredContext.get();
            Assert.notNull(result, "A Supplier<SecurityContext> returned null and is not allowed.");
            return result;
        };
        contextHolder.set(notNullDeferredContext);
    }

    public SecurityContext createEmptyContext() {
        return new MidpointSecurityContext(new SecurityContextImpl());
    }
}
