package flexflux.applications;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import flexflux.general.Vars;


public abstract class FFApplication {

	// order for the graphical version. Default : -1 : no particular order
	public static int order = -1;

	@Option(name = "-verbose", usage = "[default=false] Activates the verbose mode")
	public Boolean verbose = true;

	@Option(name = "-h", usage = "Prints this help")
	public Boolean h = false;

	@Option(name = "-web", usage = "[default=false] Whether or not FlexFlux is run from a web application", hidden=true)
	public Boolean web = false;

	public static boolean requiresSolver = true;

	public static boolean graphicalVersion = true;

	public CmdLineParser parser;

	public static String example = "";

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
			System.err.println(this.getClass().getSimpleName());
			System.err.println(this.getMessage());
			parser.printUsage(System.err);
			System.exit(0);
		}

		if (this.h) {
			System.err.println(this.getMessage());
			parser.printUsage(System.out);
			System.exit(1);
		}
		
		Vars.verbose=this.verbose;

	}


	public abstract String getMessage();
}
