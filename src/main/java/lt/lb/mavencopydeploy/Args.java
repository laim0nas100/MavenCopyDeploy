package lt.lb.mavencopydeploy;

import com.beust.jcommander.Parameter;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import lt.lb.commons.Java;

/**
 *
 * @author laim0nas100
 */
public class Args {

    @Parameter(names = "-inclext", required = false, description = "included extensions, leave empty for disable")
    public List<String> includedExt = Arrays.asList();

    @Parameter(names = "-exclext", required = false, description = "excluded extensions, leave empty for disable")
    public List<String> excludedExt = Arrays.asList("md5", "sha1");

    @Parameter(names = "-vs", required = false, description = "Source Nexus version 2 or 3")
    public int versionSource = 3;

    @Parameter(names = "-vd", required = false, description = "Source Nexus version 2 or 3")
    public int versionDest = 3;

    @Parameter(names = "-su", required = true, description = "Source Nexus user")
    public String userSrc;
    @Parameter(names = "-sp", required = true, description = "Source Nexus password")
    public String paswSrc;

    @Parameter(names = "-du", required = true, description = "Destination Nexus user")
    public String userDst;
    @Parameter(names = "-dp", required = true, description = "Destination Nexus password")
    public String paswDst;

    @Parameter(names = "-domainsrc", required = true, description = "Nexus source repository domain.")
    public String domainSrc;
    @Parameter(names = "-domaindest", required = true, description = "Nexus destination repository domain.")
    public String domainDest;

    @Parameter(names = "-idsrc", required = true, description = "ID (name) of a source repository")
    public String idSrc;

    @Parameter(names = "-iddest", required = true, description = "ID (name) of a destination repository")
    public String idDest;

    @Parameter(names = "-temppath", required = false, description = "Folder to store artifacts temporarily. Will be created if absent and will not be deleted afterwards. Should be empty, or not contain any files with number for their names."
            + "In case of compare option, this path will be used to store compare.txt file with compare info.")
    public String localPath = System.getProperty("user.home") + File.separator + "MavenCopyDeployWorkFolder";

    @Parameter(names = "-maxtemp", required = false, description = "Maximum number of temporary files to store before deleting. Set to <= 0 in order to ignore.")
    public int maxTemp = 500;

    @Parameter(names = "-tc", required = false, description = "Number of threads to use. 0 means no threads (will run as a single threaded processs), below zero means unlimited")
    public int threadCount = Java.getAvailableProcessors();

    @Parameter(names = "-compare", required = false, description = "switch to compare function")
    public boolean doCompare = false;

    @Parameter(names = "-disablelog", required = false, description = "disable logging")
    public boolean disableLog = false;

    @Parameter(names = {"--help", "-help", "help", "?"}, help = true)
    public boolean help;

}
