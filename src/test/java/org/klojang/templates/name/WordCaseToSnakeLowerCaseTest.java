package org.klojang.templates.name;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WordCaseToSnakeLowerCaseTest {

  @Test
  public void test00() {
    assertEquals("this_is_a_column_name", map("ThisIsAColumnName"));
  }

  @Test
  public void test01() {
    assertEquals("this", map("This"));
  }

  @Test
  public void test02() {
    assertEquals("this_is", map("_ThisIs"));
  }

  @Test
  public void test03() {
    assertEquals("t", map("T"));
  }

  @Test
  public void test04() {
    assertThrows(IllegalArgumentException.class, () -> map(""));
  }

  private static String map(String name) {
    return new WordCaseToSnakeLowerCase().map(name);
  }

}
