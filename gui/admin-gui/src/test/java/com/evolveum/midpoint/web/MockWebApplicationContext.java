/**
 * Copyright (c) 2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author semancik
 *
 */
public class MockWebApplicationContext implements WebApplicationContext {
	
	private ApplicationContext appContext;
	private ServletContext servletContext;

	public MockWebApplicationContext(ApplicationContext appContext, ServletContext servletContext) {
		this.appContext = appContext;
		this.servletContext = servletContext;
	}

	public boolean containsBean(String arg0) {
		return appContext.containsBean(arg0);
	}

	public boolean containsBeanDefinition(String arg0) {
		return appContext.containsBeanDefinition(arg0);
	}

	public boolean containsLocalBean(String arg0) {
		return appContext.containsLocalBean(arg0);
	}

	public <A extends Annotation> A findAnnotationOnBean(String arg0, Class<A> arg1)
			throws NoSuchBeanDefinitionException {
		return appContext.findAnnotationOnBean(arg0, arg1);
	}

	public String[] getAliases(String arg0) {
		return appContext.getAliases(arg0);
	}

	public String getApplicationName() {
		return appContext.getApplicationName();
	}

	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return appContext.getAutowireCapableBeanFactory();
	}

	public <T> T getBean(Class<T> arg0, Object... arg1) throws BeansException {
		return appContext.getBean(arg0, arg1);
	}

	public <T> T getBean(Class<T> arg0) throws BeansException {
		return appContext.getBean(arg0);
	}

	public <T> T getBean(String arg0, Class<T> arg1) throws BeansException {
		return appContext.getBean(arg0, arg1);
	}

	public Object getBean(String arg0, Object... arg1) throws BeansException {
		return appContext.getBean(arg0, arg1);
	}

	public Object getBean(String arg0) throws BeansException {
		return appContext.getBean(arg0);
	}

	public int getBeanDefinitionCount() {
		return appContext.getBeanDefinitionCount();
	}

	public String[] getBeanDefinitionNames() {
		return appContext.getBeanDefinitionNames();
	}

	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> arg0) {
		return appContext.getBeanNamesForAnnotation(arg0);
	}

	public String[] getBeanNamesForType(Class<?> arg0, boolean arg1, boolean arg2) {
		return appContext.getBeanNamesForType(arg0, arg1, arg2);
	}

	public String[] getBeanNamesForType(Class<?> arg0) {
		return appContext.getBeanNamesForType(arg0);
	}

	public String[] getBeanNamesForType(ResolvableType arg0) {
		return appContext.getBeanNamesForType(arg0);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> arg0, boolean arg1, boolean arg2)
			throws BeansException {
		return appContext.getBeansOfType(arg0, arg1, arg2);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> arg0) throws BeansException {
		return appContext.getBeansOfType(arg0);
	}

	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> arg0)
			throws BeansException {
		return appContext.getBeansWithAnnotation(arg0);
	}

	public ClassLoader getClassLoader() {
		return appContext.getClassLoader();
	}

	public String getDisplayName() {
		return appContext.getDisplayName();
	}

	public Environment getEnvironment() {
		return appContext.getEnvironment();
	}

	public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
		return appContext.getMessage(code, args, defaultMessage, locale);
	}

	public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
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

	public Resource getResource(String arg0) {
		return appContext.getResource(arg0);
	}

	public Resource[] getResources(String arg0) throws IOException {
		return appContext.getResources(arg0);
	}

	public void publishEvent(ApplicationEvent event) {
		appContext.publishEvent(event);
	}

	public long getStartupDate() {
		return appContext.getStartupDate();
	}

	public Class<?> getType(String arg0) throws NoSuchBeanDefinitionException {
		return appContext.getType(arg0);
	}

	public boolean isPrototype(String arg0) throws NoSuchBeanDefinitionException {
		return appContext.isPrototype(arg0);
	}

	public boolean isSingleton(String arg0) throws NoSuchBeanDefinitionException {
		return appContext.isSingleton(arg0);
	}

	public boolean isTypeMatch(String arg0, Class<?> arg1) throws NoSuchBeanDefinitionException {
		return appContext.isTypeMatch(arg0, arg1);
	}

	public boolean isTypeMatch(String arg0, ResolvableType arg1) throws NoSuchBeanDefinitionException {
		return appContext.isTypeMatch(arg0, arg1);
	}

	public void publishEvent(Object event) {
		appContext.publishEvent(event);
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale)
			throws NoSuchMessageException {
		return appContext.getMessage(resolvable, locale);
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

}
