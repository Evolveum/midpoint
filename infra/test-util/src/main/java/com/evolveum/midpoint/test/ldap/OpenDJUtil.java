/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.test.ldap;

import java.io.File;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.opends.server.tools.LDAPCompare;
import org.opends.server.tools.LDAPDelete;
import org.opends.server.tools.LDAPModify;
import org.opends.server.tools.LDAPSearch;

/**
 * Utility class that deals with the integrated ldap (OpenDJ)
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class OpenDJUtil {

    private String port = "10389";

    private String serverHost = "localhost";

    private String admin = "cn=Directory Manager";

    private String adminPw = "secret";

    public static final String code_id = "$Id$";

    private static final Logger log = Logger.getLogger(OpenDJUtil.class.getName());

    public OpenDJUtil(String serverHost, String port) {
        this.serverHost = serverHost;
        this.port = port;
    }

    public OpenDJUtil() {
    }

    /**
     * Add a LDIF file into the Directory Server
     * @param serverHost Server Host (Use getServerHost() of JBossTestxxx)
     * @param port Port for the DS
     * @param admin admin dn ("cn=Directory Manager")
     * @param adminpwd (password)
     * @param ldifURL (use getDeployURL of JBossTestxxx)
     * @return whether the add was success
     */
    public static boolean addLDIF(String serverHost, String port, String admin, String adminpwd, File file) {
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("LDIF file:" + file + " does not exist or not a file");
        }
        String[] cmd = new String[]{"-h", serverHost, "-p", port, "-D", admin, "-w", adminpwd, "-a", "-f", file.getAbsolutePath()};
        log.log(Level.INFO, "addLDIF: {0}", print(cmd));
        return LDAPModify.mainModify(cmd, false, System.out, System.err) == 0;
    }

    public boolean addLDIF(File file) {
        return addLDIF(serverHost, port, admin, adminPw, file);
    }

    public boolean addLdifFromDefaultLocation() {
        return addLDIF(serverHost, port, admin, adminPw, new File("target/test-data/ldif/ldap-attrib.ldif"));
    }

    /**
     * Delete a DN in the Directory Server
     * @param serverHost Server Host (Use getServerHost() of JBossTestxxx)
     * @param port Port for the DS
     * @param admin admin dn ("cn=Directory Manager")
     * @param adminpwd (password)
     * @param dnToDelete DN to delete (Eg: dc=jboss,dc=org)
     * @param recursive should children also go?
     * @return whether the delete op was success
     */
    public boolean deleteDN(String serverHost, String port, String admin, String adminpwd, String dnToDelete,
            boolean recursive) {
        System.out.println("Start delete DN");
        String rec = recursive ? "-x" : " ";

        String[] cmd = new String[]{"-h", serverHost, "-p", port, "-D", admin, "-w", adminpwd, "-V", "3", rec, "--noPropertiesFile", dnToDelete};
        log.fine("deleteDN:" + print(cmd));
        boolean result = LDAPDelete.mainDelete(cmd, false, System.out, System.err) == 0;
        System.out.println("END delete DN");
        return result;
    }

    /**
     * Recursively delete a DN
     * @param serverHost
     * @param port
     * @param admin
     * @param adminpwd
     * @param dnToDelete
     * @return
     */
    public boolean deleteDNRecursively(String serverHost, String port, String admin, String adminpwd, String dnToDelete) {
        String[] args = {"-h", serverHost, "-p", port, "-V", "3", "-D", admin, "-w", adminpwd, "-x", "--noPropertiesFile", dnToDelete};

        boolean result = LDAPDelete.mainDelete(args, false, System.out, System.err) == 0;
        return result;
    }

    /**
     * Check whether a DN exists. Typically before you do a ldap delete
     * @param serverHost
     * @param port
     * @param dn
     * @return whether the DN exists?
     */
    public boolean existsDN(String serverHost, String port, String dn) {
        String[] cmd = new String[]{"-h", serverHost, "-p", port, "-b", dn, "-s", "sub", "objectclass=*"};
        log.log(Level.FINE, "existsDN:{0}", print(cmd));
        boolean result = LDAPSearch.mainSearch(cmd, false, System.out, System.err) == 0;
        System.out.println("End Search");
        return result;
    }

    public boolean existsDN(String dn) {
        return existsDN(serverHost, port, dn);
    }

    public boolean ldapCompare(String assertion, String filter) {
        String[] cmd = new String[]{"-h", serverHost, "-p", port, "-D", admin, "-w", adminPw, assertion, filter};
        return LDAPCompare.mainCompare(cmd, false, System.out, System.err) == 0;
    }

    /**
     * Issue a ldapCompare in the standard ldapCompare cmd line syntax
     * (Eg: "-h localhost -p 1389 -D "cn=..." -w password -a -f ldif.txt)
     * @param cmdline
     * @return whether ldapCompare was success
     */
    public boolean ldapCompare(String cmdline) {
        String[] strArr = getStringArr(cmdline);
        log.fine("ldapCompare:" + print(strArr));
        return LDAPCompare.mainCompare(strArr, false, System.out, System.err) == 0;
    }

    /**
     * Issue a ldapdelete in the standard ldapdelete cmd line syntax
     * (Eg: "-h localhost -p 1389 -D "cn=..." -w password -a -f ldif.txt)
     * @param cmdline
     * @return whether ldapmodify was success
     */
    public boolean ldapDelete(String cmdline) {
        String[] strArr = getStringArr(cmdline);
        log.fine("ldapDelete:" + print(strArr));
        return LDAPDelete.mainDelete(strArr) == 0;
    }

    /**
     * Issue a ldapmodify in the standard ldapmodify cmd line syntax
     * (Eg: "-h localhost -p 1389 -D "cn=..." -w password -a -f ldif.txt)
     * @param cmdline
     * @return whether ldapmodify was success
     */
    public boolean ldapModify(String cmdline) {
        String[] strArr = getStringArr(cmdline);
        log.fine("ldapModify:" + print(strArr));
        return LDAPModify.mainModify(strArr) == 0;
    }

    //***************************************************************
    //   PRIVATE METHODS
    //***************************************************************
    private String[] getStringArr(String str) {
        StringTokenizer st = new StringTokenizer(str);
        int num = st.countTokens();
        String[] strarr = new String[num];
        int i = 0;
        while (st.hasMoreTokens()) {
            strarr[i++] = st.nextToken();
        }
        return strarr;
    }

    private static String print(String[] arr) {
        StringBuilder sb = new StringBuilder();
        int len = arr != null ? arr.length : 0;
        for (int i = 0; i < len; i++) {
            sb.append(arr[i]).append(" ");
        }
        return sb.toString();
    }
}
