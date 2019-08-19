/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.mavencopydeploy;

import com.beust.jcommander.JCommander;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author laim0nas100
 */
public class Main {

    public static class Cred {

        public String user, pass;

        public Cred(String user, String pass) {
            this.user = user;
            this.pass = pass;
        }

        public Cred() {
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, TimeoutException {

        Args arg = new Args();
        JCommander build = JCommander.newBuilder()
                .addObject(arg)
                .build();

        build.parse(args);

        if (arg.help) {
           
            build.usage();
            return;
        }
        
        CopyRepo.copyRepo(arg);

        
    }
}
