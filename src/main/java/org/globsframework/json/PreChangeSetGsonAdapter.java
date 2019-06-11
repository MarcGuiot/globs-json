package org.globsframework.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.GlobArrayField;
import org.globsframework.metamodel.fields.GlobArrayUnionField;
import org.globsframework.metamodel.fields.GlobField;
import org.globsframework.metamodel.fields.GlobUnionField;
import org.globsframework.model.*;
import org.globsframework.model.delta.DefaultFixStateChangeSet;
import org.globsframework.model.delta.DeltaGlob;
import org.globsframework.model.delta.FixStateChangeSet;
import org.globsframework.utils.exceptions.ItemNotFound;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class PreChangeSetGsonAdapter extends TypeAdapter<PreChangeSet> {
    private final GlobTypeResolver resolver;

    public PreChangeSetGsonAdapter(GlobTypeResolver resolver) {
        this.resolver = resolver;
    }

    public void write(JsonWriter out, PreChangeSet changeSet) throws IOException {
        throw new RuntimeException("A preChangedSet is not exportable , use changeSet");
    }

    public PreChangeSet read(JsonReader in) throws IOException {
        FixStateChangeSet changeSet = new DefaultFixStateChangeSet();
        JsonParser jsonParser = new JsonParser();
        Jsonreader jsonreader = new Jsonreader();
        in.beginArray();

        while (in.peek() == JsonToken.BEGIN_OBJECT) {
            JsonElement elt = jsonParser.parse(in);
            JsonObject jsonObject = elt.getAsJsonObject();
            String state = jsonObject.get("state").getAsString();
            String kind = jsonObject.get("_kind").getAsString();
            GlobType globType = resolver.get(kind);
            JsonObject key = jsonObject.get("key").getAsJsonObject();
            Key readKey = readKey(key, globType, globType.getKeyFields(), jsonreader);
            switch (state) {
                case "create":
                    DeltaGlob valuesForCreate = changeSet.getForCreate(readKey);
                    readValues(jsonObject.getAsJsonObject("newValue"), globType, valuesForCreate::setValue, jsonreader);
                    break;
                case "update":
                    DeltaGlob values = changeSet.getForUpdate(readKey);
                    readValues(jsonObject.getAsJsonObject("newValue"), globType, values::setValue, jsonreader);
                    readValues(jsonObject.getAsJsonObject("oldValue"), globType, values::setPreviousValue, jsonreader);
                    break;
                case "delete":
                    DeltaGlob newValues = changeSet.getForDelete(readKey);
                    readValues(jsonObject.getAsJsonObject("oldValue"), globType, newValues::setValue, jsonreader);
                    break;
                default:
                    throw new RuntimeException("'" + state + "' not expected (create/delete/update)");
            }
        }
        in.endArray();
        return new PreChangeSet() {
            Map<Key, Glob> local = new HashMap<>();

            public ChangeSet resolve(GlobAccessor globAccessor) {
                jsonreader.functions.forEach(g -> g.apply(key -> {
                    Glob glob = local.get(key);
                    if (glob != null) {
                        return glob;
                    }
                    if (changeSet.isCreated(key)) {
                        MutableGlob instantiate = key.getGlobType().instantiate();
                        changeSet.getNewValues(key).safeApply(instantiate::setValue);
                        local.put(key, instantiate);
                        return instantiate;
                    } else {
                        return globAccessor.get(key);
                    }
                }));

                return changeSet;
            }
        };
    }

    Key readKey(JsonObject jsonObject, GlobType globType, Field[] fields, Jsonreader jsonreader) {
        KeyBuilder keyBuilder = KeyBuilder.create(globType);
        for (Field field : fields) {
            JsonElement jsonElement = jsonObject.get(field.getName());
            if (jsonElement != null && !jsonElement.isJsonNull()) {
                field.safeVisit(jsonreader, jsonElement, keyBuilder);
            }
        }
        return keyBuilder.get();
    }

    void readValues(JsonObject jsonObject, GlobType globType, FieldValueSetter values, Jsonreader jsonreader) {
        Set<Map.Entry<String, JsonElement>> entries = jsonObject.entrySet();
        for (Map.Entry<String, JsonElement> entry : entries) {
            Field field = globType.findField(entry.getKey());
            if (field == null) {
                String message = entry.getKey() + " not found in " + globType.getName() + " got " + Arrays.toString(globType.getFields());
                message += " from " + jsonObject;
                throw new RuntimeException(message);
            }
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull()) {
                values.setValue(field, null);
            } else {
                field.safeVisit(jsonreader, value, new AbstractFieldSetter() {
                    public FieldSetter setValue(Field field1, Object value) throws ItemNotFound {
                        values.setValue(field1, value);
                        return this;
                    }
                });
            }
        }
    }

    interface FieldValueSetter {
        void setValue(Field field, Object value);
    }

    static class Jsonreader extends GSonVisitor {

        List<Function<GlobAccessor, Void>> functions = new ArrayList<>();


        Glob readGlob(JsonObject jsonObject, GlobType globType) {
            throw new RuntimeException("Bug a glob should not be created in a changeSet");
//            Field[] keyFields = globType.getKeyFields();
//            KeyBuilder keyBuilder = KeyBuilder.create(globType);
//            if (keyFields.length != 0) {
//                for (Field keyField : keyFields) {
//                    keyField.safeVisit(this, jsonObject.get(keyField.getName()), keyBuilder);
//                }
//                return globAccessor.get(keyBuilder.get());
//            } else {
//                return readGlob(jsonObject, globType);
//            }
        }

        Key readKey(JsonObject jsonObject, GlobType globType) {
            Field[] keyFields = globType.getKeyFields();
            KeyBuilder keyBuilder = KeyBuilder.create(globType);
            if (keyFields.length != 0) {
                for (Field keyField : keyFields) {
                    keyField.safeVisit(this, jsonObject.get(keyField.getName()), keyBuilder);
                }
                return keyBuilder.get();
            } else {
                throw new RuntimeException("Only key object with key are expected " + globType.getName());
            }
        }

        public void visitGlobArray(GlobArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            List<Key> keys = new ArrayList<>();
            for (JsonElement jsonElement : element.getAsJsonArray()) {
                Key key = readKey(jsonElement.getAsJsonObject(), field.getType());
                keys.add(key);
            }
            functions.add(new Function<GlobAccessor, Void>() {
                public Void apply(GlobAccessor globAccessor) {
                    Glob[] values = new Glob[keys.size()];
                    int i = 0;
                    for (Key key : keys) {
                        values[i++] = globAccessor.get(key);
                    }
                    fieldSetter.set(field, values);
                    return null;
                }
            });
        }

        public void visitUnionGlob(GlobUnionField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            for (GlobType type : field.getTypes()) {
                JsonElement jsonElement = element.getAsJsonObject().get(type.getName());
                if (jsonElement != null) {
                    Key key = readKey(element.getAsJsonObject(), type);
                    functions.add(globAccessor -> {
                        fieldSetter.set(field, globAccessor.get(key));
                        return null;
                    });
                }
            }
        }

        public void visitUnionGlobArray(GlobArrayUnionField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            List<Key> keys = new ArrayList<>();
            for (JsonElement arrayElements : element.getAsJsonArray()) {
                for (GlobType type : field.getTypes()) {
                    JsonElement jsonElement = arrayElements.getAsJsonObject().get(type.getName());
                    if (jsonElement != null) {
                        Key key = readKey(element.getAsJsonObject(), type);
                        keys.add(key);
                    }
                }
            }
            functions.add(globAccessor -> {
                Glob[] values = new Glob[keys.size()];
                int i = 0;
                for (Key key : keys) {
                    values[i++] = globAccessor.get(key);
                }
                fieldSetter.set(field, values);
                return null;
            });
        }

        public void visitGlob(GlobField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            Key key = readKey(element.getAsJsonObject(), field.getType());
            functions.add(globAccessor -> {
                fieldSetter.set(field, globAccessor.get(key));
                return null;
            });
        }
    }
}


