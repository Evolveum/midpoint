package com.evolveum.midpoint.notifications.transports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author mederly
 */
public class TransportUtil {

    static void appendToFile(String filename, String text) throws IOException {
        FileWriter fw = new FileWriter(filename, true);
        fw.append(text);
        fw.close();
    }

}
