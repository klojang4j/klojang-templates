package org.klojang.templates.name;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CamelCaseToSnakeUpperCaseTest {

  @Test
  public void test00() {
    assertEquals("THIS_IS_A_COLUMN_NAME", map("thisIsAColumnName"));
  }

  @Test
  public void test01() {
    assertEquals("THIS", map("this"));
  }

  @Test
  public void test02() {
    assertEquals("THIS_IS", map("_thisIs"));
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
    return new CamelCaseToSnakeUpperCase().map(name);
  }

}
