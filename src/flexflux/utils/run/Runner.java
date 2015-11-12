package flexflux.utils.run;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import flexflux.general.Vars;

public class Runner {
	
	/**
	 * Run the inchlib command
	 * 
	 * @return
	 * @throws IOException
	 */
	public static Boolean runExternalCommand(String cmd) throws IOException {

		if (Vars.verbose) {
			System.err.println("\n********launch inchlib\n");
			System.err.println(cmd);

		}

		Process p = null;
		try {
			p = Runtime.getRuntime().exec(cmd);
			String lineError;
			BufferedReader bre = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			while ((lineError = bre.readLine()) != null) {
				System.err.println(lineError);
			}
			p.waitFor();
		} catch (IOException e) {
			System.err.println("Error in launching the command " + cmd);
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			System.err.println("Interruption of the command " + cmd);
			e.printStackTrace();
			return false;
		} finally {
			if (p != null) {
				if (p.getOutputStream() != null)
					p.getOutputStream().close();
				if (p.getInputStream() != null)
					p.getInputStream().close();
				if (p.getErrorStream() != null)
					p.getErrorStream().close();
			}
		}

		return true;

	}
	

}
