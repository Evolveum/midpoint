package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;


@JaxbType(type = ExportType.class)
public enum RExportType implements SchemaEnum<ExportType> {

	 	PDF(ExportType.PDF),

	 	CSV(ExportType.CSV),
	
	 	XML(ExportType.XML),
	
	 	XML_EMBED(ExportType.XML_EMBED),
	
	 	HTML(ExportType.HTML),
	
	 	RTF(ExportType.RTF),
	
	 	XLS(ExportType.XLS),
	
	 	ODT(ExportType.ODT),
	
	 	ODS(ExportType.ODS),
	
	 	DOCX(ExportType.DOCX),
	
	 	XLSX(ExportType.XLSX),
	
	 	PPTX(ExportType.PPTX),
	
	 	XHTML(ExportType.XHTML),
	
	 	JXL(ExportType.JXL);
	
	
	

	    private ExportType type;

	    private RExportType(ExportType type) {
	        this.type = type;
	    }


	@Override
	public ExportType getSchemaValue() {
		return type;
	}

}
