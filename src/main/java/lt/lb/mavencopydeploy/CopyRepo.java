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
import lt.lb.commons.func.unchecked.UnsafeRunnable;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.jobs.Job;
import lt.lb.commons.jobs.JobEvent;
import lt.lb.commons.jobs.JobEventListener;
import lt.lb.commons.jobs.JobExecutor;
import lt.lb.commons.jobs.Jobs;
import lt.lb.commons.parsing.StringOp;
import lt.lb.commons.threads.FastWaitingExecutor;
import lt.lb.commons.threads.RunnableDecorators;
import lt.lb.commons.threads.sync.WaitTime;
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

    private static void jobDecorate(Consumer<Job> jobCons, Job... jobs) {
        for (Job j : jobs) {
            jobCons.accept(j);
        }
    }

    public static void copyRepo(Args arg) throws IOException, InterruptedException, TimeoutException {

        Objects.requireNonNull(arg.destUrl);

        arg.localPath = StringOp.appendIfMissing(arg.localPath, Java.getFileSeparator());
        arg.destUrl = StringOp.appendIfMissing(arg.destUrl, "/");

        Files.createDirectories(Paths.get(arg.localPath));

        Main.Cred cred = new Main.Cred(arg.userSrc, arg.paswSrc);
        Deploy deploy = new Deploy();

        BaseClient clientSrcs;
        if (arg.version == 3) {
            clientSrcs = new ClientSetup3(cred);
        } else if (arg.version == 2) {
            clientSrcs = new ClientSetup2(cred);
            arg.srcUrl = StringOp.appendIfMissing(arg.srcUrl, "/");
        } else {
            throw new IllegalArgumentException("Only supported versions are 2,3");
        }

        JobExecutor executor = new JobExecutor(new FastWaitingExecutor(20, WaitTime.ofSeconds(4)));

        ReadOnlyIterator<DownloadArtifact> allArtifactsFromRepo = clientSrcs.getAllArtifacts(arg.srcUrl);

        long fileNum = 0;

        Stream<DownloadArtifact> filter = allArtifactsFromRepo
                .toStream()
                .filter(art -> !art.getDownloadURL().endsWith("md5") || art.getDownloadURL().endsWith("sha1"));

        for (final DownloadArtifact artifactDown : ReadOnlyIterator.of(filter)) {
            String relativePath = artifactDown.getRelativePath();
            final Path path = Paths.get(arg.localPath + fileNum);
            fileNum++;
            Job jobDownload = new Job("download " + fileNum + " @" + relativePath, j -> {
                Future downloadFile = clientSrcs.downloadFile(artifactDown.getDownloadURL(), path);
                downloadFile.get();

                return null;
            });

            Job jobUpload = new Job("upload " + fileNum + " @" + relativePath, j -> {
                String where = path.toAbsolutePath().toString();
                String url = arg.destUrl + artifactDown.getRelativePath();
                deploy.executeCurlUpload(arg.userDst, arg.paswDst, where, url);
                return null;
            });

            Job jobDelete = new Job("delete " + fileNum + " @" + relativePath, j -> {
                Files.delete(path);
                return null;
            });

            jobDownload.chainForward(jobUpload);
            jobDownload.chainForward(jobDelete); //cancel if failed

            JobEventListener listenerStart = new JobEventListener() {
                @Override
                public void onEvent(JobEvent event) {
                    Log.print("Start job " + event.getCreator().getUUID());
                }
            };

            JobEventListener listenerStop = new JobEventListener() {
                @Override
                public void onEvent(JobEvent event) {
                    Log.print("End job " + event.getCreator().getUUID());
                }
            };
            jobDelete.addDependency(Jobs.standard(jobUpload, JobEvent.ON_DONE)); // must also wait until upload is done (fails/canceled/succeeded)
            JobEventListener jobEventListener = (JobEvent event) -> {
                event.getData().filter(m -> m instanceof Throwable).map(m -> (Throwable) m).ifPresent(c -> c.printStackTrace());
            };

            Job[] arr = ArrayOp.asArray(jobDownload, jobUpload, jobDelete);

            jobDecorate(j -> j.addListener(JobEvent.ON_FAILED, jobEventListener), arr);
            jobDecorate(j -> j.addListener(JobEvent.ON_EXECUTE, listenerStart), arr);
            jobDecorate(j -> j.addListener(JobEvent.ON_SUCCEEDED, listenerStop), arr);

            executor.submit(jobDownload);
            executor.submit(jobUpload);
            executor.submit(jobDelete);
            
            if(fileNum % 500 == 0){
                executor.awaitJobEmptiness(WaitTime.ofDays(100));
            }

        }
        executor.shutdown();
        executor.awaitTermination(WaitTime.ofDays(100));
        clientSrcs.close();
        Log.await(1, TimeUnit.DAYS);
    }
}
