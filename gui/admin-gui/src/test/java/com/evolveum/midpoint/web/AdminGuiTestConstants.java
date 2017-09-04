/**
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

import java.io.File;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.test.util.MidPointTestConstants;

/**
 * @author semancik
 *
 */
public class AdminGuiTestConstants {

	public static final String COMMON_DIR_NAME = "common";
    public static final File COMMON_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, COMMON_DIR_NAME);

    public static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";

    public static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
    public static final File USER_JACK_REPO_FILE = new File(COMMON_DIR, "user-jack-repo.xml");
    public static final String USER_JACK_OID = "b5541d3c-b2fd-11e5-88c0-4f82a8602266";
    public static final String USER_JACK_USERNAME = "jack";
    public static final String USER_JACK_FULL_NAME = "Jack Sparrow";
    public static final String USER_JACK_GIVEN_NAME = "Jack";
    public static final String USER_JACK_FAMILY_NAME = "Sparrow";

    public static final File USER_EMPTY_FILE = new File(COMMON_DIR, "user-empty.xml");
    public static final String USER_EMPTY_OID = "50053534-36dc-11e6-86f7-035182a6f678";
    public static final String USER_EMPTY_USERNAME = "empty";

    public static final File RESOURCE_DUMMY_FILE = new File(COMMON_DIR, "resource-dummy.xml");
    public static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";
    public static final String RESOURCE_DUMMY_NAMESPACE = MidPointConstants.NS_RI;
    public static final File RESOURCE_DUMMY_INITIALIZED_FILE = new File(COMMON_DIR, "resource-dummy-initialized.xml");
    protected static final QName RESOURCE_DUMMY_ASSOCIATION_GROUP_QNAME = new QName(RESOURCE_DUMMY_NAMESPACE, "group");

    public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

	public static final File ROLE_MAPMAKER_FILE = new File(COMMON_DIR, "role-mapmaker.xml");
	public static final String ROLE_MAPMAKER_OID = "10000000-0000-0000-0000-000000001605";

    public static final File SHADOW_ACCOUNT_JACK_DUMMY_FILE = new File(COMMON_DIR, "shadow-account-jack-dummy.xml");

}
