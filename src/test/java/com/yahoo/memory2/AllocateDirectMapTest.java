/*
 * Copyright 2015-16, Yahoo! Inc. Licensed under the terms of the Apache License 2.0. See LICENSE
 * file at the project root for terms.
 */

package com.yahoo.memory2;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.testng.annotations.Test;

/**
 * Note: this class requires the resource file:
 * <i>/memory/src/test/resources/memory_mapped.txt</i>.
 *
 * @author Praveenkumar Venkatesan
 */
//@SuppressWarnings("resource")
public class AllocateDirectMapTest {

  @Test
  public void testMapException() throws Exception {
    File dummy = createFile("dummy.txt", ""); //zero length
    try (MemoryHandler mh = MemoryHandler.writableMap(dummy, 0, dummy.length())) {

    } catch (IllegalArgumentException e) {
      // Expected;
    }
  }

  @Test
  public void testIllegalArguments() throws Exception {
    File file = new File(getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    try (MemoryHandler mh = MemoryHandler.writableMap(file, -1, Integer.MAX_VALUE)) {

      fail("Failed: testIllegalArgumentException: Position was negative.");
    } catch (Exception e) {
      // Expected;
    }

    try (MemoryHandler mh = MemoryHandler.writableMap(file, 0, -1)) {
      fail("Failed: testIllegalArgumentException: Size was negative");
    } catch (Exception e) {
      // Expected;
    }

    try (MemoryHandler mh = MemoryHandler.writableMap(file, Long.MAX_VALUE, 2)) {
      fail("Failed: testIllegalArgumentException: Sum of position + size is negative.");
    } catch (Exception e) {
      // Expected;
    }
  }

  @Test
  public void testMemoryMapAndClose() {
    File file = new File(this.getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    long memCapacity = file.length();

    try (MemoryHandler mh = MemoryHandler.writableMap(file,0, memCapacity)) {
      WritableMemory mmf = mh.getWritable();
      assertEquals(memCapacity, mmf.getCapacity());
      mh.close();
      assertFalse(mmf.isValid());
      assertEquals(AllocateDirectMap.pageCount(1, 16), 16); //check pageCounter
    } catch (Exception e) {
      fail("Failed: testMemoryMapAndFree()");
    }
  }

  @Test
  public void testMultipleUnMaps() {
    File file = new File(getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    try (MemoryHandler mh = MemoryHandler.writableMap(file, 0, file.length())) {
      WritableMemory mmf = mh.getWritable();
      if (mmf.isValid()) {
        mh.close(); // idempotent test
        mh.close();
      }
    } catch (Exception e) {
      fail("Failed: testMemoryMapAndFree()");
    }
  }

  @Test
  public void testReadUsingRegion() {
    File file = new File(getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    println("FileLen: " + file.length());
    try (MemoryHandler mh = MemoryHandler.writableMap(file, 0, file.length())) {
      WritableMemory mmf = mh.getWritable();
      mmf.load();

      Memory reg = mmf.writableRegion(512, 512).asReadOnly();
      for (int i = 0; i < 512; i++ ) {
        assertEquals(reg.getByte(i), mmf.getByte(i + 512));
      }
      mh.close();
    } catch (Exception e) {
      fail("Failed: testReadUsingRegion()");
    }
  }

  @Test
  public void testReadFailAfterFree() {
    File file = new File(getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    try (MemoryHandler mh = MemoryHandler.writableMap(file, 0, file.length())) {
      WritableMemory mmf = mh.getWritable();
      mh.close();
      char[] cbuf = new char[500];
      try {
        mmf.getCharArray(500, cbuf, 0, 500);
      } catch (AssertionError e) {
        // pass
      }
    } catch (Exception e) {
      fail("Failed: testReadFailAfterFree()");
    }
  }

  @Test
  public void testLoad() {
    File file = new File(getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    try (MemoryHandler mh = MemoryHandler.writableMap(file, 0, file.length())) {
      WritableMemory mmf = mh.getWritable();
      mmf.load();
      assertTrue(mmf.isLoaded());
      mh.close();
    } catch (Exception e) {
      fail("Failed: testLoad()");
    }
  }


  @Test
  public void testForce() throws Exception {
    String origStr = "Corectng spellng mistks";
    byte[] origArr = origStr.getBytes(UTF_8);
    File origFile = createFile("force_original.txt", origStr); //23 bytes
    long origFileBytes = origFile.length(); //23
    assertEquals(origFile.length(), origArr.length);

    String correctStr = "Correcting spelling mistakes"; //28 bytes
    byte[] correctArr = correctStr.getBytes(UTF_8);
    long bufBytes = correctArr.length; //buffer large enough

    //note that the map is created with more capacity than the original file.
    try (MemoryHandler mh = MemoryHandler.writableMap(origFile, 0, bufBytes)) {//extra 5 bytes for buffer
      WritableMemory mmf = mh.getWritable();

      mmf.load();

      // pull in existing content from file into buffer for checking
      byte[] bufArr = new byte[(int)bufBytes];
      mmf.getByteArray(0, bufArr, 0, bufArr.length);  //check get array
      //confirm orig content
      for (int i = 0; i < origFileBytes; i++) {
        assertEquals(bufArr[i], origArr[i]);
      }

      // replace content
      mmf.putByteArray(0, correctArr, 0, correctArr.length);

      mmf.force(); //writes back, expanding the file
      //confirm new content
      for (int i = 0; i < bufBytes; i++) {
        assertEquals(mmf.getByte(i), correctArr[i]);
      }

      mh.close();
    } catch (Exception e) {
      fail("Failed: testLoad()");
    }

    // Additional confirmation after closing and reopening file
    try (MemoryHandler mh2 = MemoryHandler.writableMap(origFile, 0, bufBytes)) {
      WritableMemory nmf = mh2.getWritable();
      nmf.load();

      // existing content
      byte[] nArr = new byte[(int)bufBytes];
      nmf.getByteArray(0, nArr, 0, nArr.length);

      int index = 0;
      boolean corrected = true;

      // make sure that new content is ok
      while (index < bufBytes) {
        if (correctArr[index] != nArr[index]) {
          corrected = false;
          break;
        }
        index++;
      }

      assertTrue(corrected);

      mh2.close();
    } catch (Exception e) {
      fail("Failed: testForce()." + e);
    }
  }

  private static File createFile(String fileName, String text) throws FileNotFoundException {
    File file = new File(fileName);
    file.deleteOnExit();
    PrintWriter writer;
    try {
      writer = new PrintWriter(file, UTF_8.name());
      writer.print(text);
      writer.close();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return file;
  }

  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }

}
