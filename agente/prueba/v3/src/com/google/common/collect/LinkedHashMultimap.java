package com.google.common.collect;
import java.util.*;
public class LinkedHashMultimap<K, V> implements Multimap<K, V> {
  final Map<K, Collection<V>> m = new LinkedHashMap<>();
  public static <K, V> LinkedHashMultimap<K, V> create() { return new LinkedHashMultimap<>(); }
  public boolean containsKey(Object k) { return m.containsKey(k); }
  public boolean put(K k, V v) { return m.computeIfAbsent(k, x -> new ArrayList<>()).add(v); }
  public boolean putAll(Multimap<? extends K, ? extends V> o) { for (Map.Entry<? extends K, ? extends Collection<? extends V>> e : ((Map<? extends K, ? extends Collection<? extends V>>) (Map) o.asMap()).entrySet()) for (V v : e.getValue()) put(e.getKey(), v); return true; }
  public Map<K, Collection<V>> asMap() { return m; }
  public Collection<V> get(K k) { return m.getOrDefault(k, new ArrayList<>()); }
  public Collection<V> removeAll(Object k) { Collection<V> c = m.remove(k); return c == null ? new ArrayList<>() : c; }
}
