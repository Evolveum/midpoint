package com.evolveum.midpoint.report.impl;

import java.util.ArrayList;
import java.util.List;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRDataSourceProvider;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRAbstractBeanDataSourceProvider;
import net.sf.jasperreports.engine.data.JRCsvDataSourceProvider;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;


public class MidPointDataSourceProvider implements JRDataSourceProvider{

	
	

	public static final String[] CONTEXTS = {
		 "classpath:ctx-common.xml",
         "classpath:ctx-configuration.xml",
         "classpath*:ctx-repository.xml",
         "classpath:ctx-repo-cache.xml",
         "classpath:ctx-task.xml",
         "classpath:ctx-provisioning.xml",
         "classpath:ctx-audit.xml",
         "classpath:ctx-security.xml",
         "classpath:ctx-report.xml", 
         "classpath:ctx-model.xml",
         "classpath*:ctx-workflow.xml",
         "classpath*:ctx-notifications.xml"
//         "classpath:ctx-common.xml",
//         "classpath:ctx-configuration.xml", "classpath*:ctx-repository.xml", "classpath:ctx-repo-cache.xml",
//         "classpath:ctx-audit.xml", "classpath:ctx-security.xml",
//         "classpath:ctx-model.xml", "classpath:ctx-report.xml"
         };
	 
//	 public MidPointDataSourceProvider(String s) {
//		
//	}
	 
//	 JRCsvDataSourceProvider
	
	@Override
	public boolean supportsGetFieldsOperation() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("dataSourceProvider.supportsGetFieldsOperation() not supported");
	}

	@Override
	public JRField[] getFields(JasperReport report) throws JRException, UnsupportedOperationException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("dataSourceProvider.getFields() not supported");
	}

	@Override
	public JRDataSource create(JasperReport report) throws JRException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(CONTEXTS);
		PrismContext prismContext = context.getBean("prismContext", PrismContext.class);
		ModelService model = context.getBean("modelService", ModelService.class);
		TaskManager taskManager = context.getBean("taskManager", TaskManager.class);
		
//		OperationResult initResult = new OperationResult("generate report");
//			PrismObject<UserType> userAdministrator = repoAddObjectFromFile(TEST_DIR_COMMON+"/user-administrator.xml", UserType.class, initResult);
//			repoAddObjectFromFile(TEST_DIR_COMMON +"/role-superuser.xml", RoleType.class, initResult);
			
			
//			login(userAdministrator);
			  
//			  importObjectFromFile("src/test/resources/common/user-administrator.xml");
//			  
//			  searchObjectByName(UserType.class, "administrator");
			  
//			  JasperDesign jd = JRXmlLoader.load(new File("src/test/resources/reports/report1.jrxml"));
//			  System.out.println(jd.getLanguage());
//			  JasperReport jr = JasperCompileManager.compileReport(jd);
//			  
//			  Task task = taskManager.createTaskInstance();
//			  task.
//			  Map<String, Object> params = new HashMap<String, Object>();
//			  params.put(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_CONNECTION, model);
//			  params.put(MidPointQueryExecutorFactory.PARAMETER_PRISM_CONTEXT, prismContext);
//			  params.put(MidPointQueryExecutorFactory.PARAMETER_TASK_MANAGER, taskManager);
//			  
//			  JasperPrint jasperPrint = JasperFillManager.fillReport(jr, params);
//			  File f = new File("src/test/resources/reports/report2.pdf");
//			  JasperExportManager.exportReportToPdfFile(jasperPrint, f.getAbsolutePath());
//			  System.out.println("output: " + output);
//			  jr.
//			  jr.getQuery().getLanguage();
//			  jr.getQuery().getText();
		String s = report.getQuery().getText();
		ObjectQuery q;
		if (StringUtils.isEmpty(s)){
			q = null;
		} else {
		
		try {
			SearchFilterType filter = (SearchFilterType) prismContext.parseAtomicValue(s, SearchFilterType.COMPLEX_TYPE);
			q = ObjectQuery.createObjectQuery(QueryConvertor.parseFilter(filter, UserType.class, prismContext));
		} catch (SchemaException e) {
			throw new JRException(e);
		}
		}
//		
//		
		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = task.getResult();
		List<PrismObject<? extends ObjectType>> results = new ArrayList<>();
////		Class<? extends ObjectType> clazz = UserType.class;
		try {
			List<PrismObject<UserType>> users = model.searchObjects(UserType.class, q, null, task, parentResult);
			results.addAll(users);
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
				| CommunicationException | ConfigurationException e) {
			// TODO Auto-generated catch block
			throw new JRException(e);
		} finally{
			context.close();
		}
		
		
//		
		MidPointDataSource mds = new MidPointDataSource(results);
		return mds;
	}

	@Override
	public void dispose(JRDataSource dataSource) throws JRException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("dataSourceProvider.dispose() not supported");
		
	}

}
