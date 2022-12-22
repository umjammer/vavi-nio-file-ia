/*
 * prompt://bard.google.com?value=equivalent+for+Lazy+in+java&date=2023-06-03T11:00
 */

package vavi.net.ia.dotnet;

import java.util.function.Supplier;


/**
 * TODO move to dotnet4j
 */
public class Lazy<T> {

    private final Supplier<T> supplier;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }

    private T value;
}
