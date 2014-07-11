/*******************************************************************************
 * Copyright INRA
 * 
 *  Contact: ludovic.cottret@toulouse.inra.fr
 * 
 * 
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *  In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *  The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 ******************************************************************************/
/**
 * 9 avr. 2013 
 */
package flexflux.general;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gnu.glpk.GLPK;
import org.gnu.glpk.GLPKConstants;
import org.gnu.glpk.SWIGTYPE_p_double;
import org.gnu.glpk.SWIGTYPE_p_int;
import org.gnu.glpk.glp_iocp;
import org.gnu.glpk.glp_prob;
import org.gnu.glpk.glp_smcp;

import parsebionet.biodata.BioEntity;
import parsebionet.biodata.BioNetwork;
import flexflux.interaction.InteractionNetwork;
import flexflux.interaction.RelationFactory;
import flexflux.operation.OperationFactory;
import flexflux.thread.ThreadFactoryGLPK;

/**
 * 
 * 
 * GLPK version of Bind. It transforms porblem's variables, constraints and
 * interactions into Objects usable by the GLPK solver.
 * 
 * @author lmarmiesse 9 avr. 2013
 * 
 */
public class GLPKBind extends Bind {

	private glp_prob model;
	private glp_smcp parm;
	private glp_iocp mipParm;

	/**
	 * 
	 * Map to match entities names to a GLPK column index.
	 * 
	 */
	private Map<String, Integer> vars = new HashMap<String, Integer>();

	/**
	 * 
	 * Initialization of GLPK parameters and of the right factories.
	 * 
	 */

	public void init() {

		Vars.maxThread = 1;
		model = GLPK.glp_create_prob();

		// LP params
		parm = new glp_smcp();
		GLPK.glp_init_smcp(parm);
		parm.setTol_bnd(Math.pow(10, -Vars.decimalPrecision));

		// MIP params
		mipParm = new glp_iocp();

		GLPK.glp_term_out(GLPKConstants.GLP_OFF);

		// creation of the right factories
		this.operationFactory = new OperationFactory();
		this.relationFactory = new RelationFactory();
		this.threadFactory = new ThreadFactoryGLPK(constraints,
				simpleConstraints, intNet);
	}

	public GLPKBind() {
		super();
		init();
	}

	public GLPKBind(List<Constraint> constraints,
			Map<BioEntity, Constraint> simpleConstraints,
			InteractionNetwork intNet, BioNetwork bioNet) {
		super(constraints, simpleConstraints, intNet, bioNet);
		init();
	}

	public void entitiesToSolverVars() {
		vars.clear();

		for (BioEntity entity : intNet.getNumEntities()) {
			// what default value ?
			int index = GLPK.glp_add_cols(model, 1);

			vars.put(entity.getId(), index);

			String name = entity.getId();

			if (name.length() > 250) {

				name = name.substring(0, 250);

			}
			entity.setId(name);
			GLPK.glp_set_col_name(model, index, name);
			// CV : continu variable
			GLPK.glp_set_col_kind(model, index, GLPKConstants.GLP_CV);

			GLPK.glp_set_col_bnds(model, index, GLPKConstants.GLP_DB,
					-999999.0, 999999.0);

		}
		for (BioEntity entity : intNet.getIntEntities()) {

			int index = GLPK.glp_add_cols(model, 1);

			vars.put(entity.getId(), index);
			GLPK.glp_set_col_name(model, index, entity.getId());
			// IV : interger variable
			GLPK.glp_set_col_kind(model, index, GLPKConstants.GLP_IV);

			GLPK.glp_set_col_bnds(model, index, GLPKConstants.GLP_DB, -999999,
					999999);

		}
		for (BioEntity entity : intNet.getBinaryEntities()) {

			int index = GLPK.glp_add_cols(model, 1);
			vars.put(entity.getId(), index);
			GLPK.glp_set_col_name(model, index, entity.getId());
			// BV : Binary variable
			GLPK.glp_set_col_kind(model, index, GLPKConstants.GLP_BV);

		}

	}

	// list and map are used to fill the constraints and old bounds
	// to be able to come back to the previous state after
	protected void createSolverConstraint(Constraint constraint,
			List<Object> toRemoveFromModel, Map<String, double[]> oldBounds) {

		double ub = constraint.getUb();
		double lb = constraint.getLb();

		Map<BioEntity, Double> entities = constraint.getEntities();

		int index = GLPK.glp_add_rows(model, 1);

		if (toRemoveFromModel != null) {
			toRemoveFromModel.add(0, index);
		}

		if (ub == lb) {

			GLPK.glp_set_row_bnds(model, index, GLPKConstants.GLP_FX, lb, ub);

		} else {
			GLPK.glp_set_row_bnds(model, index, GLPKConstants.GLP_DB, lb, ub);
		}
		SWIGTYPE_p_int varsIndices = GLPK.new_intArray(entities.size() + 1);
		SWIGTYPE_p_double varsCoeffs = GLPK
				.new_doubleArray(entities.size() + 1);

		int i = 1;
		for (BioEntity entity : entities.keySet()) {

			GLPK.intArray_setitem(varsIndices, i, vars.get(entity.getId()));
			GLPK.doubleArray_setitem(varsCoeffs, i, entities.get(entity));

			i++;
		}

		GLPK.glp_set_mat_row(model, index, entities.size(), varsIndices,
				varsCoeffs);

		String name = "";
		for (BioEntity entity : entities.keySet()) {
			// if it is a "simple" constraint
			name = entity.getId();

		}

		GLPK.glp_set_row_name(model, index, name);

		GLPK.delete_intArray(varsIndices);
		GLPK.delete_doubleArray(varsCoeffs);

		// if the constraint overwrites
		if (entities.size() == 1 && !constraint.getNot()
				&& false) {

			// System.err.println(constraint);
			for (BioEntity entity : entities.keySet()) {
				// if it is a "simple" constraint
				if (entities.get(entity) == 1.0) {

					if (oldBounds != null) {
						oldBounds.put(
								entity.getId(),
								new double[] {
										GLPK.glp_get_col_lb(model,
												vars.get(entity.getId())),
										GLPK.glp_get_col_ub(model,
												vars.get(entity.getId())) });
					}

					if (solverSimpleConstraints.containsKey(entity)
							&& (Integer) solverSimpleConstraints.get(entity) != -1) {

						List<Object> list = new ArrayList<Object>();
						list.add(solverSimpleConstraints.get(entity));
						deleteConstraints(list);
						solverSimpleConstraints.put(entity, -1);

					} else if (!solverSimpleConstraints.containsKey(entity)) {
						solverSimpleConstraints.put(entity, index);
					}

					// we set the bounds of the variable
					if (ub == lb) {
						GLPK.glp_set_col_bnds(model, vars.get(entity.getId()),
								GLPKConstants.GLP_FX, lb, ub);
					} else {
						GLPK.glp_set_col_bnds(model, vars.get(entity.getId()),
								GLPKConstants.GLP_DB, lb, ub);
					}
				}
			}
		}
	}

	protected void deleteConstraints(List<Object> constraints) {

		for (Object constraint : constraints) {
			int index = (Integer) constraint;

			solverSimpleConstraints.remove(index);
			// we free the row (we set the bounds to -inf +inf
			// because it keeps the basis valid
			GLPK.glp_set_row_bnds(model, index, GLPKConstants.GLP_FR, 0.0, 0.0);
		}

		// if there is a basis problem, we remake it with glp_adv_basis or
		// glp_cpx_basis
		if (GLPK.glp_warm_up(model) != 0) {
			GLPK.glp_cpx_basis(model);

		}
	}

	public void makeSolverObjective() {
		BioEntity[] entities = obj.getEntities();
		double[] coeffs = obj.getCoeffs();
		boolean maximize = obj.getMaximize();
		String name = obj.getName();

		if (name.length() < 200) {
			GLPK.glp_set_obj_name(model, name);
		} else {
			GLPK.glp_set_obj_name(model, name.substring(0, 200));
		}

		if (maximize) {
			GLPK.glp_set_obj_dir(model, GLPKConstants.GLP_ASN_MAX);
		} else {
			GLPK.glp_set_obj_dir(model, GLPKConstants.GLP_ASN_MIN);
		}

		for (String entName : vars.keySet()) {

			GLPK.glp_set_obj_coef(model, vars.get(entName), 0);
		}

		for (int i = 0; i < entities.length; i++) {

			GLPK.glp_set_obj_coef(model, vars.get(entities[i].getId()),
					coeffs[i]);

		}

	}

	public void changeObjVarValue(BioEntity e, double d) {

		GLPK.glp_set_obj_coef(model, vars.get(e.getId()), d);

	}

	public void setObjSense(boolean maximize) {

		if (maximize) {
			GLPK.glp_set_obj_dir(model, GLPKConstants.GLP_ASN_MAX);
		} else {
			GLPK.glp_set_obj_dir(model, GLPKConstants.GLP_ASN_MIN);
		}
	}

	protected void clear() {
		end();
		model = GLPK.glp_create_prob();
	}

	public boolean isMIP() {
		return GLPK.glp_get_num_int(model) > 0;
	}

	protected DoubleResult go(boolean saveResults) {

		// System.err.println(GLPK.glp_get_col_prim(model, 5430));

		int ret = GLPK.glp_simplex(model, parm);

		if (isMIP()) {
			ret = GLPK.glp_intopt(model, null);
		}

		if (ret == 0) {
			
			// if the problem is a LP
			if (!isMIP()) {
				// if there is an optimal solution
				if (GLPK.glp_get_status(model) == GLPK.GLP_OPT) {
					double result = GLPK.glp_get_obj_val(model);

					if (saveResults) {
						for (int i = 1; i <= GLPK.glp_get_num_cols(model); i++) {

							lastSolve.put(GLPK.glp_get_col_name(model, i),
									GLPK.glp_get_col_prim(model, i));
						}
					}

					return new DoubleResult(result, 0);

				}
			}
			// if it is a MIP
			else {
				if (GLPK.glp_mip_status(model) == GLPK.GLP_OPT) {
					double result = GLPK.glp_mip_obj_val(model);

					if (saveResults) {
						for (int i = 1; i <= GLPK.glp_get_num_cols(model); i++) {

							lastSolve.put(GLPK.glp_get_col_name(model, i),
									GLPK.glp_mip_col_val(model, i));
						}
					}

					return new DoubleResult(result, 0);
				}

			}
		} else {

			// System.err.println("The problem could not be solved");

		}
		return new DoubleResult(0, 1);
	}

	public void end() {
		GLPK.glp_delete_prob(model);
	}

	protected void changeVarBounds(String entity, double[] bounds) {

		double lb = bounds[0];
		double ub = bounds[1];

		if (ub == lb) {
			GLPK.glp_set_col_bnds(model, vars.get(entity),
					GLPKConstants.GLP_FX, lb, ub);
		} else {
			GLPK.glp_set_col_bnds(model, vars.get(entity),
					GLPKConstants.GLP_DB, lb, ub);
		}
	}

}
