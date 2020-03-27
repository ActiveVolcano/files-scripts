import java.io.*;

//----------------------------------------------------------------------------
/**
 * Linux running processes status.
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
			record ("df-h.txt", "df", "-h");
			record ("fdisk-l.txt", "fdisk", "-l");
			record ("mount.txt", "mount");
			record ("free-h.txt", "free", "-h");
			record ("meminfo.txt", "cat", "/proc/meminfo");
			record ("cpuinfo.txt", "cat", "/proc/cpuinfo");
			record ("netstat-anp.txt", "netstat", "-anp");
			record ("iptables-L-n.txt", "iptables", "-L", "-n");
			record ("firewall-cmd--list-port.txt", "firewall-cmd", "--list-port");
			record ("firewall-cmd--list-service.txt", "firewall-cmd", "--list-service");
			record ("route.txt", "route");
			// TODO record ("set.txt", "set");
			record ("ps-auxww.txt", "ps", "-auxww");
			record ("docker_ps.txt", "docker", "ps");
			
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

}
