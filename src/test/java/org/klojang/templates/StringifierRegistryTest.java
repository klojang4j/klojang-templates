package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.path.util.MapBuilder;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringifierRegistryTest {

  public static final Stringifier uppercaser =
      obj -> obj == null ? "" : obj.toString().toUpperCase();

  public static final Stringifier lowercaser =
      obj -> obj == null ? "" : obj.toString().toLowerCase();

  public static final Stringifier typer =
      obj -> obj == null ? "[null]" : obj.getClass().getSimpleName() + ' ' + obj;

  public static final Stringifier decimal1 = obj ->
      obj == null
          ? "0.0000"
          : new DecimalFormat("0.0000").format(Double.valueOf(obj.toString()));
  public static final Stringifier decimal2 = obj ->
      obj == null
          ? "0"
          : new DecimalFormat("#.####").format(Double.valueOf(obj.toString()));

  @Test
  public void test00() throws ParseException {
    String src = "~%uppercase:foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByGroup(uppercaser, "uppercase")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", "Bar");
    assertEquals("BAR", rs.render());
  }

  @Test
  public void test01() throws ParseException {
    String src = "~%uppercase:foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByGroup(uppercaser, "uppercase")
        .registerByName(lowercaser, "foo")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", "Bar");
    assertEquals("BAR", rs.render());
  }

  @Test
  public void test02() throws ParseException {
    String src = "~%uppercase:foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByName(lowercaser, "foo")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", "Bar");
    assertEquals("bar", rs.render());
  }

  @Test
  public void test03() throws ParseException {
    String src = "~%foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByName(lowercaser, "foo")
        .registerByType(typer, Number.class)
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", 10.7F);
    assertEquals("10.7", rs.render());
  }

  @Test
  public void test04() throws ParseException {
    String src = "~%foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByType(typer, Number.class)
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", 10.7F);
    assertEquals("Float 10.7", rs.render());
  }

  @Test
  public void test05() throws ParseException {
    String src = "~%foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .setDefaultStringifier(obj -> "don't care")
        .registerByType(typer, Number.class)
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", LocalDate.now());
    assertEquals("don't care", rs.render());
  }

  @Test
  public void test06() throws ParseException {
    String src = "~%foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .register(uppercaser, t, "foo")
        .registerByType(typer, Number.class)
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", 23d);
    assertEquals("23.0", rs.render());
  }

  @Test
  public void test07() throws ParseException {
    String src = """
        foo: ~%foo%
        price: ~%averagePrice%
        sales: ~%averageSales%
        ~%%begin:t0%
            t0_price: ~%averagePrice%
            t0_sales: ~%averageSales%
            ~%%begin:t1%
                t1_price: ~%averagePrice%
                t1_sales: ~%averageSales%
            ~%%end:t1%
        ~%%end:t0%
        """;
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByName(decimal1, "averagePrice")
        .registerByName(decimal2, "averageSales")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", 23d);
    rs.set("averagePrice", 10.23);
    rs.set("averageSales", 750_000.2);
    String s = rs.render();
    s = s.replaceAll("\\s+", "|");
    s = s.replaceAll(":\\|", ":");
    assertEquals("foo:23.0|price:10.2300|sales:750000.2|", s);
  }

  @Test
  public void test08() throws ParseException {
    String src = """
        foo: ~%foo%
        price: ~%averagePrice%
        sales: ~%averageSales%
        ~%%begin:t0%
            t0_price: ~%averagePrice%
            t0_sales: ~%averageSales%
            ~%%begin:t1%
                t1_price: ~%averagePrice%
                t1_sales: ~%averageSales%
            ~%%end:t1%
        ~%%end:t0%
        """;
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByName(decimal1, "averagePrice")
        .registerByName(decimal2, "averageSales")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = new MapBuilder()
        .set("foo", "bar")
        .set("averagePrice", 222)
        .set("averageSales", 7777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .createMap();
    rs.insert(map);
    String s = rs.render();
    s = s.replaceAll("\\s+", "|");
    s = s.replaceAll(":\\|", ":");
    assertEquals(
        "foo:bar|price:222.0000|sales:7777|t0_price:22.0000|t0_sales:77|t1_price:2.0000|t1_sales:7|",
        s);
  }

  @Test
  public void test09() throws ParseException {
    String src = """
        foo: ~%foo%
        price: ~%averagePrice%
        sales: ~%averageSales%
        ~%%begin:t0%
            t0_price: ~%averagePrice%
            t0_sales: ~%averageSales%
            ~%%begin:t1%
                t1_price: ~%averagePrice%
                t1_sales: ~%averageSales%
            ~%%end:t1%
        ~%%end:t0%
        """;
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByName(decimal1, "average*")
        .registerByName(decimal2, "averageSales")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = new MapBuilder()
        .set("foo", "bar")
        .set("averagePrice", 222)
        .set("averageSales", 7777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .createMap();
    rs.insert(map);
    String s = rs.render();
    s = s.replaceAll("\\s+", "|");
    s = s.replaceAll(":\\|", ":");
    assertEquals(
        "foo:bar|price:222.0000|sales:7777|t0_price:22.0000|t0_sales:77|t1_price:2.0000|t1_sales:7|",
        s);
  }

  @Test
  public void test10() throws ParseException {
    String src = """
        foo: ~%foo%
        price: ~%averagePrice%
        sales: ~%averageSales%
        ~%%begin:t0%
            t0_price: ~%averagePrice%
            t0_sales: ~%averageSales%
            ~%%begin:t1%
                t1_price: ~%averagePrice%
                t1_sales: ~%averageSales%
            ~%%end:t1%
        ~%%end:t0%
        """;
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByName(decimal2, "*rage*")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = new MapBuilder()
        .set("foo", "bar")
        .set("averagePrice", 222)
        .set("averageSales", 7777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .createMap();
    rs.insert(map);
    String s = rs.render();
    s = s.replaceAll("\\s+", "|");
    s = s.replaceAll(":\\|", ":");
    assertEquals(
        "foo:bar|price:222|sales:7777|t0_price:22|t0_sales:77|t1_price:2|t1_sales:7|",
        s);
  }

  @Test
  public void test11() throws ParseException {
    String src = """
        foo: ~%foo%
        price: ~%averagePrice%
        sales: ~%averageSales%
        ~%%begin:t0%
            t0_price: ~%averagePrice%
            t0_sales: ~%averageSales%
            ~%%begin:t1%
                t1_price: ~%averagePrice%
                t1_sales: ~%averageSales%
            ~%%end:t1%
        ~%%end:t0%
        """;
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByName(decimal1, "*rage*")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = new MapBuilder()
        .set("foo", "bar")
        .set("averagePrice", 222)
        .set("averageSales", 7777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .createMap();
    rs.insert(map);
    String s = rs.render();
    s = s.replaceAll("\\s+", "|");
    s = s.replaceAll(":\\|", ":");
    assertEquals(
        "foo:bar|price:222.0000|sales:7777.0000|t0_price:22.0000|t0_sales:77.0000|t1_price:2.0000|t1_sales:7.0000|",
        s);
  }

  @Test
  public void test12() throws ParseException {
    String src = """
        foo: ~%foo%
        price: ~%averagePrice%
        sales: ~%averageSales%
        ~%%begin:t0%
            t0_price: ~%averagePrice%
            t0_sales: ~%averageSales%
            ~%%begin:t1%
                t1_price: ~%averagePrice%
                t1_sales: ~%averageSales%
            ~%%end:t1%
        ~%%end:t0%
        """;
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByTemplate(decimal1, t, null)
        .registerByTemplate(decimal2, t, "t0")
        .registerByTemplate(typer, t, "t0.t1")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = new MapBuilder()
        .set("foo", "666")
        .set("averagePrice", 222)
        .set("averageSales", 777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .createMap();
    rs.insert(map);
    String s = rs.render();
    s = s.replaceAll("\\s+", "|");
    s = s.replaceAll(":\\|", ":");
    assertEquals(
        "foo:666.0000|price:222.0000|sales:777.0000|t0_price:22|t0_sales:77|t1_price:Integer|2|t1_sales:Integer|7|",
        s);
  }

  @Test
  public void test13() throws ParseException {
    String src = "~%foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .registerByType(typer, Number.class)
        .setVariableType(Float.class, t, "foo")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", (Object) null);
    assertEquals("[null]", rs.render());
  }

}
