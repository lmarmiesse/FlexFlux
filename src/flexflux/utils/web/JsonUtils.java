package flexflux.utils.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class JsonUtils {

	
	/**
	 * Transform a json file to a js file to enable direct loadin of data
	 * 
	 * @param jsonFile
	 * @param jsFile
	 * @return
	 */
	public static Boolean jsonToJs(String jsonFile, String jsFile) {

		Boolean flag = true;

		BufferedReader in = null;
		PrintWriter out = null;

		try {
			in = new BufferedReader(new FileReader(jsonFile));
			out = new PrintWriter(new File(jsFile));

			out.write("var data = ");

			String line = "";

			while ((line = in.readLine()) != null) {

				line = line.replaceAll("\"", "'");

				out.write(line);
				out.write("\n");
			}

			out.write(";");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.err.println("Json file " + jsonFile + " not found");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error reading Json file " + jsonFile);
		}

		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
					System.err.println("Error while closing " + jsonFile);
				}
			}
			if (out != null) {
				out.close();
			}
		}

		return flag;

	}
	
}
