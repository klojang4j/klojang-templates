package org.klojang.templates.name;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WordCaseToCamelCaseTest {

  @Test
  public void test00() {
    assertEquals("thisIsAColumnName", map("ThisIsAColumnName"));
  }

  @Test
  public void test01() {
    assertEquals("this", map("This"));
  }

  @Test
  public void test02() {
    assertEquals("_ThisIs", map("_ThisIs"));
  }

  @Test
  public void test03() {
    assertEquals("t", map("T"));
  }

  @Test
  public void test04() {
    assertThrows(IllegalArgumentException.class, () -> map(""));
  }

  @Test
  public void test05() {
    assertEquals("_ThisIs", WordCaseToCamelCase.mapName("_ThisIs"));
  }

  private static String map(String name) {
    return new WordCaseToCamelCase().map(name);
  }

}
