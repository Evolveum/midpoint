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
