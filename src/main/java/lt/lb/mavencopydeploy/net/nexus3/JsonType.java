package lt.lb.mavencopydeploy.net.nexus3;

import java.util.ArrayList;

/**
 *
 * @author laim0nas100
 */
public class JsonType {
    
    public ArrayList<ArtifactJson> items = new ArrayList<>();
    public String continuationToken;
    
    public static class Checksum{
        public String sha1;
        public String md5;
    }
    
    public static class ArtifactJson{
        public String downloadUrl;
        public String path;
        public String id;
        public String repository;
        public String format;
        public Checksum checksum;
    }
    
            
}
