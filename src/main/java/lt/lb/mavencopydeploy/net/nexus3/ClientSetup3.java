package lt.lb.mavencopydeploy.net.nexus3;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import lt.lb.commons.F;
import lt.lb.commons.containers.values.Value;
import lt.lb.commons.iteration.PagedIteration;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.mavencopydeploy.RepoArgs.Cred;
import lt.lb.mavencopydeploy.net.BaseClient;
import lt.lb.mavencopydeploy.net.DownloadArtifact;
import lt.lb.mavencopydeploy.net.nexus3.JsonType.ArtifactJson;
import okhttp3.ConnectionPool;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 *
 * @author laim0nas100
 */
public class ClientSetup3 extends BaseClient {

    private Gson gson;

    public ClientSetup3(Cred cred) {

        gson = new Gson();
        String credentials = Credentials.basic(cred.user, cred.pass);
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Interceptor.Chain chain) throws IOException {
                Request request = chain.request();
                Request authenticatedRequest = request.newBuilder()
                        .header("Authorization", credentials).build();
                return chain.proceed(authenticatedRequest);
            }
        };
        connectionPool = new ConnectionPool(Integer.MAX_VALUE, 5000, TimeUnit.DAYS);
        client = new OkHttpClient.Builder().connectionPool(connectionPool).addInterceptor(interceptor).build();

    }

    public ReadOnlyIterator<ArtifactJson> getAllArtifactsFromRepo(String repoFirstUrl) {

        PagedIteration<JsonType, ArtifactJson> iter = new PagedIteration<JsonType, ArtifactJson>() {
            @Override
            public JsonType getFirstPage() {
                return getRepositoryArtifacts(repoFirstUrl);
            }

            @Override
            public Iterator<ArtifactJson> getItems(JsonType info) {
                return info.items.iterator();
            }

            @Override
            public JsonType getNextPage(JsonType info) {
                return getRepositoryArtifacts(repoFirstUrl + "&continuationToken=" + info.continuationToken);
            }

            @Override
            public boolean hasNextPage(JsonType info) {
                return info.continuationToken != null;
            }
        };

        return ReadOnlyIterator.of(iter.toIterator());

    }

    public JsonType getRepositoryArtifacts(String fullUrl) {

        Value<JsonType> type = new Value<>();

        getUrlAndWait(fullUrl, resp -> {
            if (resp.isSuccessful()) {
                String str = F.unsafeCall(() -> resp.body().string());
                JsonType fromJson = gson.fromJson(str, JsonType.class);
                type.accept(fromJson);
            } else {
                throw new RuntimeException("Fail to call " + fullUrl);
            }
        });

        return type.get();

    }

    @Override
    public ReadOnlyIterator<DownloadArtifact> getAllArtifacts(String url) {
        return getAllArtifactsFromRepo(url).map(m -> new DownloadArtifact(m.downloadUrl, m.path));
    }
}
