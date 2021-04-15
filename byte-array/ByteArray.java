import java.io.*;
import java.nio.file.*;
import org.apache.commons.codec.binary.Base16;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;

//----------------------------------------------------------------------------
/**
 * Byte array to file, to Base64 string, to Base32 string, to Base16 string, to Java expression.
 *
 * <p>
 * Written by CHEN Qingcan, Spring 2020, Foshan China <br>
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * <p>
 * Run as script via Java 11: <br>
 * <code>
 * java ByteArray.java
 * </code>
 */
public final class ByteArray {

	static final BufferedReader stdin = new BufferedReader (new InputStreamReader (System.in));
	static final PrintStream stdout = System.out;
	static final PrintStream stderr = System.err;

	static Config config = null;
	static class  Config {
		String    strIn;
		Format    fmtIn;
		Format    fmtOut;
		Path      pathOut;

		//--------------------------------------------------------------------
		/** Get configuration from standard input. */
		static Config getInstance () throws IOException {
			var config = new Config ();

			// input
			stdout.printf (
				"Input source%n" +
				"\t1. Base16%n" +
				"\t3. Base32%n" +
				"\t6. Base64%n" +
				"\tF. File path%n" +
				"Choose: ");
			config.fmtIn = Format.fromChar (stdin.readLine ().trim ());
			stdout.printf ("Input string: ");
			config.strIn = stdin.readLine ().trim ();

			// output
			stdout.printf (
				"Output target%n" +
				"\t1. Base16%n" +
				"\t3. Base32%n" +
				"\t6. Base64%n" +
				"\tF. File path%n" +
				"\tJ. Java expression%n" +
				"Choose: ");
			config.fmtOut = Format.fromChar (stdin.readLine ().trim ());
			if (config.fmtOut == Format.FILE) {
				stdout.print ("Output file name: ");
				config.pathOut = Paths.get (stdin.readLine ().trim ());
			}

			return config;
		}

	}

	//------------------------------------------------------------------------
	static enum Format {
		BASE64, BASE32, BASE16, FILE, JAVA;

		static Format fromChar (final String c) throws IOException {
			switch (c.toUpperCase ()) {
			case "1": return Format.BASE16;
			case "3": return Format.BASE32;
			case "6": return Format.BASE64;
			case "F": return Format.FILE;
			case "J": return Format.JAVA;
			default : throw new IOException ("Wrong choice.");
			}
		}
	}

	//------------------------------------------------------------------------
	/** Program entry */
	public static void main (final String... args) {
		try {
			config = Config.getInstance ();
			byte[] input = new byte[0];
			switch (config.fmtIn) {
			case BASE64:
				input = new Base64 ().decode (config.strIn);
				break;
			case BASE32:
				input = new Base32 ().decode (config.strIn.toUpperCase ());
				break;
			case BASE16:
				input = new Base16 ().decode (config.strIn.toUpperCase ());
				break;
			case FILE:
				input = Files.readAllBytes (Paths.get (config.strIn));
				break;
			case JAVA:
				throw new IOException ("Wrong choice.");
			}

			String output = "";
			switch (config.fmtOut) {
			case BASE64:
				output = new Base64 ().encodeToString (input);
				break;
			case BASE32:
				output = new Base32 ().encodeToString (input);
				break;
			case BASE16:
				output = new Base16 ().encodeToString (input);
				break;
			case FILE:
				Files.write (config.pathOut, input);
				output = config.pathOut.toString ();
				break;
			case JAVA:
				var s16 = new Base16 ().encodeToString (input);
				var sb = new StringBuilder ();
				for (int i = 0 ; i < s16.length () ; i += 2) {
					sb.append ("0x").append (s16.charAt (i)).append (s16.charAt (i + 1)).append (", ");
				}
				if (sb.length () >= 2) {
					sb.setLength (sb.length () - 2);
				}
				output = sb.toString ();
			}

			stdout.println ("Result:");
			stdout.println (output);

		} catch (IOException e) {
			stderr.println (e.getMessage ());
		}
	}

}
