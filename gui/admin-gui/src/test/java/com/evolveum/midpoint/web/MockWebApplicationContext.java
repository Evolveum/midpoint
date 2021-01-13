/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;

public class MockWebApplicationContext implements WebApplicationContext {

    private final ApplicationContext appContext;
    private final ServletContext servletContext;

    public MockWebApplicationContext(ApplicationContext appContext, ServletContext servletContext) {
        this.appContext = appContext;
        this.servletContext = servletContext;
    }

    public boolean containsBean(@NotNull String arg0) {
        return appContext.containsBean(arg0);
    }

    public boolean containsBeanDefinition(@NotNull String arg0) {
        return appContext.containsBeanDefinition(arg0);
    }

    public boolean containsLocalBean(@NotNull String arg0) {
        return appContext.containsLocalBean(arg0);
    }

    public <A extends Annotation> A findAnnotationOnBean(@NotNull String arg0, @NotNull Class<A> arg1)
            throws NoSuchBeanDefinitionException {
        return appContext.findAnnotationOnBean(arg0, arg1);
    }

    public String @NotNull [] getAliases(@NotNull String arg0) {
        return appContext.getAliases(arg0);
    }

    public @NotNull String getApplicationName() {
        return appContext.getApplicationName();
    }

    public @NotNull AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        return appContext.getAutowireCapableBeanFactory();
    }

    public <T> @NotNull T getBean(@NotNull Class<T> arg0, Object @NotNull ... arg1) throws BeansException {
        return appContext.getBean(arg0, arg1);
    }

    @Override
    public <T> @NotNull ObjectProvider<T> getBeanProvider(@NotNull Class<T> aClass) {
        return appContext.getBeanProvider(aClass);
    }

    @Override
    public <T> @NotNull ObjectProvider<T> getBeanProvider(@NotNull ResolvableType resolvableType) {
        return appContext.getBeanProvider(resolvableType);
    }

    public <T> @NotNull T getBean(@NotNull Class<T> arg0) throws BeansException {
        return appContext.getBean(arg0);
    }

    public <T> @NotNull T getBean(@NotNull String arg0, @NotNull Class<T> arg1) throws BeansException {
        return appContext.getBean(arg0, arg1);
    }

    public @NotNull Object getBean(@NotNull String arg0, Object @NotNull ... arg1) throws BeansException {
        return appContext.getBean(arg0, arg1);
    }

    public @NotNull Object getBean(@NotNull String arg0) throws BeansException {
        return appContext.getBean(arg0);
    }

    public int getBeanDefinitionCount() {
        return appContext.getBeanDefinitionCount();
    }

    public String @NotNull [] getBeanDefinitionNames() {
        return appContext.getBeanDefinitionNames();
    }

    public String @NotNull [] getBeanNamesForAnnotation(@NotNull Class<? extends Annotation> arg0) {
        return appContext.getBeanNamesForAnnotation(arg0);
    }

    public String @NotNull [] getBeanNamesForType(Class<?> arg0, boolean arg1, boolean arg2) {
        return appContext.getBeanNamesForType(arg0, arg1, arg2);
    }

    public String @NotNull [] getBeanNamesForType(Class<?> arg0) {
        return appContext.getBeanNamesForType(arg0);
    }

    public String @NotNull [] getBeanNamesForType(@NotNull ResolvableType arg0) {
        return appContext.getBeanNamesForType(arg0);
    }

    @Override
    public String @NotNull [] getBeanNamesForType(@NotNull ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        return appContext.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public Class<?> getType(@NotNull String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
        return appContext.getType(name, allowFactoryBeanInit);
    }

    public <T> @NotNull Map<String, T> getBeansOfType(Class<T> arg0, boolean arg1, boolean arg2)
            throws BeansException {
        return appContext.getBeansOfType(arg0, arg1, arg2);
    }

    public <T> @NotNull Map<String, T> getBeansOfType(Class<T> arg0) throws BeansException {
        return appContext.getBeansOfType(arg0);
    }

    public @NotNull Map<String, Object> getBeansWithAnnotation(@NotNull Class<? extends Annotation> arg0)
            throws BeansException {
        return appContext.getBeansWithAnnotation(arg0);
    }

    public ClassLoader getClassLoader() {
        return appContext.getClassLoader();
    }

    public @NotNull String getDisplayName() {
        return appContext.getDisplayName();
    }

    public @NotNull Environment getEnvironment() {
        return appContext.getEnvironment();
    }

    public String getMessage(@NotNull String code, Object[] args, String defaultMessage, @NotNull Locale locale) {
        return appContext.getMessage(code, args, defaultMessage, locale);
    }

    public @NotNull String getMessage(@NotNull String code, Object[] args, @NotNull Locale locale) throws NoSuchMessageException {
        return appContext.getMessage(code, args, locale);
    }

    public String getId() {
        return appContext.getId();
    }

    public ApplicationContext getParent() {
        return appContext.getParent();
    }

    public BeanFactory getParentBeanFactory() {
        return appContext.getParentBeanFactory();
    }

    public @NotNull Resource getResource(@NotNull String arg0) {
        return appContext.getResource(arg0);
    }

    public Resource @NotNull [] getResources(@NotNull String arg0) throws IOException {
        return appContext.getResources(arg0);
    }

    public void publishEvent(@NotNull ApplicationEvent event) {
        appContext.publishEvent(event);
    }

    public long getStartupDate() {
        return appContext.getStartupDate();
    }

    public Class<?> getType(@NotNull String arg0) throws NoSuchBeanDefinitionException {
        return appContext.getType(arg0);
    }

    public boolean isPrototype(@NotNull String arg0) throws NoSuchBeanDefinitionException {
        return appContext.isPrototype(arg0);
    }

    public boolean isSingleton(@NotNull String arg0) throws NoSuchBeanDefinitionException {
        return appContext.isSingleton(arg0);
    }

    public boolean isTypeMatch(@NotNull String arg0, @NotNull Class<?> arg1) throws NoSuchBeanDefinitionException {
        return appContext.isTypeMatch(arg0, arg1);
    }

    public boolean isTypeMatch(@NotNull String arg0, @NotNull ResolvableType arg1) throws NoSuchBeanDefinitionException {
        return appContext.isTypeMatch(arg0, arg1);
    }

    public void publishEvent(@NotNull Object event) {
        appContext.publishEvent(event);
    }

    public @NotNull String getMessage(@NotNull MessageSourceResolvable resolvable, @NotNull Locale locale)
            throws NoSuchMessageException {
        return appContext.getMessage(resolvable, locale);
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public <T> @NotNull ObjectProvider<T> getBeanProvider(
            @NotNull Class<T> requiredType, boolean allowEagerInit) {
        return appContext.getBeanProvider(requiredType, allowEagerInit);
    }

    @Override
    public <T> @NotNull ObjectProvider<T> getBeanProvider(
            @NotNull ResolvableType requiredType, boolean allowEagerInit) {
        return appContext.getBeanProvider(requiredType, allowEagerInit);
    }
}
