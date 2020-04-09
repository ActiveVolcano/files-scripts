import java.io.*;
import java.nio.file.*;
import java.util.*;

//----------------------------------------------------------------------------
/**
 * Base64 to file.
 *
 * <p>
 * Written by CHEN Qingcan, Spring 2020, Foshan China <br>
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * <p>
 * Run as script via Java 11: <br>
 * <code>
 * java Base64File.java
 * </code>
 */
public final class Base64File {

	static final BufferedReader stdin = new BufferedReader (new InputStreamReader (System.in));
	static final PrintStream stdout = System.out;
	static final PrintStream stderr = System.err;
	
	static Config config = null;
	static class  Config {
		String    from;
		Path      to;

		//--------------------------------------------------------------------
		static Config getInstance (final String... args) throws IOException {
			return args.length >= 2 ? fromArgs (args) : fromStdIn ();
		}

		//--------------------------------------------------------------------
		/** Get configuration from command line arguments. */
		private static Config fromArgs (final String... args) {
			assert args != null && args.length >= 2;
			var config = new Config ();
			config.from = args[0];
			config.to   = Paths.get (args[1]);
			return config;
		}

		//--------------------------------------------------------------------
		/** Get configuration from standard input. */
		private static Config fromStdIn () throws IOException {
			var config = new Config ();
			
			// from
			stdout.print ("Base64 string or input file name: ");
			config.from = stdin.readLine ().trim ();
			
			// to
			String prompt;
			for (prompt = "" ; prompt.length () <= 0 ; prompt = stdin.readLine ().trim ()) {
				stdout.print ("Output file name: ");
			}
			config.to = Paths.get (prompt);
			
			return config;
		}

	}

	//------------------------------------------------------------------------
	/** Program entry */
	public static void main (final String... args) {
		try {
			config = Config.getInstance (args);
			var base64s = input ();
			output (base64s);
			
		} catch (IOException e) {
			stderr.println (e.getMessage ());
		}
	}

	//------------------------------------------------------------------------
	private static String input () throws IOException {
		var path = Paths.get (config.from);
		if (Files.isReadable (path)) {
			stdout.println ("Path found. Input treated as file name. Read Base64 string from it...");
			return Files.readString (path).trim ();
			
		} else {
			stdout.println ("Path not found. Input treated as Base64 string.");
			return config.from;
		}
	}

	//------------------------------------------------------------------------
	private static void output (final String base64s) throws IOException {
		try {
			var decoder = Base64.getDecoder();
			byte[] base64 = decoder.decode (base64s);
			Files.write (config.to, base64);
			stdout.printf ("%d bytes written to output file %s%n", base64.length, config.to);
			
		} catch (IllegalArgumentException e) {
			stderr.println ("Input not in Base64 format: " + e.getMessage ());
			System.exit (1);
		}
	}

}
