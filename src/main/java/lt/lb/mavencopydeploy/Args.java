package lt.lb.mavencopydeploy;

import com.beust.jcommander.Parameter;
import java.io.File;
import lt.lb.commons.Java;

/**
 *
 * @author laim0nas100
 */
public class Args {

    @Parameter(names = "-vs", required = false, description = "Source Nexus version 2 or 3")
    public Integer versionSource = 3;

    @Parameter(names = "-vd", required = false, description = "Source Nexus version 2 or 3 only used with compare option")
    public Integer versionDest = 3;

    @Parameter(names = "-su", required = true, description = "Source Nexus user")
    public String userSrc;
    @Parameter(names = "-sp", required = true, description = "Source Nexus password")
    public String paswSrc;

    @Parameter(names = "-du", required = true, description = "Destination Nexus user")
    public String userDst;
    @Parameter(names = "-dp", required = true, description = "Destination Nexus password")
    public String paswDst;

    @Parameter(names = "-urlsrc", required = true, description = "Nexus REST source service (v3 '/service/rest/v1/assets?repository=[repoId]') or repository content base (v2 /nexus/service/local/repositories/[repoId]/content')")
    public String srcUrl;
    @Parameter(names = "-urldst", required = true, description = "Nexus destination repository url. v3 starts with '/repository/[repoId]/' v2 starts with '/nexus/service/local/repositories/[repoId]/content')")
    public String destUrl;
    @Parameter(names = "-temppath", required = false, description = "Folder to store artifacts temporarily. Will be created if absent and will not be deleted afterwards. Should be empty, or not contain any files with number for their names."
            + "In case of compare option, this path will be used to store compare.txt file with compare info.")
    public String localPath = System.getProperty("user.home") + File.separator + "MavenCopyDeployWorkFolder";

    @Parameter(names = "-maxtemp", required = false, description = "Maximum number of temporary files to store before deleting. Set to <= 0 in order to ignore.")
    public Integer maxTemp = 500;

    @Parameter(names = "-tc", required = false, description = "Number of threads to use. 0 means no threads (will run as a single threaded processs)")
    public Integer threadCount = Java.getAvailableProcessors();

    @Parameter(names = "-compare", required = false, description = "switch to compare function")
    public Boolean doCompare = false;

    @Parameter(names = {"--help", "-help", "help", "?"}, help = true)
    public boolean help;

}
