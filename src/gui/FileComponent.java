/**
 * 24 juin 2013 
 */
package src.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

/**
 * @author lmarmiesse 24 juin 2013
 * 
 */
public class FileComponent extends ArgumentComponent {

	JTextField textField = new JTextField(10);
	JButton chooserButton = new JButton("Choose file");
	final JFileChooser fc = new JFileChooser(".");
	
	
	public FileComponent(String arg) {
		super(arg);

		add(textField);
		add(chooserButton);
		
		
		chooserButton.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent arg0) {
				
				int returnVal = fc.showOpenDialog(FileComponent.this);

		        if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = fc.getSelectedFile();
		            
		            textField.setText(file.getAbsolutePath());
		        } 
				
			}
			
			
			
			
		});
		
		
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
