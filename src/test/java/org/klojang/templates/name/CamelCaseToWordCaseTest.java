package org.klojang.templates.name;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CamelCaseToWordCaseTest {

  @Test
  public void test00() {
    assertEquals("ThisIsAColumnName", map("thisIsAColumnName"));
  }

  @Test
  public void test01() {
    assertEquals("This", map("this"));
  }

  @Test
  public void test02() {
    assertEquals("_thisIs", map("_thisIs"));
  }

  @Test
  public void test03() {
    assertEquals("T", map("t"));
  }

  @Test
  public void test04() {
    assertThrows(IllegalArgumentException.class, () -> map(""));
  }

  private static String map(String name) {
    return new CamelCaseToWordCase().map(name);
  }

}
