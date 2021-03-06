package flexflux.input;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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
import org.sbml.jsbml.xml.parsers.SBMLCoreParser;

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
import parsebionet.biodata.BioEntity;

public class SBMLQualReader {

	private static InteractionNetwork intNet;

	public static InteractionNetwork loadSbmlQual(String path, InteractionNetwork intNet,
			RelationFactory relationFactory) {

		SBMLQualReader.intNet = intNet;

		SBMLDocument document = null;

		try {
			document = SBMLReader.read(new File(path));
		} catch (XMLStreamException | IOException e) {
			System.err.println("Error while reading the sbml qual file " + path);
			e.printStackTrace();

			return null;
		}

		Model model = document.getModel();

		QualModelPlugin qualPlugin = (QualModelPlugin) model.getExtension("qual");

		// ////////////////////////////////initial values
		for (QualitativeSpecies species : qualPlugin.getListOfQualitativeSpecies()) {
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

							int stateNumber = Integer.parseInt(stateString.split(" ")[1]);

							if (intervalString.equals("ND")) {
								intNet.addEntityStateConstraintTranslation(ent, stateNumber, null);
							} else {
								try {
									double lb = 0;
									double ub = 0;

									String lbIncludedString = intervalString.substring(0, 1);
									String ubIncludedString = intervalString.substring(intervalString.length() - 1);

									boolean lbIncluded = lbIncludedString.equals("[");
									boolean ubIncluded = ubIncludedString.equals("]");

									intervalString = intervalString.replace("[", "");
									intervalString = intervalString.replace("]", "");

									if (intervalString.split(",")[0].equals("-inf")) {
										lb = -Double.MAX_VALUE;
									} else if (intervalString.split(",")[0].equals("+inf")) {
										lb = Double.MAX_VALUE;
									} else {
										lb = Double.parseDouble(intervalString.split(",")[0]);
									}

									if (intervalString.split(",")[1].equals("-inf")) {
										ub = -Double.MAX_VALUE;
									} else if (intervalString.split(",")[1].equals("+inf")) {
										ub = Double.MAX_VALUE;
									} else {
										ub = Double.parseDouble(intervalString.split(",")[1]);
									}

									if (!lbIncluded) {
										lb += Vars.epsilon;
									}
									if (!ubIncluded) {
										ub -= Vars.epsilon;
									}

									intNet.addEntityStateConstraintTranslation(ent, stateNumber,
											new Constraint(ent, lb, ub));
								} catch (Exception e) {
									System.err.println("Error in the description of the translation for state "
											+ stateNumber + " of species " + ent.getId());
									System.exit(0);

								}
							}

						}

					}
				}

			}

			// check that max level is specified
			if (!species.isSetMaxLevel()) {

				System.err.println("Error : no max value set for species " + species.getId());
				System.exit(0);

			} else {
				intNet.setInteractionNetworkEntityState(ent, species.getMaxLevel());
			}

			if (species.isSetInitialLevel()) {

				if (species.getInitialLevel() > species.getMaxLevel()) {

					System.err.println(
							"Error : in species " + species.getId() + ", initial level is greater than max level.");
					System.exit(0);

				}
				intNet.updateInteractionNetworkEntityState(ent, species.getInitialLevel());

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
			intNet.addInitialConstraint(ent,
					new Constraint(constMap, sp.getInitialConcentration(), sp.getInitialConcentration()));

		}

		// ////////////////:interactions
		for (Transition tr : qualPlugin.getListOfTransitions()) {

			Output out = tr.getListOfOutputs().get(0);

			String outEntityName = out.getQualitativeSpecies();

			BioEntity outEntity = intNet.getEntity(outEntityName);

			if (!intNet.getInteractionNetworkEntities().containsKey(outEntityName)) {
				System.err.println("Error : entity " + outEntityName + " is not described as a qualitative species.");
				System.exit(0);
			}

			Relation ifRelation = null;
			Unique thenRelation = null;
			Unique elseRelation = null;

			for (FunctionTerm ft : tr.getListOfFunctionTerms()) {
				double starts = 0;
				double lasts = 0;
				int priority = 1;
				boolean isPrioritySet = false;
				String interID = "";

				Interaction inter;

				int resValue = 0;

				resValue = ft.getResultLevel();

				intNet.updateInteractionNetworkEntityState(outEntity, resValue);

				if (ft.isDefaultTerm()) {

					elseRelation = new Unique(outEntity, new OperationEq(), resValue, 0);

					inter = relationFactory.makeIfThenInteraction(elseRelation, null);

					intNet.setTargetDefaultInteraction(outEntity, inter);

				} else {

					ASTNode ast = ft.getMath();

					ifRelation = createRealtion(ast);

					thenRelation = new Unique(outEntity, new OperationEq(), resValue, priority);

					inter = relationFactory.makeIfThenInteraction(thenRelation, ifRelation);

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
							if (text.startsWith("ID")) {

								interID = text.split(":")[1].trim();

							}

							if (text.startsWith("PRIORITY")) {

								priority = Integer.parseInt(text.split(":")[1].trim());
								isPrioritySet = true;

							}
						}
					}

				}

				inter.setTimeInfos(new double[] { starts, lasts });
				inter.setName(interID);
				if (isPrioritySet) {
					inter.getConsequence().setPriority(priority);
				}
				if (ft.isSetMetaId()) {
					inter.setName(ft.getMetaId());
				}

			}

		}

		return intNet;

	}

	/**
	 * checks that a value cannot be in two states
	 */

	private static void checkConsistency() {

		// check a quantitative value is not in two states
		for (BioEntity ent : intNet.getEntityStateConstraintTranslation().keySet()) {

			Set<Double> thresholds = new HashSet<Double>();

			for (Integer state : intNet.getEntityStateConstraintTranslation().get(ent).keySet()) {

				Constraint c = intNet.getEntityStateConstraintTranslation().get(ent).get(state);

				if (c == null) {
					continue;
				}

				thresholds.add(c.getLb());
				thresholds.add(c.getUb());

			}

			for (double val : thresholds) {

				boolean belongsToAState = false;
				for (Integer i : intNet.getEntityStateConstraintTranslation().get(ent).keySet()) {

					Constraint c = intNet.getEntityStateConstraintTranslation().get(ent).get(i);

					if (c == null) {
						continue;
					}

					if (val >= c.getLb() && val <= c.getUb()) {
						if (!belongsToAState) {
							belongsToAState = true;
						} else {
							System.err.println("Error : for variable " + ent.getId() + ", the value " + val
									+ " belongs to more than one qualitative state.");
							System.exit(0);
						}

					}

				}

			}

		}

		// check the quantitative values are in ascending order
		for (BioEntity ent : intNet.getEntityStateConstraintTranslation().keySet()) {

			SortedSet<Integer> s = new TreeSet<Integer>();
			s.addAll(intNet.getEntityStateConstraintTranslation().get(ent).keySet());

			double old_ub = -Double.MAX_VALUE;
			;
			int old_state = 0;
			for (int state : s) {

				Constraint c = intNet.getEntityStateConstraintTranslation().get(ent).get(state);

				if (c == null) {
					continue;
				}

				double lb = c.getLb();
				double ub = c.getUb();

				if (ub < lb) {
					System.err.println("Error : for variable " + ent.getId() + " and state " + state
							+ ", upper bound of translation is smaller than lower bound ");
					System.exit(0);
				}

				if (lb < old_ub) {
					System.err.println("Error : for variable " + ent.getId() + ". lower bound of state " + state
							+ " is smaller thean upper bound of state " + old_state
							+ ". They must be in ascending order.");
					System.exit(0);
				}

				old_ub = ub;
				old_state = state;

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

			int value = 0;

			BioEntity ent = intNet.getEntity(ast.getChild(0).toString());

			if (!intNet.getInteractionNetworkEntities().containsKey(ent.getId())) {
				System.err.println("Error : entity " + ent.getId() + " is not described as a qualitative species.");
				System.exit(0);
			}

			value = Integer.parseInt(ast.getChild(1).toString());

			Unique unique = new Unique(ent, new OperationEq(), value);

			intNet.updateInteractionNetworkEntityState(ent, value);

			return unique;

		} else if (type.toString().equals("RELATIONAL_LEQ")) {

			int value = 0;

			BioEntity ent = intNet.getEntity(ast.getChild(0).toString());

			if (!intNet.getInteractionNetworkEntities().containsKey(ent.getId())) {
				System.err.println("Error : entity " + ent.getId() + " is not described as a qualitative species.");
				System.exit(0);
			}

			value = Integer.parseInt(ast.getChild(1).toString());

			Unique unique = new Unique(ent, new OperationLe(), value);

			intNet.updateInteractionNetworkEntityState(ent, value);

			return unique;

		} else if (type.toString().equals("RELATIONAL_GEQ")) {

			int value = 0;

			BioEntity ent = intNet.getEntity(ast.getChild(0).toString());

			if (!intNet.getInteractionNetworkEntities().containsKey(ent.getId())) {
				System.err.println("Error : entity " + ent.getId() + " is not described as a qualitative species.");
				System.exit(0);
			}

			value = Integer.parseInt(ast.getChild(1).toString());

			Unique unique = new Unique(ent, new OperationGe(), value);

			intNet.updateInteractionNetworkEntityState(ent, value);

			return unique;

		} else if (type.toString().equals("RELATIONAL_LT")) {

			int value = 0;

			BioEntity ent = intNet.getEntity(ast.getChild(0).toString());

			if (!intNet.getInteractionNetworkEntities().containsKey(ent.getId())) {
				System.err.println("Error : entity " + ent.getId() + " is not described as a qualitative species.");
				System.exit(0);
			}

			value = Integer.parseInt(ast.getChild(1).toString());

			Unique unique = new Unique(ent, new OperationLt(), value);

			intNet.updateInteractionNetworkEntityState(ent, value);

			return unique;

		} else if (type.toString().equals("RELATIONAL_GT")) {

			int value = 0;

			BioEntity ent = intNet.getEntity(ast.getChild(0).toString());

			if (!intNet.getInteractionNetworkEntities().containsKey(ent.getId())) {
				System.err.println("Error : entity " + ent.getId() + " is not described as a qualitative species.");
				System.exit(0);
			}

			value = Integer.parseInt(ast.getChild(1).toString());

			Unique unique = new Unique(ent, new OperationGt(), value);

			intNet.updateInteractionNetworkEntityState(ent, value);

			return unique;

		}

		System.err.println("Error in rule : " + ast + ", missing logical rule or mathematical sign");
		System.exit(0);

		return null;
	}
}
