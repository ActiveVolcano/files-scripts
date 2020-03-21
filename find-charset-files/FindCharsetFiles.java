/*
 * Search file content by specified character set over folder tree.
 *
 * Written by CHEN Qingcan, Spring 2020, Foshan China
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * Run as script via Java 11:
 * java -cp lib/commons-io-2.6.jar FindCharsetFiles.java
 */

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.IOCase;

//----------------------------------------------------------------------------
/** Search file content by specified character set over folder tree. */
public final class FindCharsetFiles {

	static final BufferedReader stdin = new BufferedReader (new InputStreamReader (System.in));
	static final PrintStream stdout = System.out;
	static final PrintStream stderr = System.err;
	static final String HR = "--------";

	static Config config = null;
	static class Config {
		Path            where;
		List<String>    wildcard;
		String          what;
		Charset         charset;

		//--------------------------------------------------------------------
		@Override
		public String toString () {
			return String.format ("Searching '%s'%s for '%s' with character set %s...%n",
				config.where, config.wildcard, config.what, config.charset);
		}

		//--------------------------------------------------------------------
		/** Get configuration from standard input. */
		static Config fromStdIn (final String... args)
			throws IOException {
			var config = new Config ();
			String line;

			line = stdinLine ("."    , "        Where to search (default to current folder): ");
			config.where = Paths.get (line);
			line = stdinLine ("*"    , "File name wildcards (comma seperated, default to *): ");
			config.wildcard = List.of (line.split (","));
			line = stdinLine (""     , "             What to search (plain text, not regex): ");
			config.what = line;
			if (config.what.length () == 0) {
				stdout.println ("Nothing to search. Exit.");
				System.exit (0);
			}
			line = stdinLine ("UTF-8", "             Which character set (default to UTF-8): ");
			config.charset = Charset.forName (line);

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
			config = Config.fromStdIn (args);
			stdout.println (config.toString ());
			stdout.println (HR);
			var n = walkFileTree ();
			stdout.println (HR);
			stdout.printf ("Found in %d files.%n", n);
		} catch (IOException e) {
			stderr.println (e.getMessage ());
		}
	}

	//------------------------------------------------------------------------
	static int walkFileTree () throws IOException {
		final var wildcard  = new WildcardFileFilter (config.wildcard, IOCase.SYSTEM);
		final var what      = config.what.getBytes (config.charset);
		final var linefeed  = "\n".getBytes (config.charset);
		final var cound     = new AtomicInteger ();

		Files.walkFileTree (config.where, new SimpleFileVisitor<Path>() {
			final File IGNORED = null;
			@Override
			public FileVisitResult visitFile (final Path p, final BasicFileAttributes attrs) throws IOException {
				if (wildcard.accept (IGNORED, p.toString ())) {
					cound.addAndGet (isFileContainsBytes (p, what, linefeed));
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return cound.get ();
	}

	//------------------------------------------------------------------------
	/** @return 1 if the file contains these bytes; 0 if not. */
	static int isFileContainsBytes (final Path where, final byte[] what, final byte[] linefeed) {
		Objects.requireNonNull (where);
		Objects.requireNonNull (what);
		if (what.length <= 0) {
			return 0;
		}
		int found = 0, line = 1;

		try {
			byte[] content = Files.readAllBytes (where);
			for (int offset = 0 ; offset < content.length ;) {
				if (content[offset] == what[0]) {
					if (isContains (content, what, offset)) {
						found = 1;
						offset += what.length;
						stdout.printf ("%s:\tLine %d Offset %d%n", where, line, offset);
					} else {
						 offset++;
					}
				} else if (isContains (content, linefeed, offset)) {
					line++;
					offset += linefeed.length;
				} else {
					 offset++;
				}
			}

		} catch (IOException e) {
			stderr.printf ("%s: [%s] %s%n", where, e.getClass ().getName (), e.getMessage ());
		}
		return found;
	}

	//------------------------------------------------------------------------
	static boolean isContains (final byte[] content, final byte[] what, final int start) {
		if (content.length < start + what.length) {
			return false;
		}
		for (int i = start, j = 0 ; j < what.length ; i++, j++) {
			if (content[i] != what[j]) {
				return false;
			}
		}
		return true;
	}

}
