package flexflux.analyses.result;

import java.util.Comparator;

import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class MyTableRowSorter<M extends TableModel> extends TableRowSorter<M> {

	public MyTableRowSorter(M model) {
		setModel(model);
	}

	public Comparator<?> getComparator(int c) {

		return new Comparator<Object>() {
			@Override
			public int compare(Object arg0, Object arg1) {

				String s1 = (String) arg0;
				String s2 = (String) arg1;

				if (!isDouble(s1)) {
					if (!isDouble(s2)) {
						return (s1.compareToIgnoreCase(s2));
					} else {
						return -1;
					}
				} else if (!isDouble(s2)) {
					return 1;
				} else {

					if (Double.parseDouble(s1) < Double.parseDouble(s2)) {
						return -1;
					} else if (Double.parseDouble(s1) > Double.parseDouble(s2)) {
						return 1;
					} else {
						return 0;
					}
				}
			}

		};

	}

	public boolean isDouble(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (Exception e) {
			return false;
		}

	}
}
