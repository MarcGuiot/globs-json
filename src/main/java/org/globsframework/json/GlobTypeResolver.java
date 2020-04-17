package org.globsframework.json;

import org.globsframework.metamodel.GlobType;

public interface GlobTypeResolver {
    GlobType find(String name);

    default GlobType get(String name) {
        GlobType globType = find(name);
        if (globType == null) {
            throw new TypeNotFound(name);
        }
        return globType;
    }

    static GlobTypeResolver chain(GlobTypeResolver... resolvers) {
        return name -> {
            for (GlobTypeResolver resolver : resolvers) {
                GlobType globType = resolver.find(name);
                if (globType != null) {
                    return globType;
                }
            }
            return null;
        };
    }

    class TypeNotFound extends RuntimeException {
        public TypeNotFound(String name) {
            super("type '" + name + "' not found.");
        }
    }
}
