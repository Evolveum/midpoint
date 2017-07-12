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


import org.identityconnectors.framework.common.objects.AttributeInfo
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder
import org.identityconnectors.framework.common.objects.Name
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
// so the script just need to declare the different object types.

log.info("Entering "+action+" Script")

attrs = new HashSet<AttributeInfo>();

name = new AttributeInfoBuilder(Name.NAME)
name.setRequired(true)
attrs.add(name.build())

attrs.add(new AttributeInfoBuilder("description").build())
attrs.add(new AttributeInfoBuilder("field1").setReadable(false).setReturnedByDefault(false).build())
attrs.add(new AttributeInfoBuilder("field2").setReadable(false).setReturnedByDefault(false).build())
attrs.add(new AttributeInfoBuilder("field3").setReadable(false).setReturnedByDefault(false).build())
attrs.add(new AttributeInfoBuilder("field4").setReadable(false).setReturnedByDefault(false).build())
attrs.add(new AttributeInfoBuilder("field5").setReadable(false).setReturnedByDefault(false).build())
attrs.add(new AttributeInfoBuilder("field6").setReadable(false).setReturnedByDefault(false).build())
attrs.add(new AttributeInfoBuilder("field7").setReadable(false).setReturnedByDefault(false).build())
attrs.add(new AttributeInfoBuilder("field8").setReadable(false).setReturnedByDefault(false).build())
attrs.add(new AttributeInfoBuilder("publicFlag").setReadable(false).setType(Boolean.class).setReturnedByDefault(false).build())
attrs.add(new AttributeInfoBuilder("readKey").setCreateable(false).setUpdateable(false).build())
attrs.add(new AttributeInfoBuilder("writeKey").setCreateable(false).setUpdateable(false).build())

ociChannel = new ObjectClassInfoBuilder().setType("channel").addAllAttributeInfo(attrs).build()
builder.defineObjectClass(ociChannel)

log.info("Schema script done");
