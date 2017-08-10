/*
 * Copyright (c) 2016 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.web.security.MidPointApplication;

/**
 * @author semancik
 */
@ContextConfiguration(locations = {
		"classpath:ctx-webapp.xml",
        "classpath:ctx-init.xml",
        "classpath:ctx-security.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath*:ctx-repository-test.xml",
        "classpath:ctx-task.xml",
        "classpath:ctx-audit.xml",
        "classpath:ctx-configuration-test.xml",
        "classpath:ctx-common.xml",
        "classpath:ctx-security.xml",
        "classpath:ctx-provisioning.xml",
        "classpath:ctx-model.xml",
        "classpath*:ctx-workflow.xml"})
public class TestDescriptorLoader extends AbstractGuiUnitTest {
	
	private static final Trace LOGGER = TraceManager.getTrace(TestDescriptorLoader.class);

    @Test(enabled=false)
    public void testDescriptorLoader() {
    	final String TEST_NAME = "testDescriptorLoader";
		TestUtil.displayTestTile(TEST_NAME);
		
		MidPointApplication midPointApplication = new MidPointApplication();
		ServletContext mockServletContext = new ServletContext() {

			@Override
			public int getEffectiveMajorVersion() {
				return 0;
			}

			@Override
			public int getEffectiveMinorVersion() {
				return 0;
			}

			@Override
			public boolean setInitParameter(String name, String value) {
				return false;
			}

			@Override
			public ServletRegistration.Dynamic addServlet(String servletName, String className) {
				return null;
			}

			@Override
			public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
				return null;
			}

			@Override
			public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
				return null;
			}

			@Override
			public <T extends Servlet> T createServlet(Class<T> c) throws ServletException {
				return null;
			}

			@Override
			public ServletRegistration getServletRegistration(String servletName) {
				return null;
			}

			@Override
			public Map<String, ? extends ServletRegistration> getServletRegistrations() {
				return null;
			}

			@Override
			public FilterRegistration.Dynamic addFilter(String filterName, String className) {
				return null;
			}

			@Override
			public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
				return null;
			}

			@Override
			public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
				return null;
			}

			@Override
			public <T extends Filter> T createFilter(Class<T> c) throws ServletException {
				return null;
			}

			@Override
			public FilterRegistration getFilterRegistration(String filterName) {
				return null;
			}

			@Override
			public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
				return null;
			}

			@Override
			public SessionCookieConfig getSessionCookieConfig() {
				return null;
			}

			@Override
			public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {

			}

			@Override
			public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
				return null;
			}

			@Override
			public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
				return null;
			}

			@Override
			public void addListener(String className) {

			}

			@Override
			public <T extends EventListener> void addListener(T t) {

			}

			@Override
			public void addListener(Class<? extends EventListener> listenerClass) {

			}

			@Override
			public <T extends EventListener> T createListener(Class<T> c) throws ServletException {
				return null;
			}

			@Override
			public JspConfigDescriptor getJspConfigDescriptor() {
				return null;
			}

			@Override
			public ClassLoader getClassLoader() {
				return null;
			}

			@Override
			public void declareRoles(String... roleNames) {

			}

			@Override
			public String getVirtualServerName() {
				return null;
			}

			@Override
			public void setAttribute(String arg0, Object arg1) {
			}
			
			@Override
			public void removeAttribute(String arg0) {
			}
			
			@Override
			public void log(String msg, Throwable e) {
				LOGGER.error("{}", msg, e);
			}
			
			@Override
			public void log(Exception e, String msg) {
				LOGGER.error("{}", msg, e);
			}
			
			@Override
			public void log(String msg) {
				LOGGER.trace("{}", msg);
			}
			
			@Override
			public Enumeration getServlets() {
				return null;
			}
			
			@Override
			public Enumeration getServletNames() {
				return null;
			}
			
			@Override
			public String getServletContextName() {
				return "mock";
			}
			
			@Override
			public Servlet getServlet(String arg0) throws ServletException {
				return null;
			}
			
			@Override
			public String getServerInfo() {
				return null;
			}
			
			@Override
			public Set getResourcePaths(String arg0) {
				return null;
			}
			
			@Override
			public InputStream getResourceAsStream(String name) {
				return this.getClass().getClassLoader().getResourceAsStream(name);
			}
			
			@Override
			public URL getResource(String name) throws MalformedURLException {
				return this.getClass().getClassLoader().getResource(name);
			}
			
			@Override
			public RequestDispatcher getRequestDispatcher(String arg0) {
				return null;
			}
			
			@Override
			public String getRealPath(String arg0) {
				return null;
			}
			
			@Override
			public RequestDispatcher getNamedDispatcher(String arg0) {
				return null;
			}
			
			@Override
			public int getMinorVersion() {
				return 2;
			}
			
			@Override
			public String getMimeType(String arg0) {
				return null;
			}
			
			@Override
			public int getMajorVersion() {
				return 4;
			}
			
			@Override
			public Enumeration getInitParameterNames() {
				return null;
			}
			
			@Override
			public String getInitParameter(String arg0) {
				return null;
			}
			
			@Override
			public String getContextPath() {
				return null;
			}
			
			@Override
			public ServletContext getContext(String arg0) {
				return this;
			}
			
			@Override
			public Enumeration getAttributeNames() {
				return null;
			}
			
			@Override
			public Object getAttribute(String arg0) {
				return null;
			}
		};
		midPointApplication.setServletContext(mockServletContext);
		
		DescriptorLoader descriptorLoader = new DescriptorLoader();
		
		// TODO: this test does not really work. This needs to be cleaned up
		// to make it testable
		
		// WHEN
		descriptorLoader.loadData(midPointApplication);
		
		// THEN
		display("initialized loader", descriptorLoader);
    }
}
