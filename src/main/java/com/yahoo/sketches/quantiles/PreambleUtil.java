package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.Family.idToFamily;
import static com.yahoo.sketches.Util.LS;

import java.nio.ByteOrder;

import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;

//@formatter:off
/**
 * This class defines the preamble data structure and provides basic utilities for some of the key
 * fields.
 * <p>The intent of the design of this class was to isolate the detailed knowledge of the bit and 
 * byte layout of the serialized form of the sketches derived from the Sketch class into one place. 
 * This allows the possibility of the introduction of different serialization 
 * schemes with minimal impact on the rest of the library.</p>
 *  
 * <p>
 * MAP: Low significance bytes of this <i>long</i> data structure are on the right. However, the
 * multi-byte integers (<i>int</i> and <i>long</i>) are stored in native byte order. The
 * <i>byte</i> values are treated as unsigned.</p>
 * 
 * <p>An Empty virgin sketch may only have the first 8 bytes.  A non-sketch-estimating sampling
 * sketch may only have the first 16 bytes. All others will have all 24 bytes.</p> 
 * 
 * <pre>
 * Long || Start Byte Adr:
 * Adr: 
 *      ||  7 |   6   |     5    |   4   |   3   |    2   |    1   |     0              |
 *  0   || -------------K--------------- | Flags | FamID  | SerVer |  Preamble_Longs    |
 *  
 *      || 15 |  14   |    13    |  12   |  11   |   10   |    9   |     8              |
 *  1   || -----------------------------N_LONG ---------------------------------------- |
 *  
 *      || 23 |  22   |    21    |  20   |  19   |   18   |   17   |    16              |
 *  2   || ----------------------------MIN_DOUBLE-------------------------------------- |
 *
 *      || 31 |  30   |    29    |  28   |  27   |   26   |   25   |    24              |
 *  3   || ----------------------------MAX DOUBLE ------------------------------------- |
 *      || 39 |  38   |    37    |  36   |  35   |   34   |   33   |    32              |
 *  4   || ---------(unused)-------------|----------BUFFER_DOUBLES_ALLOC_INT----------- |
 *      || 47 |  46   |    45    |  44   |  43   |   42   |   41   |    40              |
 *  5   || ----------------------------START OF BUFFER----------------------------------|
 *  </pre>
 *  
 *  @author Lee Rhodes
 */
final class PreambleUtil {

  private PreambleUtil() {}

  // ###### DO NOT MESS WITH THIS FROM HERE ...
  // Preamble byte Addresses
  static final int PREAMBLE_LONGS_BYTE        = 0; //either 1 or 5
  static final int SER_VER_BYTE               = 1;
  static final int FAMILY_BYTE                = 2;
  static final int FLAGS_BYTE                 = 3;
  static final int K_INT                      = 4;  //to 7;  Stop here for empty sketch
  static final int N_LONG                     = 8;  //to 15
  static final int MIN_DOUBLE                 = 16; //to 23
  static final int MAX_DOUBLE                 = 24; //to 31
  static final int BUFFER_DOUBLES_ALLOC_INT   = 32; //to 35
  //                                            36 to 39 unused
  //Specific values for this implementation
  static final int SER_VER                    = 1;
  
  // flag bit masks
  static final int BIG_ENDIAN_FLAG_MASK       = 1;
  static final int READ_ONLY_FLAG_MASK        = 2;
  static final int EMPTY_FLAG_MASK            = 4;
  static final int COMPACT_FLAG_MASK          = 8;
  static final int ORDERED_FLAG_MASK          = 16;
  
  static final boolean NATIVE_ORDER_IS_BIG_ENDIAN  = 
      (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
  
  // STRINGS
  /**
   * Returns a human readable string summary of the internal state of the given byte array. Used
   * primarily in testing.
   * 
   * @param byteArr the given byte array.
   * @return the summary string.
   */
  public static String toString(byte[] byteArr) {
    Memory mem = new NativeMemory(byteArr);
    return toString(mem);
  }

  /**
   * Returns a human readable string summary of the internal state of the given Memory. 
   * Used primarily in testing.
   * 
   * @param mem the given Memory
   * @return the summary string.
   */
  public static String toString(Memory mem) {
    return memoryToString(mem);
  }

  private static String memoryToString(Memory mem) {
    int preLongs = (mem.getByte(PREAMBLE_LONGS_BYTE)) & 0XFF;
    int serVer = mem.getByte(SER_VER_BYTE);
    int familyID = mem.getByte(FAMILY_BYTE);
    String famName = idToFamily(familyID).toString();
    int flags = mem.getByte(FLAGS_BYTE);
    boolean bigEndian = (flags & BIG_ENDIAN_FLAG_MASK) > 0;
    String nativeOrder = ByteOrder.nativeOrder().toString();
    boolean compact = (flags & COMPACT_FLAG_MASK) > 0;
    boolean ordered = (flags & ORDERED_FLAG_MASK) > 0;
    boolean readOnly = (flags & READ_ONLY_FLAG_MASK) > 0;
    boolean empty = (flags & EMPTY_FLAG_MASK) > 0;
    int k = mem.getInt(K_INT);
    //if absent, assumed values
    long n = 0;
    double minValue = Double.POSITIVE_INFINITY;
    double maxValue = Double.NEGATIVE_INFINITY;
    int bufDoublesAlloc = 0;
    int requiredBytes = 8; 
    if (preLongs == 5) {
      n = mem.getLong(N_LONG);
      minValue = mem.getDouble(MIN_DOUBLE);
      maxValue = mem.getDouble(MAX_DOUBLE);
      bufDoublesAlloc = mem.getInt(BUFFER_DOUBLES_ALLOC_INT);
      requiredBytes = bufDoublesAlloc * 8 + 40;
    } 
    
    StringBuilder sb = new StringBuilder();
    sb.append(LS);
    sb.append("### SKETCH PREAMBLE SUMMARY:").append(LS);
    sb.append("Byte  0: Preamble Longs       : ").append(preLongs).append(LS);
    sb.append("Byte  1: Serialization Version: ").append(serVer).append(LS);
    sb.append("Byte  2: Family               : ").append(famName).append(LS);
    sb.append("Byte  3: Flags Field          : ").append(String.format("%02o", flags)).append(LS);
    sb.append("  BIG_ENDIAN_STORAGE          : ").append(bigEndian).append(LS);
    sb.append("  (Native Byte Order)         : ").append(nativeOrder).append(LS);
    sb.append("  READ_ONLY                   : ").append(readOnly).append(LS);
    sb.append("  EMPTY                       : ").append(empty).append(LS);
    sb.append("  COMPACT                     : ").append(compact).append(LS);
    sb.append("  ORDERED                     : ").append(ordered).append(LS);
    sb.append("Bytes  4-7  : K               : ").append(k).append(LS);
    if (preLongs == 1) {
      sb.append(" --ABSENT, ASSUMED:").append(LS);
    }
    sb.append("Bytes  8-15 : N                : ").append(n).append(LS);
    sb.append("Bytes 16-23 : MIN              : ").append(minValue).append(LS);
    sb.append("Bytes 24-31 : MAX              : ").append(maxValue).append(LS);
    sb.append("Bytes 32-35 : BUF DOUBLES      : ").append(bufDoublesAlloc).append(LS);

    sb.append("TOTAL Allocated Sketch Bytes   : ").append(mem.getCapacity()).append(LS);
    sb.append("TOTAL Required Sketch Bytes    : ").append(requiredBytes).append(LS);
    sb.append("### END SKETCH PREAMBLE SUMMARY").append(LS);
    return sb.toString();
  }
  
//@formatter:on
}
