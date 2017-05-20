package org.tirix.emetamath.views;

public class MathMLPrinter {
	
	public static String test() {
		StringBuffer sb= new StringBuffer();
		printHeader(sb);
		printTest(sb);
		printFooter(sb);
		return sb.toString();
	}

	public static void printHeader(StringBuffer sb) {
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
//		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD W3 HTML//EN\">\n"); 
//		sb.append("<html xmlns:m=\"http://www.w3.org/1998/Math/MathML\" lang=\"en\">\n");
		sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN\"\n"); 
		sb.append("\t\t\"http://www.w3.org/Math/DTD/mathml2/xhtml-math11-f.dtd\">\n");
//		sb.append("\t[ <!ENTITY mathml \"http://www.w3.org/1998/Math/MathML\"> ]>\n");
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n");
		sb.append("<body>\n");
	}

	public static void printTest(StringBuffer sb) {
		sb.append("And here is a test:\n");
		sb.append("<math mode=\"display\" xmlns=\"http://www.w3.org/1998/Math/MathML\">\n");
		sb.append("<mrow>\n");
		sb.append("  <mi>x</mi>\n");
		sb.append("  <mo>=</mo>\n");
		sb.append("  <mfrac>\n");
		sb.append("    <mrow>\n");
		sb.append("      <mrow>\n");
		sb.append("        <mo>-</mo>\n");
		sb.append("        <mi>b</mi>\n");
		sb.append("      </mrow>\n");
		sb.append("      <mo>&PlusMinus;</mo>\n");
		sb.append("      <msqrt>\n");
		sb.append("        <mrow>\n");
		sb.append("          <msup>\n");
		sb.append("            <mi>b</mi>\n");
		sb.append("            <mn>2</mn>\n");
		sb.append("          </msup>\n");
		sb.append("          <mo>-</mo>\n");
		sb.append("          <mrow>\n");
		sb.append("            <mn>4</mn>\n");
		sb.append("            <mi>a</mi>\n");
		sb.append("            <mi>c</mi>\n");
		sb.append("          </mrow>\n");
		sb.append("        </mrow>\n");
		sb.append("      </msqrt>\n");
		sb.append("    </mrow>\n");
		sb.append("    <mrow>\n");
		sb.append("      <mn>2</mn>\n");
		sb.append("      <mi>a</mi>\n");
		sb.append("    </mrow>\n");
		sb.append("  </mfrac>\n");
		sb.append("</mrow>\n");
		sb.append("</math>\n");
	}

	public static void printFooter(StringBuffer sb) {
		sb.append("</body>\n");
		sb.append("</html>\n");
	}
}
