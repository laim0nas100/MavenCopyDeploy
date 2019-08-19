package lt.lb.mavencopydeploy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import lt.lb.commons.ArrayOp;
import lt.lb.commons.Log;
import lt.lb.commons.func.unchecked.UnsafeRunnable;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.jobs.Dependency;
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
        arg.localPath = StringOp.appendIfMissing(arg.localPath, File.separator);
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

        AtomicLong localCount = new AtomicLong();

        for (final DownloadArtifact artifactDown : allArtifactsFromRepo) {
            if(artifactDown.getDownloadURL().endsWith("md5")){
                continue;
            }
            if(artifactDown.getDownloadURL().endsWith("sha1")){
                continue;
            }
            final Path path = Paths.get(arg.localPath + fileNum);
            fileNum++;
            Job jobDownload = new Job("download-" + fileNum, j -> {
                Future downloadFile = clientSrcs.downloadFile(artifactDown.getDownloadURL(), path);
                downloadFile.get();
                localCount.incrementAndGet();
                return null;
            });
            
            Runnable uploadRunnable = ()->{
                String where = path.toAbsolutePath().toString();
                String url = arg.destUrl + artifactDown.getRelativePath();
                deploy.executeCurlUpload(arg.userDst, arg.paswDst, where, url);
            };
            UnsafeRunnable withTimeoutRepeatUntilDone = RunnableDecorators.withTimeoutRepeatUntilDone(WaitTime.ofMinutes(5), uploadRunnable);
            Job jobUpload = new Job("upload-" + fileNum,j->{
                withTimeoutRepeatUntilDone.unsafeRun();
                return null;
            });

            Job jobDelete = new Job("delete-" + fileNum, j -> {
                Files.delete(path);
                localCount.decrementAndGet();
                return null;
            });

            jobDownload.addDependency(() -> localCount.get() < 500);
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

        }
        executor.shutdown();
        executor.awaitTermination(WaitTime.ofDays(100));
        clientSrcs.close();
        Log.await(1, TimeUnit.DAYS);
    }
}
