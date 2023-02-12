package lt.lb.mavencopydeploy.net;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.function.Predicate;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.threads.Futures;
import lt.lb.uncheckedutils.Checked;

/**
 *
 * @author laim0nas100
 */
public interface ArtifactRepo extends AutoCloseable {

    ReadOnlyIterator<DownloadArtifact> getAllArtifacts();

    public FutureTask downloadFile(DownloadArtifact artifact, OutputStream output);

    public default FutureTask downloadFile(DownloadArtifact artifact, Path file) throws IOException {
        return downloadFile(artifact, Files.newOutputStream(file, StandardOpenOption.CREATE_NEW));
    }

    public String getRoot();

    public FutureTask upload(Path file, String url);

    public default ArtifactRepo filter(Predicate<DownloadArtifact> filter) {
        ArtifactRepo me = this;
        return new ArtifactRepo() {
            @Override
            public ReadOnlyIterator<DownloadArtifact> getAllArtifacts() {
                return ReadOnlyIterator.of(me.getAllArtifacts().toStream().filter(filter));
            }

            @Override
            public FutureTask downloadFile(DownloadArtifact artifact, OutputStream output) {
                return me.downloadFile(artifact, output);
            }

            @Override
            public void close() throws Exception {
                me.close();
            }

            @Override
            public FutureTask upload(Path file, String url) {
                return me.upload(file, url);
            }

            @Override
            public String getRoot() {
                return me.getRoot();
            }

        };
    }

    public static class OnlineRepo implements ArtifactRepo {

        protected BaseClient client;
        protected String rootUrl;
        protected Deploy deploy = new Deploy();

        public OnlineRepo(BaseClient client, String rootUrl) {
            this.client = client;
            this.rootUrl = rootUrl;
        }

        @Override
        public ReadOnlyIterator<DownloadArtifact> getAllArtifacts() {
            return client.getAllArtifacts(rootUrl);
        }

        @Override
        public FutureTask downloadFile(DownloadArtifact artifact, OutputStream output) {
            return Futures.ofCallable(() -> client.downloadFile(rootUrl, output));
        }

        @Override
        public void close() throws IOException {
            client.close();
        }

        @Override
        public FutureTask upload(Path file, String url) {
            return Futures.ofRunnable(() -> {
                deploy.executeCurlUpload(client.cred.user, client.cred.pass, file.toAbsolutePath().toString(), url);
            });
        }

        @Override
        public String getRoot() {
            return rootUrl;
        }

    }

    public static class FileRepo implements ArtifactRepo {

        protected Path rootPath;

        public FileRepo(Path rootPath) {
            this.rootPath = rootPath;
        }

        @Override
        public ReadOnlyIterator<DownloadArtifact> getAllArtifacts() {
            List<DownloadArtifact> list = new ArrayList<>();
            Checked.checkedRun(() -> {
                Files.walkFileTree(rootPath, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String fullPath = file.toAbsolutePath().toString();
                        String relative = rootPath.relativize(file).toString();
                        list.add(new DownloadArtifact(relative, fullPath));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
            });

            return ReadOnlyIterator.of(list);
        }

        @Override
        public FutureTask downloadFile(DownloadArtifact artifact, OutputStream output) {

            return Futures.ofCallable(() -> {
                Path get = Paths.get(artifact.getURI());
                return Files.copy(get, output);
            });

        }

        @Override
        public void close() {
        }

        @Override
        public FutureTask upload(Path file, String url) {
            return Futures.ofCallable(() -> {
                return Files.copy(file, Paths.get(url));
            });
        }

        @Override
        public String getRoot() {
            return rootPath.toAbsolutePath().toString();
        }

    }
}
