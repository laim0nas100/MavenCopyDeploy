package lt.lb.mavencopydeploy.net;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author laim0nas100
 */
public class DownloadArtifact{
    private String uri;
    private String relativePath;
    public DownloadArtifact(String uri, String relativePath){
        this.uri = uri;
        relativePath = StringUtils.removeStart(relativePath, "/");
        relativePath = StringUtils.removeEnd(relativePath, "/");
        this.relativePath = relativePath;
    }

    public String getURI() {
        return uri;
    }

    public void setURI(String uri) {
        this.uri = uri;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
    
    public boolean include(List<String> ends){
        if(ends.isEmpty()){
            return true;
        }
        return endsWithAny(ends);
    }
    
    public boolean exclude(List<String> ends){
        if(ends.isEmpty()){
            return true;
        }
        return endsWithAny(ends);
    }
    
    public boolean endsWithAny(List<String> ends){
        for(String end:ends){
            if(StringUtils.endsWith(this.relativePath, end)){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "DownloadArtifact{" + "uri=" + uri + ", relativePath=" + relativePath + '}';
    }
    
    
}
