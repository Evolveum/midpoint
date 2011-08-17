/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.security;

import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.util.UrlUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This class represents an extension to the way DefaultRedirectStrategy works.
 * This class takes into account if the incoming request causing action by
 * Spring Security requires a "partail-response" xml redirect instead of a
 * response.sendRedirect().
 * 
 * @author Ben Simpson ben.simpson@icesoft.com
 */
public class MidPointRedirectStrategy implements RedirectStrategy {

	private static final Trace TRACE = TraceManager.getTrace(MidPointRedirectStrategy.class);
	private boolean contextRelative;

	/**
	 * Redirects the response to the supplied URL.
	 * <p>
	 * If <tt>contextRelative</tt> is set, the redirect value will be the value
	 * after the request context path. Note that this will result in the loss of
	 * protocol information (HTTP or HTTPS), so will cause problems if a
	 * redirect is being performed to change to HTTPS, for example.
	 */
	public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url)
			throws IOException {
		String redirectUrl = calculateRedirectUrl(request.getContextPath(), url);
		redirectUrl = response.encodeRedirectURL(redirectUrl);

		// we should redirect using ajax response if the case warrants
		boolean ajaxRedirect = request.getHeader("faces-request") != null
				&& request.getHeader("faces-request").toLowerCase().indexOf("ajax") > -1;

		if (ajaxRedirect) {
			// javax.faces.context.FacesContext ctxt =
			// javax.faces.context.FacesContext.getCurrentInstance();
			// ctxt.getExternalContext().redirect(redirectUrl);
			TRACE.info("Sending ajax redirect (" + ajaxRedirect + ") to: " + redirectUrl);
			String ajaxRedirectXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<partial-response><redirect url=\"" + redirectUrl
					+ "\"></redirect></partial-response>";
			response.setContentType("text/xml");
			response.getWriter().write(ajaxRedirectXml);
		} else {
			TRACE.info("Sending standard redirect to: " + redirectUrl);
			response.sendRedirect(redirectUrl);
		}
	}

	private String calculateRedirectUrl(String contextPath, String url) {
		if (!UrlUtils.isAbsoluteUrl(url)) {
			if (contextRelative) {
				return url;
			} else {
				return contextPath + url;
			}
		}

		// Full URL, including http(s)://

		if (!contextRelative) {
			return url;
		}

		// Calculate the relative URL from the fully qualified URL, minus the
		// scheme and base context.
		url = url.substring(url.indexOf("://") + 3); // strip off scheme
		url = url.substring(url.indexOf(contextPath) + contextPath.length());

		if (url.length() > 1 && url.charAt(0) == '/') {
			url = url.substring(1);
		}

		return url;
	}

	/**
	 * If <tt>true</tt>, causes any redirection URLs to be calculated minus the
	 * protocol and context path (defaults to <tt>false</tt>).
	 */
	public void setContextRelative(boolean useRelativeContext) {
		this.contextRelative = useRelativeContext;
	}
}
