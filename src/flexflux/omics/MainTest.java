package flexflux.omics;

import java.util.ArrayList;
import java.util.List;

import flexflux.general.Bind;
import flexflux.general.GLPKBind;
import flexflux.interaction.Interaction;

public class MainTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		Bind bind = new GLPKBind();

		bind.loadSbmlNetwork("/home/lmarmiesse/Bureau/sa_sbml_215902682.xml", false);

		OmicsData omicsData = OmicsDataReader.loadOmicsData("/home/lmarmiesse/Bureau/RNAseq_data.tab",
				bind.getInteractionNetwork().getEntities());
		// Sample s_mm = omicsData.getSample("RNAseq_MM");
		
		List<Sample> samples = new ArrayList<Sample>();
		samples.addAll(omicsData.getSamples());
		
		for (Sample s : samples) {
			System.out.print(s.getName()+"\t");
		}
		System.out.print("\n");

		for (Interaction inter : bind.getInteractionNetwork().getGPRInteractions()) {

			System.out.print(inter.getConsequence().getEntity().getId());

			for (Sample s : samples) {

				double expr = inter.getCondition()
						.calculateRelationQuantitativeValue(omicsData.getDataValuesForSample(s));
				System.out.print("\t" + expr);
			}
			System.out.print("\n");
			// R_SUCD3_u6m
			// break;

		}

	}

}
