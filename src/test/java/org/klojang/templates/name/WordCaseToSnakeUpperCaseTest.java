package org.klojang.templates.name;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WordCaseToSnakeUpperCaseTest {

  @Test
  public void test00() {
    assertEquals("THIS_IS_A_COLUMN_NAME", map("ThisIsAColumnName"));
  }

  @Test
  public void test01() {
    assertEquals("THIS", map("This"));
  }

  @Test
  public void test02() {
    assertEquals("THIS_IS", map("_ThisIs"));
  }

  @Test
  public void test03() {
    assertEquals("T", map("T"));
  }

  @Test
  public void test04() {
    assertThrows(IllegalArgumentException.class, () -> map(""));
  }

  @Test
  public void test05() {
    assertEquals("THIS_IS", WordCaseToSnakeUpperCase.mapName("_ThisIs"));
  }

  private static String map(String name) {
    return new WordCaseToSnakeUpperCase().map(name);
  }

}
