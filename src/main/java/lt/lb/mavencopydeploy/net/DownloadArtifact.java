package lt.lb.mavencopydeploy.net;

import lt.lb.commons.parsing.StringOp;

/**
 *
 * @author laim0nas100
 */
public class DownloadArtifact {
    private String downloadURL;
    private String relativePath;
    public DownloadArtifact(String downloadURL, String relativePath){
        this.downloadURL = downloadURL;
        relativePath = StringOp.removeStart(relativePath, "/");
        relativePath = StringOp.removeEnd(relativePath, "/");
        this.relativePath = relativePath;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
    
    
}
