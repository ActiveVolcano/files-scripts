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
 * Byte array from / to Base16 / Base32 / Base64 string,
 * C / Java escaped string,
 * Quoted-printable / URL encoded string,
 * file, Java expression.
 *
 * <p>
 * Written by CHEN Qingcan, Spring 2020 ~ Winter 2021, Foshan China <br>
 * Open source under WTFPL (Do What The Fuck You Want To Public License) http://www.wtfpl.net
 *
 * <p>
 * Run as script via Java 11: <br>
 * <code>
 * java -cp lib/commons-codec-1.15.jar ByteArray.java
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
				"\tE. Java escaped string (e.g. \\u0022Hi\\u0022)%n" +
				"\tF. File path%n" +
				"\tQ. Quoted-printable (e.g. =22Hi=22)%n" +
				"\tS. String%n" +
				"\tU. URL encoded string (e.g. %%22Hi%%22)%n" +
				"Choose: ");
			config.fmtIn = Format.fromChar (readStdinLine ());
			stdout.printf ("Input string: ");
			config.strIn = readStdinLine ();
			if (config.fmtIn == Format.C_ESCAPED        ||
			    config.fmtIn == Format.ESCAPED          ||
			    config.fmtIn == Format.QUOTED_PRINTABLE ||
			    config.fmtIn == Format.STRING           ||
			    config.fmtIn == Format.URL_ENCODED) {
				stdout.print ("Input character set: ");
				config.csIn = Charset.forName (readStdinLine ());
			}

			// output
			stdout.printf (
				"Output target:%n" +
				"\t1. Base16 (Hex)%n" +
				"\t3. Base32%n" +
				"\t6. Base64%n" +
				"\tF. File path%n" +
				"\tH. Hash (CRC32, MD5, SHA-1, SHA-256, SHA3-256)%n" +
				"\tJ. Java expression (e.g. 0x48, 0x69)%n" +
				"\tQ. Quoted-printable (e.g. =22Hi=22)%n" +
				"\tS. String%n" +
				"\tU. URL encoded string (e.g. %%22Hi%%22)%n" +
				"Choose: ");
			config.fmtOut = Format.fromChar (readStdinLine ());
			if (config.fmtOut == Format.FILE) {
				stdout.print ("Output file name: ");
				config.pathOut = Paths.get (readStdinLine ());
			}
			if (config.fmtOut == Format.QUOTED_PRINTABLE ||
			    config.fmtOut == Format.STRING           ||
			    config.fmtOut == Format.URL_ENCODED) {
				stdout.print ("Output character set: ");
				config.csOut = Charset.forName (readStdinLine ());
			}

			return config;
		}

	}

	//------------------------------------------------------------------------
	static enum Format {
		BASE16, BASE32, BASE64, C_ESCAPED, ESCAPED, FILE, HASH, JAVA, QUOTED_PRINTABLE, STRING, URL_ENCODED;

		static Format fromChar (final String c) throws IOException {
			switch (c.toUpperCase ()) {
			case "1": return Format.BASE16;
			case "3": return Format.BASE32;
			case "6": return Format.BASE64;
			case "C": return Format.C_ESCAPED;
			case "E": return Format.ESCAPED;
			case "F": return Format.FILE;
			case "H": return Format.HASH;
			case "J": return Format.JAVA;
			case "Q": return Format.QUOTED_PRINTABLE;
			case "S": return Format.STRING;
			case "U": return Format.URL_ENCODED;
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
			case ESCAPED:
				input = fromJavaEscaped (config.strIn, config.csIn);
				break;
			case FILE:
				input = Files.readAllBytes (Paths.get (config.strIn));
				break;
			case HASH:
			case JAVA:
				throw new IOException ("Wrong choice.");
			case QUOTED_PRINTABLE:
				input = URLDecoder.decode (config.strIn.replace ('=', '%'), config.csIn.name ()).getBytes (config.csIn);
				break;
			case STRING:
				input = config.strIn.getBytes (config.csIn);
				break;
			case URL_ENCODED:
				input = URLDecoder.decode (config.strIn, config.csIn.name ()).getBytes (config.csIn);
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
			case C_ESCAPED:
			case ESCAPED:
				throw new IOException ("Wrong choice.");
			case FILE:
				Files.write (config.pathOut, input);
				output = config.pathOut.toString ();
				break;
			case HASH:
				output = toHash (input);
				break;
			case JAVA:
				output = toJavaExpression (input);
				break;
			case QUOTED_PRINTABLE:
				output = URLEncoder.encode (new String (input, config.csOut), config.csOut.name ()).replace ('%', '=');
				break;
			case STRING:
				output = new String (input, config.csOut);
				break;
			case URL_ENCODED:
				output = URLEncoder.encode (new String (input, config.csOut), config.csOut.name ());
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
			var num = new StringBuilder (3);

			c.rewind ();
			if (c1 != '\\') {
				c.append (c1);
			} else if (i < input.length () - 1) {
				c1 = input.charAt (++i);
				switch (c1) {
				case 'a':  c.append ('\u0007'); break;  // BEL
				case 'b':  c.append ('\b');     break;  // BS
				case 'f':  c.append ('\f');     break;  // FF
				case 'n':  c.append ('\n');     break;  // LF
				case 'r':  c.append ('\r');     break;  // CR
				case 't':  c.append ('\t');     break;  // HT
				case 'v':  c.append ('\u000B'); break;  // VT
				case '\'': c.append ('\'');     break;  // '
				case '\"': c.append ('\"');     break;  // "
				case '\\': c.append ('\\');     break;  // \
				}
				if ((c1 == 'x' || c1 == 'X') &&         // Hex
				    (i < input.length () - 2) &&
				    isHexChar (input.charAt (i + 1)) && isHexChar (input.charAt (i + 2))
				) {
					isChar = false;
					num.setLength (0);
					num.append (input.charAt (++i));
					num.append (input.charAt (++i));
					b1 = decodeBase16 (num) [0];
				}
				if (isOctChar (c1) &&                   // Oct
				    (i < input.length () - 3) &&
				    isHexChar (input.charAt (i + 1)) &&
				    isHexChar (input.charAt (i + 2)) &&
				    isHexChar (input.charAt (i + 3))
				) {
					isChar = false;
					num.setLength (0);
					num.append (input.charAt (++i));
					num.append (input.charAt (++i));
					num.append (input.charAt (++i));
					b1 = (byte) Integer.parseInt (num.toString (), 8);
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
	/**
	 * Not to use Apache Commons because not support octal, \\uu
	 */
	private static byte[] fromJavaEscaped (final String input, Charset charset) {
		var s = new StringBuilder ();
		for (int i = 0 ; i < input.length () ; i++) {
			char c1 = input.charAt (i);

			if (c1 != '\\') {
				s.append (c1);
			} else if (i < input.length () - 1) {
				c1 = input.charAt (++i);
				switch (c1) {
				case 'b':  s.append ('\b');     break; // BS
				case 'f':  s.append ('\f');     break; // FF
				case 'n':  s.append ('\n');     break; // LF
				case 'r':  s.append ('\r');     break; // CR
				case 't':  s.append ('\t');     break; // HT
				case '\'': s.append ('\'');     break; // '
				case '\"': s.append ('\"');     break; // "
				case '\\': s.append ('\\');     break; // \
				}
				if (c1 == 'u' || c1 == 'U') {          // Hex
					if (input.charAt (i + 1) == 'u' || input.charAt (i + 1) == 'U') i++;
					var hex = new StringBuilder ();
					while (++i < input.length ()) {
						c1 = input.charAt (i);
						if (isHexChar (c1)) hex.append (c1); else break;
					}
					i--;
					if (! hex.isEmpty()) {
						int codepoint = Integer.parseInt (hex.toString (), 16);
						s.append (Character.toChars (codepoint) [0]);
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
						int codepoint = Integer.parseInt (oct.toString (), 8);
						s.append (Character.toChars (codepoint) [0]);
					}
				}
			}
		}

		return s.toString ().getBytes (charset);
	}

	//------------------------------------------------------------------------
	private static String toHash (final byte[] input) {
		try {
			var crc32enc = new java.util.zip.CRC32 ();
			crc32enc.update (input);
			long crc32long = crc32enc.getValue ();
			String crc32str = String.format ("%08X", crc32long);

			var md5enc = java.security.MessageDigest.getInstance ("MD5");
			md5enc.update (input);
			byte[] md5b = md5enc.digest();
			String md5str = new Base16 ().encodeToString (md5b);

			var sha1enc = java.security.MessageDigest.getInstance ("SHA-1");
			sha1enc.update (input);
			byte[] sha1b = sha1enc.digest();
			String sha1str = new Base16 ().encodeToString (sha1b);

			var sha256enc = java.security.MessageDigest.getInstance ("SHA-256");
			sha256enc.update (input);
			byte[] sha256b = sha256enc.digest();
			String sha256str = new Base16 ().encodeToString (sha256b);

			var sha3enc = java.security.MessageDigest.getInstance ("SHA3-256");
			sha3enc.update (input);
			byte[] sha3b = sha3enc.digest();
			String sha3str = new Base16 ().encodeToString (sha3b);

			return String.format (
				"CRC32    = %s%n" +
				"MD5      = %s%n" +
				"SHA-1    = %s%n" +
				"SHA-256  = %s%n" +
				"SHA3-256 = %s",
				crc32str, md5str, sha1str, sha256str, sha3str);
		} catch (java.security.NoSuchAlgorithmException e) {
			return e.getMessage ();
		}
	}

	//------------------------------------------------------------------------
	private static String toJavaExpression (final byte[] input) {
		var s16 = new Base16 ().encodeToString (input);
		var sb = new StringBuilder ();
		for (int i = 0 ; i < s16.length () ; ) {
			sb.append ("0x")
			  .append (s16.charAt (i++))
			  .append (s16.charAt (i++))
			  .append (", ");
		}
		if (sb.length () >= 2) {
			sb.setLength (sb.length () - 2);
		}
		return sb.toString ();
	}

}
