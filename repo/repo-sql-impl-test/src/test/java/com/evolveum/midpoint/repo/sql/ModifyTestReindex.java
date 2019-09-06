/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import static java.util.Collections.emptySet;
import static org.testng.AssertJUnit.assertEquals;

/**
 * The same as ModifyTest but with "executeIfNoChanges" (a.k.a. "reindex") option set.
 * Although this option should do no harm in objects other than certification cases and lookup tables,
 * it is better to check.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyTestReindex extends ModifyTest {

	@Override
	protected RepoModifyOptions getModifyOptions() {
		return RepoModifyOptions.createExecuteIfNoChanges();
	}

	@Test
	public void testReindex() throws Exception {
		final String TEST_NAME = "testReindex";
		TestUtil.displayTestTitle(TEST_NAME);
		OperationResult result = new OperationResult(TEST_NAME);

		PrismObject<UserType> user = prismContext.createObjectable(UserType.class)
				.name("unstable")
				.asPrismObject();
		ItemPath UNSTABLE_PATH = ItemPath.create(UserType.F_EXTENSION, "unstable");
		PrismPropertyDefinition<String> unstableDef = user.getDefinition().findPropertyDefinition(UNSTABLE_PATH);
		PrismProperty<String> unstable = unstableDef.instantiate();
		unstable.setRealValue("hi");
		user.addExtensionItem(unstable);

		String oid = repositoryService.addObject(user, null, result);

		// brutal hack -- may stop working in the future!
		unstableDef.toMutable().setIndexed(true);

		repositoryService.modifyObject(UserType.class, oid, emptySet(), getModifyOptions(), result);

		ObjectQuery query = prismContext.queryFor(UserType.class)
				.item(UNSTABLE_PATH).eq("hi")
				.build();
		int count = repositoryService.countObjects(UserType.class, query, null, result);
		assertEquals("Wrong # of objects found", 1, count);
	}

	/**
	 *  MID-5128
	 */
	@Test
	public void testReindexShadow() throws Exception {
		final String TEST_NAME = "testReindexShadow";
		TestUtil.displayTestTitle(TEST_NAME);
		OperationResult result = new OperationResult(TEST_NAME);

		String APPROVER_OID = "9123090439201432";
		PrismObject<ShadowType> shadow = prismContext.createObjectable(ShadowType.class)
				.name("unstable")
				.beginMetadata()
					.modifyApproverRef(APPROVER_OID, UserType.COMPLEX_TYPE)
				.<ShadowType>end()
				.asPrismObject();
		MutablePrismPropertyDefinition<String> def = prismContext.definitionFactory().createPropertyDefinition(new QName("http://temp/", "attr1"), DOMUtil.XSD_STRING);
		def.setIndexed(true);
		PrismProperty<String> attribute = def.instantiate();
		attribute.addRealValue("value");
		shadow.findOrCreateContainer(ShadowType.F_ATTRIBUTES).add(attribute);

		ObjectQuery query = prismContext.queryFor(ShadowType.class)
				.item(ShadowType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF).ref(APPROVER_OID, UserType.COMPLEX_TYPE)
				.build();

		// add shadow and check metadata search

		String oid = repositoryService.addObject(shadow, null, result);

		int count = repositoryService.countObjects(ShadowType.class, query, null, result);
		assertEquals("Wrong # of objects found (after creation)", 1, count);

		// break metadata in repo

		Session session = factory.openSession();

		System.out.println("definitions: " + session.createQuery("from RExtItem").list());
		System.out.println("ext values: " + session.createQuery("from ROExtString").list());

		Transaction transaction = session.beginTransaction();
		Query updateQuery = session.createQuery(
				"update com.evolveum.midpoint.repo.sql.data.common.RObjectReference set type = null where ownerOid = '" + oid
						+ "'");
		System.out.println("records modified = " + updateQuery.executeUpdate());
		transaction.commit();
		session.close();

		// verify search is broken

		count = repositoryService.countObjects(ShadowType.class, query, null, result);
		assertEquals("Wrong # of objects found (after zeroing the type)", 0, count);

		// reindex

		repositoryService.modifyObject(ShadowType.class, oid, emptySet(), getModifyOptions(), result);

		// verify search is OK

		count = repositoryService.countObjects(ShadowType.class, query, null, result);
		assertEquals("Wrong # of objects found (after reindexing)", 1, count);
	}

}
