package org.globsframework.json;

import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.GlobArrayField;
import org.globsframework.metamodel.fields.GlobArrayUnionField;
import org.globsframework.metamodel.fields.GlobField;
import org.globsframework.metamodel.fields.GlobUnionField;
import org.globsframework.model.Glob;

import java.util.*;

public class GlobTypeSet {
    public final GlobType[] globType;

    public GlobTypeSet(GlobType[] globType) {
        this.globType = globType;
    }

    public static GlobTypeSet export(GlobType globType) {
        Set<GlobType> types = new HashSet<>();
        add(globType, types);
        GlobType[] globTypes = new GlobType[types.size()];
        types.remove(globType);
        globTypes[0] = globType;
        int i = 1;
        for (GlobType type : types) {
            globTypes[i++] = type;
        }
        return new GlobTypeSet(globTypes);
    }

    private static void add(GlobType globType, Set<GlobType> types) {
        if (!types.add(globType)) {
            return;
        }
        globType.streamAnnotations().map(Glob::getType).forEach(types::add);
        Field[] fields = globType.getFields();
        for (Field field : fields) {
            field.streamAnnotations().map(Glob::getType).forEach(types::add);
            if (field instanceof GlobArrayField) {
                add(((GlobArrayField) field).getType(), types);
            }
            if (field instanceof GlobField) {
                add(((GlobField) field).getType(), types);
            }
            if (field instanceof GlobUnionField) {
                Collection<GlobType> subType = ((GlobUnionField) field).getTypes();
                for (GlobType type : subType) {
                    add(type, types);
                }
            }
            if (field instanceof GlobArrayUnionField) {
                Collection<GlobType> subType = ((GlobArrayUnionField) field).getTypes();
                for (GlobType type : subType) {
                    add(type, types);
                }
            }
        }
    }
}
