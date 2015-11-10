package flexflux.analyses.result;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parsebionet.biodata.BioEntity;
import flexflux.general.Vars;

public class TDRNAAnalysisResult extends AnalysisResult {

	private List<Map<BioEntity, Integer>> statesList = new ArrayList<Map<BioEntity, Integer>>();
	private Set<BioEntity> entitiesToCheck;

	public double deltaT;

	public TDRNAAnalysisResult(double deltaT, Set<BioEntity> entitiesToCheck) {
		this.deltaT = deltaT;
		this.entitiesToCheck = entitiesToCheck;
	}

	public void setStatesList(List<Map<BioEntity, Integer>> sl) {
		statesList = sl;
	}

	public List<Map<BioEntity, Integer>> getStatesList() {
		return statesList;
	}

	public void writeToFile(String path) {

		try {
			PrintWriter out = new PrintWriter(new File(path));

			String line = "Time\t";
			for (BioEntity ent : entitiesToCheck) {

				String s = ent.getId();

				line += s + "\t";

			}
			out.println(line);

			double time = 0;

			for (Map<BioEntity, Integer> state : statesList) {

				line = time + "\t";
				for (BioEntity ent : entitiesToCheck) {
					line += Vars.round(state.get(ent)) + "\t";
				}
				out.println(line);
				time += deltaT;

			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void plot() {

	}

	@Override
	public void writeHTML(String path) {
		try {
			PrintWriter out = new PrintWriter(new File(path));

			for (BioEntity ent : entitiesToCheck) {

				out.println("<p hidden class='2Dplot' data-plotname='" + ent.getId()
						+ "' data-xAxisName='Time (h)' data-yAxisName=''>");

				double time = 0;

				for (Map<BioEntity, Integer> state : statesList) {

					out.println("<a>" + time + ";" + Vars.round(state.get(ent)) + "</a>");
					time += deltaT;

				}
				out.println("</p>");
			}
			out.close();
		} catch (

		IOException e)

		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
