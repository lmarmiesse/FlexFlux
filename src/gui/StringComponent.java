/**
 * 21 juin 2013 
 */
package src.gui;

import javax.swing.JTextField;


/**
 * @author lmarmiesse 21 juin 2013
 * 
 */
public class StringComponent extends ArgumentComponent {

	JTextField textField = new JTextField(20);

	public StringComponent(String arg) {
		super(arg);

		add(textField);

	}

	public String[] getValue() {

		if (textField.getText().equals("")) {
			return new String[] {"",""};
		}

		return new String[] { argument, textField.getText() };
	}

	public void init(String[] strings) {
		textField.setText(strings[1]);
	}

	public void init(String string) {
		textField.setText(string);
		
	}

}
