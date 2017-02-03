package org.apache.maven.wagon.providers.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import org.junit.Test;

/**
 * @see http
 *      ://www-archive.mozilla.org/quality/networking/testing/datatests.html
 */
public class DataURITest {

	@Test
	public void testParseFromRFC() throws UnsupportedEncodingException,
			ParseException {
		assertEquals("examples (from RFC): 'A brief note'", //
				"A brief note" //
				, DataURI //
						.parse("data:,A%20brief%20note") //
						.getContents() //
		);
		assertEquals("examples (from RFC): Greek sample text 'Έχώ'", //
				"Έχώ" //
				, DataURI //
						.parse("data:text/plain;charset=iso-8859-7,%b8%f7%fe") //
						.getContents() //
		);
	}

	@Test
	public void testParseSchemeOnly() throws UnsupportedEncodingException,
			ParseException {
		assertEquals("scheme only", //
				"" //
				, DataURI //
						.parse("data:") //
						.getContents() //
		);
	}

	@Test
	public void testParseDataWithoutDataSegment()
			throws UnsupportedEncodingException, ParseException {

		assertEquals("data w/o data segment", //
				"" //
				, DataURI //
						.parse("data:,") //
						.getContents() //
		);
		assertEquals("data w/o data segment", "" //
				, DataURI //
						.parse("data:text/plain,") //
						.getContents() //
		);
		assertEquals("data w/o data segment", //
				"" //
				, DataURI //
						.parse("data:;base64,") //
						.getContents() //
		);
	}

	@Test
	public void testParseReservedChar() throws UnsupportedEncodingException,
			ParseException {
		assertEquals("data w/ traditionally reserved characters like ';'", //
				";test" //
				, DataURI //
						.parse("data:,;test") //
						.getContents() //
		);
	}

	@Test
	public void testParseUnneededChar() throws UnsupportedEncodingException,
			ParseException {
		assertEquals("data w/ unneeded ';'", //
				"test" //
				, DataURI //
						.parse("data:;,test") //
						.getContents() //
		);
	}

	@Test
	public void testParseDefulatCharset() throws UnsupportedEncodingException,
			ParseException {

		assertEquals("default mediatype w/ default character set", //
				"test" //
				, DataURI //
						.parse("data:text/plain,test") //
						.getContents() //
		);
		assertEquals("default mediatype w/ default character set", //
				"test" //
				, DataURI //
						.parse("data:text/plain;charset=US-ASCII,test") //
						.getContents() //
		);

	}

	@Test
	public void testParseMultipleCommas() throws UnsupportedEncodingException,
			ParseException {
		assertEquals("multiple commas", //
				"a,b" //
				, DataURI //
						.parse("data:,a,b") //
						.getContents() //
		);
	}

	@Test
	public void testParseBase64() throws UnsupportedEncodingException,
			ParseException {
		assertEquals("base64", //
				"" //
				, DataURI //
						.parse("data:;base64,") //
						.getContents() //
		);
		assertEquals("base64", //
				"This is a test\n" //
				, DataURI //
						.parse("data:text/html;base64,VGhpcyBpcyBhIHRlc3QK") //
						.getContents() //
		);
	}

	@Test
	public void testParseNullCharset() throws UnsupportedEncodingException,
			ParseException {
		assertEquals("null characters ignored.", //
				"test" //
				, DataURI //
						.parse("data:;charset=,test") //
						.getContents() //
		);
	}

	@Test
	public void testParseISO88598I() throws UnsupportedEncodingException,
			ParseException {
		assertEquals(
				"ISO-8859-8 in Base64 ", //
				"\u05E9\u05DC\u05D5\u05DD" //
				,
				DataURI //
				.parse("data:text/plain;charset=iso-8859-8-i;base64,+ezl7Q==") //
						.getContents() //
		);
		assertEquals(
				"ISO-8859-8 in URL-encoding ", //
				"\u05E9\u05DC\u05D5\u05DD" //
				,
				DataURI //
				.parse("data:text/plain;charset=iso-8859-8-i,%f9%ec%e5%ed") //
						.getContents() //
		);
	}

	@Test
	public void testParseUTF8() throws UnsupportedEncodingException,
			ParseException {
		assertEquals(
				"UTF-8 in Base64", //
				"\u05E9\u05DC\u05D5\u05DD" //
				,
				DataURI //
				.parse("data:text/plain;charset=UTF-8;base64,16nXnNeV150=") //
						.getContents() //
		);
		assertEquals(
				"UTF-8 in URL-encoding", //
				"\u05E9\u05DC\u05D5\u05DD" //
				,
				DataURI //
				.parse("data:text/plain;charset=UTF-8,%d7%a9%d7%9c%d7%95%d7%9d") //
						.getContents() //
		);
	}

	@Test
	public void testParseMimetype() throws UnsupportedEncodingException,
			ParseException {
		assertEquals(
				"text/html", //
				"<html><head><title>Test</title></head><body><p>This is a test</body></html>\n", //
				DataURI //
				.parse("data:text/html;base64,PGh0bWw+PGhlYWQ+PHRpdGxlPlRlc3Q8L3RpdGxlPjwvaGVhZD48Ym9keT48cD5UaGlzIGlzIGEgdGVzdDwvYm9keT48L2h0bWw+Cg==") //
						.getContents() //
		);
		assertEquals("SVG", //
				"<?xml version=" //
				, DataURI //
						.parse("data:image/svg+xml,%3C?xml%20version=") //
						.getContents() //
		);
	}

	@Test
	public void testParseInvalidString() throws UnsupportedEncodingException,
			ParseException {
		try {
			DataURI.parse("data:test").getContents();
			fail("data w/o leading comma : <mediatype> errors ignored");
		} catch (ParseException e) {
		}
		try {
			DataURI.parse("data:;base64").getContents();
			fail("empty base64 data ingnored.");
		} catch (ParseException e) {
		}
		try {
			DataURI.parse("data:;base64,hello").getContents();
			fail("incorrectly encoded base64 data ignored");
		} catch (ParseException e) {
		}
		try {
			DataURI.parse("data:text/plain;charset=thing;base64;test")
					.getContents();
			fail("Leading comma not present");
		} catch (ParseException e) {
		}
		try {
			DataURI.parse("data:text/plain;charset=thing;base64,test")
					.getContents();
			fail("all options, with invalid charset");
		} catch (UnsupportedEncodingException e) {
		}
	}
}
