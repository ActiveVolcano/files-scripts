import java.io.*;
import java.util.*;
import java.text.*;

//----------------------------------------------------------------------------
/**
 * A simple truncate command that can be run under Windows.
 * Only the -s (--size) option without prefix or suffix is supported.
 * Run in interactive mode if no command-line arguments.
 *
 * <p>
 * Written by CHEN Qingcan, Summer 2022, Foshan China <br>
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * <p>
 * Run as script via Java 11: <br>
 * <code>
 * java TruncateFile.java
 * </code>
 */
public final class TruncateFile {

	static final BufferedReader stdin = new BufferedReader (new InputStreamReader (System.in));
	static final PrintStream    stdout = System.out, stderr = System.err;

	static Config config = null;
	static class Config {
		File where  = null;
		Long size   = 0L;

		//--------------------------------------------------------------------
		/** Get configuration from command line arguments. */
		static Config fromArgs (final String... args)
		throws ParseException {
			var config = new Config ();
			boolean toSize = false;
			for (String arg1 : args) {
				if (toSize) {
					toSize = false;
					config.size = NumberFormat.getInstance().parse(arg1).longValue();
				} else if (isNonCaseIn (List.of ("-s", "-size", "--size", "/s", "/size"), arg1)) {
					toSize = true;
				} else if (config.where == null) {
					config.where = new File (arg1);
				} else {
					throw new ParseException ("Too many arguments.", args.length);
				}
			}
			
			if (config.where == null) throw new ParseException ("Too few arguments.", args.length);
			if (toSize) throw new ParseException ("Lack of size value.", args.length);
			return config;
		}

		//--------------------------------------------------------------------
		static boolean isNonCaseIn (final Collection<String> collection, final String test) {
			for (String c1 : collection) {
				if (c1.equalsIgnoreCase (test)) {
					return true;
				}
			}
			return false;
		}

		//--------------------------------------------------------------------
		/** Get configuration from standard input. */
		static Config fromStdIn ()
		throws IOException, ParseException {
			var config = new Config ();
			String line;

			line = stdinLine ("" , "File: ");
			config.where = new File (line);
			line = stdinLine ("0", "Size (default to 0): ");
			config.size  = NumberFormat.getInstance().parse(line).longValue();

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
			config = args.length > 0 ? Config.fromArgs (args) : Config.fromStdIn ();
			truncate ();
		} catch (Exception e) {
			stderr.println (e.getMessage ());
		}
	}

	//------------------------------------------------------------------------
	private static void truncate () throws IOException {
		try (var f = new RandomAccessFile (config.where, "rw")) {
			f.setLength (config.size);
		}
	}

}
