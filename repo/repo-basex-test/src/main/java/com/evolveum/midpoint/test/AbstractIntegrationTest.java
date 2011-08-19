/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.testng.annotations.*;
import org.testng.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * @author Radovan Semancik
 *
 */
public abstract class AbstractIntegrationTest extends OpenDJUnitTestAdapter {
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractIntegrationTest.class);
	
	/**
	 * Utility to control embedded OpenDJ instance (start/stop)
	 */
	protected static OpenDJUtil djUtil;

	/**
	 * Unmarshalled resource definition to reach the embedded OpenDJ instance.
	 * Used for convenience - the tests method may find it handy.
	 */
	protected static JAXBContext jaxbctx;
	protected static Unmarshaller unmarshaller;
	
	@Autowired(required = true)
	protected RepositoryService repositoryService;
	protected static boolean systemInitialized = false;

	@Autowired(required = true)
	protected TaskManager taskManager;
	
	public AbstractIntegrationTest() throws JAXBException {
		djUtil = new OpenDJUtil();
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		unmarshaller = jaxbctx.createUnmarshaller();
	}
	
	// We need this complicated init as we want to initialize repo only once.
	// JUnit will
	// create new class instance for every test, so @Before and @PostInit will
	// not work
	// directly. We also need to init the repo after spring autowire is done, so
	// @BeforeClass won't work either.
	@BeforeTest
	public void initSystemConditional() throws Exception {
		LOGGER.trace("initSystemConditional: systemInitialized={}",systemInitialized);
		if (!systemInitialized) {
			LOGGER.trace("initSystemConditional: invoking initSystem");
			initSystem();
			systemInitialized = true;
		}
	}
	
	abstract public void initSystem() throws Exception;
	
	protected ObjectType addObjectFromFile(String filePath) throws Exception {
		LOGGER.trace("addObjectFromFile: {}",filePath);
		ObjectType object = unmarshallJaxbFromFile(filePath, ObjectType.class);
		System.out.println("obj: " + object.getName());
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName() + ".addObjectFromFile");
		if (object instanceof TaskType) {
			Assert.assertNotNull(taskManager,"Task manager is not initialized");
			taskManager.addTask((TaskType)object, result);
		} else {
			Assert.assertNotNull(repositoryService,"Repository service is not initialized");
			repositoryService.addObject(object, result);
		}
		return object;
	}
	
	protected static <T> T unmarshallJaxbFromFile(String filePath, Class<T> clazz) throws FileNotFoundException,JAXBException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		Object object = unmarshaller.unmarshal(fis);
		T objectType = ((JAXBElement<T>) object).getValue();
		return objectType;
	}

}
