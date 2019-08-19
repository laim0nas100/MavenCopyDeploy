/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.mavencopydeploy.net.nexus3;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import lt.lb.commons.F;
import lt.lb.commons.Log;
import lt.lb.commons.containers.values.StringValue;
import lt.lb.commons.containers.values.Value;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.mavencopydeploy.Main.Cred;
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

        JsonType first = getRepositoryArtifacts(repoFirstUrl);

        Value<ReadOnlyIterator<ArtifactJson>> items = new Value<>(ReadOnlyIterator.of(first.items));
        if (first.continuationToken == null) {
            return items.get();
        }
        final String cont = repoFirstUrl + "&continuationToken=";
        StringValue nextToken = new StringValue(first.continuationToken);

        Iterator<ArtifactJson> iterator = new Iterator<ArtifactJson>() {
            @Override
            public boolean hasNext() {
                if (items.get().hasNext()) {
                    return true;
                }

                if (nextToken.isNotNull()) {
                    return true;
                }
                return false;
            }

            @Override
            public ArtifactJson next() {
                if (items.get().hasNext()) {
                    return items.get().getNext();
                }

                if (hasNext()) { // need to make request
                    JsonType newArtifacts = getRepositoryArtifacts(cont + nextToken.get());
                    items.set(ReadOnlyIterator.of(newArtifacts.items));
                    nextToken.set(newArtifacts.continuationToken);
                    return next();
                } else {
                    throw new NoSuchElementException();
                }
            }

        };

        return ReadOnlyIterator.of(iterator);

    }

    public JsonType getRepositoryArtifacts(String fullUrl) {

        Value<JsonType> type = new Value<>();

        getUrlAndWait(fullUrl, resp -> {
            if (resp.isSuccessful()) {
                String str = F.unsafeCall(() -> resp.body().string());
                JsonType fromJson = gson.fromJson(str, JsonType.class);
                type.accept(fromJson);
            } else {
                Log.print("Failed response");
            }
        });

        return type.get();

    }

    @Override
    public ReadOnlyIterator<DownloadArtifact> getAllArtifacts(String url) {
        return getAllArtifactsFromRepo(url).map(m -> new DownloadArtifact(m.downloadUrl, m.path));
    }
}
