package com.evolveum.midpoint.ninja.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProxyCreator {

    public static <T> T getProxy(Class<T> type, Supplier<T> factory) {
        return (T) Proxy.newProxyInstance(ProxyCreator.class.getClassLoader(), new Class<?>[]{type},
            new LazyInvocationHandler<>(factory));
    }

    private static class LazyInvocationHandler<T> implements InvocationHandler {

        private T target;

        private Supplier<T> factory;

        public LazyInvocationHandler(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("equals")) {
                return (proxy == args[0]);
            } else if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            }

            try {
                return method.invoke(getTarget(method), args);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }

        private Object getTarget(Method method) {
            if (target == null) {
                target = factory.get();
            }
            return target;
        }

    }
}
