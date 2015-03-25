package org.gnu.glpk;

import java.util.LinkedList;

/**
 * This class manages callbacks from the MIP solver.
 * <p>The GLPK MIP solver calls method {@link #callback(long) callback} in
 * the branch-and-cut algorithm. A listener to the callback can be used to
 * influence the sequence in which nodes of the search tree are evaluated, or
 * to supply a heuristic solution. To find out why the callback is issued
 * use method {@link GLPK#glp_ios_reason(glp_tree) GLPK.glp_ios_reason}.
 */
public final class GlpkCallback {
    /**
     * List of callback listeners.
     */
    private static LinkedList<GlpkCallbackListener> listeners
            = new LinkedList<GlpkCallbackListener>();

    /**
     * Constructor.
     */
    private GlpkCallback() {
    }

    /**
     * Callback method called by native library.
     * @param cPtr pointer to search tree
     */
    public static void callback(final long cPtr) {
        glp_tree tree;
        tree = new glp_tree(cPtr, false);
        for (GlpkCallbackListener listener : listeners) {
            listener.callback(tree);
        }
    }

    /**
     * Adds a listener for callbacks.
     * @param listener listener for callbacks
     */
    public static void addListener(final GlpkCallbackListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener for callbacks.
     * @param listener listener for callbacks
     */
    public static void removeListener(final GlpkCallbackListener listener) {
        listeners.remove(listener);
    }

}
