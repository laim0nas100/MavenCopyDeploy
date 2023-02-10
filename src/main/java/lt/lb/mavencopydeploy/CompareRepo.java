package lt.lb.mavencopydeploy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import lt.lb.commons.DLog;
import lt.lb.commons.Java;
import lt.lb.commons.io.TextFileIO;
import lt.lb.commons.iteration.For;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.threads.executors.FastExecutor;
import lt.lb.commons.threads.Futures;
import lt.lb.mavencopydeploy.net.BaseClient;
import lt.lb.mavencopydeploy.net.DownloadArtifact;
import lt.lb.mavencopydeploy.net.nexus2.ClientSetup2;
import lt.lb.mavencopydeploy.net.nexus3.ClientSetup3;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author laim0nas100
 */
public class CompareRepo {

    public static class ArtifactDiff extends DownloadArtifact {

        public boolean inSource;
        public boolean inDest;

        public ArtifactDiff(String downloadURL, String relativePath) {
            super(downloadURL, relativePath);
        }
    }

    static Future<ArrayList<DownloadArtifact>> collect(Executor exe, ReadOnlyIterator<DownloadArtifact> iter) {
        FutureTask<ArrayList<DownloadArtifact>> ofSupplier = Futures.ofSupplier(() -> iter.toArrayList());
        exe.execute(ofSupplier);
        return ofSupplier;
    }

    public static final void compare(Args arg) throws InterruptedException, ExecutionException, FileNotFoundException, UnsupportedEncodingException, IOException {

        ArrayList<ArtifactDiff> compare = compareTruly(arg);
//        .filter(art -> !art.exclude(arg.excludedExt) && art.include(arg.includedExt))
        String finalPath = StringUtils.appendIfMissing(arg.localPath, Java.getFileSeparator()) + "compare.txt";

        DLog.print("Write compared repo information to " + finalPath);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("In both:");
        compare.stream().filter(f -> f.inDest && f.inSource).map(m -> m.getRelativePath()).forEach(lines::add);
        lines.add("");
        lines.add("Only in source:");
        compare.stream().filter(f -> !f.inDest && f.inSource).map(m -> m.getRelativePath()).forEach(lines::add);
        lines.add("");
        lines.add("Only in destination:");
        compare.stream().filter(f -> f.inDest && !f.inSource).map(m -> m.getRelativePath()).forEach(lines::add);

        TextFileIO.writeToFile(finalPath, lines);

    }

    static final ArrayList<ArtifactDiff> compareTruly(Args arg) throws InterruptedException, ExecutionException, IOException {
        RepoArgs source = RepoArgs.fromSource(arg);
        

        RepoArgs dest = RepoArgs.fromDest(arg);
        
        Executor exe = new FastExecutor(arg.threadCount);
        BaseClient clientSource = source.version == 3 ? new ClientSetup3(source.getCred()) : new ClientSetup2(source.getCred());
        BaseClient clientDest = dest.version == 3 ? new ClientSetup3(dest.getCred()) : new ClientSetup2(dest.getCred());
        HashMap<String, ArtifactDiff> map = new HashMap<>();

        FutureTask<List<DownloadArtifact>> taskDest = new FutureTask<>(() -> {
            return clientDest.getAllArtifacts(dest.resolveUrl()).toStream()
                    .filter(art -> !art.exclude(arg.excludedExt) && art.include(arg.includedExt))
                    .map(m -> {
                        DLog.print("Reading dest " + m.getRelativePath());
                        return m;
                    }).collect(Collectors.toList());
        });

        FutureTask<List<DownloadArtifact>> taskSource = new FutureTask<>(() -> {
            return clientSource.getAllArtifacts(source.resolveUrl()).toStream()
                    .filter(art -> !art.exclude(arg.excludedExt) && art.include(arg.includedExt))
                    .map(m -> {
                        DLog.print("Reading src " + m.getRelativePath());
                        return m;
                    }).collect(Collectors.toList());
        });

        exe.execute(taskDest);

        exe.execute(taskSource);

        For.elements().iterate(taskSource.get(), (i, art) -> {
            ArtifactDiff diff = new ArtifactDiff(art.getDownloadURL(), art.getRelativePath());
            diff.inSource = true;
            map.put(diff.getRelativePath(), diff);

        });
        For.elements().iterate(taskDest.get(), (i, art) -> {
            if (map.containsKey(art.getRelativePath())) {
                map.get(art.getRelativePath()).inDest = true;
            } else {
                ArtifactDiff diff = new ArtifactDiff(art.getDownloadURL(), art.getRelativePath());
                diff.inDest = true;
                map.put(art.getRelativePath(), diff);
            }
        });

        ArrayList<ArtifactDiff> collect = map.values().stream().sorted(Comparator.comparing(c -> c.getRelativePath())).collect(Collectors.toCollection(() -> new ArrayList<>()));

        clientDest.close();
        clientSource.close();
        return collect;

    }
}
