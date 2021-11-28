import java.io.*;
import java.nio.*;
import java.nio.charset.Charset;
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
		Charset   csIn;
		Format    fmtOut;
		Path      pathOut;
		Charset   csOut;

		//--------------------------------------------------------------------
		/** Get configuration from standard input. */
		static Config getInstance () throws IOException {
			var config = new Config ();

			// input
			stdout.printf (
				"Input source%n" +
				"\t1. Base16 (Hex)%n" +
				"\t3. Base32%n" +
				"\t6. Base64%n" +
				"\tC. C escaped string (e.g. \\x22Hi\\x22)%n" +
				"\tF. File path%n" +
				"\tS. String%n" +
				"Choose: ");
			config.fmtIn = Format.fromChar (readStdinLine ());
			stdout.printf ("Input string: ");
			config.strIn = readStdinLine ();
			if (config.fmtIn == Format.C_ESCAPED ||
			    config.fmtIn == Format.STRING) {
				stdout.print ("Input string encoding (character set): ");
				config.csIn = Charset.forName (readStdinLine ());
			}

			// output
			stdout.printf (
				"Output target%n" +
				"\t1. Base16 (Hex)%n" +
				"\t3. Base32%n" +
				"\t6. Base64%n" +
				"\tF. File path%n" +
				"\tJ. Java expression (e.g. 0x48, 0x69)%n" +
				"\tS. String%n" +
				"Choose: ");
			config.fmtOut = Format.fromChar (readStdinLine ());
			if (config.fmtOut == Format.FILE) {
				stdout.print ("Output file name: ");
				config.pathOut = Paths.get (readStdinLine ());
			}
			if (config.fmtOut == Format.STRING) {
				stdout.print ("Output string encoding (character set): ");
				config.csOut = Charset.forName (readStdinLine ());
			}

			return config;
		}

	}

	//------------------------------------------------------------------------
	static enum Format {
		BASE64, BASE32, BASE16, C_ESCAPED, FILE, JAVA, STRING;

		static Format fromChar (final String c) throws IOException {
			switch (c.toUpperCase ()) {
			case "1": return Format.BASE16;
			case "3": return Format.BASE32;
			case "6": return Format.BASE64;
			case "C": return Format.C_ESCAPED;
			case "F": return Format.FILE;
			case "J": return Format.JAVA;
			case "S": return Format.STRING;
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
			case BASE16:
				input = decodeBase16 (config.strIn);
				break;
			case BASE32:
				input = new Base32 ().decode (config.strIn);
				break;
			case BASE64:
				input = new Base64 ().decode (config.strIn);
				break;
			case C_ESCAPED:
				input = fromCEscaped (config.strIn, config.csIn);
				break;
			case FILE:
				input = Files.readAllBytes (Paths.get (config.strIn));
				break;
			case JAVA:
				throw new IOException ("Wrong choice.");
			case STRING:
				input = config.strIn.getBytes (config.csIn);
				break;
			}

			String output = "";
			switch (config.fmtOut) {
			case BASE16:
				output = new Base16 ().encodeToString (input);
				break;
			case BASE32:
				output = new Base32 ().encodeToString (input);
				break;
			case BASE64:
				output = new Base64 ().encodeToString (input);
				break;
			case FILE:
				Files.write (config.pathOut, input);
				output = config.pathOut.toString ();
				break;
			case JAVA:
				output = toJavaExpression (input);
				break;
			case STRING:
				output = new String (input, config.csOut);
				break;
			}

			stdout.printf ("Result (%d bytes):%n%s%n", input.length, output);

		} catch (IOException e) {
			stderr.println (e.getMessage ());
		}
	}

	//------------------------------------------------------------------------
	private static String readStdinLine () throws IOException {
		return stdin.readLine ().trim ();
	}

	//------------------------------------------------------------------------
	private static Base16 base16 = new Base16 ();
	private static byte[] decodeBase16 (final String input) {
		// toUpperCase to avoid IllegalArgumentException: Invalid octet
		return base16.decode (input.toUpperCase ());
	}
	private static byte[] decodeBase16 (final StringBuilder input) {
		return decodeBase16 (input.toString ());
	}

	//------------------------------------------------------------------------
	private static byte[] fromCEscaped (final String input, Charset charset) {
		CharBuffer c = CharBuffer.allocate (1);
		ByteBuffer b = ByteBuffer.allocate (input.length () * (int) charset.newEncoder ().maxBytesPerChar ());
		for (int i = 0 ; i < input.length () ; i++) {
			char c1 = input.charAt (i);
			boolean isChar = true;
			byte b1 = 0;

			c.rewind ();
			if (c1 == '\\' && i < input.length () - 1) {
				if (input.charAt (i + 1) == 'a') {
					// BEL
					c.append ('\u0007');
					i++;
				} else if (input.charAt (i + 1) == 'b') {
					// BS
					c.append ('\b');
					i++;
				} else if (input.charAt (i + 1) == 'f') {
					// FF
					c.append ('\f');
					i++;
				} else if (input.charAt (i + 1) == 'n') {
					// LF
					c.append ('\n');
					i++;
				} else if (input.charAt (i + 1) == 'r') {
					// CR
					c.append ('\r');
					i++;
				} else if (input.charAt (i + 1) == 't') {
					// HT
					c.append ('\t');
					i++;
				} else if (input.charAt (i + 1) == 'v') {
					// VT
					c.append ('\u000B');
					i++;
				} else if (input.charAt (i + 1) == '\'') {
					// '
					c.append ('\'');
					i++;
				} else if (input.charAt (i + 1) == '\"') {
					// "
					c.append ('\"');
					i++;
				} else if (input.charAt (i + 1) == '\\') {
					// \
					c.append ('\\');
					i++;
				} else if (input.charAt (i + 1) == 'x' || input.charAt (i + 1) == 'X') {
					// Hex
					var hex = new StringBuilder ();
					i += 2;
					if (i < input.length ()) {
						c1 = input.charAt (i);
						if (isHexChar (c1)) hex.append (c1);
						i++;
						if (i < input.length ()) {
							c1 = input.charAt (i);
							if (isHexChar (c1)) hex.append (c1);
							i++;
						}
					}
					i--;
					if (! hex.isEmpty()) {
						isChar = false;
						b1 = decodeBase16 (hex) [0];
					}
				}
			} else {
				c.append (c1);
			}
			
			if (isChar)
				b.put (charset.encode (c.rewind ()));
			else
				b.put (b1);
		}

		byte[] b0 = new byte [b.position ()];
		b.rewind ().get (b0);
		return b0;
	}

	//------------------------------------------------------------------------
	private static boolean isHexChar (final char c1) {
		return (c1 >= '0' && c1 <= '9') ||
		       (c1 >= 'A' && c1 <= 'F') ||
		       (c1 >= 'a' && c1 <= 'f');
	}

	//------------------------------------------------------------------------
	private static String toJavaExpression (final byte[] input) {
		var s16 = new Base16 ().encodeToString (input);
		var sb = new StringBuilder ();
		for (int i = 0 ; i < s16.length () ; i += 2) {
			sb.append ("0x").append (s16.charAt (i)).append (s16.charAt (i + 1)).append (", ");
		}
		if (sb.length () >= 2) {
			sb.setLength (sb.length () - 2);
		}
		return sb.toString ();
	}

}
