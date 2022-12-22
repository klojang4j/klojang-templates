package org.klojang.templates.name;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SnakeCaseToCamelCaseTest {

  @Test
  public void test00() {
    assertEquals("thisIsAColumnName", map("this_is_a_column_name"));
  }

  @Test
  public void test01() {
    assertEquals("thisIsAColumnName", map("this__is_a_column__name"));
  }

  @Test
  public void test02() {
    assertEquals("this", map("this"));
  }

  @Test
  public void test03() {
    assertEquals("thisIsAColumnName", map("THIS_IS_A_COLUMN_NAME"));
  }

  @Test
  public void test04() {
    assertEquals("thisIsAColumnName", map("This__Is_A_Column__Name"));
  }

  @Test
  public void test05() {
    assertEquals("t", map("T"));
  }

  @Test()
  public void test06() {
    assertThrows(IllegalArgumentException.class, () -> map("_"));
  }

  @Test()
  public void test07() {
    assertThrows(IllegalArgumentException.class, () -> map("__"));
  }

  private static String map(String name) {
    return new SnakeCaseToCamelCase().map(name);
  }
}
