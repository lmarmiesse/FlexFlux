/**
 * 24 juin 2013 
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * @author lmarmiesse 24 juin 2013
 * 
 */
public class GuiRunAnalysis extends Thread {

	private Process p;
	private String[] commandArray;

	public GuiRunAnalysis(String[] commandArray) {
		this.commandArray = commandArray;
	}

	public void run() {

		// initialize the frame of loading
		final JFrame running = new JFrame();

		running.setLayout(new BorderLayout());
		// JProgressBar progressBar = new JProgressBar();
		// progressBar.setIndeterminate(true);
		running.setTitle("Analysis is running");

		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.PAGE_AXIS));

		JTextArea logArea = new JTextArea();
		logArea.setFont(new Font("monospaced", Font.PLAIN, 12));
		logArea.setEditable(false);
		logArea.setBackground(new Color(0, 0, 0));
		logArea.setForeground(new Color(255, 255, 255));

		center.add(new JScrollPane(logArea));

		// running.add(progressBar, BorderLayout.PAGE_START);
		running.add(center, BorderLayout.CENTER);
		final JButton cancelAnalysis = new JButton("Cancel analysis");

		cancelAnalysis.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				// we kill the process
				p.destroy();
				stop();
				running.dispose();
				if (cancelAnalysis.getText().equals("Cancel analysis")) {
					JOptionPane.showMessageDialog(null, "Analysis canceled",
							"", JOptionPane.INFORMATION_MESSAGE);
				}

			}

		});

		running.add(cancelAnalysis, BorderLayout.PAGE_END);
		running.setSize(600, 600);
		running.setLocationRelativeTo(null);
		running.setVisible(true);

		// to stop the process if the window is closed
		running.addWindowListener(new WindowListener() {

			@Override
			public void windowActivated(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowClosing(WindowEvent arg0) {
				p.destroy();
				stop();
				running.dispose();
				if (cancelAnalysis.getText().equals("Cancel analysis")) {
					JOptionPane.showMessageDialog(null, "Analysis canceled",
							"", JOptionPane.INFORMATION_MESSAGE);
				}
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowIconified(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void windowOpened(WindowEvent arg0) {
				// TODO Auto-generated method stub

			}

		});

		try {

			String logContent = "";

			ProcessBuilder builder = new ProcessBuilder(commandArray);
			builder.redirectErrorStream(true);
			p = builder.start();

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));

			int ci;
			double t1 = System.currentTimeMillis();
			while ((ci = stdInput.read()) != -1) {
				char c = (char) ci;

				logContent += c;
				double t2 = System.currentTimeMillis();

				if (t2 - t1 > 500) {
					logArea.setText(logContent);
					logArea.setCaretPosition(logArea.getText().length());
					t1 = t2;
				}

			}

			logArea.setText(logContent);
			logArea.setCaretPosition(logArea.getText().length());

			while ((ci = stdError.read()) != -1) {
				char c = (char) ci;

				logContent += c;
				double t2 = System.currentTimeMillis();

				if (t2 - t1 > 500) {
					logArea.setText(logContent);
					logArea.setCaretPosition(logArea.getText().length());
					t1 = t2;
				}

			}

			logArea.setText(logContent);
			logArea.setCaretPosition(logArea.getText().length());

			logContent += "Command line : \n";

			int caretPosition = logArea.getText().length();

			// we detect the OS
			String os = System.getProperty("os.name").toLowerCase();

			if (os.indexOf("win") >= 0) {
				logContent += "Flexflux.bat ";
			} else {
				logContent += "./Flexflux.sh ";
			}

			logContent += commandArray[4].substring(commandArray[4]
					.lastIndexOf("ux") + 2) + " ";

			int i = 0;
			for (String s : commandArray) {
				if (i > 4) {
					logContent += s + " ";
				}
				i++;
			}
			logArea.setText(logContent);
			logArea.setCaretPosition(caretPosition);

			// we wait for the process to be over
			p.waitFor();

			running.setTitle("Analysis is over");
			cancelAnalysis.setText("Close window");
			running.revalidate();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
