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

  @Test(expectedExceptions = RuntimeException.class)
  public void testMapException() throws Exception  {
    File dummy = createFile("dummy.txt", "");
    WritableMemory.writableMap(dummy, 0, dummy.length()); //zero length
  }

  @Test
  public void testIllegalArguments() throws Exception {
    File file = new File(getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    try {
      WritableMemory.writableMap(file, -1, Integer.MAX_VALUE);
      fail("Failed: testIllegalArgumentException: Position was negative.");
    } catch (Exception e) {
      // Expected;
    }

    try {
      WritableMemory.writableMap(file, 0, -1);
      fail("Failed: testIllegalArgumentException: Size was negative");
    } catch (Exception e) {
      // Expected;
    }

    try {
      WritableMemory.writableMap(file, Long.MAX_VALUE, 2);
      fail("Failed: testIllegalArgumentException: Sum of position + size is negative.");
    } catch (Exception e) {
      // Expected;
    }
  }

  @Test
  public void testMemoryMapAndClose() {
    File file = new File(this.getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    long memCapacity = file.length();

    try {
      //AllocateMemoryMappedFile mmf AllocateMemoryMappedFile.getInstance(file, 0, file.length(), false);
      WritableMemory mmf = WritableMemory.writableMap(file, 0, memCapacity);
      assertEquals(memCapacity, mmf.getCapacity());
      mmf.close();
      assertFalse(mmf.isValid());
      assertEquals(AllocateDirectMap.pageCount(1, 16), 16); //check pageCounter
    } catch (Exception e) {
      fail("Failed: testMemoryMapAndFree()");
    }
  }

  @Test
  public void testMultipleUnMaps() {
    File file = new File(getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    WritableMemory mmf = null;
    try {
      mmf = WritableMemory.writableMap(file, 0, file.length());
    } catch (Exception e) {
      if (mmf != null && mmf.isValid()) {
        mmf.close();
      }
      fail("Failed: testMultipleUnMaps()");
    }
    if (mmf != null) {
      mmf.close(); // idempotent test
      mmf.close();
    }

  }

  @Test
  public void testReadUsingRegion() {
    File file = new File(getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    println("FileLen: " + file.length());
    try {
      WritableMemory mmf = WritableMemory.writableMap(file, 0, file.length());
      mmf.load();

      Memory reg = mmf.region(512, 512);
      for (int i = 0; i < 512; i++ ) {
        assertEquals(reg.getByte(i), mmf.getByte(i + 512));
      }
      mmf.close();
    } catch (Exception e) {
      fail("Failed: testReadUsingRegion()");
    }
  }

  @Test
  public void testReadFailAfterFree() {
    File file = new File(getClass().getClassLoader().getResource("memory_mapped.txt").getFile());
    try {
      WritableMemory mmf = WritableMemory.writableMap(file, 0, file.length());
      mmf.close();
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
    try {
      WritableMemory mmf = WritableMemory.writableMap(file, 0, file.length());
      mmf.load();
      assertTrue(mmf.isLoaded());
      mmf.close();
    } catch (Exception e) {
      fail("Failed: testLoad()");
    }
  }


  @Test
  public void testForce() throws Exception {
    File org = createFile("force_original.txt", "Corectng spellng mistks");
    long orgBytes = org.length();
    try {
      // extra 5bytes for buffer
      int buf = (int) orgBytes + 5;
      WritableMemory mmf = WritableMemory.writableMap(org, 0, buf);
      mmf.load();

      // existing content
      byte[] c = new byte[buf];
      mmf.getByteArray(0, c, 0, c.length);

      // add content
      String cor = "Correcting spelling mistakes";
      byte[] b = cor.getBytes(UTF_8);
      mmf.putByteArray(0, b, 0, b.length);

      mmf.force();
      mmf.close();

      WritableMemory nmf = WritableMemory.writableMap(org, 0, buf);
      nmf.load();

      // existing content
      byte[] n = new byte[buf];
      nmf.getByteArray(0, n, 0, n.length);

      int index = 0;
      boolean corrected = true;

      // make sure that new content is diff
      while (index < buf) {
        if (b[index] != n[index]) {
          corrected = false;
          break;
        }
        index++;
      }

      assertTrue(corrected);

      nmf.close();
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
