/**
 * 24 juin 2013 
 */
package flexflux.gui;

import javax.swing.JComboBox;

/**
 * @author lmarmiesse
 * 24 juin 2013
 *
 */
public class ChoiceComponent extends ArgumentComponent{


	JComboBox<String> choices = new JComboBox<String>();
	
	
	public ChoiceComponent(String arg) {
		super(arg);
		
		add(choices);
		
	}

	public void addChoice(String s){
		choices.addItem(s);
	}
	
	public String[] getValue() {
		return new String[] { argument, choices.getSelectedItem().toString()};
	}
	
	
	
	public void init(String[] strings) {
		
		choices.setSelectedItem(strings[1]);
		
	}

	public void init(String string) {
		choices.setSelectedItem(string);
	}

}
