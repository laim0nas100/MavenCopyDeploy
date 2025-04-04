package lt.lb.mavencopydeploy;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author laim0nas100
 */
public class RepoArgs {

    public String user;
    public String password;
    public String repoDomain;
    public String repoId;
    public Integer version = 3;

    public boolean write = false;

    public String resolveUrl() {
        String domain = StringUtils.appendIfMissing(repoDomain, "/");
        if (version == 3) {
            if (!write) {
                return domain + String.format("service/rest/v1/assets?repository=%s", repoId);
            } else {
                return domain + String.format("repository/%s/", repoId);
            }
        } else if (version == 2) {
            return domain + String.format("nexus/service/local/repositories/%s/content", repoId);
        }
        throw new IllegalArgumentException("Fail to resolve url for " + domain + " with id " + repoId);
    }

    public RepoArgs() {

    }

    public static RepoArgs fromSource(Args arg) {
        RepoArgs source = new RepoArgs();
        source.password = arg.paswSrc;
        source.user = arg.userSrc;
        source.repoDomain = arg.domainSrc;
        source.version = arg.versionSource;
        source.repoId = arg.idSrc;
        source.write = false;

        return source;
    }

    public static RepoArgs fromDest(Args arg) {
        RepoArgs dest = new RepoArgs();
        dest.password = arg.paswDst;
        dest.user = arg.userDst;
        dest.repoDomain = arg.domainDest;
        dest.version = arg.versionDest;
        dest.repoId = arg.idDest;
        dest.write = arg.mode == Args.Mode.COPY;

        return dest;
    }

    public Cred getCred() {
        Cred cred = new Cred(user, password);
        cred.origin = repoDomain;
        cred.host = cred.origin;
        cred.host = StringUtils.removeStart(cred.host, "https://");
        cred.host = StringUtils.removeStart(cred.host, "http://");
        cred.host = StringUtils.removeEnd(cred.host, "/");
        cred.repoID = repoId;
        return cred;
    }

    public static class Cred {

        public String origin;
        public String host;
        public String user, pass;
        
        public String cookie;
        public String repoID;
        
        public Cred(String user, String pass) {
            this.user = user;
            this.pass = pass;
        }

        public Cred() {
        }
        
    }
}
