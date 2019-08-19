/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.mavencopydeploy.net.nexus3;

import java.util.ArrayList;

/**
 *
 * @author Lemmin
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
