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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class GlobGSonDeserializer {
    public static final Gson GSON = new Gson();
    public static final GSonVisitor G_SON_VISITOR = new GSonVisitor() {
        Glob readGlob(JsonObject jsonObject, GlobType globType) {
            return GlobGSonDeserializer.readGlob(jsonObject, globType);
        }
    };
    private static final ReadJsonWithReaderFieldVisitor fieldVisitor = new ReadJsonWithReaderFieldVisitor();
    private static Logger LOGGER = LoggerFactory.getLogger(GlobGSonDeserializer.class);

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

    public static MutableGlob readGlob(JsonObject jsonObject, GlobType globType) {
        MutableGlob instantiate;
        instantiate = globType.instantiate();
        for (Field field : globType.getFields()) {
            JsonElement jsonElement = jsonObject.get(field.getName());
            if (jsonElement != null) {
                field.safeVisit(G_SON_VISITOR, jsonElement, instantiate);
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
                if (in.peek() != JsonToken.NULL) {
                    field.safeVisit(fieldVisitor, instantiate, in);
                } else {
                    in.skipValue();
                    instantiate.setValue(field, null);
                }
            } else {
                in.skipValue();
            }
        }
        return instantiate;
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
                field.safeVisit(G_SON_VISITOR, stringJsonElementEntry.getValue(), instantiate);
            }
        }
        return instantiate;
    }

    private static class ReadJsonWithReaderFieldVisitor implements FieldVisitorWithTwoContext<FieldSetter, JsonReader> {

        public void visitInteger(IntegerField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextInt());
        }

        public void visitIntegerArray(IntegerArrayField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
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

        public void visitDouble(DoubleField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextDouble());
        }

        public void visitDoubleArray(DoubleArrayField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
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

        public void visitString(StringField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
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
                    throw new RuntimeException(parse.toString() + " not managed in " + field.getFullName());
                }
            } else {
                JsonToken peek = jsonReader.peek();
                switch (peek) {
                    case STRING:
                        mutableGlob.set(field, jsonReader.nextString());
                        break;
                    case NUMBER:
                        mutableGlob.set(field, Double.toString(jsonReader.nextDouble()));
                        break;
                    case BOOLEAN:
                        mutableGlob.set(field, Boolean.toString(jsonReader.nextBoolean()));
                        break;
                }
            }
        }

        public void visitStringArray(StringArrayField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
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

        public void visitBoolean(BooleanField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextBoolean());
        }

        public void visitBooleanArray(BooleanArrayField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
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

        public void visitBigDecimal(BigDecimalField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, new BigDecimal(jsonReader.nextString()));
        }

        public void visitBigDecimalArray(BigDecimalArrayField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
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

        public void visitLong(LongField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, jsonReader.nextLong());
        }

        public void visitLongArray(LongArrayField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
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


        public void visitDate(DateField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
            DateTimeFormatter dateConverter = GSonUtils.getCachedDateFormatter(field);
            mutableGlob.set(field, LocalDate.from(dateConverter.parse(jsonReader.nextString())));
        }


        // gestion a revoir

        public void visitDateTime(DateTimeField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
            DateTimeFormatter dateConverter = GSonUtils.getCachedDateTimeFormatter(field);
            String text = jsonReader.nextString();
            if (field.hasAnnotation(JsonDateTimeFormatType.UNIQUE_KEY)) {
                Glob annotation = field.getAnnotation(JsonDateTimeFormatType.UNIQUE_KEY);
                String nullValue = annotation.get(JsonDateTimeFormatType.NULL_VALUE);
                if (text.equals(nullValue) || "".equals(text)) {
                    return;
                }
                Boolean aBoolean = annotation.get(JsonDateTimeFormatType.AS_LOCAL);
                if (aBoolean) {
                    mutableGlob.set(field, ZonedDateTime.of(LocalDateTime.from(dateConverter.parse(text)), ZoneId.systemDefault()));
                    return;
                }
            }
            mutableGlob.set(field, ZonedDateTime.from(dateConverter.parse(text)));
        }

        public void visitBlob(BlobField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
            mutableGlob.set(field, Base64.getDecoder().decode(jsonReader.nextString()));
        }

        public void visitGlob(GlobField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginObject();
            mutableGlob.set(field, readFields(jsonReader, field.getType()));
            jsonReader.endObject();
        }

        public void visitGlobArray(GlobArrayField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
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

        public void visitUnionGlob(GlobUnionField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
            jsonReader.beginObject();
            String name = jsonReader.nextName();
            jsonReader.beginObject();
            GlobType globType = field.get(name);
            mutableGlob.set(field, readFields(jsonReader, globType));
            jsonReader.endObject();
            jsonReader.endObject();
        }

        public void visitUnionGlobArray(GlobArrayUnionField field, FieldSetter mutableGlob, JsonReader jsonReader) throws Exception {
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

}
