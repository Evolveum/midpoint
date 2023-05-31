package com.evolveum.midpoint.ninja.upgrade;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.testng.annotations.Test;

public class JansiTest {

    @Test//(enabled = false)
    public void testJANSI() throws Exception {
        AnsiConsole.systemInstall();

//        System.out.print(Ansi.ansi().a("vilko\n"));
//        System.out.print(Ansi.ansi().cursorUpLine().eraseLine());
//        System.out.print(Ansi.ansi().a("janko\n"));

        System.out.println(Ansi.ansi().fgBlue().a("Start").reset());
        for (int i = 0; i < 10; i++) {
            System.out.println(Ansi.ansi().cursorUpLine().eraseLine(Ansi.Erase.ALL).fgGreen().a(i).reset());
            Thread.sleep(500);
        }
        System.out.println(Ansi.ansi().fgRed().a("Complete").reset());

        AnsiConsole.systemUninstall();
    }
}
