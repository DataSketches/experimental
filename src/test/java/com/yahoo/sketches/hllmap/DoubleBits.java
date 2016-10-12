/* <p>Copyright (c) 2005-2013, N. Lee Rhodes, Los Altos, California.
 * All Rights Reserved.<br>
 * THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THE ECLIPSE PUBLIC
 * LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THE PROGRAM
 * CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.</p>
 * <p>You should have received a copy of the Eclipse Public License, v1.0 along
 * with this program.  Otherwise, a copy can be obtained from
 * http://www.eclipse.org/org/documents/epl-v10.php.</p>
 */
package com.yahoo.sketches.hllmap;

import static com.yahoo.sketches.Util.characterPad;

/**
 * If you are developing any functions that operate directly on the IEEE 754
 * double-precision 64-bit fields you will need this class.
 * @author Lee Rhodes
 */
public final class DoubleBits {
    public static final long SIGN = 1L << 63; //only the MSB (sign) set
    public static final long DEXP1 = 1L << 52; //an exponent value of 1
    public static final long DEXP2 = 2L << 52; //an exponent value of 2
    public static final long DMANMASK = DEXP1 - 1L; //mantissa mask (52 1's)
    public static final long DEXP1023 = 1023L << 52; //exp = 1023 (10 1's)
    public static final long DEXP1025 = 1025L << 52; //exp = 1025
    public static final long DEXP1026 = 1026L << 52; //exp = 1026
    public static final long DEXPMASK = 2047L << 52;//exp = 2047 (11 1's), mask.

    private DoubleBits() {
        //Empty Constructor
    }

    /**
     * Returns true if the IEEE 754 sign bit is set even if the value, d, represents
     * a NaN or zero.
     * @param d the given value
     * @return the sign bit.
     */
    public static boolean isNegative(double d) {
        long bits = Double.doubleToRawLongBits(d);
        return ((bits & SIGN) != 0L);
    }

    /**
     * Returns true if the value is a +/- denormalized number but not +/- zero.
     * @param d the given value
     * @return true if the value is a +/- denormalized number but not +/- zero
     */
    public static boolean isDenormal(double d) {
        long bits = Double.doubleToRawLongBits(d);
        return (d != 0.0) && ((bits & DEXPMASK) == 0L) ;
    }

    /**
     * Returns true if the value is +/- Infinity
     * @param d the given value
     * @return true if the value is +/- Infinity
     */
    public static boolean isInfinity(double d) {
        long bits = Double.doubleToRawLongBits(d);
        return ((bits & ~SIGN) == DEXPMASK);
    }

    /**
     * Returns true if the value is not a NaN or Infinity
     * @param d the given value
     * @return true if the value is not a NaN or Infinity
     */
    public static boolean isValid(double d) {
        long bits = Double.doubleToRawLongBits(d);
        return ((bits & DEXPMASK) != DEXPMASK);
    }

    /**
     * Returns true if the value is a valid argument for the log function.
     * In other words, it cannot be +/- NaN, zero, or Infinity, but it may
     * be a denormal number.
     * @param d the given value
     * @return true if the value is a valid argument for the log function
     */
    public static boolean isValidLogArgument(double d) {
        long bits = Double.doubleToRawLongBits(d);
        return ((bits + DEXP1) > DEXP1);
    }

    /**
     * Returns true if the value is a denormal or not a valid argument for the
     * log function.
     * @param d the given value
     * @return true if the value is a denormal or not a valid argument for the
     * log function
     */
    public static boolean isDenormalOrNotValidLogArgument(double d) {
        long bits = Double.doubleToRawLongBits(d);
        return ((bits + DEXP1) < DEXP2);
    }

    /**
     * Returns the signum function of the argument; zero if the argument
     * is zero, 1.0 if the argument is greater than zero, -1.0 if the
     * argument is less than zero.  If the argument is +/- NaN or zero,
     * the result is the same as the argument.<br/>
     *
     * Note that this is a faster, simpler replacement for the needlessly
     * complicated implementation in sun.misc.FpUtils.
     *
     * @param d the floating-point value whose signum is to be returned
     * @return the signum function of the argument
     */
    public static double signum(double d) {
        return ((d != d) || (d == 0.0))? d
            : (d < 0.0)? -1.0: 1.0;
    }

    /**
     * Returns the 11 bit exponent of a double flush right as an int.
     * @param d the given value
     * @return the exponent bits.
     */
    public static int exponentToIntBits(double d) {
        long bits = Double.doubleToRawLongBits(d);
        return (int)((bits & DEXPMASK) >>> 52);
    }

    /**
     * Returns the 52 bits of the mantissa as a long.
     * @param d the given value
     * @return the mantissa bits.
     */
    public static long mantissaToLongBits(double d) {
        long bits = Double.doubleToRawLongBits(d);
        return bits & DMANMASK;
    }

    /**
     * Returns the mathematical Base 2 exponent as an int. The offset
     * has been removed.
     * @param d the given value
     * @return the mathematical Base 2 exponent
     */
    public static int base2Exponent(double d) {
        int e = exponentToIntBits(d);
        return (e == 0)? -1022: e - 1023;
    }

    /**
     * Given the double value d, replace the exponent bits
     * with the raw exponent value provided in exp.  If you are converting from
     * a mathematical Base 2 exponent, the offset of 1023 must be added first.
     * @param d the given value
     * @param exp the given exponent
     * @return the adjusted value of d
     */
    public static double setRawExponentBits(double d, int exp) {
        long bits = Double.doubleToRawLongBits(d);
        bits &= ~DEXPMASK; //remove old exponent bits
        bits |= (exp & 0x7FFL) << 52;//insert the new exponent
        return Double.longBitsToDouble(bits);
    }

    /**
     * Given the double value d, replace the mantissa bits
     * with the given mantissa.
     * @param d the given value
     * @param man the given mantissa
     * @return the adjusted value of d
     */
    public static double setMantissaBits(double d, long man) {
        long bits = Double.doubleToRawLongBits(d);
        bits &= ~DMANMASK; //remove old mantissa bits
        bits |= (DMANMASK & man); //insert the new mantissa
        return Double.longBitsToDouble(bits);
    }

    /**
     * Returns the given double with the sign bit set as specified by the
     * given boolean.
     * @param d the given value
     * @param negative true if negative
     * @return the adjusted value of d
     */
    public static double setSignBit(double d, boolean negative) {
        long bits = Double.doubleToRawLongBits(d);
        return negative
            ? Double.longBitsToDouble(bits | SIGN)
            : Double.longBitsToDouble(bits & ~SIGN);
    }

    /**
     * Given a double, this returns a decimal value representing the effective
     * fractional value of the double's mantissa, which is independent of the
     * value of the exponent with only one exception. If the exponent field 'e'
     * of a double is in the range [1, 2046] the value of a double can be
     * expressed mathematically as 1.fraction X 2^(e-1023). If the exponent
     * value e == 0, the value is expressed as 0.fraction X 2^(-1022), which is
     * called a denormal number. Denormal numbers are only required for
     * magnitudes less than 2.2250738585072014E-308. Very small indeed and much
     * smaller than normal rounding errors.
     *
     * @param d the given value
     * @return a value representing the fractional value of the
     *         double's mantissa. It will be in the format 1.fraction or
     *         0.fraction depending on the exponent as explained above.
     */
    public static double mantissaFraction(double d) {
        long bits = Double.doubleToRawLongBits(d);
        long exp = bits & DEXPMASK;
        long sign = bits & SIGN; //required in case d is a NaN or zero
        //remov exp & sign, insert exponent for 1.0
        long man = (bits & DMANMASK) | DEXP1023;
        double d2 = Double.longBitsToDouble(man); //back to double as 1.xxxxx
        d2 = (exp == 0L)? d2 - 1.0: d2;
        return (sign != 0L)? -d2: d2;
    }

    /**
     * Returns the given double as a string of the form
     * <pre>
     *  SP.FFFFF...FFFBTEEEE
     *  Where
     *    S = the sign of the magnitude only if negative, no space if positive
     *    P = mantissa prefix of 1 or 0
     *    F = decimal fraction
     *    B = indicates binary power of 2 representation
     *    T = the sign of the exponent only if negative
     *    E = the exponent of 2
     * </pre>
     * @param d the given value
     * @return the given double as a string
     */
    public static String doubleToBase2String(double d) {
        boolean sign = isNegative(d);
        if (d == 0.0) return sign? "-0.0B0": "0.0B0";
        return Double.toString(mantissaFraction(d)) + "B" + base2Exponent(d);
    }

    /**
     * Returns a formatted string representing the fields of an
     * IEEE 754 64-bit double-precision floating point value.
     * The output string is in the form:
     * <pre>
     *  S EEEEEEEEEEE MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * </pre>
     * where S represents the sign bit, E represents bits of the Exponent field,
     * and M represents bits of the Mantissa field of the double value.
     * @param d the <i>double</i> value to be examined.
     * @return the formatted string of the fields of a <i>double</i>.
     */
    public static String doubleToBitString(double d) {
        long bits = Double.doubleToRawLongBits(d);
        return longBitsToDoubleBitString(bits);
    }

    /**
     * Returns a formatted string representing the fields of an
     * IEEE 754 64-bit double-precision floating point value presented as a
     * <i>long</i>.  The output string is in the form:
     * <pre>
     *  S EEEEEEEEEEE MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM
     * </pre>
     * where S represents the sign bit, E represents bits of the Exponent field,
     * and M represents bits of the Mantissa field of the double value.
     * @param bits the <i>double</i> value in the form of a <i>long</i> to be
     * examined.
     * @return the formatted string of the fields of a <i>double</i>.
     */
    public static String longBitsToDoubleBitString(long bits) {
        int intSign = ((bits & SIGN) != 0L)? 1: 0;
        int e = (int)((bits & DEXPMASK) >>> 52);
        String estr = characterPad(Integer.toBinaryString(e), 11, '0', false);
        long m = bits & DMANMASK;
        String mstr = characterPad(Long.toBinaryString(m), 52, '0', false);
        return intSign + " " + estr + " " + mstr;
    }


    /**
     * Helper method to determine whether a specified string is a
     * parsable numeric value or not.
     *
     * @param string the input string to analyze.
     *
     * @return true if the value is numeric (integer, float, double); false if
     *         not.
     */
    public static boolean isNumeric(String string) {
        boolean isNum = false;
        try {
           Double.parseDouble(string);
           isNum=true;
        }  catch (NumberFormatException exc ) {
            // We have a non numeric value.
        }
        return isNum;
    }

    private static void test(double d, String txt) {
        System.out.println("Test     :"+txt);
        System.out.println("Value    : "+d);
        System.out.println("Bits     : "+doubleToBitString(d));
        System.out.println("Base2    : "+doubleToBase2String(d));
        System.out.println("Negative : "+isNegative(d));
        System.out.println("Denormal : "+isDenormal(d));
        System.out.println("Infinity : "+isInfinity(d));
        System.out.println("Valid    : "+isValid(d));
        System.out.println("Valid Log: "+isValidLogArgument(d));
        System.out.println("Signum   : "+signum(d));
        System.out.println();
    }

    public static void main(String[] args) {
        test(4.9E-324,"+Denormal");
        test(-4.9E-324,"-Denormal");
        test(0.0,"+Zero");
        test(-0.0,"-Zero");
        test(1.0,"+1.0");
        test(-1.0,"-1.0");
        test(Double.MAX_VALUE,"MAX_VALUE");
        test(Double.MIN_VALUE,"MIN_VALUE");
        test(Double.POSITIVE_INFINITY,"+INFINITY");
        test(Double.NEGATIVE_INFINITY,"-INFINITY");
        test(Double.NaN,"+QNaN"); //Quiet NaN
        test(setSignBit(Double.NaN, true),"-QNaN"); //-Quiet NaN
    }
}