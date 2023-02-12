package lt.lb.mavencopydeploy.net;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.mavencopydeploy.RepoArgs.Cred;
import lt.lb.uncheckedutils.Checked;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

/**
 *
 * @author laim0nas100
 */
public abstract class BaseClient {

    protected Cred cred;
    protected OkHttpClient client;
    protected ConnectionPool connectionPool;

    public BaseClient(Cred cred) {
        this.cred = cred;
    }
    
    public void close() throws IOException {
        client.dispatcher().cancelAll();
        client.dispatcher().executorService().shutdown();
        connectionPool.evictAll();
    }

    public void getUrlAndWait(String url, Consumer<Response> cons) {
        Checked.uncheckedRun(() -> getUrl(url, cons).get());
    }

    public Future getUrl(String url, Consumer<Response> cons) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        CompletableFuture future = new CompletableFuture();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException ioe) {
                ioe.printStackTrace();
                future.complete(null);
            }

            @Override
            public void onResponse(Call call, Response resp) throws IOException {
                Checked.checkedRun(() -> {
                    cons.accept(resp);
                }).ifPresent(t -> future.completeExceptionally(t));
                Checked.checkedRun(resp::close);
                future.complete(null);

            }
        });
        return future;

    }

    public Future downloadFile(String url, OutputStream output) {

        return getUrl(url, resp -> {
            Checked.uncheckedRun(() -> {
                BufferedSink buffer = Okio.buffer(Okio.sink(output));
                buffer.writeAll(resp.body().source());
                buffer.close();
            });
        });

    }

    public Future downloadFile(String url, Path file) {

        return getUrl(url, resp -> {
            Checked.uncheckedRun(() -> {
                BufferedSink buffer = Okio.buffer(Okio.sink(file, StandardOpenOption.CREATE_NEW));
                buffer.writeAll(resp.body().source());
                buffer.close();
            });
        });

    }

    public abstract ReadOnlyIterator<DownloadArtifact> getAllArtifacts(String url);
}
