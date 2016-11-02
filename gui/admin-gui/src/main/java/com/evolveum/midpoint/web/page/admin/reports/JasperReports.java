/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.reports;

import java.io.ByteArrayOutputStream;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRPdfExporter;

import org.apache.wicket.request.resource.ByteArrayResource;

public class JasperReports extends ByteArrayResource {
	private static String contentType = "application/pdf; charset=UTF-8";

	public JasperReports(JasperPrint print) {
		super(contentType, getData(print));
	}

	public static byte[] getData(JasperPrint print) {
		JRPdfExporter exporter = new JRPdfExporter();
		exporter.setParameter(JRExporterParameter.CHARACTER_ENCODING, "UTF-8");

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
		exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, os);
		try {
			exporter.exportReport();

		} catch (JRException e) {

			e.printStackTrace();

		}

		return os.toByteArray();

	}

}
