package flexflux.general;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.qual.QualitativeModel;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;
import org.sbml.jsbml.ext.qual.FunctionTerm;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.Transition;

import parsebionet.biodata.BioEntity;
import flexflux.interaction.And;
import flexflux.interaction.Interaction;
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

	private static Bind bind;

	public static void loadSbmlQual(String path, Bind bind,
			RelationFactory relationFactory) {

		SBMLQualReader.bind = bind;

		SBMLDocument document = null;

		try {
			document = SBMLReader.read(new File(path));
		} catch (XMLStreamException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Model model = document.getModel();

		QualitativeModel qualPlugin = (QualitativeModel) model
				.getExtension("qual");

		// ////////////////////////////////initial values

		for (QualitativeSpecies species : qualPlugin
				.getListOfQualitativeSpecies()) {

			if (bind.getInteractionNetwork().getEntity(species.getId()) == null) {

				System.err.println("Error : unknown entity " + species.getId());
				System.exit(0);

			}

			if (species.isSetInitialLevel()) {

				BioEntity ent = bind.getInteractionNetwork().getEntity(
						species.getId());

				Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
				constMap.put(ent, 1.0);
				bind.getInteractionNetworkSimpleConstraints().put(
						ent,
						new Constraint(constMap, species.getInitialLevel(),
								species.getInitialLevel()));

			}

		}

		// ////////////////:interactions

		for (Transition tr : qualPlugin.getListOfTransitions()) {

			Output out = tr.getListOfOutputs().get(0);

			String outEntityName = out.getQualitativeSpecies();

			Relation ifRelation = null;
			Unique thenRelation = null;
			Unique elseRelation = null;

			// tr.get
			for (FunctionTerm ft : tr.getListOfFunctionTerms()) {
				if (ft.isDefaultTerm()) {

					String result = "";

					if (ft.getNotes() != null) {
						result = ft.getNotes().getChildAt(0).getCharacters();
					} else {
						result = String.valueOf(ft.getResultLevel());
					}

					elseRelation = new Unique(bind.getInteractionNetwork()
							.getEntity(outEntityName), new OperationEq(),
							Double.parseDouble(result));

				} else {

					String result = "";

					if (ft.getNotes() != null) {
						result = ft.getNotes().getChildAt(0).getCharacters();
					} else {
						result = String.valueOf(ft.getResultLevel());
					}

					ASTNode ast = ft.getMath();

					ifRelation = createRealtion(ast);

					thenRelation = new Unique(bind.getInteractionNetwork()
							.getEntity(outEntityName), new OperationEq(),
							Double.parseDouble(result));

				}
			}

			// /////////// we create and add the interactions
			Interaction thenInteraction = relationFactory
					.makeIfThenInteraction(thenRelation, ifRelation);

			bind.getInteractionNetwork().addAddedIntercation(thenInteraction);

			Relation ifInversedRelation = new InversedRelation(ifRelation);

			// the else interaction
			Interaction elseInteraction = relationFactory
					.makeIfThenInteraction(elseRelation, ifInversedRelation);

			bind.getInteractionNetwork().addAddedIntercation(elseInteraction);

			bind.getInteractionNetwork().addTargetInteractions(
					elseRelation.getEntity(), thenInteraction, elseInteraction);
			// /////////

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

		// System.out.println(type);

		if (type.toString().equals("LOGICAL_AND")) {
			return new And();
		} else if (type.toString().equals("LOGICAL_OR")) {
			return new Or();
		} else if (type.toString().equals("RELATIONAL_EQ")) {

			Unique unique = new Unique(bind.getInteractionNetwork().getEntity(
					ast.getChild(0).toString()), new OperationEq(),
					Double.parseDouble(ast.getChild(1).toString()));

			return unique;

		} else if (type.toString().equals("RELATIONAL_LEQ")) {

			Unique unique = new Unique(bind.getInteractionNetwork().getEntity(
					ast.getChild(0).toString()), new OperationLe(),
					Double.parseDouble(ast.getChild(1).toString()));

			return unique;

		} else if (type.toString().equals("RELATIONAL_GEQ")) {

			Unique unique = new Unique(bind.getInteractionNetwork().getEntity(
					ast.getChild(0).toString()), new OperationGe(),
					Double.parseDouble(ast.getChild(1).toString()));

			return unique;

		} else if (type.toString().equals("RELATIONAL_LT")) {

			Unique unique = new Unique(bind.getInteractionNetwork().getEntity(
					ast.getChild(0).toString()), new OperationLt(),
					Double.parseDouble(ast.getChild(1).toString()));

			return unique;

		} else if (type.toString().equals("RELATIONAL_GT")) {

			Unique unique = new Unique(bind.getInteractionNetwork().getEntity(
					ast.getChild(0).toString()), new OperationGt(),
					Double.parseDouble(ast.getChild(1).toString()));

			return unique;

		} else if (type.toString().equals("LOGICAL_NOT")) {

			return new InversedRelation(createRealtion(ast.getChild(0)));

		}

		return null;
	}

}