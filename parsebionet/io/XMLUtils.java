/*******************************************************************************
 * Copyright INRA
 * 
 *  Contact: ludovic.cottret@toulouse.inra.fr
 * 
 * 
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *  In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *  The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 ******************************************************************************/
package parsebionet.io;

import java.io.File;
import java.io.IOException;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.dom.DocumentImpl;

public class XMLUtils {

    // feature ids
    /** Namespaces feature id (http://xml.org/sax/features/namespaces). */
    protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";
    /** Validation feature id (http://xml.org/sax/features/validation). */
    protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";
    /** Schema validation feature id (http://apache.org/xml/features/validation/schema). */
    protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";
    /** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
    protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";
    /** Honour all schema locations feature id (http://apache.org/xml/features/honour-all-schemaLocations). */
    protected static final String HONOUR_ALL_SCHEMA_LOCATIONS_ID = "http://apache.org/xml/features/honour-all-schemaLocations";
    /** Validate schema annotations feature id (http://apache.org/xml/features/validate-annotations). */
    protected static final String VALIDATE_ANNOTATIONS_ID = "http://apache.org/xml/features/validate-annotations";
    /** Dynamic validation feature id (http://apache.org/xml/features/validation/dynamic). */
    protected static final String DYNAMIC_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/dynamic";
    /** XInclude feature id (http://apache.org/xml/features/xinclude). */
    protected static final String XINCLUDE_FEATURE_ID = "http://apache.org/xml/features/xinclude";
    /** XInclude fixup base URIs feature id (http://apache.org/xml/features/xinclude/fixup-base-uris). */
    protected static final String XINCLUDE_FIXUP_BASE_URIS_FEATURE_ID = "http://apache.org/xml/features/xinclude/fixup-base-uris";
    /** XInclude fixup language feature id (http://apache.org/xml/features/xinclude/fixup-language). */
    protected static final String XINCLUDE_FIXUP_LANGUAGE_FEATURE_ID = "http://apache.org/xml/features/xinclude/fixup-language";
    
    XMLUtils(){}

    static public Document open(String inputFile) throws IOException, SAXException 
    {
		// Opens the XML file and parses it
	    Document document;
	    DOMParser parser;
	    
		parser = new DOMParser();
		setXMLParserFeatures(parser);
        parser.parse(inputFile);
        document = parser.getDocument();
        
        // Checking if the document can be traversed
        if (!document.isSupported("Traversal", "2.0")) {
            // This cannot happen with our DOMParser...
            throw new RuntimeException("This DOM Document does not support Traversal");
        }
        
        return document;
    }
    
    static public Document openOrCreateDocument(String inputFile) throws IOException, SAXException
    {
    	File file = new File(inputFile);
    	if( file.exists() ){
    		return open(inputFile);
    	}
    	
    	Document document = new DocumentImpl();
    	return document;
    }
    
	private static void setXMLParserFeatures(DOMParser parser)
	{
		try{
			parser.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", true);
	        parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
	        parser.setProperty("http://cyberneko.org/html/properties/default-encoding", "ascii");

		}
		catch(Exception e){
			System.err.println(e.getMessage());
		}
//      set parser features
        try {
            parser.setFeature(NAMESPACES_FEATURE_ID, true);
        }
        catch (SAXException e) {
            System.err.println("warning: Parser does not support feature ("+NAMESPACES_FEATURE_ID+")");
        }
        try {
            parser.setFeature(VALIDATION_FEATURE_ID, false);
        }
        catch (SAXException e) {
            System.err.println("warning: Parser does not support feature ("+VALIDATION_FEATURE_ID+")");
        }
        try {
            parser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, false);
        }
        catch (SAXException e) {
            System.err.println("warning: Parser does not support feature ("+SCHEMA_VALIDATION_FEATURE_ID+")");
        }
        try {
            parser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, false);
        }
        catch (SAXException e) {
            System.err.println("warning: Parser does not support feature ("+SCHEMA_FULL_CHECKING_FEATURE_ID+")");
        }
        try {
            parser.setFeature(HONOUR_ALL_SCHEMA_LOCATIONS_ID, false);
        }
        catch (SAXException e) {
            System.err.println("warning: Parser does not support feature ("+HONOUR_ALL_SCHEMA_LOCATIONS_ID+")");
        }
        try {
            parser.setFeature(VALIDATE_ANNOTATIONS_ID, false);
        }
        catch (SAXException e) {
            System.err.println("warning: Parser does not support feature ("+VALIDATE_ANNOTATIONS_ID+")");
        }
        try {
            parser.setFeature(DYNAMIC_VALIDATION_FEATURE_ID, false);
        }
        catch (SAXException e) {
            System.err.println("warning: Parser does not support feature ("+DYNAMIC_VALIDATION_FEATURE_ID+")");
        }
        try {
            parser.setFeature(XINCLUDE_FEATURE_ID, false);
        }
        catch (SAXException e) {
            System.err.println("warning: Parser does not support feature ("+XINCLUDE_FEATURE_ID+")");
        }
        try {
            parser.setFeature(XINCLUDE_FIXUP_BASE_URIS_FEATURE_ID, false);
        }
        catch (SAXException e) {
            System.err.println("warning: Parser does not support feature ("+XINCLUDE_FIXUP_BASE_URIS_FEATURE_ID+")");
        }
        try {
            parser.setFeature(XINCLUDE_FIXUP_LANGUAGE_FEATURE_ID, false);
        }
        catch (SAXException e) {
            System.err.println("warning: Parser does not support feature ("+XINCLUDE_FIXUP_LANGUAGE_FEATURE_ID+")");
        }        
	}    
}
