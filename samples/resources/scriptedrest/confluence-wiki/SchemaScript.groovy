/*
 * Copyright (c) 2010-2017 Evolveum
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


import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder

// Parameters:
// The connector sends the following:
// connection: handler to the REST Client
// (see: http://groovy.codehaus.org/modules/http-builder/apidocs/groovyx/net/http/RESTClient.html)
// configuration : handler to the connector's configuration object
// action: a string describing the action ("SCHEMA" here)
// log: a handler to the Log facility
// builder: SchemaBuilder instance for the connector
//
// The connector will make the final call to builder.build()
// so the scipt just need to declare the different object types.

// This sample shows how to create 2 basic ObjectTypes: __ACCOUNT__ and __GROUP__.
// It works with OpenDJ 2.6 REST sample attribute:
// http://docs.forgerock.org/en/opendj/2.6.0/admin-guide/index/appendix-rest2ldap.html

log.info("Entering "+action+" Script");

// Declare the __ACCOUNT__ attributes

// name
nameAIB = new AttributeInfoBuilder(Name.NAME);
nameAIB.setRequired(true);
nameAIB.setUpdateable(false);
nameAIB.setCreateable(true); // only detect existence

//avatar -- 256x256
avatarAIB = new AttributeInfoBuilder("avatar", byte[].class);
avatarAIB.setUpdateable(true);
// read only custom avatars, default.png is ignored
avatarAIB.setReadable(true); // returned only in findByUID/Name
avatarAIB.setReturnedByDefault(false);

// if needed: email, url, fullname, can get by method getUser


// returned only in findByUID/Name
thumbnailLinkAIB = new AttributeInfoBuilder("thumbnailLink");
thumbnailLinkAIB.setUpdateable(false); // only read, not to send
thumbnailLinkAIB.setReadable(true);
thumbnailLinkAIB.setReturnedByDefault(false);

accAttrsInfo = new HashSet<AttributeInfo>();
accAttrsInfo.add(nameAIB.build());
accAttrsInfo.add(avatarAIB.build());
accAttrsInfo.add(thumbnailLinkAIB.build());

// Create the __ACCOUNT__ Object class
final ObjectClassInfo ociAccount = new ObjectClassInfoBuilder().setType("__ACCOUNT__").addAllAttributeInfo(accAttrsInfo).build();
builder.defineObjectClass(ociAccount);

log.info("Schema script done");
