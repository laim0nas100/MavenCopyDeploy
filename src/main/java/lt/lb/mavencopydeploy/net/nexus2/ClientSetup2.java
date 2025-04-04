package lt.lb.mavencopydeploy.net.nexus2;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import lt.lb.mavencopydeploy.RepoArgs.Cred;
import lt.lb.mavencopydeploy.net.BaseClient;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lt.lb.commons.ArrayOp;
import lt.lb.commons.containers.values.Value;
import lt.lb.commons.iteration.ChildrenIteratorProvider;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.mavencopydeploy.net.DownloadArtifact;
import lt.lb.uncheckedutils.Checked;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author laim0nas100
 */
public class ClientSetup2 extends BaseClient {

    DocumentBuilderFactory docBuilderFactory;

    public ClientSetup2(Cred cred) {
        super(cred);
        docBuilderFactory = DocumentBuilderFactory.newInstance();
    }

    public static ArrayList<Node> toList(NodeList nodes) {
        int size = nodes.getLength();
        ArrayList<Node> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(nodes.item(i));
        }
        return list;
    }

    public static List<String> getValue(Node node, String... path) {
        if (path.length == 0) {
            return Arrays.asList(node.getTextContent());
        } else {

            ArrayList<Node> list = toList(node.getChildNodes());

            ArrayList<String> text = new ArrayList<>();
            String[] newPath = ArrayOp.removeByIndex(path, 0);
            for (Node n : list) {
                if (n.getNodeName().equals(path[0])) {
                    text.addAll(getValue(n, newPath));
                }
            }
            return text;
        }

    }

    public ReadOnlyIterator<ArtifactType2> getArtifacts(String repoRootUrl) {
        ChildrenIteratorProvider<ArtifactType2> provider = new ChildrenIteratorProvider<ArtifactType2>() {
            @Override
            public Iterable<ArtifactType2> getChildren(ArtifactType2 t) {
                 if (t.leaf) {
                    return ReadOnlyIterator.of();
                }
                Value<ReadOnlyIterator<ArtifactType2>> value = new Value<>();
                getUrlAndWait(t.resourceURI, resp -> {

                    ReadOnlyIterator<ArtifactType2> artifacts = Checked.uncheckedCall(() -> {
                        String xml = resp.body().string();
                        DocumentBuilder dBuilder = docBuilderFactory.newDocumentBuilder();

                        Document doc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
                        ArrayList<Node> toList = toList(doc.getElementsByTagName("content-item"));
                        return ReadOnlyIterator.of(toList)
                                .map(m -> {
                                    ArtifactType2 art = new ArtifactType2();
                                    art.leaf = Boolean.parseBoolean(getValue(m, "leaf").get(0));
                                    art.relativePath = getValue(m, "relativePath").get(0);
                                    art.resourceURI = getValue(m, "resourceURI").get(0);
                                    return art;
                                });
                    });

                    value.set(artifacts);

                });
                return value.get();
            }
        };
        ArtifactType2 fakeRoot = new ArtifactType2();
        fakeRoot.leaf = false;
        fakeRoot.relativePath = "";
        fakeRoot.resourceURI = repoRootUrl;

        Stream<ArtifactType2> onlyArtifacts = provider.DFSiterator(fakeRoot).toStream().filter(p -> p.leaf);
        return ReadOnlyIterator.of(onlyArtifacts);
    }

    @Override
    public ReadOnlyIterator<DownloadArtifact> getAllArtifacts(String url) {
        return getArtifacts(url).map(m -> new DownloadArtifact(m.resourceURI, m.relativePath));
    }
}
