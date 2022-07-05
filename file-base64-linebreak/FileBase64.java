import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.text.NumberFormat;
import javax.imageio.ImageIO;
import javax.swing.*;

//----------------------------------------------------------------------------
/**
 * Encode file or clipboard to Base64 form with linebreak,
 * so that there are no more than 76 characters per line,
 * according to RFC 2045.
 *
 * <p>
 * Written by CHEN Qingcan, Summer 2022, Foshan China <br>
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * <p>
 * GUI run as script via Java 11: <br>
 * <code>
 * java FileBase64.java
 * </code>
 *
 * <p>
 * CLI run as script via Java 11: <br>
 * <code>
 * java FileBase64.java {InputFilePath}
 * </code>
 */
public final class FileBase64 {

	static final BufferedReader stdin = new BufferedReader (new InputStreamReader (System.in));
	static final PrintStream    stdout = System.out, stderr = System.err;

	static enum InputSource { FILE, CLIPBOARD_IMAGE, CLIPBOARD_STRING };
	static class Config {
		InputSource from    = InputSource.FILE;
		Path        where   = null;
		String      format  = null;
		Charset     charset = null;

		/** New file configuration. */
		Config (final Path p) {
			from    = InputSource.FILE;
			where   = p;
		}
		/** New clipboard image configuration. */
		Config (final String imageWriterFormat) {
			from    = InputSource.CLIPBOARD_IMAGE;
			format  = imageWriterFormat;
		}
		/** New clipboard string configuration. */
		Config (final Charset stringWriterCharset) {
			from    = InputSource.CLIPBOARD_STRING;
			charset = stringWriterCharset;
		}
		/** Get configuration from command line arguments. */
		Config (final String... args) {
			this (Path.of (args[0]));
		}
	}

	//------------------------------------------------------------------------
	JFrame	frmMain	= new JFrame ("File to Base64 with Linebreak");
	JPanel	pnlButton	= new JPanel (new FlowLayout (FlowLayout.LEADING));
	JButton	btnFromFile	= new JButton ("From File");
	JButton	btnFromClipboardImage	= new JButton ("From Clipboard Image");
	JButton	btnFromClipboardString	= new JButton ("From Clipboard String");
	JButton	btnCopy	= new JButton ("Copy");
	JButton	btnSave	= new JButton ("Save");
	JButton	btnClear	= new JButton ("Clear");
	JTextArea	txtBase64	= new JTextArea ();
	JLabel	lblCount	= new JLabel ("0 characters");

	void gui () {
		// Frame
		try { UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName ()); }
		catch (Exception e) { stderr.println (e.getMessage ()); }
		frmMain.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
		frmMain.setMinimumSize (new Dimension (640, 480));
		frmMain.setLocationByPlatform (true);
		frmMain.setLayout (new BorderLayout());
		// North
		addComponent (pnlButton,
			btnFromFile, btnFromClipboardImage, btnFromClipboardString, btnCopy, btnSave, btnClear);
		frmMain.add (pnlButton, BorderLayout.NORTH);
		// Center
		txtBase64.setFont (new Font (Font.MONOSPACED, Font.PLAIN, 12));
		frmMain.add (new JScrollPane (txtBase64), BorderLayout.CENTER);
		// South
		frmMain.add (lblCount, BorderLayout.SOUTH);

		// Font
		setFont (new Font (Font.SANS_SERIF, Font.PLAIN, 12),
			btnFromFile, btnFromClipboardImage, btnFromClipboardString, btnCopy, btnSave, btnClear, lblCount);

		// Event
		btnFromFile.addActionListener (a -> actionFromFile (a));
		btnFromClipboardImage.addActionListener (a -> actionFromClipboardImage (a));
		btnFromClipboardString.addActionListener (a -> actionFromClipboardString (a));
		btnCopy.addActionListener (a -> actionCopy (a));
		btnSave.addActionListener (a -> actionSave (a));
		btnClear.addActionListener (a -> actionClear (a));

		frmMain.setVisible (true);
	}

	//------------------------------------------------------------------------
	void actionFromFile (final ActionEvent a) {
		JFileChooser chooser = new JFileChooser ();
		if (chooser.showOpenDialog (frmMain) == JFileChooser.APPROVE_OPTION) {
			result (new Config (chooser.getSelectedFile().toPath()));
		}
	}

	void actionFromClipboardImage (final ActionEvent a) {
		var clipboard = clipboard().getContents (null);
		if (clipboard.isDataFlavorSupported (DataFlavor.imageFlavor)) {
			String format = (String) JOptionPane.showInputDialog (frmMain, "Image format", "Base64",
				JOptionPane.QUESTION_MESSAGE, /* Icon */ null,
				ImageIO.getWriterFormatNames(), "PNG");
			if (format != null) result (new Config (format));
		} else showFailedDialog ("No image in clipboard.");
	}

	void actionFromClipboardString (final ActionEvent a) {
		var clipboard = clipboard().getContents (null);
		if (clipboard.isDataFlavorSupported (DataFlavor.stringFlavor)) {
			String charset = JOptionPane.showInputDialog (frmMain, "Character set", "UTF-8");
			if (charset != null)
			try { result (new Config (Charset.forName (charset))); }
			catch (Exception e) { showExceptionDialog (e); }
		} else showFailedDialog ("No string in clipboard.");
	}
	
	void actionCopy (final ActionEvent a) {
		clipboard().setContents (new StringSelection (txtBase64.getText ()), null);
		showDoneDialog ("Copied to clipboard.");
	}

	void actionSave (final ActionEvent a) {
		JFileChooser chooser = new JFileChooser ();
		if (chooser.showSaveDialog (frmMain) == JFileChooser.APPROVE_OPTION) try {
			Files.writeString (chooser.getSelectedFile().toPath(), txtBase64.getText ());
			showDoneDialog ("Saved to " + chooser.getSelectedFile().toString());
		} catch (Exception e) { showExceptionDialog (e); }
	}
	
	void actionClear (final ActionEvent a) {
		txtBase64.setText ("");
		lblCount.setText ("0 characters");
	}
	
	//------------------------------------------------------------------------
	String result (final Config config) {
		String base64 = base64linebreak (config);
		txtBase64.setText (base64);
		lblCount.setText (NumberFormat.getInstance().format (base64.length()) + " characters");
		return base64;
	}

	//------------------------------------------------------------------------
	static void addComponent (final Container container, final Component... components) {
		for (Component component1 : components) container.add (component1);
	}
	
	static void setFont (final Font font, final Component... components) {
		for (Component component1 : components) component1.setFont (font);
	}

	static Clipboard clipboard () {
		return Toolkit.getDefaultToolkit().getSystemClipboard();
	}

	//------------------------------------------------------------------------
	void showDoneDialog (final String message) {
		JOptionPane.showMessageDialog (frmMain,
			message,
			"Base64",
			JOptionPane.INFORMATION_MESSAGE);
	}
	void showFailedDialog (final String message) {
		JOptionPane.showMessageDialog (frmMain,
			message,
			"Failed",
			JOptionPane.WARNING_MESSAGE);
	}
	void showExceptionDialog (final Throwable e) {
		JOptionPane.showMessageDialog (frmMain,
			e.getMessage(),
			e.getClass().getName(),
			JOptionPane.WARNING_MESSAGE);
	}

	//------------------------------------------------------------------------
	/** Program entry */
	public static void main (final String... args) {
		if (args != null && args.length >= 1) try {
			stdout.println (base64linebreak (new Config (args)));
		} catch (Exception e) {
			stderr.println (e.getMessage ());
		} else {
			new FileBase64 ().gui ();
		}
	}

	//------------------------------------------------------------------------
	static String base64linebreak (final Config config) {
		Objects.requireNonNull (config);
		try {
			byte[] all = new byte [0];

			if (config.from == InputSource.FILE) {
				all = Files.readAllBytes (config.where);

			} else if (config.from == InputSource.CLIPBOARD_IMAGE) try (var out = new ByteArrayOutputStream ()) {
				var img = (BufferedImage) clipboard().getContents (null).getTransferData (DataFlavor.imageFlavor);
				ImageIO.write (img, "PNG", out);
				all = out.toByteArray();

			} else if (config.from == InputSource.CLIPBOARD_STRING) try (var out = new ByteArrayOutputStream ()) {
				var s = (String) clipboard().getContents (null).getTransferData (DataFlavor.stringFlavor);
				all = s.getBytes (config.charset);
			}

			return Base64.getMimeEncoder ().encodeToString (all);
		} catch (Exception e) { return e.getMessage (); }
	}

}
