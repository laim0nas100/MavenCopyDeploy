package lt.lb.mavencopydeploy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lt.lb.commons.F;
import lt.lb.commons.Java;
import lt.lb.commons.Log;
import lt.lb.commons.io.TextFileIO;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.misc.ExtComparator;
import lt.lb.commons.parsing.StringOp;
import lt.lb.commons.threads.executors.FastExecutor;
import lt.lb.commons.threads.Futures;
import lt.lb.mavencopydeploy.Main.Cred;
import lt.lb.mavencopydeploy.net.BaseClient;
import lt.lb.mavencopydeploy.net.DownloadArtifact;
import lt.lb.mavencopydeploy.net.nexus2.ClientSetup2;
import lt.lb.mavencopydeploy.net.nexus3.ClientSetup3;

/**
 *
 * @author Laimonas Beniušis
 */
public class CompareRepo {

    public static class ArtifactDiff extends DownloadArtifact {

        public boolean inSource;
        public boolean inDest;

        public ArtifactDiff(String downloadURL, String relativePath) {
            super(downloadURL, relativePath);
        }
    }

    public static ReadOnlyIterator<DownloadArtifact> filterNoHashes(ReadOnlyIterator<DownloadArtifact> artifacts) {
        Stream<DownloadArtifact> filter = artifacts
                .toStream()
                .filter(art -> !StringOp.endsWithAny(art.getDownloadURL(), "md5", "sha1"));

        return ReadOnlyIterator.of(filter);
    }

    public static Future<ArrayList<DownloadArtifact>> collect(Executor exe, ReadOnlyIterator<DownloadArtifact> iter) {
        FutureTask<ArrayList<DownloadArtifact>> ofSupplier = Futures.ofSupplier(() -> iter.toArrayList());
        exe.execute(ofSupplier);
        return ofSupplier;
    }

    public static final void compare(Args arg) throws InterruptedException, ExecutionException, FileNotFoundException, UnsupportedEncodingException {
        RepoArgs source = new RepoArgs();
        source.password = arg.paswSrc;
        source.user = arg.userSrc;
        source.repoUrl = arg.srcUrl;
        source.version = arg.versionSource;

        RepoArgs dest = new RepoArgs();
        dest.password = arg.paswDst;
        dest.user = arg.userDst;
        dest.repoUrl = arg.destUrl;
        dest.version = arg.versionDest;
        Executor exe = new FastExecutor(arg.threadCount);
        ArrayList<ArtifactDiff> compare = compare(exe, source, dest);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("In both:");
        compare.stream().filter(f -> f.inDest && f.inSource).map(m -> m.getRelativePath()).forEach(lines::add);
        lines.add("");
        lines.add("Only in source:");
        compare.stream().filter(f -> !f.inDest && f.inSource).map(m -> m.getRelativePath()).forEach(lines::add);
        lines.add("");
        lines.add("Only in destination:");
        compare.stream().filter(f -> f.inDest && !f.inSource).map(m -> m.getRelativePath()).forEach(lines::add);
        String finalPath = StringOp.appendIfMissing(arg.localPath, Java.getFileSeparator())+"compare.txt";
        TextFileIO.writeToFile(finalPath, lines);

    }

    public static final ArrayList<ArtifactDiff> compare(Executor exe, RepoArgs source, RepoArgs dest) throws InterruptedException, ExecutionException {
        Cred credSource = new Cred(source.user, source.password);
        Cred credDest = new Cred(dest.user, dest.password);
        BaseClient clientSource = source.version == 3 ? new ClientSetup3(credSource) : new ClientSetup2(credSource);
        BaseClient clientDest = dest.version == 3 ? new ClientSetup3(credDest) : new ClientSetup2(credDest);

        Future<ArrayList<DownloadArtifact>> collectDst = collect(exe, filterNoHashes(clientDest.getAllArtifacts(dest.repoUrl)));
        Future<ArrayList<DownloadArtifact>> collectSrc = collect(exe, filterNoHashes(clientSource.getAllArtifacts(source.repoUrl)));
        HashMap<String, ArtifactDiff> map = new HashMap<>();

        F.iterate(collectSrc.get(), (i, art) -> {
            ArtifactDiff diff = new ArtifactDiff(art.getDownloadURL(), art.getRelativePath());
            diff.inSource = true;

            map.put(diff.getRelativePath(), diff);

        });
        F.iterate(collectDst.get(), (i, art) -> {
            if (map.containsKey(art.getRelativePath())) {
                map.get(art.getRelativePath()).inDest = true;
            } else {
                ArtifactDiff diff = new ArtifactDiff(art.getDownloadURL(), art.getRelativePath());
                diff.inDest = true;
                map.put(art.getRelativePath(), diff);
            }
        });

        return map.values().stream().sorted(ExtComparator.ofValue(c -> c.getRelativePath())).collect(Collectors.toCollection(() -> new ArrayList<>()));

    }
}
