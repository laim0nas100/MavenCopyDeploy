package lt.lb.mavencopydeploy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import lt.lb.commons.DLog;
import lt.lb.commons.F;
import lt.lb.commons.Java;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.threads.executors.FastWaitingExecutor;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.jobsystem.Dependencies;
import lt.lb.jobsystem.Job;
import lt.lb.jobsystem.JobExecutor;
import lt.lb.jobsystem.ScheduledJobExecutor;
import lt.lb.jobsystem.events.JobEvent;
import lt.lb.jobsystem.events.JobEventListener;
import lt.lb.jobsystem.events.SystemJobEventName;
import lt.lb.mavencopydeploy.net.BaseClient;
import lt.lb.mavencopydeploy.net.DownloadArtifact;
import lt.lb.mavencopydeploy.net.nexus2.ClientSetup2;
import lt.lb.mavencopydeploy.net.nexus3.ClientSetup3;
import lt.lb.uncheckedutils.func.UncheckedConsumer;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author laim0nas100
 */
public class DownloadRepo {

    static JobEventListener listenerStart = (JobEventListener) (JobEvent event) -> {
        DLog.print("Start job " + event.getCreator().getUUID());
    };

    static JobEventListener listenerStop = (JobEventListener) (JobEvent event) -> {
        DLog.print("End job " + event.getCreator().getUUID());
    };
    static JobEventListener listenerError = (JobEvent event) -> {
        event.getData().ifPresent(err -> {
            if (err instanceof Throwable) {
                Throwable th = F.cast(err);
                DLog.print("Error: " + th.getClass().getName() + " " + th.getMessage());
            }
        });
    };

    private static void jobDecorate(Job... jobs) {
        for (Job j : jobs) {
            j.addListener(SystemJobEventName.ON_EXECUTE, listenerStart);
            j.addListener(SystemJobEventName.ON_DONE, listenerStop);
            j.addListener(SystemJobEventName.ON_EXCEPTIONAL, listenerError);
        }
    }

    public static void downloadRepo(Args arg) throws IOException, InterruptedException, TimeoutException {

        Objects.requireNonNull(arg.domainSrc);

        arg.downloadPath = StringUtils.appendIfMissing(arg.downloadPath, Java.getFileSeparator());
        arg.domainDest = StringUtils.appendIfMissing(arg.domainDest, "/");

        RepoArgs src = RepoArgs.fromSource(arg);

        Files.createDirectories(Paths.get(arg.downloadPath));

        BaseClient clientSrcs;
        if (arg.versionSource == 3) {
            clientSrcs = new ClientSetup3(src.getCred());
        } else if (arg.versionSource == 2) {
            clientSrcs = new ClientSetup2(src.getCred());
            arg.domainSrc = StringUtils.appendIfMissing(arg.domainSrc, "/");
        } else {
            throw new IllegalArgumentException("Only supported versions are 2,3");
        }

        JobExecutor executor = new ScheduledJobExecutor(new FastWaitingExecutor(20, WaitTime.ofSeconds(4)));

        ReadOnlyIterator<DownloadArtifact> allArtifactsFromRepo = clientSrcs.getAllArtifacts(src.resolveUrl());

        long fileNum = 0;

        Stream<DownloadArtifact> filter = allArtifactsFromRepo.toStream()
                .filter(art -> !art.exclude(arg.excludedExt) && art.include(arg.includedExt));
        for (final DownloadArtifact artifactDown : ReadOnlyIterator.of(filter)) {
            fileNum++;
            String id = " " + fileNum + " @" + artifactDown.getRelativePath();
            String relativePath = arg.downloadPath + StringUtils.removeStart(artifactDown.getRelativePath(), Java.getFileSeparator());
            final Path path = Paths.get(relativePath);
            UncheckedConsumer<Job> createDirs = k -> {
                Files.createDirectories(path);
            };

            UncheckedConsumer<Job> download = k -> {
                Future downloadFile = clientSrcs.downloadFile(artifactDown.getURI(), path);
                downloadFile.get();
            };

            Job jobCreateDirs = new Job("createDirs" + id, createDirs);
            Job jobDownload = new Job("download" + id, download);

            jobDownload.addDependency(Dependencies.standard(jobCreateDirs, SystemJobEventName.ON_SUCCESSFUL));

            jobDecorate(jobCreateDirs, jobDownload);

            executor.submitAll(jobCreateDirs, jobDownload);

        }
        executor.shutdownAndWait(100, TimeUnit.DAYS);
        clientSrcs.close();
    }
}
