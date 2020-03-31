import java.io.*;
import java.util.*;

//----------------------------------------------------------------------------
/**
 * Hex view of part of a file.
 *
 * <p>
 * Written by CHEN Qingcan, Spring 2020, Foshan China <br>
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * <p>
 * Run as script via Java 11: <br>
 * <code>
 * java FilePartHex.java
 * </code>
 */
public final class FilePartHex {

	static final BufferedReader stdin = new BufferedReader (new InputStreamReader (System.in));
	static final PrintStream stdout = System.out;
	static final PrintStream stderr = System.err;
	static final String HR = "--------";

	static Config config = null;
	static class  Config {
		String    where;
		Integer   offset = 0;
		Integer   length = 0x10 * 0x10;

		//--------------------------------------------------------------------
		static Config getInstance (final String... args) throws IOException {
			return args.length > 0 ? fromArgs (args) : fromStdIn ();
		}

		//--------------------------------------------------------------------
		/** Get configuration from command line arguments. */
		static Config fromArgs (final String... args) {
			var config = new Config ();
			if (args.length >= 1) {
				config.where = args[0];
			}
			try {
				if (args.length >= 2) {
					config.offset = Integer.valueOf (args[1]);
				}
				if (args.length >= 3) {
					config.length = Integer.valueOf (args[2]);
				}
			} catch (NumberFormatException e) {
				stderr.println ("Argument offset and length should be a number.");
			}
			return config;
		}

		//--------------------------------------------------------------------
		/** Get configuration from standard input. */
		private static Config fromStdIn () throws IOException {
			var config = new Config ();
			config.where = stdinLine ("", "File path: ");
			try { config.offset = Integer.valueOf (stdinLine ("0", "Offset (default to 0): ")); }
			catch (NumberFormatException e) { /* ignore */ }
			try { config.length = Integer.valueOf (stdinLine ("256", "Length (default to 256): ")); }
			catch (NumberFormatException e) { /* ignore */ }
			return config;
		}

		//--------------------------------------------------------------------
		/** Prompt and then read a line from standard input. Return default value if empty input. */
		static String stdinLine (final String defaultLine, final String format, final Object... args)
			throws IOException {
			stdout.printf (format, args);
			String line = stdin.readLine ().trim ();
			return line.length () > 0 ? line : defaultLine;
		}

	}

	//------------------------------------------------------------------------
	/** Program entry */
	public static void main (final String... args) {
		try {
			config = Config.getInstance (args);
			var r  = input ();
			output (r);
		} catch (IOException e) {
			stderr.println (e.getMessage ());
		}
	}

	//------------------------------------------------------------------------
	/** Read bytes from file */
	private static byte[] input () throws FileNotFoundException, IOException {
		try (var in = new FileInputStream (config.where)) {
			var r = new byte [config.length];
			in.skip (config.offset);
			int n = in.read (r, 0, config.length);
			return Arrays.copyOf (r, n > 0 ? n : 0);
		}
	}

	//------------------------------------------------------------------------
	/** Bytes in hex to standard output. */
	private static void output (final byte[] r) {
		// table header
		stdout.println (config.where);
		stdout.printf ("offset %d (%X in hex) length %d (%X in hex)%n",
			config.offset, config.offset, config.length, config.length);
		
		// column header
		stdout.print ("\t  ");
		for (int i = 0 ; i < 0x10 ; i++) {
			stdout.printf ("%02X ", i);
		}
		stdout.printf ("%n\t ");
		for (int i = 0 ; i < 0x10 * 3 ; i++) {
			stdout.print ('-');
		}
		
		var visible = new StringBuilder ();
		for (int i = 0 ; i < r.length ; i++) {
			if (i % 0x10 == 0) {
				// line header
				stdout.println (visible.toString ());
				visible.setLength (0);
				stdout.printf ("%02X\t| ", config.offset + i);
			}
			
			stdout.printf ("%02X ", r[i]);
			visible.append (r[i] >= 0x20 && r[i] <= 0x7E ? (char) r[i] : '.');
		}
		stdout.println (visible.toString ());
	}

}
