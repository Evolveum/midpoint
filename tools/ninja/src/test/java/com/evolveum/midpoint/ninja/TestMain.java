package com.evolveum.midpoint.ninja;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TestMain {

    public static void main(String[] args) {
        //        jc.parse("-m", "./src/test/resources/midpoint-home",
//                "export",
//                "-O", "./export.xml",
//                "-t", "users",
//                "-o", SystemObjectsType.USER_ADMINISTRATOR.value());
        String[] input = new String[]{"-v", "-m", "./src/test/resources/midpoint-home",
                "export",
                "-O", "./export.xml",
                "-t", "roles"};
//                "-f", "<inOid xmlns=\"http://prism.evolveum.com/xml/ns/public/query-3\"><value>00000000-0000-0000-0000-000000000002</value></inOid>");

        input = new String[]{"test"};

        Main.main(input);
    }
}
