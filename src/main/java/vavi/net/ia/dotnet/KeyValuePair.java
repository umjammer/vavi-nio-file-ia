/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.net.ia.dotnet;


import java.util.Map;


/**
 * KeyValuePair.
 *
 * TODO move to dotnet4j
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-12-18 nsano initial version <br>
 */
public class KeyValuePair<K, V> implements Map.Entry<K, V> {
    K key;
    V value;
    public KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
    }
    public K getKey() { return key; }
    public V getValue() { return value; }

    @Override
    public V setValue(V value) {
        return this.value = value;
    }
}
