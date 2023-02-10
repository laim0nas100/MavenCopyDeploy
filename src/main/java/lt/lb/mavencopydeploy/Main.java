package lt.lb.mavencopydeploy;

import com.beust.jcommander.JCommander;
import java.nio.file.Files;
import java.nio.file.Paths;
import lt.lb.commons.DLog;

/**
 *
 * @author laim0nas100
 */
public class Main {

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
     *
     * @param arg
     * @throws Exception
     */
    public static void launch(Args arg) throws Exception {
        DLog.main().async = true;
        DLog.main().stackTrace = false;
        DLog.main().disable = arg.disableLog;

        switch (arg.mode) {
            case COPY: {
                Files.createDirectories(Paths.get(arg.localPath));
                CopyRepo.copyRepo(arg);
                break;
            }
            case COMPARE: {
                Files.createDirectories(Paths.get(arg.localPath));
                CompareRepo.compare(arg);
                break;
            }
            case DOWNLOAD: {
                DownloadRepo.downloadRepo(arg);
                break;
            }
        }
        DLog.close();
    }
}
