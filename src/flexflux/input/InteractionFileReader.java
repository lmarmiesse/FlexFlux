package flexflux.input;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import parsebionet.biodata.BioEntity;
import flexflux.general.Constraint;
import flexflux.interaction.And;
import flexflux.interaction.Interaction;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.Or;
import flexflux.interaction.Relation;
import flexflux.interaction.RelationFactory;
import flexflux.interaction.RelationWithList;
import flexflux.interaction.Unique;
import flexflux.operation.OperationFactory;

public class InteractionFileReader {
	
	private static RelationFactory relationFactory;
	private static InteractionNetwork intNet;
	private static OperationFactory operationFactory;

	public static InteractionNetwork readInteractionFile(String path,
			InteractionNetwork intNetwork, RelationFactory relFactory) {
		
		operationFactory = new OperationFactory();
	
		relationFactory=relFactory;
		intNet=intNetwork;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(path));

			String line;
			int nbLine = 1;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("#") || line.equals("")) {
					nbLine++;
					continue;
				}

				Relation ifRelation = null;
				Unique thenRelation = null;
				Unique elseRelation = null;

				double thenBegins = 0.0, thenLasts = 0.0, elseBegins = 0.0, elseLasts = 0.0;

				Pattern pattern = Pattern
						.compile("IF\\[([^\\]]*)\\]THEN\\[(.*)\\]ELSE\\[(.*)\\]");

				Matcher matcher = pattern.matcher(line);

				if (matcher.find()) {

					if (matcher.groupCount() != 3) {
						System.err.println(line);
						System.err.println("Error in interaction file line "
								+ nbLine + ", interaction not conform");

						System.exit(0);
					}

					String[] ifPart = matcher.group(1).split("\\]\\[");
					ifRelation = makeRelationFromString(ifPart[0], nbLine);

					String[] thenPart = matcher.group(2).split("\\]\\[");
					thenRelation = makeUniqueFromCondition(thenPart[0], nbLine);

					if (thenPart.length > 1) {
						thenBegins = Double.parseDouble(thenPart[1]);
					}
					if (thenPart.length > 2) {
						thenLasts = Double.parseDouble(thenPart[2]);
					}

					String[] elsePart = matcher.group(3).split("\\]\\[");
					elseRelation = makeUniqueFromCondition(elsePart[0], nbLine);

					if (elsePart.length > 1) {
						elseBegins = Double.parseDouble(elsePart[1]);
					}
					if (elsePart.length > 2) {
						elseLasts = Double.parseDouble(elsePart[2]);
					}

					// ///ERROR HANDLING
					String thenEntity = thenPart[0].replaceAll("\\s", "")
							.split("<=|>=|=|<|>|\\*")[0];

					String elseEntity = elsePart[0].replaceAll("\\s", "")
							.split("<=|>=|=|<|>|\\*")[0];

					if (!thenEntity.equals(elseEntity)) {
						System.err
								.println("Error in interaction file line "
										+ nbLine
										+ ", not the same entity in the THEN and the ELSE part");
						System.exit(0);
					}
					// /////////// we create and add the interactions
					Interaction thenInteraction = relationFactory
							.makeIfThenInteraction(thenRelation, ifRelation);

					intNet.addTargetConditionalInteraction(
							intNet.getEntity(thenEntity), thenInteraction);

					thenInteraction.setTimeInfos(new double[] { thenBegins,
							thenLasts });

					// the else interaction
					Interaction elseInteraction = relationFactory
							.makeIfThenInteraction(elseRelation, null);

					elseInteraction.setTimeInfos(new double[] { elseBegins,
							elseLasts });

					intNet.setTargetDefaultInteraction(
							intNet.getEntity(elseEntity), elseInteraction);
					// /////////

				}

				else if (line.replace("\t", " ").split(" ").length == 2) {

					String[] splitLine = line.replace("\t", " ").split(" ");

					// if the entity does not exist yet
					if (intNet.getEntity(splitLine[0]) == null) {

						
						intNet.addNumEntity(new BioEntity(splitLine[0]));
//						System.err
//								.println("Error : unknown variable in interaction file : "
//										+ splitLine[0] + " line " + nbLine);
//						System.exit(0);
					}

					BioEntity ent = intNet.getEntity(splitLine[0]);
					double initValue = 0.0;
					try {
						initValue = Double.parseDouble(splitLine[1]);
					} catch (Exception e) {

						System.err.println("Error in interaction file line "
								+ nbLine + " init value must be a number");
						System.exit(0);
					}

					Map<BioEntity, Double> constMap = new HashMap<BioEntity, Double>();
					constMap.put(ent, 1.0);
					intNet.addInitialConstraint(ent, new Constraint(constMap,
							initValue, initValue));

				}

				else {
					System.err.println(line);
					System.err.println("Error in interaction file line "
							+ nbLine + ", interaction not conform");

					System.exit(0);
				}

				nbLine++;
			}

			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return intNet;

	}
	
	
	/**
	 * Creates a Relation from a string. This is used when parsing the
	 * interaction file.
	 * 
	 * @param line
	 *            String to create the Relation from.
	 * @param nbLine
	 *            Line number of the interaction file.
	 * @return The created Relation.
	 */
	private static Relation makeRelationFromString(String line, int nbLine) {

		String ifExpr = line;

		String[] conditions = ifExpr.split(" AND | OR ");

		Relation rel = null;
		// no Parentheses
		if (!line.contains("(")) {

			if (line.contains(" AND ") && line.contains(" OR ")) {

				System.err.println("Error in interaction file line " + nbLine
						+ ", AND and OR must be separated by parentheses");
				return null;
			}

			if (conditions.length > 1) {

				if (line.contains(" AND ")) {
					rel = (And) relationFactory.makeAnd();
				} else if (line.contains(" OR ")) {
					rel = (Or) relationFactory.makeOr();
				} else {
					System.err.println("Error in interaction file line "
							+ nbLine + ", AND/OR not found");
					return null;
				}
				for (String s : conditions) {

					((RelationWithList) rel)
							.addRelation(makeUniqueFromCondition(s, nbLine));
				}

			} else {

				rel = makeUniqueFromCondition(conditions[0], nbLine);

			}
		}
		// if there are parentheses
		else {
			// list of all the expressions with parentheses (first level)
			List<String> ParenthesesExpressions = new ArrayList<String>();

			int indexStartExpr = 0;
			int indexEndExpr = 0;
			// gives the level of parentheses
			int level = 0;
			boolean inExpr = false;

			String copy = "";

			for (int i = 0; i < ifExpr.length(); i++) {

				if (ifExpr.charAt(i) == ')') {
					level--;
					if (level == -1) {
						System.err.println("Error in interaction file line "
								+ nbLine + ", parentheses error");
						return null;
					}

					if (inExpr && level == 0) {
						indexEndExpr = i;
						inExpr = false;
						ParenthesesExpressions.add(ifExpr.substring(
								indexStartExpr + 1, indexEndExpr));
					}

				}
				if (level == 0) {
					copy += ifExpr.charAt(i);
				}
				if (ifExpr.charAt(i) == '(') {
					level++;

					if (!inExpr && level == 1) {
						inExpr = true;
						indexStartExpr = i;
					}
				}
			}

			// if there as not as many ( and )
			if (level != 0
					|| StringUtils.countMatches(copy, "()") != ParenthesesExpressions
							.size()) {
				System.err.println("Error in interaction file line " + nbLine
						+ ", parentheses error");
				return null;
			}

			if (copy.contains(" AND ") && copy.contains(" OR ")) {

				System.err.println("Error in interaction file line " + nbLine
						+ ", AND and OR must be separated by parentheses");
				return null;
			}

			String[] OtherExpressions = copy.split(" AND | OR ");

			for (String expr : OtherExpressions) {
				if (!expr.replaceAll("\\s", "").equals("()")) {
					ParenthesesExpressions.add(expr);
				}
			}

			if (ParenthesesExpressions.size() == 1) {

				return makeRelationFromString(ParenthesesExpressions.get(0),
						nbLine);
			} else {

				if (copy.contains(" AND ")) {
					rel = (And) relationFactory.makeAnd();
				} else if (copy.contains(" OR ")) {
					rel = (Or) relationFactory.makeOr();
				} else {
					System.err.println("Error in interaction file line "
							+ nbLine + ", AND/OR not found");
					return null;
				}

				for (String expr : ParenthesesExpressions) {
					Relation r = makeRelationFromString(expr, nbLine);

					if (r != null) {
						((RelationWithList) rel)
								.addRelation(makeRelationFromString(expr,
										nbLine));
					} else {
						return null;
					}
				}

			}

		}

		return rel;
	}
	
	/**
	 * Creates a Unique from a string. Used when parsing the interaction file.
	 * 
	 * @param condition
	 *            The string to create the Unique from.
	 * @param nbLine
	 *            Line number of the interaction file.
	 * @return The created Unique.
	 * 
	 *         TODO : Manque de commentaires : a quoi ca sert ????
	 * 
	 */
	private static Unique makeUniqueFromCondition(String condition, int nbLine) {

		String[] splitedCondition = condition.replaceAll("\\s", "").split(
				"<=|>=|=|<|>|\\*");

		String name = splitedCondition[0];

		// if the entity does not exist yet
		if (intNet.getEntity(name) == null) {
			
			intNet.addNumEntity(new BioEntity(name));

//			System.err
//					.println("Error : unknown variable in interaction file : "
//							+ name + " line " + nbLine);
//			System.exit(0);

		}

		if (splitedCondition.length == 1) {

			return (Unique) relationFactory.makeUnique(intNet.getEntity(name),
					operationFactory.makeNotEq(), 0);

		} else {
			Double value = 0.0;

			try {
				value = Double.parseDouble(splitedCondition[1]);
			} catch (NumberFormatException e) {

				System.err.println("Error in interaction file line " + nbLine);
				System.exit(0);

			}

			return (Unique) relationFactory.makeUnique(intNet.getEntity(name),
					operationFactory.makeOperationFromString(condition), value);

		}

	}

}
