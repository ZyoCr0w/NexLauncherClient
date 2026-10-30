package com.google.common.collect;
public interface Multimap<K, V> { boolean containsKey(Object k); boolean put(K k, V v); boolean putAll(Multimap<? extends K, ? extends V> m); java.util.Map<K, java.util.Collection<V>> asMap(); java.util.Collection<V> get(K k); java.util.Collection<V> removeAll(Object k); }
