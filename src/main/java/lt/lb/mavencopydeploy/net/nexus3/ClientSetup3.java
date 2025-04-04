package lt.lb.mavencopydeploy.net.nexus3;

import com.google.gson.Gson;
import java.util.Base64;
import java.util.Iterator;
import lt.lb.commons.DLog;
import lt.lb.commons.containers.values.Value;
import lt.lb.commons.iteration.PagedIteration;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.iteration.streams.MakeStream;
import lt.lb.mavencopydeploy.RepoArgs.Cred;
import lt.lb.mavencopydeploy.net.BaseClient;
import lt.lb.mavencopydeploy.net.DownloadArtifact;
import lt.lb.mavencopydeploy.net.nexus3.JsonType.ArtifactJson;
import lt.lb.uncheckedutils.Checked;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author laim0nas100
 */
public class ClientSetup3 extends BaseClient {

    private Gson gson;

    public ClientSetup3(Cred cred) {
        super(cred);
        gson = new Gson();
        if(StringUtils.isBlank(cred.cookie)){
            authenticate();
        }else{
            cookies += cred.cookie;
        }

    }

    protected void authenticate() {
        RequestBody formBody = new FormBody.Builder()
                .add("username", Base64.getEncoder().encodeToString(cred.user.getBytes()))
                .add("password", Base64.getEncoder().encodeToString(cred.pass.getBytes()))
                .build();

        String url = cred.origin + "service/rapture/session";
        Request request = new Request.Builder()
                .post(formBody)
                .url(url)
                .build();

        Call newCall = client.newCall(request);
        Response resp = Checked.uncheckedCall(() -> {
            return newCall.execute();
        });

        DLog.print(resp);

        DLog.print(resp.headers());

        String get = resp.headers().get("Set-Cookie");
        
        String sessionID = MakeStream.from(StringUtils.split(get,';')).filter(f-> StringUtils.contains(f, "NXSESSIONID")).findFirst().get();

        cookies += sessionID;

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

        return ReadOnlyIterator.of(iter.iterator());

    }

    public JsonType getRepositoryArtifacts(String fullUrl) {

        Value<JsonType> type = new Value<>();

        getUrlAndWait(fullUrl, resp -> {
            if (resp.isSuccessful()) {
                String str = Checked.uncheckedCall(() -> resp.body().string());
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
