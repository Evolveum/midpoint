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

import java.nio.file.Files

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

log.info("Entering "+action+" Script, attributes: "+attributes);

switch ( objectClass ) {
case "__ACCOUNT__":

    userName = uid;

    switch (action) {

        case "UPDATE":
            if (!attributes.containsKey("avatar")) {
                log.info("UPDATEs without avatar, ignored");
            }
            else {

                avatar = attributes.get("avatar");
                if (avatar == null || avatar.size()==0) {
                    log.info("deleting avatar... (update to default.png)");

                    avatarFileName = "default.png";
                    String mpHome = System.getProperty("midpoint.home")
                    //todo: better solution?
                    File file = new File(mpHome+'icf-connectors/'+avatarFileName);
                    def avatar = Files.readAllBytes(file.toPath());
                    updatePicture(avatar, userName, avatarFileName);

                } else { // avatar is not empty
                    log.info("updating avatar...");
                    avatar = avatar.get(0);

                    updatePicture(avatar, userName, userName + '.png');
                }
            }

            break;


    }

    break

}
return uid;


def updatePicture(avatarOrig, userName, avatarFileName) {
    ByteArrayInputStream bis = new ByteArrayInputStream(avatarOrig);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    def img = ImageIO.read(bis)

    int newWidth = 256 //img.width * scale
    int newHeight = img.height * 256 / img.width//img.height * scale

    new BufferedImage(newWidth, newHeight, img.type).with { i ->
        createGraphics().with {
            setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC)
            drawImage(img, 0, 0, newWidth, newHeight, null)
            dispose()
        }
        ImageIO.write(i, 'png', bos)
        //ImageIO.write(i, 'png', new File("D://gusto.png") )
    }

    byte[] avatar = bos.toByteArray();


    avatarSize = avatar.size();

    def base64 = new sun.misc.BASE64Encoder().encode(avatar)
    base64 = base64.replaceAll("(\r\n|\n)", ""); // remove new lines
    def body = '[ "'+userName+'", "'+avatarFileName+'", "image/png", "'+base64+'" ]';
    log.info("Sending avatar with size " + avatarSize + " for user: " + userName/*+", body: {0}", body*/);
    // for debuging over Fiddler
    // connection.setProxy('localhost', 8888, 'http');

    resp = connection.post(path: 'https://wiki.evolveum.com/rpc/json-rpc/confluenceservice-v2/addProfilePicture',
            headers: ['Accept': '*/*', 'Content-Type': 'application/json'],
            body: body);

    log.info("avatar updated {0} for {1}", resp.getData(), userName);
}
