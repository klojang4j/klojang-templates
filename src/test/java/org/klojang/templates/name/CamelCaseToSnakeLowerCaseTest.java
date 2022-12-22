package org.klojang.templates.name;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CamelCaseToSnakeLowerCaseTest {

  @Test
  public void test00() {
    assertEquals("this_is_a_column_name", map("thisIsAColumnName"));
  }

  @Test
  public void test01() {
    assertEquals("this", map("this"));
  }

  @Test
  public void test02() {
    assertEquals("_this_is", map("_thisIs"));
  }

  @Test
  public void test03() {
    assertEquals("t", map("t"));
  }

  private static String map(String name) {
    return new CamelCaseToSnakeLowerCase().map(name);
  }
}
