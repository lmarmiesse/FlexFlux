package flexflux.applications;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public abstract class FFApplication {

	// order for the graphical version. Default : -1 : no particular order
	public static int order = -1;

	@Option(name = "-verbose", usage = "[default=false] Activates the verbose mode")
	public Boolean verbose = false;

	@Option(name = "-h", usage = "Prints this help")
	public Boolean h = false;

	public static boolean requiresSolver = true;

	public static boolean graphicalVersion = true;

	public CmdLineParser parser;

	/**
	 * Constructor
	 */
	public FFApplication() {
		parser = new CmdLineParser(this);
	}

	/**
	 * parse args from main
	 * 
	 * @param args
	 */
	public void parseArguments(String[] args) {

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println(this.getMessage());
			parser.printUsage(System.err);
			System.err.println(this.getMessage());
			System.exit(0);
		}

		if (this.h) {
			System.err.println(this.getMessage());
			parser.printUsage(System.out);
			System.exit(1);
		}

	}

	/**
	 * abstract method to get the message
	 * 
	 * @return
	 */
	public abstract String getMessage();

	/**
	 * abstract methode to get the example
	 * 
	 * @return
	 */
	public abstract String getExample();

	// /**
	// * The following static block is needed in order to load the libSBML Java
	// * interface library when the application starts.
	// */
	// static {
	// System.loadLibrary("sbmlj");
	// }

}
