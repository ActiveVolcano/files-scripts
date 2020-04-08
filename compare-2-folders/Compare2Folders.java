import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

//----------------------------------------------------------------------------
/**
 * Compare 2 folders.
 *
 * <p>
 * Written by CHEN Qingcan, Spring 2020, Foshan China <br>
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * <p>
 * Run as script via Java 11: <br>
 * <code>
 * java -cp lib/commons-io-2.6.jar Compare2Folders.java
 * </code>
 */
public final class Compare2Folders {

	static final int KB = 1024;
	static final int PARTIAL_COMPARE_SIZE = 4 * KB;
	
	static final BufferedReader stdin = new BufferedReader (new InputStreamReader (System.in));
	static final PrintStream stdout = System.out;
	static final PrintStream stderr = System.err;
	static final String HR = "--------";
	
	//------------------------------------------------------------------------
	static enum CompareLevel {
		SIZE,
		PARTIAL,
		FULL;
		
		static CompareLevel valueOf (final char c) {
			switch (c) {
			default:
			case 'S': return SIZE;
			case 'P': return PARTIAL;
			case 'F': return FULL;
			}
		}
	}

	//------------------------------------------------------------------------
	static Config config = null;
	static class  Config {
		Path            whereA;
		Path            whereB;
		CompareLevel    levelCompare    = CompareLevel.SIZE;
		List<String>    exclude         = List.of ();

		//--------------------------------------------------------------------
		@Override public String toString () {
			var s = String.format ("Comparing (A) %s and (B) %s with %s compare excluding %s...",
				config.whereA, config.whereB, config.levelCompare, config.exclude);
			if (config.levelCompare == CompareLevel.PARTIAL) {
				s += String.format (" (partial compare size = %,d)", PARTIAL_COMPARE_SIZE);
			}
			return s;
		}

		//--------------------------------------------------------------------
		static Config getInstance (final String... args) throws IOException {
			if (args.length == 0) {
				return fromStdIn ();
			} else if (args.length >= 2) {
				return fromArgs (args);
			} else {
				return die ();
			}
		}

		//--------------------------------------------------------------------
		private static Config die () {
			stderr.println ("Command line arguments:");
			stderr.println ("[path A] [path B] [-partial | -full] [-exclude wildcards (comma seperated)]");
			System.exit (1);
			return null;
		}

		//--------------------------------------------------------------------
		/** Get configuration from command line arguments. */
		private static Config fromArgs (final String... args) {
			var config = new Config ();
			int where = 0;
			
			for (String arg1 : args) {
				if (isContains (arg1, "-partial", "--partial")) {
					config.levelCompare = CompareLevel.PARTIAL;
				} else if (isContains (arg1, "-full", "--full")) {
					config.levelCompare = CompareLevel.FULL;
				} else if (isContains (arg1, "-exclude", "--exclude")) {
					where = -1;
				} else if (where == -1) {
					config.exclude = List.of (arg1.split (","));
				} else if (where == 0) {
					where = 1;
					config.whereA = Path.of (arg1);
				} else {
					config.whereB = Path.of (arg1);
				}
			}
			
			return config;
		}

		//--------------------------------------------------------------------
		private static boolean isContains (final String content, final String... what) {
			for (String what1 : what) {
				if (content.equalsIgnoreCase (what1)) {
					return true;
				}
			}
			return false;
		}

		//--------------------------------------------------------------------
		/** Get configuration from standard input. */
		private static Config fromStdIn () throws IOException {
			var config = new Config ();
			String line;

			line = stdinLine (".", "Path A: ");
			config.whereA = Paths.get (line);
			line = stdinLine (".", "Path B: ");
			config.whereB = Paths.get (line);
			line = stdinLine ("S", "Size only / Partial / Full file hash (S/P/F, default to S): ");
			config.levelCompare = CompareLevel.valueOf (line.charAt (0));
			line = stdinLine ("",  "Exclude file name wildcards (comma seperated, default to empty): ");
			config.exclude = List.of (line.split (","));

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
			stdout.println (config.toString ());
			stdout.println (HR);

			stdout.println ("Path\tA\tB");
			long count = walkFolderTree (config.whereA, config.whereB, TurnAB.AB)
			           + walkFolderTree (config.whereB, config.whereA, TurnAB.BA);

			stdout.println (HR);
			stdout.printf ("Different folders & files: %d%n", count);
			
		} catch (IOException e) {
			stderr.println (e.getMessage ());
		}
	}

	//------------------------------------------------------------------------
	private static enum TurnAB { AB, BA };
	
	private static long walkFolderTree (final Path base, final Path target, final TurnAB turnAB)
		throws IOException {
		final var wildcards = new WildcardFileFilter (config.exclude, IOCase.SYSTEM);
		final var count     = new AtomicLong (0);
		
		Files.walkFileTree (base, new SimpleFileVisitor<Path>() {
			
			//--------------------------------------------------------------------
			@Override
			public FileVisitResult preVisitDirectory (final Path base2, final BasicFileAttributes attrs) {
				if (wildcards.accept (base2.toFile ())) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				
				Path relative = base.relativize (base2), target2 = target.resolve (relative);
				if (! Files.exists (target2, LinkOption.NOFOLLOW_LINKS)) {
					count.incrementAndGet ();
					String sayNoTarget = turnAB == TurnAB.AB ? "\t-\tX" : "\tX\t-";
					stdout.println (relative + File.separator + sayNoTarget);
					return FileVisitResult.SKIP_SUBTREE;
				}
				
				return FileVisitResult.CONTINUE;
			}
			
			//--------------------------------------------------------------------
			@Override
			public FileVisitResult visitFile (final Path base2, final BasicFileAttributes attrs) {
				Path relative = base.relativize (base2);
				String relatives = relative.toString ();
				Path target2 = target.resolve (relative);
				
				try {
					if (Files.exists (target2, LinkOption.NOFOLLOW_LINKS)) {
						if (turnAB == TurnAB.AB) {
							int c = compareFileSize (base2, target2, relatives);
							if (c > 0) {
								count.addAndGet (c);
							} else if (config.levelCompare != CompareLevel.SIZE) {
								count.addAndGet (compareFileContent (base2, target2,
									config.levelCompare == CompareLevel.PARTIAL ? PARTIAL_COMPARE_SIZE : 0, relatives));
							}
						}
							
					} else {
						count.incrementAndGet ();
						String sayNoTarget = turnAB == TurnAB.AB ? "\t-\tX" : "\tX\t-";
						stdout.println (relative + sayNoTarget);
					}
					
				} catch (IOException e) {
					count.incrementAndGet ();
					stdout.printf ("%s\t%s%n", relatives, e.getMessage ());
				}
				
				return FileVisitResult.CONTINUE;
			}
			
			//--------------------------------------------------------------------
			@Override
			public FileVisitResult visitFileFailed (final Path base2, final IOException exc) throws IOException {
				Path relative = base.relativize (base2), target2 = target.resolve (relative);
				var exists = Files.exists (target2, LinkOption.NOFOLLOW_LINKS) ? "-" : "X";
				var prompt = "\t%s\t%s%n";
				if (turnAB == TurnAB.AB) {
					stdout.printf (prompt, exc.getMessage (), exists);
				} else {
					stdout.printf (prompt, exists, exc.getMessage ());
				}
				return FileVisitResult.CONTINUE;
			}
			
		});
		return count.get ();
	}

	//------------------------------------------------------------------------
	/** @return 0: equal, 1: different */
	private static int compareFileSize (final Path p1, final Path p2, final String relatives)
		throws IOException {
		long s1 = Files.size (p1), s2 = Files.size (p2);
		if (s1 == s2) {
			return 0;
		} else {
			stdout.printf ("%s\tsize = %d\tsize = %d%n", relatives, s1, s2);
			return 1;
		}
	}

	//------------------------------------------------------------------------
	/**
	 * @param limit 0 or negative stands for no limit 
	 * @return 0: equal, 1: different
	 */
	private static int compareFileContent (final Path p1, final Path p2, final int limit, final String relatives)
		throws IOException {
		boolean folder1 = Files.isDirectory (p1), folder2 = Files.isDirectory (p2);
		if (folder1 && folder2) {
			return 0;
		} else if (folder1 != folder2) {
			stdout.printf ("%s\t%s\t%s%n", relatives, folder1 ? "folder" : "-", folder2 ? "folder" : "-");
			return 1;
		}
		
		try (var in1 = Files.newInputStream (p1); var in2 = Files.newInputStream (p2)) {
			
			for (long i = 0 ; limit <= 0 || i < limit; i++) {
				int b1 = in1.read (), b2 = in2.read ();
				if (b1 < 0 && b2 < 0) {
					return 0;
				}
				if (b1 != b2 || b1 < 0 || b2 < 0) {
					stdout.printf ("%s\tdiff @ %d\tdiff @ %d%n", relatives, i, i);
					return 1;
				}
			}
		}
		return 0;
	}

}
