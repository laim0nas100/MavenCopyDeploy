package lt.lb.mavencopydeploy.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import lt.lb.commons.F;
import lt.lb.commons.DLog;
import lt.lb.uncheckedutils.Checked;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author laim0nas100
 */
public class Deploy {



    public void executeCurlUpload(
            String user,
            String pass,
            String file,
            String fullUrl
    ) {

            Checked.uncheckedRun(()->{
                executeProcess("curl","-v","-u",user+":"+pass,"--upload-file",file,fullUrl);
            });
            
    }
    
    public void executeProcess(String... cmd) throws IOException{
//        DLog.print("Executing listenerStopprocess");
//        DLog.print(cmd);
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream();
        Process start = builder.start();
        InputStream is = start.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = reader.readLine()) != null) {
           DLog.println(line);
        }
        start.exitValue();
    }
    
    public void executeProcess2(String... cmd) throws IOException{
        DLog.print("Executing process");
        
        String c = StringUtils.join(cmd, " ");
        DLog.print(c);
        Process process = Runtime.getRuntime().exec(c);
        InputStream is = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = reader.readLine()) != null) {
           DLog.println(line);
        }
        Checked.uncheckedRun(process::waitFor);
        process.exitValue();
    }
}
