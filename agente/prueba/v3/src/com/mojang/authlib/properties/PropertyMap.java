package com.mojang.authlib.properties;
import com.google.common.collect.*; import java.util.*;
public final class PropertyMap implements Multimap<String, Property> {
  final LinkedHashMultimap<String, Property> m = LinkedHashMultimap.create();
  public PropertyMap(Multimap<String, Property> o) { m.putAll(o); }
  public boolean containsKey(Object k) { return m.containsKey(k); }
  public boolean put(String k, Property v) { throw new UnsupportedOperationException(); }
  public boolean putAll(Multimap<? extends String, ? extends Property> o) { throw new UnsupportedOperationException(); }
  public Collection<Property> get(String k) { return Collections.unmodifiableCollection(m.get(k)); }
  public Collection<Property> removeAll(Object k) { throw new UnsupportedOperationException(); }
  public Map<String, Collection<Property>> asMap() { return Collections.unmodifiableMap(m.asMap()); }
}
