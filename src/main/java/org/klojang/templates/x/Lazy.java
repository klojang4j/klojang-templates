package org.klojang.templates.x;

import org.klojang.templates.VarGroup;

import java.util.function.Supplier;

public record Lazy(Supplier<Object> value, VarGroup varGroup) {

}
