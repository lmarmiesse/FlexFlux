/**
 * 21 juin 2013 
 */
package flexflux.gui;

import javax.swing.JCheckBox;


/**
 * @author lmarmiesse
 * 21 juin 2013
 *
 */
public class BooleanComponent extends ArgumentComponent{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	JCheckBox checkbox = new JCheckBox();
	
	public BooleanComponent(String arg) {
		super(arg);
	
		add(checkbox);
	
	}

	
	public String[] getValue() {
		if (checkbox.isSelected()){
			return new String[] {argument};
		}
		
		return new String[] {""};
	}

	public void init(String[] strings) {
		
		
		if (strings[0]!=""){
			checkbox.setSelected(true);
		}
		
	}

	public void init(String string) {
		if (string!="false"){
			checkbox.setSelected(true);
		}
	}
	
}
