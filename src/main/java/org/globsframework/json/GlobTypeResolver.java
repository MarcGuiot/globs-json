package org.globsframework.json;

import org.globsframework.metamodel.GlobType;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    GlobTypeResolver ERROR = name -> {
        throw new TypeNotFound(name);
    };

    static GlobTypeResolver from(GlobType... types){
        Map<String, GlobType> collect = Arrays.asList(types).stream().collect(Collectors.toMap(GlobType::getName, Function.identity()));
        return collect::get;
    }
}
