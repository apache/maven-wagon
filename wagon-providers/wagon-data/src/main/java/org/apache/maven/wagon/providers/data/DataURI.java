package org.apache.maven.wagon.providers.data;

import static org.apache.maven.wagon.providers.data.CharsetAlias.charsetAlias;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.Properties;

import org.codehaus.plexus.util.Base64;

/**
 * Class for parsing and representing data URLs.
 */

public class DataURI {
    private static final String SCHEME = "data:";
    private static final String DEFAULT_CONTENTTYPE = "text/plain";
    private static final String DEFAULT_CHARSET = "US-ASCII";

    private static final String PARAMNAME_CONTENTTYPE = "Content-Type";
    private static final String PARAMNAME_CHARSET = "charset";
    private static final String PARAMNAME_CONTENTENCODING = "Content-Encoding";

    private static final String BASE64_ENCODING = "base64";

    private final String contents;
    private final String mimetype;

    public static final DataURI parse(URL url) throws ParseException,
	    UnsupportedEncodingException {
	return parse(url.toExternalForm());
    }

    public static final DataURI parse(String dataurl) throws ParseException,
	    UnsupportedEncodingException {
	if (!dataurl.startsWith(SCHEME)) {
	    throw new ParseException("Data URI must starts with: '" + SCHEME
		    + "'", 0);
	}
	// dataurl := "data:" [ mediatype ] [ ";base64" ] "," data
	// mediatype := [ type "/" subtype ] *( ";" parameter )
	// data := *urlchar
	// parameter := attribute "=" value
	String mediatype = null;
	final Properties params = new Properties();

	// cut off "data:"
	final int idxC = dataurl.indexOf(',');
	final String urlContent;
	if (idxC < 0) {
	    if (dataurl.length() == SCHEME.length()) {
		urlContent = "";
	    } else {
		throw new ParseException("Leading comma must be present",
			dataurl.length());
	    }
	} else {
	    for (String param : dataurl.substring(SCHEME.length(), idxC).split(
		    ";")) {
		if (param.indexOf('=') > 0) {
		    int idx = param.indexOf('=');
		    params.setProperty(param.substring(0, idx),
			    param.substring(idx + 1));
		    continue;
		}
		if (mediatype == null && param.length() > 0) {
		    params.setProperty(PARAMNAME_CONTENTENCODING, param);
		    continue;
		}
		if (BASE64_ENCODING.equals(param)) {
		    params.setProperty(PARAMNAME_CONTENTENCODING,
			    BASE64_ENCODING);
		    continue;
		}
	    }
	    urlContent = dataurl.substring(idxC + 1);
	}
	params.setProperty(PARAMNAME_CONTENTTYPE,
		mediatype == null ? DEFAULT_CONTENTTYPE : mediatype);

	final String charset = charsetAlias.get(
		params.getProperty(PARAMNAME_CHARSET, DEFAULT_CHARSET),
		DEFAULT_CHARSET);
	final String contents;
	if (BASE64_ENCODING.equals(params
		.getProperty(PARAMNAME_CONTENTENCODING))) {
	    final byte[] b = urlContent.getBytes(charset);
	    if (!Base64.isArrayByteBase64(b)) {
		throw new ParseException("invalid base64 String", idxC);
	    }
	    contents = new String(Base64.decodeBase64(b), charset);
	    // TODO change base64 check with:
	    // http://www.perlmonks.org/?node_id=775835
	    if (!urlContent.equals(new String(Base64.encodeBase64(contents
		    .getBytes(charset)), charset))) {
		throw new ParseException("invalid base64 String", idxC);
	    }
	} else {
	    contents = URLDecoder.decode(urlContent, charset);
	}
	return new DataURI(params.getProperty(PARAMNAME_CONTENTTYPE), contents);
    }

    public DataURI(String mimetype, String content) {
	this.mimetype = mimetype;
	this.contents = content;
    }

    public String getMimetype() {
	return mimetype;
    }

    public String getContents() {
	return contents;
    }
}