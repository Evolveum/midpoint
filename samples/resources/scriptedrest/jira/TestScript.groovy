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


// Parameters:
// The connector sends the following:
// connection: handler to the REST Client
// (see: http://groovy.codehaus.org/modules/http-builder/apidocs/groovyx/net/http/RESTClient.html)
// configuration : handler to the connector's configuration object
// action: a string describing the action ("TEST" here)
// log: a handler to the Log facility


log.info("Entering "+action+" Script");
try{
    //connection.ignoreSSLIssues(); //to ignore certificate validation
    connection.get( path : "https://jira.evolveum.com/rest/api/latest/myself")
}
catch(Exception e){
    throw e
}
