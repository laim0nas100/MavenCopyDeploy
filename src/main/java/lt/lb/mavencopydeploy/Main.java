package lt.lb.mavencopydeploy;

import com.beust.jcommander.JCommander;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lt.lb.commons.Log;
import lt.lb.mavencopydeploy.net.BaseClient;

/**
 *
 * @author laim0nas100
 */
public class Main {
    
    static List<BaseClient> openedClients = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        
        Args arg = new Args();
        JCommander build = JCommander.newBuilder()
                .addObject(arg)
                .build();

        build.parse(args);

        if (arg.help) {
           
            build.usage();
            return;
        }

        launch(arg);

        
    }
    
    /**
     * Can launch as a library
     * @param arg
     * @throws Exception 
     */
    public static void launch(Args arg) throws Exception{
        Log.main().async = true;
        Log.main().stackTrace = false;
        Log.main().disable = arg.disableLog;
        
        Files.createDirectories(Paths.get(arg.localPath));
        if(arg.doCompare){
            CompareRepo.compare(arg);
        }else{
            CopyRepo.copyRepo(arg);
        }
        for(BaseClient c:openedClients){
            c.close();
        }
        
        Log.close();
    }
}
