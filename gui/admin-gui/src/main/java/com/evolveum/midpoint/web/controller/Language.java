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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

/**
 * 
 * @author lazyman
 */
public class Language implements Serializable {

	private static final long serialVersionUID = -7653202257375931789L;
	private final transient Trace logger = TraceManager.getTrace(Language.class);
	private Locale currentLocale;

	public Locale getCurrentLocale() {
		if (currentLocale == null) {
			Application application = FacesContext.getCurrentInstance().getApplication();
			Locale locale = application.getDefaultLocale();
			if (locale == null) {
				logger.warn("Default locale not found, using locale: '" + Locale.ENGLISH.toString() + "'.");
				locale = Locale.ENGLISH;
			}
			setCurrentLocale(locale);
		}

		return currentLocale;
	}

	public void setCurrentLocale(Locale locale) {
		try {
			if (locale == null) {
				throw new IllegalArgumentException("Locale can't be null.");
			}

			Application application = FacesContext.getCurrentInstance().getApplication();
			ResourceBundle bundle = ResourceBundle.getBundle(application.getMessageBundle(), locale);
			if (bundle != null) {
				this.currentLocale = locale;

				FacesContext.getCurrentInstance().getViewRoot().setLocale(this.currentLocale);
			} else {
				throw new RuntimeException("Can't find resource bundle for locale '" + locale + "'.");
			}
		} catch (Exception ex) {
			LoggingUtils.logException(logger, "Couldn't set locale '{}'", ex, locale);
		}
	}

	public void setCurrentLanguage(final String locale) {
		if (locale == null || locale.isEmpty()) {
			throw new IllegalArgumentException("Locale can't be null or empty string.");
		}

		String[] array = locale.split("_");
		if (array.length == 0) {
			throw new IllegalArgumentException("Unknown locale format '" + locale
					+ "', should be en, en_US, en_US_US for example.");
		}
		String language = array[0];
		String country = "";
		String variant = "";
		if (array.length > 1) {
			country = array[1];
		}
		if (array.length > 2) {
			variant = array[2];
		}

		setCurrentLocale(new Locale(language, country, variant));
	}

	public List<Locale> getSupportedLocales() {
		List<Locale> locales = new ArrayList<Locale>();
		Application application = FacesContext.getCurrentInstance().getApplication();
		for (Iterator<Locale> it = application.getSupportedLocales(); it.hasNext();) {
			Locale locale = it.next();
			locales.add(locale);
		}

		Collections.sort(locales, new Comparator<Locale>() {

			@Override
			public int compare(Locale l1, Locale l2) {
				return String.CASE_INSENSITIVE_ORDER.compare(l1.toString(), l2.toString());
			}
		});

		return locales;
	}
}
