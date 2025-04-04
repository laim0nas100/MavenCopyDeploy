package lt.lb.mavencopydeploy;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import java.io.File;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import lt.lb.commons.Java;
import lt.lb.commons.containers.collections.ImmutableCollections;
import lt.lb.commons.iteration.streams.MakeStream;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author laim0nas100
 */
public class Args {

    public static enum Mode {
        COMPARE, COPY, DOWNLOAD
    }

    @Parameter(names = "-mode", required = false, description = "COPY/COMPARE/DOWNLOAD", converter = ModeConverter.class)
    public Mode mode = Mode.DOWNLOAD;

    @Parameter(names = "-inclext", required = false, description = "included extensions, leave empty for disable")
    public List<String> includedExt = ImmutableCollections.listOf();

    @Parameter(names = "-exclext", required = false, description = "excluded extensions, leave empty for disable")
    public List<String> excludedExt = ImmutableCollections.listOf("md5", "sha1");

    @Parameter(names = "-vs", required = false, description = "Source Nexus version 2 or 3")
    public int versionSource = 3;

    @Parameter(names = "-vd", required = false, description = "Source Nexus version 2 or 3")
    public int versionDest = 3;

    @Parameter(names = "-su", required = true, description = "Source Nexus user")
    public String userSrc;
    @Parameter(names = "-sp", required = true, description = "Source Nexus password")
    public String paswSrc;

    @Parameter(names = "-du", required = false, description = "Destination Nexus user")
    public String userDst;
    @Parameter(names = "-dp", required = false, description = "Destination Nexus password")
    public String paswDst;

    @Parameter(names = "-srcdomain", required = true, description = "Nexus source repository domain.")
    public String domainSrc;
    @Parameter(names = "-destdomain", required = false, description = "Nexus destination repository domain.")
    public String domainDest;

    @Parameter(names = "-srcid", required = true, description = "ID (name) of a source repository")
    public String idSrc;

    @Parameter(names = "-destid", required = false, description = "ID (name) of a destination repository")
    public String idDest;

    @Parameter(names = "-temppath", required = false, description = "Folder to store artifacts temporarily. Will be created if absent and will not be deleted afterwards. Should be empty, or not contain any files with number for their names."
            + "In case of compare option, this path will be used to store compare.txt file with compare info.")
    public String localPath = System.getProperty("user.home") + File.separator + "MavenCopyDeployWorkFolder";

    @Parameter(names = "-maxtemp", required = false, description = "Maximum number of temporary files to store before deleting. Set to <= 0 in order to ignore.")
    public int maxTemp = 500;

    @Parameter(names = "-tc", required = false, description = "Number of threads to use. 0 means no threads (will run as a single threaded processs), below zero means unlimited")
    public int threadCount = Java.getAvailableProcessors();

    @Parameter(names = "-downloadpath", required = false, description = "download")
    public String downloadPath = System.getProperty("user.home") + File.separator + "MavenCopyDeployWorkFolder" + File.separator + "download";

    @Parameter(names = "-disablelog", required = false, description = "disable logging")
    public boolean disableLog = false;

    @Parameter(names = {"--help", "-help", "help", "?"}, help = true)
    public boolean help;

    @Override
    public String toString() {
        return "Args{" + "mode=" + mode + ", includedExt=" + includedExt + ", excludedExt=" + excludedExt + ", versionSource=" + versionSource + ", versionDest=" + versionDest + ", userSrc=" + userSrc + ", paswSrc=" + paswSrc + ", userDst=" + userDst + ", paswDst=" + paswDst + ", domainSrc=" + domainSrc + ", domainDest=" + domainDest + ", idSrc=" + idSrc + ", idDest=" + idDest + ", localPath=" + localPath + ", maxTemp=" + maxTemp + ", threadCount=" + threadCount + ", downloadPath=" + downloadPath + ", disableLog=" + disableLog + ", help=" + help + '}';
    }

    
    
    
    public static <T extends Enum<T>> List<String> enumNames(Class<T> cls) {
        return MakeStream.from(EnumSet.allOf(cls))
                .sorted(Comparator.comparing(m -> m.ordinal()))
                .map(m -> m.name())
                .toList();
    }

    public static <T extends Enum<T>> T enumFromString(Class<T> cls, String value) {
        Objects.requireNonNull(value);

        EnumSet<T> all = EnumSet.allOf(cls);

        for (T e : all) {
            if (StringUtils.equalsIgnoreCase(e.name(), value)) {
                return e;
            }
        }
        throw new IllegalArgumentException(value + " is not part of acceptable values:" + enumNames(cls));

    }

    public static class ModeConverter implements IStringConverter<Mode> {

        @Override
        public Mode convert(String value) {
            return enumFromString(Mode.class, value);
        }

    }
}
