/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.mavencopydeploy.net;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import lt.lb.commons.F;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.threads.Futures;
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
 * @author Lemmin
 */
public abstract class BaseClient {
    protected OkHttpClient client;
    protected ConnectionPool connectionPool;
    
    public void close() throws IOException {
        client.dispatcher().cancelAll();
        client.dispatcher().executorService().shutdown();
        connectionPool.evictAll();
    }

    public void getUrlAndWait(String url, Consumer<Response> cons) {
        F.unsafeRun(() -> getUrl(url, cons).get());
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
                F.checkedRun(() -> {
                    cons.accept(resp);
                }).ifPresent(t->future.completeExceptionally(t));
                F.checkedRun(resp::close);
                future.complete(null);

            }
        });
        return future;

    }

    public Future downloadFile(String url, Path file) {

        return getUrl(url, resp -> {
            F.unsafeRun(() -> {
                BufferedSink buffer = Okio.buffer(Okio.sink(file, StandardOpenOption.CREATE_NEW));
                buffer.writeAll(resp.body().source());
                buffer.close();
            });
        });

    }
    
    public abstract ReadOnlyIterator<DownloadArtifact> getAllArtifacts(String url);
}
