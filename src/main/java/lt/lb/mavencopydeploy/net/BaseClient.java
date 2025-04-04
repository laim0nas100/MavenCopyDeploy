package lt.lb.mavencopydeploy.net;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.mavencopydeploy.RepoArgs.Cred;
import lt.lb.uncheckedutils.Checked;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author laim0nas100
 */
public abstract class BaseClient {

    protected Cred cred;
    protected OkHttpClient client;
    protected ConnectionPool connectionPool;
    protected String credentials;
    protected String cookies="";

    public BaseClient(Cred cred) {
        this.cred = cred;
        credentials = Credentials.basic(cred.user, cred.pass);
        Interceptor interceptor = (Interceptor.Chain chain) -> chain.proceed(interceptImpl(chain).build());
        connectionPool = new ConnectionPool(Integer.MAX_VALUE, 5000, TimeUnit.DAYS);
        client = new OkHttpClient.Builder().connectionPool(connectionPool).addInterceptor(interceptor).build();
    }

    public Request.Builder interceptImpl(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder builder = request.newBuilder();

        builder.header("Authorization", credentials);
        builder.header("Origin", cred.origin);
        builder.header("Referer", cred.origin);
        if (StringUtils.isNotBlank(cookies)) {
            builder.header("Cookie", cookies);
        }

        return builder;
//                        .header("Host", cre.host)
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
                }).ifPresent(t -> {
                    future.completeExceptionally(t);
                    t.printStackTrace();
                });
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
                long writeAll = buffer.writeAll(resp.body().source());
                buffer.close();
            });
        });

    }

    public abstract ReadOnlyIterator<DownloadArtifact> getAllArtifacts(String url);
}
