package lt.lb.mavencopydeploy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lt.lb.commons.ArrayOp;
import lt.lb.commons.Java;
import lt.lb.commons.Log;
import lt.lb.commons.SafeOpt;
import lt.lb.commons.func.unchecked.UnsafeConsumer;
import lt.lb.commons.func.unchecked.UnsafeRunnable;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.jobsystem.Job;
import lt.lb.jobsystem.events.JobEvent;
import lt.lb.jobsystem.events.JobEventListener;
import lt.lb.jobsystem.JobExecutor;
import lt.lb.commons.parsing.StringOp;
import lt.lb.commons.threads.executors.FastWaitingExecutor;
import lt.lb.commons.threads.sync.WaitTime;
import lt.lb.jobsystem.Dependencies;
import lt.lb.jobsystem.ScheduledJobExecutor;
import lt.lb.jobsystem.events.SystemJobEventName;
import lt.lb.mavencopydeploy.net.BaseClient;
import lt.lb.mavencopydeploy.net.Deploy;
import lt.lb.mavencopydeploy.net.DownloadArtifact;
import lt.lb.mavencopydeploy.net.nexus2.ClientSetup2;
import lt.lb.mavencopydeploy.net.nexus3.ClientSetup3;

/**
 *
 * @author laim0nas100
 */
public class CopyRepo {

    static JobEventListener listenerStart = (JobEventListener) (JobEvent event) -> {
        Log.print("Start job " + event.getCreator().getUUID());
    };

    static JobEventListener listenerStop = (JobEventListener) (JobEvent event) -> {
        Log.print("End job " + event.getCreator().getUUID());
    };
    static JobEventListener listenerError = (JobEvent event) -> {
        SafeOpt<?> ofOptional = SafeOpt.ofOptional(event.getData());
        ofOptional.select(Throwable.class).ifPresent(m -> m.printStackTrace());
    };

    private static void jobDecorate(Job... jobs) {
        for (Job j : jobs) {
            j.addListener(SystemJobEventName.ON_EXECUTE, listenerStart);
            j.addListener(SystemJobEventName.ON_DONE, listenerStop);
            j.addListener(SystemJobEventName.ON_EXCEPTIONAL, listenerError);
        }
    }

    public static void copyRepo(Args arg) throws IOException, InterruptedException, TimeoutException {

        Objects.requireNonNull(arg.domainDest);

        arg.localPath = StringOp.appendIfMissing(arg.localPath, Java.getFileSeparator());
        arg.domainDest = StringOp.appendIfMissing(arg.domainDest, "/");
        
        RepoArgs src = RepoArgs.fromSource(arg);
        RepoArgs dest = RepoArgs.fromDest(arg);

        int maxTemp = arg.maxTemp;

        Files.createDirectories(Paths.get(arg.localPath));

        Deploy deploy = new Deploy();

        BaseClient clientSrcs;
        if (arg.versionSource == 3) {
            clientSrcs = new ClientSetup3(src.getCred());
        } else if (arg.versionSource == 2) {
            clientSrcs = new ClientSetup2(src.getCred());
            arg.domainSrc = StringOp.appendIfMissing(arg.domainSrc, "/");
        } else {
            throw new IllegalArgumentException("Only supported versions are 2,3");
        }
        Main.openedClients.add(clientSrcs);

        JobExecutor executor = new ScheduledJobExecutor(new FastWaitingExecutor(20, WaitTime.ofSeconds(4)));

        ReadOnlyIterator<DownloadArtifact> allArtifactsFromRepo = clientSrcs.getAllArtifacts(src.resolveUrl());

        long fileNum = 0;
        AtomicLong tempFiles = new AtomicLong(0);

        Stream<DownloadArtifact> filter = allArtifactsFromRepo.toStream()
                .filter(art -> !art.exclude(arg.excludedExt) && art.include(arg.includedExt));

        for (final DownloadArtifact artifactDown : ReadOnlyIterator.of(filter)) {
            fileNum++;
            String id = " " + fileNum + " @" + artifactDown.getRelativePath();
            final Path path = Paths.get(arg.localPath + fileNum);

            UnsafeConsumer<Job> download = k -> {
                Future downloadFile = clientSrcs.downloadFile(artifactDown.getDownloadURL(), path);
                downloadFile.get();
            };

            UnsafeConsumer<Job> upload = k -> {
                String where = path.toAbsolutePath().toString();
                String url = dest.resolveUrl() + artifactDown.getRelativePath();
                deploy.executeCurlUpload(dest.user, dest.password, where, url);
            };

            UnsafeConsumer<Job> delete = k -> {
                Files.delete(path);
            };

            Job jobDownload = new Job("download" + id, download);

            Job jobUpload = new Job("upload" + id, upload);

            Job jobDelete = new Job("delete" + id, delete);

            if (maxTemp > 0) {
                jobDownload.addDependency(() -> tempFiles.get() <= maxTemp);
            }

            jobUpload.addDependency(Dependencies.standard(jobDownload, SystemJobEventName.ON_SUCCESSFUL));

            jobDelete.addDependency(Dependencies.standard(jobDownload, SystemJobEventName.ON_SUCCESSFUL));

            jobDelete.addDependency(Dependencies.standard(jobUpload, SystemJobEventName.ON_DONE));

            jobDecorate(jobDownload, jobUpload, jobDelete);
            if (maxTemp > 0) {
                jobDownload.addListener(SystemJobEventName.ON_SUCCESSFUL, j -> tempFiles.incrementAndGet());
            }
            if (maxTemp > 0) {
                jobDelete.addListener(SystemJobEventName.ON_SUCCESSFUL, j -> tempFiles.decrementAndGet());
            }

            executor.submitAll(jobDownload, jobUpload, jobDelete);

        }
        executor.shutdownAndWait(100, TimeUnit.DAYS);
    }
}
