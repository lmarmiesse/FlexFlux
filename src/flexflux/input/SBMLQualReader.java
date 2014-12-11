package flexflux.input;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.qual.FunctionTerm;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.xml.XMLNode;

import parsebionet.biodata.BioEntity;
import flexflux.general.Constraint;
import flexflux.general.Vars;
import flexflux.interaction.And;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.InversedRelation;
import flexflux.interaction.Or;
import flexflux.interaction.Relation;
import flexflux.interaction.RelationFactory;
import flexflux.interaction.RelationWithList;
import flexflux.interaction.Unique;
import flexflux.operation.OperationEq;
import flexflux.operation.OperationGe;
import flexflux.operation.OperationGt;
import flexflux.operation.OperationLe;
import flexflux.operation.OperationLt;

public class SBMLQualReader {

	private static InteractionNetwork intNet;

	public static InteractionNetwork loadSbmlQual(String path,
			InteractionNetwork intNet, RelationFactory relationFactory) {

		SBMLQualReader.intNet = intNet;

		SBMLDocument document = null;

		try {
			document = SBMLReader.read(new File(path));
		} catch (XMLStreamException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Model model = document.getModel();

		QualModelPlugin qualPlugin = (QualModelPlugin) model
				.getExtension("qual");

		// ////////////////////////////////initial values
		for (QualitativeSpecies species : qualPlugin
				.getListOfQualitativeSpecies()) {
			if (intNet.getEntity(species.getId()) == null) {

				intNet.addNumEntity(new BioEntity(species.getId()));

			}

			BioEntity ent = intNet.getEntity(species.getId());

			intNet.addInteractionNetworkEntity(ent);

			if (species.getNotes() != null) {
				XMLNode htmlNode = species.getNotes().getChildAt(1);

				for (int i = 0; i < htmlNode.getChildCount(); i++) {
					XMLNode node = htmlNode.getChildAt(i);
					if (node.getName().equals("p")) {
						String text = node.getChildAt(0).getCharacters();

						if (text.startsWith("STATE")) {
							String stateString = text.split(":")[0];
							String intervalString = text.split(":")[1];

							intervalString = intervalString.replaceAll(" ", "");

							int stateNumber = Integer.parseInt(stateString
									.split(" ")[1]);

							if (intervalString.equals("ND")) {
								intNet.addEntityStateConstraintTranslation(ent,
										stateNumber, null);
							} else {
								double lb = 0;
								double ub = 0;

								String lbIncludedString = intervalString
										.substring(0, 1);
								String ubIncludedString = intervalString
										.substring(intervalString.length() - 1);

								boolean lbIncluded = lbIncludedString
										.equals("[");
								boolean ubIncluded = ubIncludedString
										.equals("]");

								intervalString = intervalString
										.replace("[", "");
								intervalString = intervalString
										.replace("]", "");

								if (intervalString.split(",")[0].equals("-inf")) {
									lb = -Double.MAX_VALUE;
								} else if (intervalString.split(",")[0]
										.equals("+inf")) {
									lb = Double.MAX_VALUE;
								} else {
									lb = Double.parseDouble(intervalString
											.split(",")[0]);
								}

								if (intervalString.split(",")[1].equals("-inf")) {
									ub = -Double.MAX_VALUE;
								} else if (intervalString.split(",")[1]
										.equals("+inf")) {
									ub = Double.MAX_VALUE;
								} else {
									ub = Double.parseDouble(intervalString
											.split(",")[1]);
								}

								if (!lbIncluded) {
									lb += Vars.epsilon;
								}
								if (!ubIncluded) {
									ub -= Vars.epsilon;
								}

								intNet.addEntityStateConstraintTranslation(ent,
										stateNumber,
										new Constraint(ent, lb, ub));
							}

						}

					}
				}

			}

			if (species.isSetInitialLevel()) {

				Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
				constMap.put(ent, 1.0);

				int initValue = species.getInitialLevel();
				
				intNet.addInitialState(ent, initValue);
				
		
			}

		}

		checkConsistency();

		for (Species sp : model.getListOfSpecies()) {

			if (intNet.getEntity(sp.getId()) == null) {

				intNet.addNumEntity(new BioEntity(sp.getId()));

			}

			BioEntity ent = intNet.getEntity(sp.getId());

			Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
			constMap.put(ent, 1.0);
			intNet.addInitialConstraint(
					ent,
					new Constraint(constMap, sp.getInitialConcentration(), sp
							.getInitialConcentration()));

		}

		// ////////////////:interactions
		for (Transition tr : qualPlugin.getListOfTransitions()) {

			Output out = tr.getListOfOutputs().get(0);

			String outEntityName = out.getQualitativeSpecies();

			BioEntity outEntity = intNet.getEntity(outEntityName);

			Relation ifRelation = null;
			Unique thenRelation = null;
			Unique elseRelation = null;

			for (FunctionTerm ft : tr.getListOfFunctionTerms()) {
				double starts = 0;
				double lasts = 0;

				Interaction inter;

				double resValue = 0;

				resValue = ft.getResultLevel();

				if (ft.isDefaultTerm()) {

					elseRelation = new Unique(outEntity, new OperationEq(),
							resValue);

					inter = relationFactory.makeIfThenInteraction(elseRelation,
							null);

					intNet.setTargetDefaultInteraction(outEntity, inter);

				} else {

					ASTNode ast = ft.getMath();

					ifRelation = createRealtion(ast);

					thenRelation = new Unique(outEntity, new OperationEq(),
							resValue);

					inter = relationFactory.makeIfThenInteraction(thenRelation,
							ifRelation);

					intNet.addTargetConditionalInteraction(outEntity, inter);

				}

				// read time infos of the interaction in the notes
				if (ft.getNotes() != null) {
					XMLNode htmlNode = ft.getNotes().getChildAt(1);

					for (int i = 0; i < htmlNode.getChildCount(); i++) {
						XMLNode node = htmlNode.getChildAt(i);
						if (node.getName().equals("p")) {
							String text = node.getChildAt(0).getCharacters();
							if (text.startsWith("STARTS")) {

								starts = Double.parseDouble(text.split(":")[1]);

							}
							if (text.startsWith("LASTS")) {

								lasts = Double.parseDouble(text.split(":")[1]);

							}
						}
					}

				}

				inter.setTimeInfos(new double[] { starts, lasts });

			}

		}
		return intNet;

	}

	/**
	 * checks that a value cannot be in two states
	 */

	private static void checkConsistency() {

		for (BioEntity ent : intNet.getEntityStateConstraintTranslation()
				.keySet()) {

			Set<Double> thresholds = new HashSet<Double>();

			for (Integer state : intNet.getEntityStateConstraintTranslation()
					.get(ent).keySet()) {
				Constraint c = intNet.getEntityStateConstraintTranslation()
						.get(ent).get(state);

				if (c == null) {
					continue;
				}

				thresholds.add(c.getLb());
				thresholds.add(c.getUb());

			}

			for (double val : thresholds) {

				boolean belongsToAState = false;
				for (Integer i : intNet.getEntityStateConstraintTranslation()
						.get(ent).keySet()) {

					Constraint c = intNet.getEntityStateConstraintTranslation()
							.get(ent).get(i);

					if (c == null) {
						continue;
					}

					if (val >= c.getLb() && val <= c.getUb()) {
						if (!belongsToAState) {
							belongsToAState = true;
						} else {
							System.err
									.println("Error : for variable "
											+ ent.getId()
											+ ", the value "
											+ val
											+ " belongs to more than one qualitative state.");
							System.exit(0);
						}

					}

				}

			}

		}

	}

	private static Relation createRealtion(ASTNode ast) {

		Relation rel = getRightRelation(ast);

		for (ASTNode astChild : ast.getListOfNodes()) {

			try {
				RelationWithList rel2 = (RelationWithList) rel;
				if (rel2 != null) {

					rel2.addRelation(createRealtion(astChild));
				}
			} catch (ClassCastException e) {

			}

		}
		return rel;

	}

	private static Relation getRightRelation(ASTNode ast) {

		Type type = ast.getType();

		if (type.toString().equals("LOGICAL_AND")) {
			return new And();
		} else if (type.toString().equals("LOGICAL_OR")) {
			return new Or();
		} else if (type.toString().equals("LOGICAL_NOT")) {

			return new InversedRelation(createRealtion(ast.getChild(0)));

		} else if (type.toString().equals("RELATIONAL_EQ")) {

			double value = 0;

			BioEntity ent = intNet.getEntity(ast.getChild(0).toString());

			value = Double.parseDouble(ast.getChild(1).toString());

			Unique unique = new Unique(ent, new OperationEq(), value);

			return unique;

		} else if (type.toString().equals("RELATIONAL_LEQ")) {

			double value = 0;

			BioEntity ent = intNet.getEntity(ast.getChild(0).toString());

			value = Double.parseDouble(ast.getChild(1).toString());

			Unique unique = new Unique(ent, new OperationLe(), value);

			return unique;

		} else if (type.toString().equals("RELATIONAL_GEQ")) {

			double value = 0;

			BioEntity ent = intNet.getEntity(ast.getChild(0).toString());

			value = Double.parseDouble(ast.getChild(1).toString());

			Unique unique = new Unique(ent, new OperationGe(), value);

			return unique;

		} else if (type.toString().equals("RELATIONAL_LT")) {

			double value = 0;

			BioEntity ent = intNet.getEntity(ast.getChild(0).toString());

			value = Double.parseDouble(ast.getChild(1).toString());

			Unique unique = new Unique(ent, new OperationLt(), value);

			return unique;

		} else if (type.toString().equals("RELATIONAL_GT")) {

			double value = 0;

			BioEntity ent = intNet.getEntity(ast.getChild(0).toString());

			value = Double.parseDouble(ast.getChild(1).toString());

			Unique unique = new Unique(ent, new OperationGt(), value);

			return unique;

		}

		return null;
	}
}