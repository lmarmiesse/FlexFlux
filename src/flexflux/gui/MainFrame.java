/**
 * 21 juin 2013 
 */
package flexflux.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jfree.ui.RefineryUtilities;
import org.kohsuke.args4j.Option;

/**
 * 
 * FlexFlux GUI frame
 * 
 * 
 * @author lmarmiesse 21 juin 2013
 * 
 */
public class MainFrame extends JFrame {

	Map<Option, Field> optionToField = new HashMap<Option, Field>();
	List<Option> requiredOptions = new ArrayList<Option>();
	List<Option> optionalOptions = new ArrayList<Option>();

	List<ArgumentComponent> argComponents = new ArrayList<ArgumentComponent>();

	Map<String, String[]> previousArguments = new HashMap<String, String[]>();

	private List<String> solvers;
	private List<Class> executableClasses = new ArrayList<Class>();

	private JComboBox executableList = new JComboBox();

	JPanel northPanel = new JPanel();
	JPanel centerPanel = new JPanel();
	JPanel choice = new JPanel();
	JPanel description = new JPanel();

	JLabel descriptionText = new JLabel();

	private JPanel argsPanel = new JPanel();
	private JPanel requiredArgsPanel = new JPanel();
	private JPanel optionalArgsPanel = new JPanel();

	String[] columnNames = { "Agument name", "Value" };

	private boolean hasSolver = true;

	public MainFrame(List<String> solvers, List<Class> classes) {

		if (solvers.size() == 0) {

			hasSolver = false;
			String message = "No solver is well configured. Check the configuration file.\nYou wont be able to use "
					+ "all of Flexflux functions";
			 JOptionPane.showMessageDialog(new JFrame(), message,
			 "No solver found", JOptionPane.WARNING_MESSAGE);
//			System.exit(0);
		}

		this.solvers = solvers;

		if (hasSolver == false) {
			
			for (Class cl : classes){
				try {
					
					if(!(boolean) cl.getField("requiresSolver").get(null)){
						this.executableClasses.add(cl);
					}
					
					
				} catch (NoSuchFieldException | SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			

		} else {
			this.executableClasses = classes;
		}
		
		
		fillExecList();

		JPanel northPanel = new JPanel();

		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.PAGE_AXIS));
		northPanel.add(choice);
		northPanel.add(description);

		description.add(descriptionText);

		choice.setLayout(new FlowLayout());
		choice.add(new JLabel("Choose your analysis : "));
		choice.add(executableList);
		executableList.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				updateAnalysis(executableList.getSelectedIndex());
			}

		});

		JButton run = new JButton("Run");

		choice.add(run);

		run.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				run();

			}

		});

		argsPanel.setLayout(new BoxLayout(argsPanel, BoxLayout.PAGE_AXIS));

		requiredArgsPanel.setBorder(BorderFactory
				.createTitledBorder("Required arguments"));
		optionalArgsPanel.setBorder(BorderFactory
				.createTitledBorder("Optional arguments"));

		requiredArgsPanel.setLayout(new GridLayout(0, 2));
		optionalArgsPanel.setLayout(new GridLayout(0, 2));

		argsPanel.add(requiredArgsPanel);
		argsPanel.add(optionalArgsPanel);

		centerPanel.add(argsPanel);

		updateAnalysis(0);

		this.add(northPanel, BorderLayout.NORTH);
		this.add(centerPanel, BorderLayout.CENTER);
		setTitle("FlexFlux");

		pack();
		RefineryUtilities.centerFrameOnScreen(this);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);

	}

	public void fillExecList() {
		for (Class c : executableClasses) {
			executableList.addItem(c.getSimpleName().replace("Flexflux", ""));
		}
	}

	/**
	 * Updates the frame when a new analysis is chosen.
	 * 
	 * @param selectedIndex
	 */
	private void updateAnalysis(int selectedIndex) {
		Class selectedClass = executableClasses.get(selectedIndex);

		// we check the previous arguments to keep them

		for (ArgumentComponent comp : argComponents) {

			for (String s : comp.getValue()) {
				if (!s.equals("")) {
					continue;
				}
			}
			previousArguments.put(comp.getArgument(), comp.getValue());
		}

		requiredOptions.clear();
		optionalOptions.clear();
		argComponents.clear();

		for (Field f : selectedClass.getDeclaredFields()) {

			if (f.getName().equals("message")) {
				try {
					descriptionText.setText("<html><p>Description : </p><p>"
							+ ((String) f.get(selectedClass.newInstance()))
									.replaceAll("\n", "<br>") + "</p></html>");
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			for (Annotation a : f.getDeclaredAnnotations()) {
				if (a instanceof Option) {

					Option option = (Option) a;
					optionToField.put(option, f);
					if (!f.getType().getName().equals("boolean")) {

						if (option.required()) {
							requiredOptions.add(0, option);
						} else {
							optionalOptions.add(0, option);
						}
					} else {
						if (option.required()) {
							requiredOptions.add(option);
						} else {
							optionalOptions.add(option);
						}
					}

				}
			}

		}

		try {
			requiredArgsPanel.removeAll();
			optionalArgsPanel.removeAll();
			for (Option opt : requiredOptions) {
				JLabel n = new JLabel(opt.name());
				n.setToolTipText(opt.usage());
				requiredArgsPanel.add(n);

				ArgumentComponent p = getRightComponent(opt.metaVar(),
						opt.name());
				requiredArgsPanel.add(p);
				argComponents.add(p);

				p.init(String.valueOf(optionToField.get(opt).get(
						selectedClass.newInstance())));

				if (previousArguments.containsKey(opt.name())) {
					p.init(previousArguments.get(opt.name()));
				}
			}
			for (Option opt : optionalOptions) {
				JLabel n = new JLabel(opt.name());
				n.setToolTipText(opt.usage());
				optionalArgsPanel.add(n);
				ArgumentComponent p = getRightComponent(opt.metaVar(),
						opt.name());
				optionalArgsPanel.add(p);
				argComponents.add(p);
				p.init(String.valueOf(optionToField.get(opt).get(
						selectedClass.newInstance())));
				if (previousArguments.containsKey(opt.name())) {
					p.init(previousArguments.get(opt.name()));
				}
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (requiredOptions.size() == 0) {
			requiredArgsPanel.add(new JLabel("No arguments"));
		}
		if (optionalOptions.size() == 0) {
			optionalArgsPanel.add(new JLabel("No arguments"));
		}

		pack();
		RefineryUtilities.centerFrameOnScreen(this);
	}

	private ArgumentComponent getRightComponent(String metaVar, String name) {

		if (!metaVar.equals("")) {
			if (metaVar.equals("File")) {
				return new FileComponent(name);
			} else if (metaVar.equals("Solver")) {

				ChoiceComponent c = new ChoiceComponent(name);
				for (String s : solvers) {
					c.addChoice(s);
				}

				return c;
			} else if (metaVar.startsWith("[") && metaVar.endsWith("]")) {

				ChoiceComponent c = new ChoiceComponent(name);
				for (String s : ((String) metaVar.subSequence(1,
						metaVar.length() - 1)).split(",")) {
					c.addChoice(s);
				}

				return c;

			}

			else {
				return new StringComponent(name);
			}
		}

		else {
			return new BooleanComponent(name);
		}

	}

	public void run() {

		Class selectedClass = executableClasses.get(executableList
				.getSelectedIndex());

		List<String> argsList = new ArrayList<String>();

		for (ArgumentComponent comp : argComponents) {

			for (String s : comp.getValue()) {
				if (!s.equals("")) {
					argsList.add(s);
				}
			}
		}

		String[] args = new String[argsList.size()];

		int i = 0;
		for (String s : argsList) {
			args[i] = s;
			i++;
		}

		try {

			String jvm = System.getProperty("java.home") + File.separator
					+ "bin" + File.separator + "java";
			String classpath = System.getProperty("java.class.path");
			String libpath = System.getProperty("java.library.path");

			List<String> command = new ArrayList<String>();
			command.add(jvm);
			command.add("-Djava.library.path=" + libpath);
			command.add("-cp");
			command.add(classpath);
			command.add(selectedClass.getName());
			command.addAll(Arrays.asList(args));

			String[] commandArray = new String[command.size()];

			int j = 0;
			for (String a : command) {
				commandArray[j] = a;
				j++;
			}

			GuiRunAnalysis analysis = new GuiRunAnalysis(commandArray);
			analysis.start();

		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
