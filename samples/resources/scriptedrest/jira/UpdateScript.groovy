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

import groovy.json.JsonSlurper
import static java.awt.RenderingHints.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO


// Parameters:
// The connector sends us the following:
// connection : handler to the REST Client
// (see: http://groovy.codehaus.org/modules/http-builder/apidocs/groovyx/net/http/RESTClient.html)
// configuration : handler to the connector's configuration object
//
// action: String correponding to the action (UPDATE/ADD_ATTRIBUTE_VALUES/REMOVE_ATTRIBUTE_VALUES)
//   - UPDATE : For each input attribute, replace all of the current values of that attribute
//     in the target object with the values of that attribute.
//   - ADD_ATTRIBUTE_VALUES: For each attribute that the input set contains, add to the current values
//     of that attribute in the target object all of the values of that attribute in the input set.
//   - REMOVE_ATTRIBUTE_VALUES: For each attribute that the input set contains, remove from the current values
//     of that attribute in the target object any value that matches one of the values of the attribute from the input set.

// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// uid: a String representing the entry uid
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
// password: password string, clear text (only for UPDATE)
// options: a handler to the OperationOptions Map

log.info("Entering "+action+" Script, uid: "+uid+", attributes: "+attributes);

switch ( objectClass ) {
case "__ACCOUNT__":

    userName = uid;

    switch (action) {

        case "UPDATE":
            if (!attributes.containsKey("avatar")) {
                log.info("UPDATE without avatar, ignored");
            } else {

                avatar = attributes.get("avatar");
                if (avatar == null || avatar.size() == 0) {
                    log.info("deleting avatar...");

                    deleteAvatar(userName);

                } else { // avatar is not empty
                    log.info("updating avatar...");
                    avatarOrig = avatar.get(0);

                    ByteArrayInputStream bis = new ByteArrayInputStream(avatarOrig);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    def img = ImageIO.read(bis)

                    // fixing width, 1 pixxel cropped in jira (when 48, jira returns Internal Server Error)
                    int newWidth = 49 //img.width * scale
                    int newHeight = img.height * 49 / img.width//img.height * scale

                    new BufferedImage(newWidth, newHeight, img.type).with { i ->
                        createGraphics().with {
                            setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC)
                            drawImage(img, 0, 0, newWidth, newHeight, null)
                            dispose()
                        }
                        ImageIO.write(i, 'png', bos)
                        //ImageIO.write( i, 'png', new File("D://gusto.png") )
                    }

                    byte[] avatar = bos.toByteArray();


                    avatarSize = avatar.size();
                    avatarFileName = userName + '.png';

                    // delete old avatar
                    deleteAvatar(userName);

                    log.ok("Sending resized avatar with size " + avatarSize + " for user: " + userName);
                    // for debuging over Fiddler
                    //connection.setProxy('localhost', 8888, 'http');

                    resp = connection.post(path: 'https://jira.evolveum.com/rest/api/2/user/avatar/temporary',
                            headers: ['X-Atlassian-Token': 'no-check', 'Accept': '*/*', 'Content-Type': 'image/png'],
                            contentType: 'application/octet-stream',
                            query: ['username': userName, 'filename': avatarFileName, 'size': avatarSize],
                            body: avatar);

                    def slurper = new JsonSlurper()
                    def respJson = slurper.parseText(resp.getData().text) // need to manually convert
                    log.ok("temporary response: {0}", respJson /* resp.getData()==null ? resp : resp.getData().text*/);

                    cropper = '{"cropperWidth": "' + (respJson.cropperWidth == null ? 48 : respJson.cropperWidth) + '","cropperOffsetX": "' + (respJson.cropperOffsetX == null ? 0 : respJson.cropperOffsetX) + '","cropperOffsetY": "' + (respJson.cropperOffsetY == null ? 0 : respJson.cropperOffsetY) + '", "needsCropping": false}';
                    log.ok("cropper settings: {0}", cropper);
                    respJson = connection.post(path: 'https://jira.evolveum.com/rest/api/2/user/avatar',
                            headers: ['X-Atlassian-Token': 'no-check'/*, 'Content-Type:' : 'application/json'*/],
                            query: ['username': userName],
                            body: cropper);
                    log.ok("avatar response: {0}", respJson.getData() == null ? respJson : respJson.getData());
                    avatarId = respJson != null && respJson.getData() != null && respJson.getData()."id" != null ? respJson.getData()."id" : null
                    log.ok("avatar Id: " + avatarId);

                    respPut = connection.put(path: 'https://jira.evolveum.com/rest/api/2/user/avatar',
                            headers: ['X-Atlassian-Token': 'no-check'/*, 'Content-Type:' : 'application/json'*/],
                            query: ['username': userName],
                            body: respJson.getData());

                    log.info("avatar updated {0}", respPut);

                }
            }

            break;

    }

    break

}
return uid;


def deleteAvatar(userName) {
    resp = connection.get(path: "https://jira.evolveum.com/rest/api/latest/user",
            query: ['username' : userName]);
    json = resp.getData();
    def avatarId = null
    if (json."avatarUrls" != null && json."avatarUrls"."48x48" != null) {
        //https:jira.evolveum.com/secure/useravatar?ownerId=oscar&avatarId=11113"
        // in system avatars url is https://jira.evolveum.com/secure/useravatar?avatarId=10122 (without ownerId)
        avatarUrl = json."avatarUrls"."48x48";
        log.ok("Avatar url to delete: {0}", avatarUrl);
        avatarId = avatarUrl.split("=");
        if (avatarId.size()==3) { // delete only non system avatars
            avatarId = avatarId[2]
        }
        else {
            avatarId = null;
            log.ok("System avatar deletion ignored");
        }
    }
    if (avatarId!=null) {
        connection.delete(path: "https://jira.evolveum.com/rest/api/latest/user/avatar/" + avatarId,
                query: ['username': userName]);
        log.info("Avatar removed for user: " + userName+", id: "+avatarId);
    }
}
