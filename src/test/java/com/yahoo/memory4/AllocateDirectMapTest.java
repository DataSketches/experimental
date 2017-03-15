/*
 * Copyright 2017, Yahoo! Inc. Licensed under the terms of the
 * Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.memory4;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.testng.annotations.Test;

public class AllocateDirectMapTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMapException() throws Exception {
    File dummy = createFile("dummy.txt", ""); //zero length
    Memory.map(dummy, 0, dummy.length());
  }

  @Test
  public void testIllegalArguments() throws Exception {
    File file = new File(getClass().getClassLoader().getResource("GettysburgAddress.txt").getFile());
    try (ResourceHandler rh = Memory.map(file, -1, Integer.MAX_VALUE)) {
      fail("Failed: testIllegalArgumentException: Position was negative.");
    } catch (IllegalArgumentException e) {
      //ok
    }

    try (ResourceHandler rh = Memory.map(file, 0, -1)) {
      fail("Failed: testIllegalArgumentException: Size was negative.");
    } catch (IllegalArgumentException e) {
      //ok
    }

    try (ResourceHandler rh = Memory.map(file, Long.MAX_VALUE, 2)) {
      fail("Failed: testIllegalArgumentException: Sum of position + size is negative.");
    } catch (IllegalArgumentException e) {
      //ok
    }
  }

  @Test
  public void testMapAndMultipleClose() throws Exception {
    File file = new File(getClass().getClassLoader().getResource("GettysburgAddress.txt").getFile());
    long memCapacity = file.length();
    try (ResourceHandler rh = Memory.map(file, 0, memCapacity)) {
      Memory map = rh.get();
      assertEquals(memCapacity, map.getCapacity());
      rh.close();
      rh.close();
      map.getCapacity(); //throws assertion error
    } catch (AssertionError e) {
      //OK
    }
    assertEquals(AllocateDirectMap.pageCount(1, 16), 16); //check pageCounter
  }

  @Test
  public void testReadFailAfterClose() throws Exception {
    File file = new File(getClass().getClassLoader().getResource("GettysburgAddress.txt").getFile());
    long memCapacity = file.length();
    try (ResourceHandler rh = Memory.map(file, 0, memCapacity)) {
      Memory mmf = rh.get();
      rh.close();
      mmf.getByte(0);
    } catch (AssertionError e) {
      //OK
    }
  }

  @Test
  public void testLoad() throws Exception {
    File file = new File(getClass().getClassLoader().getResource("GettysburgAddress.txt").getFile());
    long memCapacity = file.length();
    try (ResourceHandler rh = Memory.map(file, 0, memCapacity)) {
      rh.load();
      assertTrue(rh.isLoaded());
      rh.close();
    }
  }

  @Test
  public void testForce() throws Exception {
    String origStr = "Corectng spellng mistks";
    File origFile = createFile("force_original.txt", origStr); //23
    long origBytes = origFile.length();
    String correctStr = "Correcting spelling mistakes"; //28
    byte[] correctByteArr = correctStr.getBytes(UTF_8);
    long corrBytes = correctByteArr.length;

    try (ResourceHandler rh = Memory.map(origFile, 0, origBytes)) {
      Memory map = rh.get();
      rh.load();
      assertTrue(rh.isLoaded());
      //confirm orig string
      byte[] buf = new byte[(int)origBytes];
      map.getByteArray(0, buf, 0, (int)origBytes);
      String bufStr = new String(buf, UTF_8);
      assertEquals(bufStr, origStr);
    }

    try (WritableResourceHandler wrh = WritableMemory.map(origFile, 0, corrBytes)) { //longer
      WritableMemory wMap = wrh.get();
      wrh.load();
      assertTrue(wrh.isLoaded());
      // over write content
      wMap.putByteArray(0, correctByteArr, 0, (int)corrBytes);
      wrh.force();
      //confirm correct string
      byte[] buf = new byte[(int)corrBytes];
      wMap.getByteArray(0, buf, 0, (int)corrBytes);
      String bufStr = new String(buf, UTF_8);
      assertEquals(bufStr, correctStr);
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
