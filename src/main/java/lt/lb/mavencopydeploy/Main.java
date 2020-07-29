package lt.lb.mavencopydeploy;

import com.beust.jcommander.JCommander;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import lt.lb.commons.Log;

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

    public static void main(String[] args) throws IOException, InterruptedException, TimeoutException, ExecutionException {

        Args arg = new Args();
        JCommander build = JCommander.newBuilder()
                .addObject(arg)
                .build();

        build.parse(args);

        if (arg.help) {
           
            build.usage();
            return;
        }
        if(arg.doCompare){
            CompareRepo.compare(arg);
        }else{
            CopyRepo.copyRepo(arg);
        }
        
        
        Log.close();

        
    }
}
