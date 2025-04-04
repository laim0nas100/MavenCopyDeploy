
# MavenCopyDeploy

Sonatype nexus repository migration/download tool. 

Uses REST API and curl command to upload files.
Supports nexus versions 3 and 2 (both ways).

For command line usage read [Args.java ](https://github.com/laim0nas100/MavenCopyDeploy/blob/master/src/main/java/lt/lb/mavencopydeploy/Args.java) file, or type "-help".

Ver. src| Ver. dest| Function| Supported
-|-|- |-
2|2| copy|yes
2|3| copy|yes
3|2| compare|yes
3|3| compare|yes
2|?|download|yes
3|?|download|yes
