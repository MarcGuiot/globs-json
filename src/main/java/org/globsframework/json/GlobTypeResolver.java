package org.globsframework.json;

import org.globsframework.metamodel.GlobType;

public interface GlobTypeResolver {
    GlobType get(String name);
}
