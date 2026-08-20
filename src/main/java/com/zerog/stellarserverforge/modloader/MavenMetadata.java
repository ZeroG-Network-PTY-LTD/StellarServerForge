package com.zerog.stellarserverforge.modloader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Maven {@code maven-metadata.xml} file: {@code versioning/release} and the full
 * {@code versioning/versions/version} list (spec §6.1-6.3).
 */
public final class MavenMetadata {

    private final String release;
    private final List<String> versions;

    private MavenMetadata(String release, List<String> versions) {
        this.release = release;
        this.versions = versions;
    }

    public String release() {
        return release;
    }

    public List<String> versions() {
        return versions;
    }

    public boolean contains(String version) {
        return versions.contains(version);
    }

    public static MavenMetadata parse(Path xmlFile) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().parse(xmlFile.toFile());
            doc.getDocumentElement().normalize();

            String release = firstTextContent(doc, "release");
            List<String> versions = new ArrayList<>();
            NodeList versionNodes = doc.getElementsByTagName("versions");
            if (versionNodes.getLength() > 0) {
                Element versionsEl = (Element) versionNodes.item(0);
                NodeList versionEls = versionsEl.getElementsByTagName("version");
                for (int i = 0; i < versionEls.getLength(); i++) {
                    versions.add(versionEls.item(i).getTextContent().trim());
                }
            }
            return new MavenMetadata(release, versions);
        } catch (Exception e) {
            throw new IOException("Failed to parse maven metadata: " + xmlFile, e);
        }
    }

    private static String firstTextContent(Document doc, String tag) {
        NodeList nodes = doc.getElementsByTagName(tag);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : null;
    }
}
