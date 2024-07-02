package org.klojang.templates;

import org.junit.jupiter.api.Test;
import org.klojang.path.util.MapBuilder;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringifierRegistryTest {

  public static final Stringifier uppercaser =
      obj -> obj == null ? "" : obj.toString().toUpperCase();

  public static final Stringifier lowercaser =
      obj -> obj == null ? "" : obj.toString().toLowerCase();

  public static final Stringifier typer =
      obj -> obj == null ? "[null]" : obj.getClass().getSimpleName() + '@' + obj;


  static final NumberFormat NF1=NumberFormat.getInstance(Locale.US);
  static final NumberFormat NF2=NumberFormat.getInstance(Locale.US);

  static {
    NF1.setMinimumIntegerDigits(1);
    NF1.setMinimumFractionDigits(4);
    NF1.setMaximumFractionDigits(4);
    NF1.setGroupingUsed(false);
    NF2.setMinimumIntegerDigits(0);
    NF2.setMinimumFractionDigits(0);
    NF2.setMaximumFractionDigits(4);
    NF2.setGroupingUsed(false);

  }

  public static final Stringifier decimal1 = obj ->
      obj == null
          ? "0.0000"
          : NF1.format(Double.valueOf(obj.toString()));
  public static final Stringifier decimal2 = obj ->
      obj == null
          ? "0"
          : NF2.format(Double.valueOf(obj.toString()));

  @Test
  public void test00() throws ParseException {
    String src = "~%uppercase:foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .forVarGroup("uppercase", uppercaser)
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
        .forVarGroup("uppercase", uppercaser)
        .forName("foo", lowercaser)
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
        .forName("foo", lowercaser)
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
        .forName("foo", lowercaser)
        .forType(Number.class, typer)
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
        .forType(Number.class, typer)
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", 10.7F);
    assertEquals("Float@10.7", rs.render());
  }

  @Test
  public void test05() throws ParseException {
    String src = "~%foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .setDefaultStringifier(obj -> "don't care")
        .forType(Number.class, typer)
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
        .register(t, uppercaser, "foo")
        .forType(Number.class, typer)
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
        .forName("averagePrice", decimal1)
        .forName("averageSales", decimal2)
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
        .forName("averagePrice", decimal1)
        .forName("averageSales", decimal2)
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = MapBuilder.begin()
        .set("foo", "bar")
        .set("averagePrice", 222)
        .set("averageSales", 7777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .build();
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
        .forName("average*", decimal1)
        .forName("averageSales", decimal2)
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = MapBuilder.begin()
        .set("foo", "bar")
        .set("averagePrice", 222)
        .set("averageSales", 7777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .build();
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
        .forName("*rage*", decimal2)
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = MapBuilder.begin()
        .set("foo", "bar")
        .set("averagePrice", 222)
        .set("averageSales", 7777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .build();
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
        .forName("*rage*", decimal1)
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = MapBuilder.begin()
        .set("foo", "bar")
        .set("averagePrice", 222)
        .set("averageSales", 7777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .build();
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
        .register(t, decimal1)
        .forTemplate(t, "t0", decimal2)
        .forTemplate(t, "t0.t1", typer)
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = MapBuilder.begin()
        .set("foo", "666")
        .set("averagePrice", 222)
        .set("averageSales", 777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .build();
    rs.insert(map);
    String s = rs.render();
    s = s.replaceAll("\\s+", "|");
    s = s.replaceAll(":\\|", ":");
    assertEquals(
        "foo:666.0000|price:222.0000|sales:777.0000|t0_price:22|t0_sales:77|t1_price:Integer@2|t1_sales:Integer@7|",
        s);
  }

  @Test
  public void test13() throws ParseException {
    String src = "~%foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .configure()
        .forType(Number.class, typer)
        .setType(Float.class, t, "foo")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    rs.set("foo", null);
    assertEquals("[null]", rs.render());
  }

  @Test
  public void test14() throws ParseException {
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
        .register(t, decimal1, "averagePrice")
        .forTemplate(t, "t0", decimal2, "averagePrice")
        .forTemplate(t, "t0.t1", typer, "averagePrice")
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    Map<String, Object> map = MapBuilder.begin()
        .set("foo", "666")
        .set("averagePrice", 222)
        .set("averageSales", 777)
        .set("t0.averagePrice", 22)
        .set("t0.averageSales", 77)
        .set("t0.t1.averagePrice", 2)
        .set("t0.t1.averageSales", 7)
        .build();
    rs.insert(map);
    String s = rs.render();
    s = s.replaceAll("\\s+", "|");
    s = s.replaceAll(":\\|", ":");
    assertEquals(
        "foo:666|price:222.0000|sales:777|t0_price:22|t0_sales:77|t1_price:Integer@2|t1_sales:7|",
        s);
  }

  @Test
  public void test15() throws ParseException {
    String src = "~%html:foo%";
    Template t = Template.fromString(src);
    StringifierRegistry reg = StringifierRegistry
        .cleanSlate()
        .freeze();
    RenderSession rs = t.newRenderSession(reg);
    String s = rs.set("foo", "<td>").render();
    assertEquals("<td>", s);
  }

}
