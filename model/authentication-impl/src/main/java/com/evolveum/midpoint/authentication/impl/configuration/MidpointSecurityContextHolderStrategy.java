package com.evolveum.midpoint.authentication.impl.configuration;

import com.evolveum.midpoint.authentication.impl.MidpointSecurityContext;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.util.Assert;

import java.util.function.Supplier;

public class MidpointSecurityContextHolderStrategy implements SecurityContextHolderStrategy {
    private static final ThreadLocal<Supplier<SecurityContext>> CONTEXT_HOLDER = new ThreadLocal<>();

    public void clearContext() {
        CONTEXT_HOLDER.remove();
    }

    public SecurityContext getContext() {
        return this.getDeferredContext().get();
    }

    public Supplier<SecurityContext> getDeferredContext() {
        Supplier<SecurityContext> result = CONTEXT_HOLDER.get();
        if (result == null) {
            SecurityContext context = this.createEmptyContext();
            result = () -> context;
            CONTEXT_HOLDER.set(result);
        }

        return result;
    }

    public void setContext(SecurityContext context) {
        Assert.notNull(context, "Only non-null SecurityContext instances are permitted");
        CONTEXT_HOLDER.set(() -> context);
    }

    public void setDeferredContext(Supplier<SecurityContext> deferredContext) {
        Assert.notNull(deferredContext, "Only non-null Supplier instances are permitted");
        Supplier<SecurityContext> notNullDeferredContext = () -> {
            SecurityContext result = deferredContext.get();
            Assert.notNull(result, "A Supplier<SecurityContext> returned null and is not allowed.");
            return result;
        };
        CONTEXT_HOLDER.set(notNullDeferredContext);
    }

    public SecurityContext createEmptyContext() {
        return new MidpointSecurityContext(new SecurityContextImpl());
    }
}
