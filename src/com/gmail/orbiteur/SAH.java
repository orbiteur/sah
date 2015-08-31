package com.gmail.orbiteur;

import java.io.*;
import java.util.*;
import java.lang.Character;
import java.lang.Integer;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** SAH (Simple API for HTML) is a SAX (https://en.wikipedia.org/wiki/Simple_API_for_XML) like parser for HTML
 *
 * It will parse html files using the SAX API even if they are not valid xml files.
 *
 * @author orbiteur (orbiteur@gmail.com)
 * @version 0.1 (09/01/2015)
 *
 */

public class SAH {
    LinkedList<String> attrs;	
    public String attr, tag;
    public boolean isClosed;
    static private Hashtable<String,String> entities;

    private static void debug (String s) {
	//	System.out.println (s);
    }
    
    public SAH () {
	attrs = new LinkedList<String>();	
	isClosed = false;

	initEntities ();
    }
    
    
    protected void startElement (String tag) {
	debug("begin " + tag);
    }

    protected void endElement (String tag) {
	debug("end " + tag);
    }

    protected void characters (String data) {
	debug("data " + data);	
    }

    protected void comment (String data) {
	debug("comment " + data);	
    }

    protected void styleElement (String data) {
	debug("style " + data);	
    }

    protected void scriptElement (String data) {
	debug("script " + data);	
    }

    public String getAttribute (String name) {
	for (int i = 0; i < attrs.size(); i+=2) {
	    if (attrs.get (i).equalsIgnoreCase (name))
		return attrs.get (i+1);
	}
	return null;
    }

    public boolean parse (String s) {
	Pattern p1 = Pattern.compile ("([^<]*)(<([^ >]+)([^>]*)>)?");
	Pattern p2 = Pattern.compile ("([a-zA-Z0-9_-]+)\\s*(=\\s*([\"\'])([^>\"\']+))");
	Pattern p3 = Pattern.compile ("([a-zA-Z0-9_-]+)\\s*(=\\s*([a-zA-Z0-9_-][^> \n\r]*))");
	Pattern p4 = Pattern.compile ("(.*?)</script>",
				      Pattern.CASE_INSENSITIVE | 
				      Pattern.DOTALL);
	Pattern p5 = Pattern.compile ("(.*?)</style>",
				      Pattern.CASE_INSENSITIVE | 
				      Pattern.DOTALL);
	Pattern p6 = Pattern.compile ("(.*?)-->",
				      Pattern.CASE_INSENSITIVE | 
				      Pattern.DOTALL);
	Pattern p7 = Pattern.compile ("(.*?)\\]\\]\\s*",
				      Pattern.CASE_INSENSITIVE | 
				      Pattern.DOTALL);
	Matcher m1 = p1.matcher(s);

	while(m1.find()) {
	    attrs.clear();

	    if (m1.group(1).length() != 0) 
		characters (decodeEntities (m1.group(1)));

	    if (m1.group (2) == null || m1.group (3) == null) {
		break;
	    }

	    tag = m1.group (3).toLowerCase();
	    attr = m1.group (4);
	    
	    isClosed = attr.endsWith ("/");

	    if (tag.charAt (0) == '/') {
		endElement (tag.substring (1));
	    } else {
  		Matcher m2 = p2.matcher (attr);

		while (m2.find()) {
		    attrs.add (m2.group (1));
		    attrs.add (decodeEntities (m2.group (4)));
		    debug ("[" + m2.group(1) + "] = " + m2.group(4));
		}
		
		Matcher m3 = p3.matcher (attr);

		while (m3.find()) {
		    attrs.add (m3.group (1));
		    attrs.add (m3.group (3));
		    debug ("[" + m3.group(1) + "] = " + m3.group(3));
		}


		if (tag.equals ("script")) {
		    Matcher m4 = p4.matcher (s);
		    if (m4.find (m1.end())) {
			scriptElement (m4.group(1));
			m1.region (m4.end(), s.length());
		    } else {
			debug ("Missing script end");
		    }
		} else if (tag.equals ("style")) {
		    Matcher m5 = p5.matcher (s);
		    if (m5.find (m1.end())) {
			styleElement (m5.group(1));
			m1.region (m5.end(), s.length());
		    } else {
			debug ("Missing style end");
		    }
		} else if (tag.startsWith ("!--")) {
		    if (attr.endsWith ("--")) 
			comment (attr.substring (0, attr.length() - 2));
		    else 
			debug ("Missing comment end");
		} else if (tag.startsWith ("![CDATA[")) {
		    Matcher m7 = p7.matcher (s);
		    if (m7.find (m1.end())) {
			characters (tag.substring (8) + m7.group(1));
			m1.region (m7.end(), s.length());
		    } else {
			debug ("Missing CDATA end");
		    }
		} else {
		    startElement (tag);
		}
	    }
	}

	return true;
    }

    static public String decodeEntities (String s) {
	if (s == null) return null;

	Pattern p = Pattern.compile ("&([^;]+);");
	Matcher m = p.matcher(s);
	StringBuffer sb = new StringBuffer();

	while(m.find()) {
	    String e = m.group(1);
	    if (e.charAt (0) == '#') {
		if (e.charAt (1) == 'x') {
		    char c = (char) Integer.parseInt (e.substring (2, e.length()),16);
		    m.appendReplacement (sb, Character.toString(c));
		} else {
		    char c = (char) Integer.parseInt (e.substring (1, e.length()));
		    m.appendReplacement (sb, Character.toString (c));
		}
	    } else {
		String r = entities.get (e.toLowerCase());
		if (r == null) 
		    debug ("Unknown entity " + e);
		else 
		    m.appendReplacement (sb, r);
	    }
	}
	m.appendTail (sb);

	return sb.toString();
    }


    public boolean parseFile (File file) {
	try {	
	    if (!file.exists()) 	    
		throw new FileNotFoundException();

	    InputStreamReader reader = new InputStreamReader (new FileInputStream(file));
	    //	    FileReader reader = new FileReader(file);
	    char[] chars = new char[(int) file.length()];
	    reader.read(chars);
	    reader.close();
	    String content = new String(chars);

	    debug ("Parsing " + file + " length=" + 
		   file.length());

	    return parse (content);
	} catch (FileNotFoundException e) {
	    e.printStackTrace();  	    
	} catch (IOException e) {
	    e.printStackTrace();  
	}
	return true;
    }

    public static void main(String... args) {
	SAH p = new SAH ();
	p.parseFile (new File (args[0]));
    }

    protected void initEntities () {
	entities = new Hashtable<String,String>();

	entities.put ("nbsp", "\u00A0"); 
	entities.put ("iexcl", "\u00A1"); 
	entities.put ("cent", "\u00A2"); 
	entities.put ("pound", "\u00A3"); 
	entities.put ("curren", "\u00A4"); 
	entities.put ("yen", "\u00A5"); 
	entities.put ("brvbar", "\u00A6"); 
	entities.put ("sect", "\u00A7"); 
	entities.put ("uml", "\u00A8"); 
	entities.put ("copy", "\u00A9"); 
	entities.put ("ordf", "\u00AA"); 
	entities.put ("laquo", "\u00AB"); 
	entities.put ("not", "\u00AC"); 
	entities.put ("shy", "\u00AD"); 
	entities.put ("reg", "\u00AE"); 
	entities.put ("macr", "\u00AF"); 
	entities.put ("deg", "\u00B0"); 
	entities.put ("plusmn", "\u00B1"); 
	entities.put ("sup2", "\u00B2"); 
	entities.put ("sup3", "\u00B3"); 
	entities.put ("acute", "\u00B4"); 
	entities.put ("micro", "\u00B5"); 
	entities.put ("para", "\u00B6"); 
	entities.put ("middot", "\u00B7"); 
	entities.put ("cedil", "\u00B8"); 
	entities.put ("sup1", "\u00B9"); 
	entities.put ("ordm", "\u00BA"); 
	entities.put ("raquo", "\u00BB"); 
	entities.put ("frac14", "\u00BC"); 
	entities.put ("frac12", "\u00BD"); 
	entities.put ("frac34", "\u00BE"); 
	entities.put ("iquest", "\u00BF"); 
	entities.put ("agrave", "\u00C0"); 
	entities.put ("aacute", "\u00C1"); 
	entities.put ("acirc", "\u00C2"); 
	entities.put ("atilde", "\u00C3"); 
	entities.put ("auml", "\u00C4"); 
	entities.put ("aring", "\u00C5"); 
	entities.put ("aelig", "\u00C6"); 
	entities.put ("ccedil", "\u00C7"); 
	entities.put ("egrave", "\u00C8"); 
	entities.put ("eacute", "\u00C9"); 
	entities.put ("ecirc", "\u00CA"); 
	entities.put ("euml", "\u00CB"); 
	entities.put ("igrave", "\u00CC"); 
	entities.put ("iacute", "\u00CD"); 
	entities.put ("icirc", "\u00CE"); 
	entities.put ("iuml", "\u00CF"); 
	entities.put ("eth", "\u00D0"); 
	entities.put ("ntilde", "\u00D1"); 
	entities.put ("ograve", "\u00D2"); 
	entities.put ("oacute", "\u00D3"); 
	entities.put ("ocirc", "\u00D4"); 
	entities.put ("otilde", "\u00D5"); 
	entities.put ("ouml", "\u00D6"); 
	entities.put ("times", "\u00D7"); 
	entities.put ("oslash", "\u00D8"); 
	entities.put ("ugrave", "\u00D9"); 
	entities.put ("uacute", "\u00DA"); 
	entities.put ("ucirc", "\u00DB"); 
	entities.put ("uuml", "\u00DC"); 
	entities.put ("yacute", "\u00DD"); 
	entities.put ("thorn", "\u00DE"); 
	entities.put ("szlig", "\u00DF"); 
	entities.put ("agrave", "\u00E0"); 
	entities.put ("aacute", "\u00E1"); 
	entities.put ("acirc", "\u00E2"); 
	entities.put ("atilde", "\u00E3"); 
	entities.put ("auml", "\u00E4"); 
	entities.put ("aring", "\u00E5"); 
	entities.put ("aelig", "\u00E6"); 
	entities.put ("ccedil", "\u00E7"); 
	entities.put ("egrave", "\u00E8"); 
	entities.put ("eacute", "\u00E9"); 
	entities.put ("ecirc", "\u00EA"); 
	entities.put ("euml", "\u00EB"); 
	entities.put ("igrave", "\u00EC"); 
	entities.put ("iacute", "\u00ED"); 
	entities.put ("icirc", "\u00EE"); 
	entities.put ("iuml", "\u00EF"); 
	entities.put ("eth", "\u00F0"); 
	entities.put ("ntilde", "\u00F1"); 
	entities.put ("ograve", "\u00F2"); 
	entities.put ("oacute", "\u00F3"); 
	entities.put ("ocirc", "\u00F4"); 
	entities.put ("otilde", "\u00F5"); 
	entities.put ("ouml", "\u00F6"); 
	entities.put ("divide", "\u00F7"); 
	entities.put ("oslash", "\u00F8"); 
	entities.put ("ugrave", "\u00F9"); 
	entities.put ("uacute", "\u00FA"); 
	entities.put ("ucirc", "\u00FB"); 
	entities.put ("uuml", "\u00FC"); 
	entities.put ("yacute", "\u00FD"); 
	entities.put ("thorn", "\u00FE"); 
	entities.put ("yuml", "\u00FF"); 
	entities.put ("fnof", "\u0192"); 
	entities.put ("alpha", "\u0391"); 
	entities.put ("beta", "\u0392"); 
	entities.put ("gamma", "\u0393"); 
	entities.put ("delta", "\u0394"); 
	entities.put ("epsilon", "\u0395"); 
	entities.put ("zeta", "\u0396"); 
	entities.put ("eta", "\u0397"); 
	entities.put ("theta", "\u0398"); 
	entities.put ("iota", "\u0399"); 
	entities.put ("kappa", "\u039A"); 
	entities.put ("lambda", "\u039B"); 
	entities.put ("mu", "\u039C"); 
	entities.put ("nu", "\u039D"); 
	entities.put ("xi", "\u039E"); 
	entities.put ("omicron", "\u039F"); 
	entities.put ("pi", "\u03A0"); 
	entities.put ("rho", "\u03A1"); 
	entities.put ("sigma", "\u03A3"); 
	entities.put ("tau", "\u03A4"); 
	entities.put ("upsilon", "\u03A5"); 
	entities.put ("phi", "\u03A6"); 
	entities.put ("chi", "\u03A7"); 
	entities.put ("psi", "\u03A8"); 
	entities.put ("omega", "\u03A9"); 
	entities.put ("alpha", "\u03B1"); 
	entities.put ("beta", "\u03B2"); 
	entities.put ("gamma", "\u03B3"); 
	entities.put ("delta", "\u03B4"); 
	entities.put ("epsilon", "\u03B5"); 
	entities.put ("zeta", "\u03B6"); 
	entities.put ("eta", "\u03B7"); 
	entities.put ("theta", "\u03B8"); 
	entities.put ("iota", "\u03B9"); 
	entities.put ("kappa", "\u03BA"); 
	entities.put ("lambda", "\u03BB"); 
	entities.put ("mu", "\u03BC"); 
	entities.put ("nu", "\u03BD"); 
	entities.put ("xi", "\u03BE"); 
	entities.put ("omicron", "\u03BF"); 
	entities.put ("pi", "\u03C0"); 
	entities.put ("rho", "\u03C1"); 
	entities.put ("sigmaf", "\u03C2"); 
	entities.put ("sigma", "\u03C3"); 
	entities.put ("tau", "\u03C4"); 
	entities.put ("upsilon", "\u03C5"); 
	entities.put ("phi", "\u03C6"); 
	entities.put ("chi", "\u03C7"); 
	entities.put ("psi", "\u03C8"); 
	entities.put ("omega", "\u03C9"); 
	entities.put ("thetasym", "\u03D1"); 
	entities.put ("upsih", "\u03D2"); 
	entities.put ("piv", "\u03D6"); 
	entities.put ("bull", "\u2022"); 
	entities.put ("hellip", "\u2026"); 
	entities.put ("prime", "\u2032"); 
	entities.put ("prime", "\u2033"); 
	entities.put ("oline", "\u203E"); 
	entities.put ("frasl", "\u2044"); 
	entities.put ("weierp", "\u2118"); 
	entities.put ("image", "\u2111"); 
	entities.put ("real", "\u211C"); 
	entities.put ("trade", "\u2122"); 
	entities.put ("alefsym", "\u2135"); 
	entities.put ("larr", "\u2190"); 
	entities.put ("uarr", "\u2191"); 
	entities.put ("rarr", "\u2192"); 
	entities.put ("darr", "\u2193"); 
	entities.put ("harr", "\u2194"); 
	entities.put ("crarr", "\u21B5"); 
	entities.put ("larr", "\u21D0"); 
	entities.put ("uarr", "\u21D1"); 
	entities.put ("rarr", "\u21D2"); 
	entities.put ("darr", "\u21D3"); 
	entities.put ("harr", "\u21D4"); 
	entities.put ("forall", "\u2200"); 
	entities.put ("part", "\u2202"); 
	entities.put ("exist", "\u2203"); 
	entities.put ("empty", "\u2205"); 
	entities.put ("nabla", "\u2207"); 
	entities.put ("isin", "\u2208"); 
	entities.put ("notin", "\u2209"); 
	entities.put ("ni", "\u220B"); 
	entities.put ("prod", "\u220F"); 
	entities.put ("sum", "\u2211"); 
	entities.put ("minus", "\u2212"); 
	entities.put ("lowast", "\u2217"); 
	entities.put ("radic", "\u221A"); 
	entities.put ("prop", "\u221D"); 
	entities.put ("infin", "\u221E"); 
	entities.put ("ang", "\u2220"); 
	entities.put ("and", "\u2227"); 
	entities.put ("or", "\u2228"); 
	entities.put ("cap", "\u2229"); 
	entities.put ("cup", "\u222A"); 
	entities.put ("int", "\u222B"); 
	entities.put ("there4", "\u2234"); 
	entities.put ("sim", "\u223C"); 
	entities.put ("cong", "\u2245"); 
	entities.put ("asymp", "\u2248"); 
	entities.put ("ne", "\u2260"); 
	entities.put ("equiv", "\u2261"); 
	entities.put ("le", "\u2264"); 
	entities.put ("ge", "\u2265"); 
	entities.put ("sub", "\u2282"); 
	entities.put ("sup", "\u2283"); 
	entities.put ("nsub", "\u2284"); 
	entities.put ("sube", "\u2286"); 
	entities.put ("supe", "\u2287"); 
	entities.put ("oplus", "\u2295"); 
	entities.put ("otimes", "\u2297"); 
	entities.put ("perp", "\u22A5"); 
	entities.put ("sdot", "\u22C5"); 
	entities.put ("lceil", "\u2308"); 
	entities.put ("rceil", "\u2309"); 
	entities.put ("lfloor", "\u230A"); 
	entities.put ("rfloor", "\u230B"); 
	entities.put ("lang", "\u2329"); 
	entities.put ("rang", "\u232A"); 
	entities.put ("loz", "\u25CA"); 
	entities.put ("spades", "\u2660"); 
	entities.put ("clubs", "\u2663"); 
	entities.put ("hearts", "\u2665"); 
	entities.put ("diams", "\u2666"); 
	entities.put ("amp", "\u0026");
	entities.put ("lt", "\u003C"); 
	entities.put ("gt", "\u003E"); 
	entities.put ("oelig", "\u0152"); 
	entities.put ("oelig", "\u0153"); 
	entities.put ("scaron", "\u0160"); 
	entities.put ("scaron", "\u0161"); 
	entities.put ("yuml", "\u0178"); 
	entities.put ("circ", "\u02C6"); 
	entities.put ("quot", "\"");
	entities.put ("tilde", "\u02DC"); 
	entities.put ("ensp", "\u2002"); 
	entities.put ("emsp", "\u2003"); 
	entities.put ("thinsp", "\u2009"); 
	entities.put ("zwnj", "\u200C"); 
	entities.put ("zwj", "\u200D"); 
	entities.put ("lrm", "\u200E"); 
	entities.put ("rlm", "\u200F"); 
	entities.put ("ndash", "\u2013"); 
	entities.put ("mdash", "\u2014"); 
	entities.put ("lsquo", "\u2018"); 
	entities.put ("rsquo", "\u2019"); 
	entities.put ("sbquo", "\u201A"); 
	entities.put ("ldquo", "\u201C"); 
	entities.put ("rdquo", "\u201D"); 
	entities.put ("bdquo", "\u201E"); 
	entities.put ("dagger", "\u2020"); 
	entities.put ("dagger", "\u2021"); 
	entities.put ("permil", "\u2030"); 
	entities.put ("lsaquo", "\u2039"); 
	entities.put ("rsaquo", "\u203A"); 
	entities.put ("euro", "\u20AC"); 
    }
}
