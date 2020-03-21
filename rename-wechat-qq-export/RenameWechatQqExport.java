/*
 * Rename Tencent instant messaging applications (Wechat, QQ, TIM...) exported pictures.
 *
 * Written by CHEN Qingcan, Spring 2020, Foshan China
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * Run as script via Java 11:
 * java RenameWechatQqExport.java
 */

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

//----------------------------------------------------------------------------
/** Search file content by specified character set over folder tree. */
public final class RenameWechatQqExport {

	static final WildcardFileFilter WILDCARDS  = new WildcardFileFilter (
		List.of ("*.jpg", "*.jpeg", "*.jpe", "*.png", "*.gif", "*.bmp"),
		IOCase.SYSTEM);

	static final List<String> PREFIX_REMOVE = List.of (
		"΢��ͼƬ", "TIMͼƬ", "QQͼƬ");

	static final BufferedReader stdin = new BufferedReader (new InputStreamReader (System.in));
	static final PrintStream stdout = System.out;
	static final PrintStream stderr = System.err;
	static final String HR = "--------";

	static Config config = null;
	static class Config {
		Path            where   = null;
		boolean         yes     = false;

		//--------------------------------------------------------------------
		@Override
		public String toString () {
			return String.format ("Searching for Tencent IM exported pictures in %s...",
				config.where);
		}

		//--------------------------------------------------------------------
		/** Get configuration from command line arguments. */
		static Config fromArgs (final String... args) {
			var config = new Config ();
			for (String arg1 : args) {
				if (isNonCaseIn (List.of ("-y", "-yes", "--yes", "/y", "/yes"), arg1)) {
					config.yes = true;
				} else if (config.where == null) {
					config.where = Paths.get (arg1);
				} else {
					stderr.println ("Too many arguments.");
					System.exit (1);
				}
			}
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
		static Config fromStdIn (final Config c) throws IOException {
			Objects.requireNonNull (c);
			if (c.where != null) {
				return c;
			}
			c.where = Paths.get (stdinLine (".", "Where to search (default to current folder): "));
			return c;
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
			config = Config.fromArgs  (args);
			config = Config.fromStdIn (config);
			stdout.println (config.toString ());
			stdout.println (HR);
			var n = walkFileTree ();
			stdout.println (HR);
			stdout.printf ("Renamed %d files.%n", n);

		} catch (IOException e) {
			stderr.println (e.getMessage ());
		}
	}

	//------------------------------------------------------------------------
	private static int walkFileTree () throws IOException {
		final var cound = new AtomicInteger ();

		Files.walkFileTree (config.where, new SimpleFileVisitor<Path>() {
			final File IGNORED = null;
			@Override
			public FileVisitResult visitFile (final Path p, final BasicFileAttributes attrs) throws IOException {
				if (WILDCARDS.accept (IGNORED, p.toString ())) {
					cound.addAndGet (rename (p));
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return cound.get ();
	}

	//------------------------------------------------------------------------
	/** @return 1 if the file renamed; 0 if not. */
	private static int rename (final Path p) {
		Objects.requireNonNull (p);
		for (String prefix1 : PREFIX_REMOVE) {
			if (p.getFileName ().toString ().startsWith (prefix1)) {
				return removePrefix (p, prefix1.length ());
			}
		}
		return 0;
	}

	//------------------------------------------------------------------------
	/** @return 1 if the file renamed; 0 if not. */
	private static int removePrefix (final Path path, final int prefix) {
		if (path == null || prefix <= 0 || path.getFileName ().toString ().length () <= prefix) {
			return 0;
		}
		File src = path.toFile ();
		String destName = path.getFileName ().toString ().substring (prefix);
		if (destName.startsWith ("_") && destName.length () > 1) {
			destName = destName.substring (1);
		}
		File dest = new File (path.getParent ().toString (), destName);
		stdout.printf ("%s -> %s%n", src.getName (), dest.getName ());
		// TODO confirm rename
		src.renameTo (dest);
		return 1;
	}

	// TODO mmexport timestamp daytime

}
