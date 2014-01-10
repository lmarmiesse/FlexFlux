/**
 * 21 juin 2013 
 */
package src.gui;

import java.awt.FlowLayout;

import javax.swing.JPanel;

/**
 * @author lmarmiesse 21 juin 2013
 * 
 */
public abstract class ArgumentComponent extends JPanel {

	protected String argument;

	public ArgumentComponent(String arg) {
		setLayout(new FlowLayout());
		this.argument = arg;
	}

	public abstract String[] getValue();
	
	public String getArgument(){
		return argument;
	}

	public abstract void init(String[] strings);
	
	public abstract void init(String string);

}
