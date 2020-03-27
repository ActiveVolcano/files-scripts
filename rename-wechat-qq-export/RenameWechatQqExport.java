import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

//----------------------------------------------------------------------------
/**
 * Batch rename Tencent instant messaging applications (Wechat, QQ, TIM...) exported pictures in a folder.
 *
 * <p>
 * Written by CHEN Qingcan, Spring 2020, Foshan China <br>
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * <p>
 * Run as script via Java 11: <br>
 * <code>
 * java -cp lib/commons-io-2.6.jar RenameWechatQqExport.java
 * </code>
 */
public final class RenameWechatQqExport {

	static final WildcardFileFilter WILDCARDS  = new WildcardFileFilter (
		List.of ("*.jpg", "*.jpeg", "*.jpe", "*.png", "*.gif", "*.bmp"),
		IOCase.SYSTEM);

	static final List<String> PREFIX_TO_REMOVE = List.of (
		// Source file in GBK
		// String constants in UTF-16BE ΢��ͼƬ TIMͼƬ QQͼƬ
		"\u5FAE\u4FE1\u56FE\u7247", "TIM\u56FE\u7247", "QQ\u56FE\u7247", "mmexport");

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
			c.where = Paths.get (stdinLine (".", "Where to search (default to current folder):  "));
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
			stdout.printf  ("Renamed %d files.%n", n);

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
					cound.addAndGet (checkNamePrefix (p));
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return cound.get ();
	}

	//------------------------------------------------------------------------
	/** @return 1 if the file renamed; 0 if not. */
	private static int checkNamePrefix (final Path p) {
		Objects.requireNonNull (p);
		for (String prefix1 : PREFIX_TO_REMOVE) {
			if (getFileNameNoPath (p).startsWith (prefix1)) {
				return removeNamePrefix (p, prefix1.length ());
			}
		}
		return 0;
	}

	//------------------------------------------------------------------------
	/** @return Only file name with extensive name, excluding the folder path part. */
	private static String getFileNameNoPath (final Path p) {
		return (p != null && p.getNameCount () >= 1) ?
			p.getName (p.getNameCount () - 1).toString () :
			"";
	}

	//------------------------------------------------------------------------
	/** @return Only file name without extensive name */
	private static String getFileNameNoExt (final String n) {
		int i = n.lastIndexOf ('.');
		return i >= 0 ? n.substring (0, i) : n;
	}

	//------------------------------------------------------------------------
	/** @return Extensive name of a file, including the dot character '.' */
	private static String getExtName (final Path p) {
		String n = getFileNameNoPath (p);
		int i = n.lastIndexOf ('.');
		return i >= 0 ? n.substring (i) : "";
	}

	//------------------------------------------------------------------------
	/** @return 1 if the file renamed; 0 if not. */
	private static int removeNamePrefix (final Path src, final int prefix) {
		if (src == null || prefix <= 0) {
			return 0;
		}
		String srcName = getFileNameNoPath (src);
		if (srcName.length () <= prefix) {
			return 0;
		}
		
		String destName = srcName.substring (prefix);
		if (destName.startsWith ("_") && destName.length () > 1) {
			destName = destName.substring (1);
		}
		destName = timestamp2daytime (getFileNameNoExt (destName)) + getExtName (src);
		Path dest = src.resolveSibling (destName);
		
		stdout.printf ("%s -> %s%n", srcName, destName);
		try {
			if (Files.exists (dest, LinkOption.NOFOLLOW_LINKS)) {
				if (! Config.stdinLine ("n", "Replace existing? (Y/N) ").equalsIgnoreCase ("y")) {
					return 0;
				}
			}
			Files.move (src, dest, StandardCopyOption.REPLACE_EXISTING);
			return 1;

		} catch (IOException e) {
			stderr.println (e.getMessage ());
			return 0;
		}
	}

	//------------------------------------------------------------------------
	/** @return Convert timestamp to daytime, or return the same value if it is not in millisecond timestamp format. */
	private static String timestamp2daytime (String timestamp) {
		try {
			long ms = Long.parseLong (timestamp);
			if (ms < 1000000000000L || ms > 9999999999999L) {
				return timestamp;
			}
			return new SimpleDateFormat ("yyyyMMddHHmmss").format (new Date (ms));
			
		} catch (NumberFormatException e) {
			return timestamp;
		}
	}

}
