import groovyx.net.http.HttpResponseException

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
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// action: a string describing the action ("SEARCH" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map
// query: a handler to the Query Map
//
// The Query map describes the filter used.
//
// query = [ operation: "CONTAINS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "ENDSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "STARTSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "EQUALS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = null : then we assume we fetch everything
//
// AND and OR filter just embed a left/right couple of queries.
// query = [ operation: "AND", left: query1, right: query2 ]
// query = [ operation: "OR", left: query1, right: query2 ]
//
// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

log.info("Entering "+action+" Script, query: "+query);

def result = [];

switch ( objectClass ) {
case "__ACCOUNT__":
    if (query!=null && ("__UID__".equalsIgnoreCase(query.get("left")) || "__NAME__".equalsIgnoreCase(query.get("left")))
            && ("CONTAINS".equalsIgnoreCase(query.get("operation")) || ("EQUALS".equalsIgnoreCase(query.get("operation")))) ) {
        log.ok("Exact query on: " + query.get("right"))
        resp = connection.get(path: "https://jira.evolveum.com/rest/api/latest/user",
                query: ['username' : query.get("right")]);
        json = resp.getData();
        log.ok("JSON response:\n" + json);

        res = parseUser(json, true);
        result.add(res);
    }
    else {
        def projects = [];

        respProj = connection.get(path: "https://jira.evolveum.com/rest/api/latest/project");
        dataProj = respProj.getData();
        dataProj.each {
            projects.add(it.key);
        }
        log.info("Finding users in projects: {0}", projects);

        def uniqueUsers = [];
        for (String project : projects) {

            def startAt=0;
            def maxResults=500; // max is 1000, default 50
            def haveNew = true;

            log.ok("Find all users in project: "+project+", in one iteration we read "+maxResults+" users");
            try {
                while (haveNew) {

                    resp = connection.get(path: "https://jira.evolveum.com/rest/api/latest/user/assignable/search",
                            query: ['project': project, 'startAt': startAt, 'maxResults': maxResults]);

                    data = resp.getData();
                    log.ok("JSON response size {0}, data:\n{1}", data!=null ? data.size : null, data);
                    if (data==null || data.size==0) {
                        haveNew = false
                    }
                    else {
                        // has only 1 account
                        if (data.capacity == null) {
                            res = parseUser(data, false);
                            if (!uniqueUsers.contains(res.__NAME__)) {
                                result.add(res);
                                uniqueUsers.add(res.__NAME__);
                            }
                        } else {
                            // has more accounts need to iterate
                            data.each {
                                res = parseUser(it, false);
                                if (!uniqueUsers.contains(res.__NAME__)) {
                                    result.add(res);
                                    uniqueUsers.add(res.__NAME__);
                                }
                            }
                        }
                        startAt = startAt + maxResults;
                    }
                }
            } catch (HttpResponseException e) {
                log.info("ERROR to read users for project: {0}, {1}", project, e);
            }

        }
    }

    break


}

log.info("result: \n" + result);
return result;


def parseUser(it, readImage) {
    log.ok("user: "+it);
    id = it."name";

    def avatarUrl = null
    if (it."avatarUrls" != null && it."avatarUrls"."48x48" != null)
        avatarUrl = it."avatarUrls"."48x48";

//    log.ok("name: "+id+", avatarUrl: "+avatarUrl);

    byte[] avatar = null; // send only not default profile pictures
    if (readImage && avatarUrl != null && avatarUrl.contains("ownerId")) {
        thumbnailPrefix = avatarUrl.split("\\?");

        thumbnailQuerys = thumbnailPrefix[1].split("&");

        ownerId = null;
        avatarId = null;
        for (int i=0; i<thumbnailQuerys.size(); i++) {
            p = thumbnailQuerys[i].split("=");
            key = p[0];
            if ("ownerId".equals(key)){
                ownerId = p[1];
            }
            else if ("avatarId".equals(key)) {
                avatarId = p[1];
            }
        }

        log.ok("JSON GET profile picture url: {0}, ownerId: {1}, avatarId: {2}", thumbnailPrefix[0], ownerId, avatarId);

        connection.setUri(thumbnailPrefix[0]);
        respImage = connection.get(path: thumbnailPrefix[0],
                headers: ['Content-Type': 'image/png'],
                contentType: 'application/octet-stream',
                query: [avatarId : avatarId, ownerId : ownerId]
            )
        ByteArrayInputStream bais = respImage.getData();
        avatar = new byte[bais.available()];
        bais.read(avatar);
    }

    // self, timeZone, locale
    return [__UID__         : id,
            __NAME__        : id,
            key             : it.key,
            emailAddress    : it.emailAddress,
            avatarUrl       : avatarUrl,
            displayName     : it.displayName,
            active          : it.active,
            avatar          : avatar
    ];
}
