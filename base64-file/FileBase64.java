import java.io.*;
import java.nio.file.*;
import java.util.*;

//----------------------------------------------------------------------------
/**
 * File to Base64.
 *
 * <p>
 * Written by CHEN Qingcan, Spring 2020, Foshan China <br>
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * <p>
 * Run as script via Java 11: <br>
 * <code>
 * java FileBase64.java
 * </code>
 */
public final class FileBase64 {

	static final BufferedReader stdin = new BufferedReader (new InputStreamReader (System.in));
	static final PrintStream stdout = System.out;
	static final PrintStream stderr = System.err;
	
	static Config config = null;
	static class  Config {
		Path      from;
		/** empty stands for output to console not to file */
		String    to     = "";

		//--------------------------------------------------------------------
		static Config getInstance (final String... args) throws IOException {
			return args.length >= 1 ? fromArgs (args) : fromStdIn ();
		}

		//--------------------------------------------------------------------
		/** Get configuration from command line arguments. */
		private static Config fromArgs (final String... args) {
			assert args != null && args.length >= 1;
			var config = new Config ();
			config.from = Paths.get (args[0]);
			if (args.length >= 2) {
				config.to = args[1];
			}
			return config;
		}

		//--------------------------------------------------------------------
		/** Get configuration from standard input. */
		private static Config fromStdIn () throws IOException {
			var config = new Config ();
			
			// from
			String prompt;
			for (prompt = "" ; prompt.length () <= 0 ; prompt = stdin.readLine ().trim ()) {
				stdout.print ("Input file name: ");
			}
			config.from = Paths.get (prompt);
			
			// to
			stdout.print ("Output file name (empty to console): ");
			config.to = stdin.readLine ().trim ();
			
			return config;
		}

	}

	//------------------------------------------------------------------------
	/** Program entry */
	public static void main (final String... args) {
		try {
			config = Config.getInstance (args);
			var b  = Files.readAllBytes (config.from);
			output (b);
			
		} catch (IOException e) {
			stderr.println (e.getMessage ());
		}
	}

	//------------------------------------------------------------------------
	private static void output (final byte[] b) throws IOException {
		var base64s = Base64.getEncoder().encodeToString (b);
		if (config.to.length () <= 0) {
			stdout.println (base64s);
		} else {
			Files.writeString (Path.of (config.to), base64s);
			stdout.printf ("%d bytes written to output file %s%n", base64s.length (), config.to);
		}
	}

}
