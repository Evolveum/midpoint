import groovyx.net.http.HttpResponseException

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.nio.file.Files

import static java.awt.RenderingHints.KEY_INTERPOLATION
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC

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
        def filter = ""; // empty filter
        def readAvatar = false; // read only if filter is set

        if (query!=null && ("__UID__".equalsIgnoreCase(query.get("left")) || "__NAME__".equalsIgnoreCase(query.get("left")))
                && ("CONTAINS".equalsIgnoreCase(query.get("operation")) || ("EQUALS".equalsIgnoreCase(query.get("operation")))) ) {
            log.ok("Exact query on: " + query.get("right"))
            filter = query.get("right");
            readAvatar = true;
        }

        def start=0;
        def limit=500; // max is 1000, default 25
        def haveNew = true;

        log.ok("Find users with filter: '"+filter+"' (empty=find all), in one iteration we read "+limit+" users");

        byte[] defaultAvatar = null;
        if (readAvatar) {
            avatarFileName = "bitbucket.default.png";
            String mpHome = System.getProperty("midpoint.home")
            //todo: better solution?
            File file = new File(mpHome+'icf-connectors/'+avatarFileName);
            defaultAvatar = Files.readAllBytes(file.toPath());
        }


        try {
            while (haveNew) {

                resp = connection.get(path: "http://localhost:7990/rest/api/1.0/admin/users",
                        query: ['start': start, 'limit': limit, 'filter' : filter]);

                data = resp.getData();
                log.ok("JSON response size {0}, isLastPage {1}, data:\n{2}", data!=null ? data.size : null, data!=null ? data.isLastPage : null, data);
                if (data==null || data.isLastPage) {
                    haveNew = false;
                }
                if (data!=null){
                    // has only 1 account
                    if (data.values.capacity == null) {
                        res = parseUser(data.values, readAvatar, defaultAvatar);
                        result.add(res);
                    } else {
                        // has more accounts need to iterate
                        data.values.each {
                            res = parseUser(it, readAvatar, defaultAvatar);
                            result.add(res);
                        }
                    }
                    start = start + limit;
                }
            }
        } catch (HttpResponseException e) {
            log.info("ERROR to read users for project: {0}, {1}", project, e);
        }

        break


}

log.info("result: \n" + result);
return result;


def parseUser(it, readImage, defaultAvatar) {
    log.ok("user: "+it);
    id = it.name;

    def avatarUrl = null;
    def selfUrl = it.links.self[0].href;
    if (selfUrl) {
        avatarUrl = selfUrl+"/avatar.png";
    }

    byte[] avatar = null; // send only not default profile pictures
    if (readImage && avatarUrl != null) {
        respImage = connection.get(path: avatarUrl,
                headers: ['Content-Type': 'image/png'],
                contentType: 'application/octet-stream'
            )
        ByteArrayInputStream bais = respImage.getData();
        avatar = new byte[bais.available()];
        bais.read(avatar);
        // default avatar we don't need
        if (Arrays.equals(avatar, defaultAvatar)) {
            avatar = null;
        }
    }

    return [__UID__         : id,
            __NAME__        : id,
            avatarLink      : avatarUrl,
            avatar          : avatar
    ];
}
