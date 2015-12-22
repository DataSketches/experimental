package com.yahoo.sketches.tuple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class SerializerDeserializer {
  public static enum SketchType { QuickSelectSketch, CompactSketch, ArrayOfDoublesQuickSelectSketch, ArrayOfDoublesCompactSketch }
  public static final int TYPE_BYTE_OFFSET = 1;

  protected static final Map<String, Method> deserializeMethodCache = new HashMap<String, Method>();

  public static void validateType(ByteBuffer buffer, SketchType expectedType) {
    byte sketchTypeByte = buffer.get();
    SketchType sketchType = getSketchType(sketchTypeByte);
    if (!sketchType.equals(expectedType)) throw new RuntimeException("Sketch Type mismatch. Expected " + expectedType.name() + ", got " + sketchType.name());
  }

  public static void validateType(byte sketchTypeByte, SketchType expectedType) {
    SketchType sketchType = getSketchType(sketchTypeByte);
    if (!sketchType.equals(expectedType)) throw new RuntimeException("Sketch Type mismatch. Expected " + expectedType.name() + ", got " + sketchType.name());
  }

  public static SketchType getSketchTypeAbsolute(ByteBuffer buffer) {
    byte sketchTypeByte = buffer.get(TYPE_BYTE_OFFSET);
    return getSketchType(sketchTypeByte);
  }

  public static ByteBuffer serializeToByteBuffer(Object object) {
    try {
      String className = object.getClass().getName();
      ByteBuffer objectBuffer = ((ByteBuffer) object.getClass().getMethod("serializeToByteBuffer", (Class<?>[])null).invoke(object));
      ByteBuffer buffer = ByteBuffer.allocate(1 + className.length() + objectBuffer.capacity());
      buffer.put((byte)className.length()).put(className.getBytes());
      buffer.put(objectBuffer.array());
      return buffer;
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  public static Object deserializeFromByteBuffer(ByteBuffer buffer) {
    int classNameLength = buffer.get();
    byte[] classNameBuffer = new byte[classNameLength];
    buffer.get(classNameBuffer);
    String className = new String(classNameBuffer);
    return deserializeFromByteBuffer(buffer, className);
  }

  public static Object deserializeFromByteBuffer(ByteBuffer buffer, String className) {
    try {
      Method method = deserializeMethodCache.get(className);
      if (method == null) {
          method = Class.forName(className).getMethod("deserializeFromByteBuffer", ByteBuffer.class);
          deserializeMethodCache.put(className, method);
      }
      return method.invoke(null, buffer);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static SketchType getSketchType(byte sketchTypeByte) {
    if (sketchTypeByte < 0 || sketchTypeByte >= SketchType.values().length) throw new RuntimeException("Invalid Sketch Type " + sketchTypeByte);
    SketchType sketchType = SketchType.values()[sketchTypeByte];
    return sketchType;
  }
}
