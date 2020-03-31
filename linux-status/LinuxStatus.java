import java.io.*;
import java.nio.file.*;
import java.util.*;

//----------------------------------------------------------------------------
/**
 * Collect Linux running status, including processes, networks, mountpoints, environment variables, into a folder named linux.status.
 *
 * <p>
 * Written by CHEN Qingcan, Spring 2020, Foshan China <br>
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * <p>
 * Run as script via Java 11: <br>
 * <code>
 * java LinuxStatus.java
 * </code>
 */
public final class LinuxStatus {

	static final PrintStream stdout = System.out;
	static final PrintStream stderr = System.err;
	
	private static final File DIR = new File ("linux.status");

	//------------------------------------------------------------------------
	/** Program entry */
	public static void main (final String... args) {
		try {
			DIR.mkdir ();
			record ("date--rfc-3339.txt",               "date", "--rfc-3339=seconds");
			record ("df-h.txt",                         "df", "-h");
			record ("fdisk-l.txt",                      "fdisk", "-l");
			record ("mount.txt",                        "mount");
			record ("free-h.txt",                       "free", "-h");
			record ("meminfo.txt",                      "cat", "/proc/meminfo");
			record ("cpuinfo.txt",                      "cat", "/proc/cpuinfo");
			record ("ip_addr.txt",                      "ip", "addr");
			record ("netstat-anp.txt",                  "netstat", "-anp");
			record ("iptables-L-n.txt",                 "iptables", "-L", "-n");
			record ("firewall-cmd--list-port.txt",      "firewall-cmd", "--list-port");
			record ("firewall-cmd--list-service.txt",   "firewall-cmd", "--list-service");
			record ("route.txt",                        "route");
			record ("ps-auxww.txt",                     "ps", "-auxww");
			record ("docker_ps.txt",                    "docker", "ps");
			env    ("set.txt");
			plusProcessPath ("ps-auxww.txt");
			
		} catch (IOException e) {
			stderr.printf ("[%s] %s%n", e.getClass ().getName (), e.getMessage ());
		}
	}

	//------------------------------------------------------------------------
	/** Record command output to a file. */
	private static void record (final String outFileName, final String... command) throws IOException {
		stdout.printf ("Recoding: %s\t", String.join (" ", command));
		File out = new File (DIR, outFileName);
		try {
			new ProcessBuilder(command)
				.directory (DIR)
				.redirectErrorStream (true)
				.redirectOutput (out)
				.start()
				.waitFor ();
		} catch (InterruptedException e) { /* ignore */ }
		stdout.println ("(Done)");
	}

	//------------------------------------------------------------------------
	/** Record environment variables to a file. */
	private static void env (final String outFileName) throws IOException {
		stdout.print ("Recoding: set\t");
		var env = System.getenv ();
		var out = new StringBuilder ();
		var path = Paths.get (DIR.toString (), outFileName);
		env.forEach ((key, value) -> out.append (String.format ("%s=%s%n", key, value)));
		Files.writeString (path, out.toString ());
		stdout.println ("(Done)");
	}

	//------------------------------------------------------------------------
	/** Plus process information with path. */
	private static void plusProcessPath (final String psFileName) throws IOException {
		var path  = Paths.get (DIR.toString (), psFileName);
		var lines = Files.readAllLines (path);
		if (lines.size () <= 0) {
			return;
		}
		
		lines.set (0, lines.get (0) + "        cwd        exe        cmdline");
		for (int i = 1 ; i < lines.size () ; i++) {
			var s = new Scanner (lines.get (i));
			s.next (); // USER
			var pid  = s.next ();
			var proc = Paths.get ("/proc/", pid);
			String cwd = "", exe = "", cmdline = "";
			try { cwd  = proc.resolve ("cwd").toRealPath ().toString (); }
			catch (NoSuchFileException e) { /* ignore */ }
			try { exe  = proc.resolve ("exe").toRealPath ().toString (); }
			catch (NoSuchFileException e) { /* ignore */ }
			try { cmdline = Files.readString (proc.resolve ("cmdline")); }
			catch (NoSuchFileException e) { /* ignore */ }
			lines.set (i, String.format ("%s    %s    %s    %s", lines.get (i), cwd, exe, cmdline));
		}
		Files.write (path, lines);
	}

}
