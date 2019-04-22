package org.globsframework.json;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.*;
import org.globsframework.model.FieldSetter;
import org.globsframework.model.Glob;
import org.globsframework.model.MutableGlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GlobGSonDeserializer {
    private static Logger LOGGER = LoggerFactory.getLogger(GlobGSonDeserializer.class);
    private static final Gson GSON = new Gson();
    private static final GSonVisitor gsonVisitor = new GSonVisitor();
    private static final ReadJsonWithReaderFieldVisitor fieldVisitor = new ReadJsonWithReaderFieldVisitor();

    public GlobGSonDeserializer() {
    }


    public static Glob deserialize(JsonElement json, GlobTypeResolver globTypeResolver) throws JsonParseException {
        if (json == null || json instanceof JsonNull) {
            return null;
        }
        MutableGlob instantiate = null;
        try {
            JsonObject jsonObject = (JsonObject) json;
            String type = jsonObject.get(GlobsGson.KIND_NAME).getAsString();
            GlobType globType = globTypeResolver.get(type);
            instantiate = readGlob(jsonObject, globType);
        } catch (Exception e) {
            LOGGER.error("Fail to parse : " + GSON.toJson(json), e);
            throw e;
        }
        return instantiate;
    }

    private static MutableGlob readGlob(JsonObject jsonObject, GlobType globType) {
        MutableGlob instantiate;
        instantiate = globType.instantiate();
        for (Field field : globType.getFields()) {
            JsonElement jsonElement = jsonObject.get(field.getName());
            if (jsonElement != null) {
                field.safeVisit(gsonVisitor, jsonElement, instantiate);
            }
        }
        return instantiate;
    }

    public static Glob readFields(JsonReader in, GlobType globType) throws IOException {
        MutableGlob instantiate = globType.instantiate();
        while (in.hasNext() && in.peek() == JsonToken.NAME) {
            String name = in.nextName();
            Field field = globType.findField(name);
            if (field != null) {
                field.safeVisit(fieldVisitor, instantiate, in);
            }
            else {
                in.skipValue();
            }
        }
        return instantiate;
    }

    private static class ReadJsonWithReaderFieldVisitor implements FieldVisitorWithTwoContext<MutableGlob, JsonReader> {

        public void visitInteger(IntegerField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextInt());
        }

        public void visitIntegerArray(IntegerArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            int[] values = new int[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextInt();
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        public void visitDouble(DoubleField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextDouble());
        }

        public void visitDoubleArray(DoubleArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            double[] values = new double[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextDouble();
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        public void visitString(StringField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            JsonToken peek = jsonReader.peek();
            if (peek != JsonToken.NULL) {
                if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY)) {
                    JsonParser jsonParser = new JsonParser();
                    JsonElement parse = jsonParser.parse(jsonReader);
                    if (parse.isJsonArray()) {
                        mutableGlob.set(field, GSON.toJson(parse.getAsJsonArray()));
                    } else if (parse.isJsonObject()) {
                        mutableGlob.set(field, GSON.toJson(parse.getAsJsonObject()));
                    } else if (parse.isJsonPrimitive()) {
                        mutableGlob.set(field, GSON.toJson(parse.getAsJsonPrimitive()));
                    } else {
                        throw new RuntimeException(peek + " not managed in " + field.getFullName());
                    }
                } else {
                    mutableGlob.set(field, jsonReader.nextString());
                }
            }
        }

        public void visitStringArray(StringArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            String[] values = new String[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY)) {
                    JsonParser jsonParser = new JsonParser();
                    JsonElement parse = jsonParser.parse(jsonReader);
                    if (parse.isJsonArray()) {
                        values[count++] = GSON.toJson(parse.getAsJsonArray());
                    } else if (parse.isJsonObject()) {
                        values[count++] = GSON.toJson(parse.getAsJsonObject());
                    } else if (parse.isJsonPrimitive()) {
                        values[count++] = GSON.toJson(parse.getAsJsonPrimitive());
                    } else {
                        throw new RuntimeException(parse + " not managed in " + field.getFullName());
                    }
                } else {
                    values[count++] = jsonReader.nextString();
                }
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        public void visitBoolean(BooleanField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextBoolean());
        }

        public void visitBooleanArray(BooleanArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            boolean[] values = new boolean[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextBoolean();
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        public void visitBigDecimal(BigDecimalField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, new BigDecimal(jsonReader.nextString()));
        }

        public void visitBigDecimalArray(BigDecimalArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            BigDecimal[] values = new BigDecimal[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = new BigDecimal(jsonReader.nextString());
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        public void visitLong(LongField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextLong());
        }

        public void visitLongArray(LongArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            long[] values = new long[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                values[count++] = jsonReader.nextLong();
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        public void visitDate(DateField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, LocalDate.from(DateTimeFormatter.ISO_DATE.parse(jsonReader.nextString())));
        }

        public void visitDateTime(DateTimeField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(jsonReader.nextString())));
        }

        public void visitBlob(BlobField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, Base64.getDecoder().decode(jsonReader.nextString()));
        }

        public void visitGlob(GlobField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginObject();
            mutableGlob.set(field, readFields(jsonReader, field.getType()));
            jsonReader.endObject();
        }

        public void visitGlobArray(GlobArrayField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            Glob[] values = new Glob[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                jsonReader.beginObject();
                values[count++] = readFields(jsonReader, field.getType());
                jsonReader.endObject();
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }

        public void visitUnionGlob(GlobUnionField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginObject();
            String name = jsonReader.nextName();
            jsonReader.beginObject();
            GlobType globType = field.get(name);
            mutableGlob.set(field, readFields(jsonReader, globType));
            jsonReader.endObject();
            jsonReader.endObject();
        }

        public void visitUnionGlobArray(GlobArrayUnionField field, MutableGlob mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginArray();
            Glob[] values = new Glob[16];
            int count = 0;
            while (jsonReader.peek() != JsonToken.END_ARRAY) {
                if (values.length == count) {
                    values = Arrays.copyOf(values, values.length * 2);
                }
                jsonReader.beginObject();
                String name = jsonReader.nextName();
                jsonReader.beginObject();
                values[count++] = readFields(jsonReader, field.get(name));
                jsonReader.endObject();
                jsonReader.endObject();
            }
            jsonReader.endArray();
            mutableGlob.set(field, Arrays.copyOf(values, count));
        }
    }

    public static Glob read(JsonReader in, GlobTypeResolver resolver) throws IOException {
        in.beginObject();
        if (in.hasNext() && in.peek() != JsonToken.END_OBJECT) {
            String name = in.nextName();
            if (name.equalsIgnoreCase(GlobsGson.KIND_NAME)) {
                String kind = in.nextString();
                Glob glob = readFields(in, resolver.get(kind));
                in.endObject();
                return glob;
            } else {
                return readFieldByField(in, name, resolver);
            }
        }
        return null;
    }

    private static Glob readFieldByField(JsonReader in, String name, GlobTypeResolver resolver) throws IOException {
        JsonParser jsonParser = new JsonParser();
        Map<String, JsonElement> values = new HashMap<>();
        values.put(name, jsonParser.parse(in));
        while (in.peek() != JsonToken.END_OBJECT) {
            values.put(in.nextName(), jsonParser.parse(in));
        }
        in.endObject();
        JsonElement kindElement = values.get(GlobsGson.KIND_NAME);
        if (kindElement == null) {
            throw new RuntimeException("kind not found in " + values);
        }
        GlobType type = resolver.get(kindElement.getAsString());
        MutableGlob instantiate = type.instantiate();
        for (Map.Entry<String, JsonElement> stringJsonElementEntry : values.entrySet()) {
            Field field = type.findField(stringJsonElementEntry.getKey());
            if (field != null) {
                field.safeVisit(gsonVisitor, stringJsonElementEntry.getValue(), instantiate);
            }
        }
        return instantiate;
    }

     static class GSonVisitor implements FieldVisitorWithTwoContext<JsonElement, FieldSetter> {

         public void visitInteger(IntegerField integerField, JsonElement element, FieldSetter fieldSetter) {
            fieldSetter.set(integerField, element.getAsInt());
        }

        public void visitIntegerArray(IntegerArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            int[] value = new int[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = jsonElement.getAsInt();
            }
            fieldSetter.set(field, value);
        }

        public void visitDouble(DoubleField doubleField, JsonElement element, FieldSetter fieldSetter) {
            fieldSetter.set(doubleField, element.getAsDouble());
        }

        public void visitDoubleArray(DoubleArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            double[] value = new double[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = jsonElement.getAsDouble();
            }
            fieldSetter.set(field, value);
        }

        public void visitString(StringField stringField, JsonElement element, FieldSetter fieldSetter) {
            if (stringField.hasAnnotation(IsJsonContentType.UNIQUE_KEY) || element.isJsonObject() || element.isJsonArray()) {
                if (element.isJsonObject()) {
                    JsonObject jsonObject = element.getAsJsonObject();
                    if (jsonObject != null) {
                        fieldSetter.set(stringField, GSON.toJson(jsonObject));
                    }
                } else if (element.isJsonArray()) {
                    JsonArray jsonArray = element.getAsJsonArray();
                    if (jsonArray != null) {
                        fieldSetter.set(stringField, GSON.toJson(jsonArray));
                    }
                }
            } else {
                fieldSetter.set(stringField, element.getAsString());
            }
        }

        public void visitStringArray(StringArrayField field, JsonElement arrayElements, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = arrayElements.getAsJsonArray();
            String value[] = new String[asJsonArray.size()];
            int i = 0;
            for (JsonElement element : asJsonArray) {
                if (field.hasAnnotation(IsJsonContentType.UNIQUE_KEY) || element.isJsonObject() || element.isJsonArray()) {
                    if (element.isJsonObject()) {
                        JsonObject jsonObject = element.getAsJsonObject();
                        if (jsonObject != null) {
                            value[i] = GSON.toJson(jsonObject);
                        }
                    } else if (element.isJsonArray()) {
                        JsonArray jsonArray = element.getAsJsonArray();
                        if (jsonArray != null) {
                            value[i] = GSON.toJson(jsonArray);
                        }
                    }
                } else {
                    value[i] = element.getAsString();
                }
                ++i;
            }
            fieldSetter.set(field, value);
        }

        public void visitBoolean(BooleanField booleanField, JsonElement element, FieldSetter fieldSetter) {
            fieldSetter.set(booleanField, element.getAsBoolean());
        }

        public void visitBooleanArray(BooleanArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            boolean[] value = new boolean[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = jsonElement.getAsBoolean();
            }
            fieldSetter.set(field, value);
        }

        public void visitBigDecimal(BigDecimalField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            fieldSetter.set(field, element.getAsBigDecimal());
        }

        public void visitBigDecimalArray(BigDecimalArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            BigDecimal[] value = new BigDecimal[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = jsonElement.getAsBigDecimal();
            }
            fieldSetter.set(field, value);

        }

        public void visitLong(LongField longField, JsonElement element, FieldSetter fieldSetter) {
            fieldSetter.set(longField, element.getAsLong());
        }

        public void visitLongArray(LongArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            long[] value = new long[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = jsonElement.getAsLong();
            }
            fieldSetter.set(field, value);
        }

        public void visitDate(DateField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            fieldSetter.set(field, LocalDate.from(DateTimeFormatter.ISO_DATE.parse(element.getAsString())));
        }

        public void visitDateTime(DateTimeField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            fieldSetter.set(field, ZonedDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(element.getAsString())));
        }

        public void visitBlob(BlobField blobField, JsonElement element, FieldSetter fieldSetter) {
            fieldSetter.set(blobField,
                    Base64.getDecoder().decode(element.getAsString()));
        }

        public void visitGlob(GlobField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            fieldSetter.set(field, readGlob(element.getAsJsonObject(), field.getType()));
        }

        public void visitGlobArray(GlobArrayField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            Glob[] value = new Glob[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = readGlob(jsonElement.getAsJsonObject(), field.getType());
            }
            fieldSetter.set(field, value);
        }

        public void visitUnionGlob(GlobUnionField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonObject asJsonObject = element.getAsJsonObject();
            Collection<GlobType> types = field.getTypes();
            for (GlobType type : types) {
                if (asJsonObject.has(type.getName())) {
                    fieldSetter.set(field, readGlob(asJsonObject.getAsJsonObject(type.getName()), type));
                    return;
                }
            }
            throw new RuntimeException("For " + field.getFullName() + " one of " + field.getTypes() + " is expected");
        }

        public void visitUnionGlobArray(GlobArrayUnionField field, JsonElement element, FieldSetter fieldSetter) throws Exception {
            JsonArray asJsonArray = element.getAsJsonArray();
            Glob[] value = new Glob[asJsonArray.size()];
            int i = 0;
            for (JsonElement jsonElement : asJsonArray) {
                value[i++] = readInnerGlob(field, jsonElement);
            }
            fieldSetter.set(field, value);
        }

         private MutableGlob readInnerGlob(GlobArrayUnionField field, JsonElement element) {
             JsonObject asJsonObject = element.getAsJsonObject();
             Collection<GlobType> types = field.getTypes();
             for (GlobType type : types) {
                 if (asJsonObject.has(type.getName())) {
                     return readGlob(asJsonObject.getAsJsonObject(type.getName()), type);
                 }
             }
             throw new RuntimeException("For " + field.getFullName() + " one of " + field.getTypes() +
                     " is expected got : " + asJsonObject);
         }
     }
}
