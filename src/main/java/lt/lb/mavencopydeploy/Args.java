package lt.lb.mavencopydeploy;

import com.beust.jcommander.Parameter;
import java.io.File;

/**
 *
 * @author laim0nas100
 */
public class Args {

    @Parameter(names ="-sourceVersion", description ="Source Nexus version (2 or 3)")
    public Integer version = 3;
    
    @Parameter(names = "-us", required = true, description = "Source Nexus user")
    public String userSrc;
    @Parameter(names = "-ps", required = true, description = "Source Nexus password")
    public String paswSrc;

    @Parameter(names = "-ud", required = true, description = "Destination Nexus user")
    public String userDst;
    @Parameter(names = "-pd", required = true, description = "Destination Nexus password")
    public String paswDst;

    
    @Parameter(names = "-srcUrl", required = true, description = "Nexus REST source service (v3 '/service/rest/v1/assets?repository=[repoId]') or repository content base (v2 /nexus/service/local/repositories/[repoId]/content')")
    public String srcUrl;
    @Parameter(names = "-destUrl", required = true, description = "Nexus destination repository url. v3 starts with '/repository/[repoId]/' v2 starts with '/nexus/service/local/repositories/[repoId]/content')")
    public String destUrl;
    @Parameter(names = "-localPath", required = false, description = "Folder to store artifacts temporarily. Will be created if absent, but will not be deleted afterwards. Should be empty, or not contain any files with number for their names.")
    public String localPath = System.getProperty("user.dir") + File.separator + "localFolder";

    @Parameter(names = {"--help", "-help", "help", "?"}, help = true)
    public boolean help;
    
    public Object clone() throws CloneNotSupportedException{
        return super.clone();
    }

}
