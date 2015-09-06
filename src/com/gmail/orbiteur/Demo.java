package com.gmail.orbiteur;

import java.io.*;
import com.gmail.orbiteur.SAH;

public class Demo extends SAH {
    static public void print (String s) {
	System.out.println (s); 
	/*
	try {
	    System.out.println (new String (s.getBytes ("UTF-8")));
	} catch (UnsupportedEncodingException e) {

	}
	*/
   }

    protected void startElement (String tag) {
	print("begin " + tag);
    }

    protected void endElement (String tag) {
	print("end " + tag);
    }

    protected void characters (String data) {
	print("data " + data);	
    }

    protected void comment (String data) {
	print("comment " + data);	
    }

    protected void styleElement (String data) {
	print("style " + data);	
    }

    protected void scriptElement (String data) {
	print("script " + data);	
    }

    public static void main(String... args) {
	SAH p = new Demo ();
	print ("opening " + args[0]);
	p.parseFile (new File (args[0]));
    }
}

