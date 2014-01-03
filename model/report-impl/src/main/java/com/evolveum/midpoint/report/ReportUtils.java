package com.evolveum.midpoint.report;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.base.JRBasePen;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignReportTemplate;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportParameterConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;

public class ReportUtils {

	private static String MIDPOINT_HOME = System.getProperty("midpoint.home"); 
    private static String EXPORT_DIR = MIDPOINT_HOME + "export/";
    
	
	private static Class getClassType(QName clazz)
    {
    	//TODO
    	return java.lang.String.class;
    }
    
    
    public static JasperDesign createJasperDesign(ReportType reportType) throws JRException
	{
		//JasperDesign
		JasperDesign jasperDesign = new JasperDesign();
		String reportName = reportType.getName().getOrig(); 
		jasperDesign.setName(reportName.replace("\\s", ""));
		
		switch (reportType.getReportOrientation())
		{
			case LANDSCAPE :
			default:
			{
				jasperDesign.setOrientation(OrientationEnum.LANDSCAPE);
				jasperDesign.setPageWidth(842);
				jasperDesign.setPageHeight(595);
				jasperDesign.setColumnWidth(802);
			}
			break;
			case PORTRAIT :
			{
				jasperDesign.setOrientation(OrientationEnum.LANDSCAPE);
				jasperDesign.setPageWidth(595);
				jasperDesign.setPageHeight(842);
				jasperDesign.setColumnWidth(555);
			}
			break;
		}
		jasperDesign.setColumnSpacing(0);
		jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
		jasperDesign.setLeftMargin(20);
		jasperDesign.setRightMargin(20);
		jasperDesign.setTopMargin(20);
		jasperDesign.setBottomMargin(20);
		
		
		//Parameters
		//two parameters are there every time - template styles and logo image and will be excluded
		List<ReportParameterConfigurationType> parameters = new ArrayList<ReportParameterConfigurationType>();
		parameters.addAll(reportType.getReportParameter());
		boolean isTemplateStyle = false;
		
		for(ReportParameterConfigurationType parameterRepo : parameters)
		{
			JRDesignParameter parameter = new JRDesignParameter();
			parameter.setName(parameterRepo.getNameParameter());
			parameter.setValueClass(getClassType(parameterRepo.getClassTypeParameter()));
			jasperDesign.addParameter(parameter);
			isTemplateStyle = isTemplateStyle || parameter.getName().equals("BaseTemplateStyles");			
		}
				
		//Template Style or Styles
		if (isTemplateStyle)
		{
			JRDesignReportTemplate templateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{BaseTemplateStyles}"));
			jasperDesign.addTemplate(templateStyle);
		}
		else
		{
			JRDesignStyle baseStyle = new JRDesignStyle();
			baseStyle.setName("Base");
			baseStyle.setDefault(true);
			baseStyle.setHorizontalAlignment(HorizontalAlignEnum.LEFT);
			baseStyle.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			baseStyle.setFontSize(10);
			baseStyle.setPdfFontName("Helvetica");
			baseStyle.setPdfEncoding("Cp1252");
			baseStyle.setPdfEmbedded(false);
			jasperDesign.addStyle(baseStyle);
			
			JRDesignStyle titleStyle = new JRDesignStyle();
			titleStyle.setName("Title");
			titleStyle.setParentStyle(baseStyle);
			titleStyle.setMode(ModeEnum.OPAQUE);
			titleStyle.setBackcolor(Color.decode("#267994"));
			titleStyle.setForecolor(Color.decode("#FFFFFF"));
			titleStyle.setFontSize(26);
			jasperDesign.addStyle(titleStyle);
			
			JRDesignStyle pageHeaderStyle = new JRDesignStyle();
			pageHeaderStyle.setName("Page header");
			pageHeaderStyle.setParentStyle(baseStyle);
			pageHeaderStyle.setForecolor(Color.decode("#000000"));
			pageHeaderStyle.setFontSize(12);
			jasperDesign.addStyle(pageHeaderStyle);
			
			JRDesignStyle columnHeaderStyle = new JRDesignStyle();
			columnHeaderStyle.setName("Column header");
			columnHeaderStyle.setParentStyle(baseStyle);
			columnHeaderStyle.setHorizontalAlignment(HorizontalAlignEnum.CENTER);
			columnHeaderStyle.setMode(ModeEnum.OPAQUE);
			columnHeaderStyle.setBackcolor(Color.decode("#333333"));
			columnHeaderStyle.setForecolor(Color.decode("#FFFFFF"));
			columnHeaderStyle.setFontSize(12);
			jasperDesign.addStyle(columnHeaderStyle);
			
			JRDesignStyle detailStyle = new JRDesignStyle();
			detailStyle.setName("Detail");
			detailStyle.setParentStyle(baseStyle);
			detailStyle.setBold(false);
			jasperDesign.addStyle(detailStyle);
			
			JRDesignStyle pageFooterStyle = new JRDesignStyle();
			pageFooterStyle.setName("Page footer");
			pageFooterStyle.setParentStyle(baseStyle);
			pageFooterStyle.setForecolor(Color.decode("#000000"));
			pageFooterStyle.setFontSize(9);
			jasperDesign.addStyle(pageFooterStyle);
		}
		
		//Fields
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportField())
		{
			JRDesignField field = new JRDesignField();
			field.setName(fieldRepo.getNameReportField());
			field.setValueClass(getClassType(fieldRepo.getClassTypeField()));	
			jasperDesign.addField(field);
		}

		//Background
		JRDesignBand bandBackground = new JRDesignBand();
		bandBackground.setHeight(30);
		bandBackground.setSplitType(SplitTypeEnum.STRETCH);
		jasperDesign.setBackground(bandBackground);
		
		//Title
		//band size depends on the number of parameters
		//two pre-defined parameters were excluded
		int secondColumn = Math.round(jasperDesign.getColumnWidth()/2 - 1);
		JRDesignBand titleBand = new JRDesignBand();
		int height = 70 + Math.max(40, parameters.size()*20);
		titleBand.setHeight(height);
		titleBand.setSplitType(SplitTypeEnum.STRETCH);
		
		JRDesignFrame frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(0);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(70);
		frame.setStyleNameReference("Title");
		titleBand.addElement(frame);
		
		JRDesignStaticText staticText = new JRDesignStaticText();
		staticText.setX(10);
		staticText.setY(15);
		staticText.setWidth(266);
		staticText.setHeight(40);
		staticText.setStyleNameReference("Title");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("DataSource Report");
		frame.addElement(staticText);
		
		JRDesignImage image = new JRDesignImage(new JRDesignStyle().getDefaultStyleProvider());
		image.setX(589);
		image.setY(15);
		image.setWidth(203);
		image.setHeight(40);
		image.setStyleNameReference("Title");
		image.setExpression(new JRDesignExpression("$P{LOGO_PATH}"));
		frame.addElement(image);
		
		 
		staticText = new JRDesignStaticText();
		staticText.setX(secondColumn);
		staticText.setY(70);
		staticText.setWidth(150);
		staticText.setHeight(20);
		staticText.setStyleNameReference("Page header");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("Report generated on:");
		titleBand.addElement(staticText);
		
		JRDesignTextField textField = new JRDesignTextField();
		textField.setX(secondColumn + 150);
		textField.setY(70);
		textField.setWidth(250);
		textField.setHeight(20);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page header");
		textField.setBold(false);
		textField.setExpression(new JRDesignExpression("new java.util.Date()"));
		titleBand.addElement(textField);
		
		staticText = new JRDesignStaticText();
		staticText.setX(secondColumn);
		staticText.setY(90);
		staticText.setWidth(150);
		staticText.setHeight(20);
		staticText.setStyleNameReference("Page header");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("Number of records:");
		titleBand.addElement(staticText);
		
		textField = new JRDesignTextField();
		textField.setX(secondColumn + 150);
		textField.setY(90);
		textField.setWidth(250);
		textField.setHeight(20);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page header");
		textField.setBold(false);
		textField.setEvaluationTime(EvaluationTimeEnum.REPORT);
		textField.setBlankWhenNull(true);
		textField.setExpression(new JRDesignExpression("$V{REPORT_COUNT}"));
		titleBand.addElement(textField);
		
		parameters.remove(0);
		parameters.remove(0);
		int y = 70;
		for(ReportParameterConfigurationType parameterRepo : parameters)
		{
			staticText = new JRDesignStaticText();
			staticText.setX(2);
			staticText.setY(y);
			staticText.setWidth(150);
			staticText.setHeight(20);
			staticText.setStyleNameReference("Page header");
			staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			staticText.setText(parameterRepo.getDescriptionParameter() + ":");
			titleBand.addElement(staticText);
			
			textField = new JRDesignTextField();
			textField.setX(160);
			textField.setY(y);
			textField.setWidth(240);
			textField.setHeight(20);
			textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
			textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			textField.setStyleNameReference("Page header");
			textField.setBold(false);
			textField.setExpression(new JRDesignExpression("$P{"+ parameterRepo.getNameParameter() + "}"));
			titleBand.addElement(textField);
			
			y = y + 20;
		}
		
		jasperDesign.setTitle(titleBand);
	
		//Page header
		JRDesignBand pageHeaderBand = new JRDesignBand();
		pageHeaderBand.setSplitType(SplitTypeEnum.STRETCH);
		jasperDesign.setPageHeader(pageHeaderBand);
		
		//Column header
		JRDesignBand columnHeaderBand = new JRDesignBand();
		columnHeaderBand.setSplitType(SplitTypeEnum.STRETCH);
		columnHeaderBand.setHeight(24);

		frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(5);
		//frame.setWidth(799);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(19);
		frame.setStyleNameReference("Column header");
		//frame.setRemoveLineWhenBlank(true);
		
		int x = 0;
		int width = 0;
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportField())
		{
			staticText = new JRDesignStaticText();
			staticText.setX(x);
			staticText.setY(0);
			width =  Math.round((float) ((frame.getWidth()/100) * fieldRepo.getWidthField()));
			staticText.setWidth(width);
			staticText.setHeight(18);
			staticText.setStyleNameReference("Column header");
			staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			staticText.setText(fieldRepo.getNameHeaderField());
			frame.addElement(staticText);
			x = x + width;
		}
		
		columnHeaderBand.addElement(frame);
		jasperDesign.setColumnHeader(columnHeaderBand);
		
		//Detail
		JRDesignBand detailBand = new JRDesignBand();
		detailBand.setHeight(20);
		detailBand.setSplitType(SplitTypeEnum.PREVENT);
		
		frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(1);
		//frame.setWidth(799);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(19);
		frame.setStyleNameReference("Detail");
		
		x = 0;
		width = 0;
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportField())
		{
			textField = new JRDesignTextField();
			textField.setX(x);
			textField.setY(1);
			width = Math.round((float) ((frame.getWidth()/100) * fieldRepo.getWidthField())); 
			textField.setWidth(width);
			textField.setHeight(18);
			textField.setStretchWithOverflow(true);
			textField.setBlankWhenNull(true);
			textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			textField.setStyleNameReference("Detail");
			textField.setExpression(new JRDesignExpression("$F{" + fieldRepo.getNameReportField() + "}"));
			frame.addElement(textField);
			x = x + width;
		}
		
		JRDesignLine line = new JRDesignLine();
		line.setX(0);
		line.setY(3);
		//line.setWidth(800);
		line.setWidth(jasperDesign.getColumnWidth()-2);
		line.setHeight(1);
		line.setPositionType(PositionTypeEnum.FIX_RELATIVE_TO_BOTTOM);
		JRBasePen pen = new JRBasePen(line);
		pen.setLineWidth((float) 0.5);
		pen.setLineColor(Color.decode("#999999"));
		frame.addElement(line);
		
		detailBand.addElement(frame);
		((JRDesignSection)jasperDesign.getDetailSection()).addBand(detailBand);		
		
		//Column footer
		JRDesignBand columnFooterBand = new JRDesignBand();
		columnFooterBand.setHeight(7);
		columnFooterBand.setSplitType(SplitTypeEnum.STRETCH);		
		
		line = new JRDesignLine();
		line.setX(0);
		line.setY(3);
		//line.setWidth(800);
		line.setWidth(jasperDesign.getColumnWidth()-2);
		line.setHeight(1);
		line.setPositionType(PositionTypeEnum.FIX_RELATIVE_TO_BOTTOM);
		pen = new JRBasePen(line);
		pen.setLineWidth((float) 0.5);
		pen.setLineColor(Color.decode("#999999"));
		columnFooterBand.addElement(line);
		jasperDesign.setColumnFooter(columnFooterBand);

		//Page footer
		JRDesignBand pageFooterBand = new JRDesignBand();
		pageFooterBand.setHeight(32);
		pageFooterBand.setSplitType(SplitTypeEnum.STRETCH);
		frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(1);
		//frame.setWidth(800);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(24);
		frame.setStyleNameReference("Page footer");
		frame.setMode(ModeEnum.TRANSPARENT);
		
		textField = new JRDesignTextField();
		textField.setX(2);
		textField.setY(1);
		textField.setWidth(197);
		textField.setHeight(20);
		//textField.setPattern("EEEEE dd MMMMM yyyy");
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page footer");
		textField.setExpression(new JRDesignExpression("new java.util.Date()"));
		frame.addElement(textField);
		
		textField = new JRDesignTextField();
		textField.setX(680);
		textField.setY(1);
		textField.setWidth(80);
		textField.setHeight(20);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page footer");
		textField.setExpression(new JRDesignExpression("\"Page \" + String.valueOf($V{PAGE_NUMBER}) + \" of\""));
		frame.addElement(textField);
		
		textField = new JRDesignTextField();
		textField.setX(760);
		textField.setY(1);
		textField.setWidth(40);
		textField.setHeight(20);
		textField.setEvaluationTime(EvaluationTimeEnum.REPORT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page footer");	
		textField.setExpression(new JRDesignExpression("$V{PAGE_NUMBER}"));
		frame.addElement(textField);
		
		pageFooterBand.addElement(frame);
		
		jasperDesign.setPageFooter(pageFooterBand);

		return jasperDesign;
	}

       
    public static String getReportOutputFilePath(ReportType reportType){
    	
    	String output =  EXPORT_DIR + reportType.getName().getOrig();
    	switch (reportType.getReportExport())
        {
        	case PDF : output = output + ".pdf";
        		break;
          	case CSV : output = output + ".csv";
      			break;
          	case XML : output = output + ".xml";
          		break;
          	case XML_EMBED : output = output + "_embed.xml";
          		break;
          	case HTML : output = output + ".html";
          		break;
          	case RTF : output = output + ".rtf";
          		break;
          	case XLS : output = output + ".xls";
  				break;
          	case ODT : output = output + ".odt";
          		break;
          	case ODS : output = output + ".ods";
  				break;
          	case DOCX : output = output + ".docx";
  				break;
          	case XLSX : output = output + ".xlsx";
  				break;
          	case PPTX : output = output + ".pptx";
          		break;
          	case XHTML : output = output + ".x.html";
  				break;
          	case JXL : output = output + ".jxl.xls";
          		break; 	
			default:
				break;
        }
    	
    	return output;
    }
    
}
