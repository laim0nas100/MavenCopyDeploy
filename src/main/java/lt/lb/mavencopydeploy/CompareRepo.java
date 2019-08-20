/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.mavencopydeploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lt.lb.commons.F;
import lt.lb.commons.Log;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.misc.ExtComparator;
import lt.lb.commons.parsing.StringOp;
import lt.lb.commons.threads.FastExecutor;
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
                .filter(art -> !StringOp.endsWithAny(art.getDownloadURL(), "md5","sha1"));

        return ReadOnlyIterator.of(filter);
    }
    
    public static Future<ArrayList<DownloadArtifact>> collect(Executor exe, ReadOnlyIterator<DownloadArtifact> iter){
        FutureTask<ArrayList<DownloadArtifact>> ofSupplier = Futures.ofSupplier(() -> iter.toArrayList());
        exe.execute(ofSupplier);
        return ofSupplier;
    }

    public static final ArrayList<ArtifactDiff> compare(RepoArgs source, RepoArgs dest) throws InterruptedException, ExecutionException {
        Cred credSource = new Cred(source.user, source.password);
        Cred credDest = new Cred(dest.user, dest.password);
        BaseClient clientSource = source.version == 3 ? new ClientSetup3(credSource) : new ClientSetup2(credSource);
        BaseClient clientDest = dest.version == 3 ? new ClientSetup3(credDest) : new ClientSetup2(credDest);

        Future<ArrayList<DownloadArtifact>> collectDst = collect(new FastExecutor(1),filterNoHashes(clientDest.getAllArtifacts(dest.repoUrl)));
        Future<ArrayList<DownloadArtifact>> collectSrc = collect(new FastExecutor(1),filterNoHashes(clientSource.getAllArtifacts(source.repoUrl)));
        HashMap<String, ArtifactDiff> map = new HashMap<>();

        F.iterate(collectSrc.get(), (i, art) -> {
            ArtifactDiff diff = new ArtifactDiff(art.getDownloadURL(), art.getRelativePath());
            diff.inSource = true;

            map.put(diff.getRelativePath(), diff);
            Log.print("src", i);

        });
        F.iterate(collectDst.get(), (i, art) -> {
            if (map.containsKey(art.getRelativePath())) {
                map.get(art.getRelativePath()).inDest = true;
            } else {
                ArtifactDiff diff = new ArtifactDiff(art.getDownloadURL(), art.getRelativePath());
                diff.inDest = true;
                map.put(art.getRelativePath(), diff);
            }
            Log.print("dst", i);
        });

        return map.values().stream().sorted(ExtComparator.ofValue(c -> c.getRelativePath())).collect(Collectors.toCollection(() -> new ArrayList<>()));

    }
}
