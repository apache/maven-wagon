package org.apache.maven.wagon.providers.http.links;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.StringUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LinkParser {
    private Set/*<Pattern>*/ skips = new HashSet/*<Pattern>*/();

    public LinkParser() {
        // Apache Fancy Index Sort Headers
        skips.add(Pattern.compile("\\?C.*=.*"));

        // URLs with excessive paths.
        skips.add(Pattern.compile("/[^/]*/"));

        // URLs that to a parent directory.
        skips.add(Pattern.compile("\\.\\./"));
    }

    public Set/*<String>*/ collectLinks(URI baseURI, InputStream stream) throws SAXException,
            IOException {
        DOMParser parser = new DOMParser();
        parser.setFeature("http://cyberneko.org/html/features/augmentations", true);
        parser.setProperty("http://cyberneko.org/html/properties/names/elems", "upper");
        parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "upper");
        parser.parse(new InputSource(stream));

        Set/*<String>*/ links = new HashSet/*<String>*/();

        recursiveLinkCollector(parser.getDocument(), baseURI, links);

        return links;
    }

    private void recursiveLinkCollector(Node node, URI baseURI, Set/*<String>*/ links) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
//            System.out.println("Element <" + node.getNodeName() + dumpAttributes((Element) node) + ">");
            if ("A".equals(node.getNodeName())) {
                Element anchor = (Element) node;
                NamedNodeMap nodemap = anchor.getAttributes();
                Node href = nodemap.getNamedItem("HREF");
                if (href != null) {
                    String link = cleanLink(baseURI, href.getNodeValue());
//                    System.out.println("HREF (" + href.getNodeValue() + " => " + link + ")");
                    if (isAcceptableLink(link)) {
                        links.add(link);
                    }
                }
            }
        }

        Node child = node.getFirstChild();
        while (child != null) {
            recursiveLinkCollector(child, baseURI, links);
            child = child.getNextSibling();
        }
    }

//    private String dumpAttributes(Element elem) {
//        StringBuffer buf = new StringBuffer();
//        NamedNodeMap nodemap = elem.getAttributes();
//        int len = nodemap.getLength();
//        for (int i = 0; i < len; i++) {
//            Node att = nodemap.item(i);
//            buf.append(" ");
//            buf.append(att.getNodeName()).append("=\"");
//            buf.append(att.getNodeValue()).append("\"");
//        }
//        return buf.toString();
//    }

    public String cleanLink(URI baseURI, String link) {
        if (StringUtils.isEmpty(link)) {
            return "";
        }

        String ret = link;

        try {
            URI linkuri = new URI(ret);
            URI relativeURI = baseURI.relativize(linkuri).normalize();
            ret = relativeURI.toASCIIString();
            if (ret.startsWith(baseURI.getPath())) {
                ret = ret.substring(baseURI.getPath().length());
            }
        } catch (URISyntaxException e) {
        }

        return ret;
    }

    private boolean isAcceptableLink(String link) {
        if (StringUtils.isEmpty(link)) {
            return false;
        }

        for (Iterator it = skips.iterator(); it.hasNext(); ) {
            Pattern skipPat = (Pattern) it.next();
            if (skipPat.matcher(link).find()) {
                return false;
            }
        }

        return true;
    }
}
