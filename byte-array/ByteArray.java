import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import org.apache.commons.codec.binary.Base16;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;

//----------------------------------------------------------------------------
/**
 * Byte array from / to Base16 / Base32 / Base64 string, C escaped string, file, Java expression.
 *
 * <p>
 * Written by CHEN Qingcan, Spring 2020 ~ Winter 2021, Foshan China <br>
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
				"Input source:%n" +
				"\t1. Base16 (Hex)%n" +
				"\t3. Base32%n" +
				"\t6. Base64%n" +
				"\tC. C escaped string (e.g. \\x22Hi\\x22)%n" +
				"\tF. File path%n" +
				"\tS. String%n" +
				"\tU. URL encoded string (e.g. %%22Hi%%22)%n" +
				"Choose: ");
			config.fmtIn = Format.fromChar (readStdinLine ());
			stdout.printf ("Input string: ");
			config.strIn = readStdinLine ();
			if (config.fmtIn == Format.C_ESCAPED   ||
			    config.fmtIn == Format.STRING      ||
			    config.fmtIn == Format.URL_ENCODED) {
				stdout.print ("Input string encoding (character set): ");
				config.csIn = Charset.forName (readStdinLine ());
			}

			// output
			stdout.printf (
				"Output target:%n" +
				"\t1. Base16 (Hex)%n" +
				"\t3. Base32%n" +
				"\t6. Base64%n" +
				"\tF. File path%n" +
				"\tJ. Java expression (e.g. 0x48, 0x69)%n" +
				"\tS. String%n" +
				"\tU. URL encoded string (e.g. %%22Hi%%22)%n" +
				"Choose: ");
			config.fmtOut = Format.fromChar (readStdinLine ());
			if (config.fmtOut == Format.FILE) {
				stdout.print ("Output file name: ");
				config.pathOut = Paths.get (readStdinLine ());
			}
			if (config.fmtOut == Format.STRING ||
			    config.fmtOut == Format.URL_ENCODED) {
				stdout.print ("Output string encoding (character set): ");
				config.csOut = Charset.forName (readStdinLine ());
			}

			return config;
		}

	}

	//------------------------------------------------------------------------
	static enum Format {
		BASE64, BASE32, BASE16, C_ESCAPED, FILE, JAVA, STRING, URL_ENCODED;

		static Format fromChar (final String c) throws IOException {
			switch (c.toUpperCase ()) {
			case "1": return Format.BASE16;
			case "3": return Format.BASE32;
			case "6": return Format.BASE64;
			case "C": return Format.C_ESCAPED;
			case "F": return Format.FILE;
			case "J": return Format.JAVA;
			case "U": return Format.URL_ENCODED;
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
			case URL_ENCODED:
				input = URLDecoder.decode (config.strIn, config.csIn.name ()).getBytes (config.csIn);
				break;
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
			case URL_ENCODED:
				output = URLEncoder.encode (new String (input, config.csOut), config.csOut.name ());
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
			if (c1 != '\\') {
				c.append (c1);
			} else if (i < input.length () - 1) {
				c1 = input.charAt (++i);
				switch (c1) {
				case 'a':  c.append ('\u0007'); break; // BEL
				case 'b':  c.append ('\b');     break; // BS
				case 'f':  c.append ('\f');     break; // FF
				case 'n':  c.append ('\n');     break; // LF
				case 'r':  c.append ('\r');     break; // CR
				case 't':  c.append ('\t');     break; // HT
				case 'v':  c.append ('\u000B'); break; // VT
				case '\'': c.append ('\'');     break; // '
				case '\"': c.append ('\"');     break; // "
				case '\\': c.append ('\\');     break; // \
				}
				if (c1 == 'x' || c1 == 'X') {          // Hex
					var hex = new StringBuilder ();
					while (++i < input.length ()) {
						c1 = input.charAt (i);
						if (isHexChar (c1)) hex.append (c1); else break;
					}
					i--;
					if (! hex.isEmpty()) {
						isChar = false;
						b1 = decodeBase16 (hex) [0];
					}
				}
				if (isOctChar (c1)) {                  // Oct
					var oct = new StringBuilder ();
					for (; i < input.length () ; i++) {
						c1 = input.charAt (i);
						if (isOctChar (c1)) oct.append (c1); else break;
					}
					i--;
					if (! oct.isEmpty()) {
						isChar = false;
						b1 = (byte) Integer.parseInt (oct.toString (), 8);
					}
				}
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
	private static boolean isOctChar (final char c1) {
		return (c1 >= '0' && c1 <= '7');
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
