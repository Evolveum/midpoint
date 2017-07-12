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
import org.apache.commons.codec.binary.Hex

import static java.awt.RenderingHints.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO


import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition
import org.apache.cxf.jaxrs.ext.multipart.Attachment
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody

import javax.ws.rs.core.MediaType



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

                        resp = connection.delete(path: "http://localhost:7990/rest/api/1.0/users/"+userName+"/avatar.png");
                        log.info("Avatar removed for user: " + userName+", response {0}: ", resp==null ? null : resp.getData());

                    } else { // avatar is not empty
                        log.info("updating avatar...");
                        avatarOrig = avatar.get(0);

                        ByteArrayInputStream bis = new ByteArrayInputStream(avatarOrig);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        def img = ImageIO.read(bis)

                        // default maximum is 1024x1024, but default.png is 256x256, we use this size in resize
                        int newWidth = 256 //img.width * scale
                        int newHeight = img.height * 256 / img.width//img.height * scale

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

                        log.ok("Sending resized avatar with size " + avatarSize + " for user: " + userName);
                        // for debuging over Fiddler
                        //connection.setProxy('localhost', 8888, 'http');


                        String hexNewLine = "0D0A";
                        byte[] byteNewLine = Hex.decodeHex(hexNewLine.toCharArray());
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

                        // avatar as JPG  - no conversion to PNG
//                        outputStream.write('--------------------------86c9c0934f0c28c5'.getBytes());
//                        outputStream.write(byteNewLine);
//                        outputStream.write('Content-Disposition: form-data; name="avatar"; filename="gusto.jpg'.getBytes());
//                        outputStream.write(byteNewLine);
//                        outputStream.write('Content-Type: image/jpeg'.getBytes());
//                        outputStream.write(byteNewLine);
//                        outputStream.write(byteNewLine);
//
//                        outputStream.write(avatarOrig); //avatar as JPG
//
//                        outputStream.write(byteNewLine);
//                        outputStream.write('--------------------------86c9c0934f0c28c5--'.getBytes());
//                        outputStream.write(byteNewLine);

                        // avatar as converted PNG
                        outputStream.write('--------------------------86c9c0934f0c28c5'.getBytes());
                        outputStream.write(byteNewLine);
                        outputStream.write('Content-Disposition: form-data; name="avatar"; filename="avatar.png"'.getBytes());
                        outputStream.write(byteNewLine);
                        outputStream.write('Content-Type: application/octet-stream'.getBytes());
                        outputStream.write(byteNewLine);
                        outputStream.write(byteNewLine);

                        outputStream.write(avatar); //avatar as PNG

                        outputStream.write(byteNewLine);
                        outputStream.write('--------------------------86c9c0934f0c28c5--'.getBytes());
                        outputStream.write(byteNewLine);

                        byte[] bodyBytes = outputStream.toByteArray();

                        resp = connection.post(path: "http://localhost:7990/rest/api/1.0/users/"+userName+"/avatar.png",
                                headers: ['X-Atlassian-Token': 'no-check', 'Accept': '*/*', 'Content-Type': 'multipart/form-data; boundary=------------------------86c9c0934f0c28c5'],
                                contentType: 'application/octet-stream',
                                body: bodyBytes);

                        log.ok("response: {0}", resp.getData()==null ? resp : resp.getData());
                    }
                }

                break;

        }

        break

}
return uid;
