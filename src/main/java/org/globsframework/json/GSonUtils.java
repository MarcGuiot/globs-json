package org.globsframework.json;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.globsframework.json.annottations.JsonDateFormatType;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.fields.DateField;
import org.globsframework.metamodel.fields.DateTimeField;
import org.globsframework.model.Glob;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GSonUtils {

    public static Map<String, DateTimeFormatter> CACHE_DATE = new ConcurrentHashMap<>();
    public static Map<String, DateTimeFormatter> CACHE_DATE_TIME = new ConcurrentHashMap<>();

    public static Glob decode(Reader reader, GlobType globType) {
        Glob glob = null;
        try {
            JsonReader in = new JsonReader(reader);
            in.beginObject();
            glob = GlobGSonDeserializer.readFields(in, globType);
            in.endObject();
        } catch (IOException e) {
            throw new RuntimeException("Fail to convert to Glob", e);
        }
        return glob;
    }

    public static Glob[] decodeArray(Reader reader, GlobType globType) {
        List<Glob> globs = new ArrayList<>();
        decodeArray(reader, globType, globs::add);
        return globs.toArray(new Glob[0]);
    }

    public static long decodeArray(Reader reader, GlobType globType, Consumer<Glob> consumer) {
        long count = 0;
        try {
            JsonReader in = new JsonReader(reader);
            in.beginArray();
            while (in.peek() != JsonToken.END_ARRAY) {
                in.beginObject();
                Glob e = GlobGSonDeserializer.readFields(in, globType);
                consumer.accept(e);
                count++;
                in.endObject();
            }
        } catch (IOException e) {
            throw new RuntimeException("Fail to convert to Glob", e);
        }
        return count;
    }

    public static String encode(Glob glob, boolean withKind) {
        StringWriter out = new StringWriter();
         encode(out, glob, withKind);
        return out.toString();
    }

    public static String encodeGlobType(GlobType globType) {
        GlobTypeSet globTypeSet = GlobTypeSet.export(globType);
        Gson gson = GlobsGson.create(name -> null);
        return gson.toJson(globTypeSet);
    }

    public static GlobType decodeGlobType(String json, GlobTypeResolver resolver, boolean ignore) {
        Gson gson = GlobsGson.createBuilder(resolver, ignore).create();
        GlobTypeSet globTypeSet = gson.fromJson(json, GlobTypeSet.class);
        return globTypeSet.globType[0];
    }

    public static void encode(Writer out, Glob glob, boolean withKind) {
        try {
            JsonWriter jsonWriter = new JsonWriter(out);
            jsonWriter.beginObject();
            if (withKind) {
                jsonWriter.name(GlobsGson.KIND_NAME).value(glob.getType().getName());
            }
            JsonFieldValueVisitor jsonFieldValueVisitor = new JsonFieldValueVisitor(jsonWriter);
            glob.safeAccept(jsonFieldValueVisitor);
            jsonWriter.endObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static DateTimeFormatter getCachedDateFormatter(DateField field) {
        DateTimeFormatter dateConverter;
        if (field.hasAnnotation(JsonDateFormatType.UNIQUE_KEY)) {
            Glob annotation = field.getAnnotation(JsonDateFormatType.UNIQUE_KEY);
            String pattern = annotation.get(JsonDateFormatType.FORMAT);
            DateTimeFormatter dateTimeFormatter = CACHE_DATE.get(pattern);
            if (dateTimeFormatter == null) {
                dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
                CACHE_DATE.put(pattern, dateTimeFormatter);
            }
            dateConverter = dateTimeFormatter;
        } else {
            dateConverter = DateTimeFormatter.ISO_DATE;
        }
        return dateConverter;
    }

    public static DateTimeFormatter getCachedDateTimeFormatter(DateTimeField field) {
        DateTimeFormatter dateConverter;
        if (field.hasAnnotation(JsonDateTimeFormatType.UNIQUE_KEY)) {
            Glob annotation = field.getAnnotation(JsonDateTimeFormatType.UNIQUE_KEY);
            String pattern = annotation.get(JsonDateTimeFormatType.FORMAT);
            DateTimeFormatter dateTimeFormatter = CACHE_DATE_TIME.get(pattern);
            if (dateTimeFormatter == null) {
                dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
                CACHE_DATE_TIME.put(pattern, dateTimeFormatter);
            }
            dateConverter = dateTimeFormatter;
        } else {
            dateConverter = DateTimeFormatter.ISO_DATE_TIME;
        }
        return dateConverter;
    }

    public static String encode(Glob glob[], boolean withKind) {
        try {
            StringWriter out = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(out);
            JsonFieldValueVisitor jsonFieldValueVisitor = new JsonFieldValueVisitor(jsonWriter);
            jsonWriter.beginArray();
            for (Glob v : glob) {
                jsonWriter.beginObject();
                if (withKind) {
                    jsonWriter.name(GlobsGson.KIND_NAME).value(v.getType().getName());
                }
                v.safeAccept(jsonFieldValueVisitor);
                jsonWriter.endObject();
            }
            jsonWriter.endArray();
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException("In encode", e);
        }
    }

    public static String normalize(String json) {
        JsonParser jsonParser = new JsonParser();
        Gson gson = new Gson();
        return gson.toJson(jsonParser.parse(json));
    }

    public static class WriteGlob {
        private final Writer writer;
        private final JsonFieldValueVisitor jsonFieldValueVisitor;
        private final JsonWriter jsonWriter;
        private boolean withKind;

        public WriteGlob(Writer writer, boolean withKind) {
            this.writer = writer;
            jsonWriter = new JsonWriter(writer);
            this.withKind = withKind;
            jsonFieldValueVisitor = new JsonFieldValueVisitor(jsonWriter);
            try {
                jsonWriter.beginArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void push(Glob glob) {
            try {
                jsonWriter.beginObject();
                if (withKind) {
                    jsonWriter.name(GlobsGson.KIND_NAME).value(glob.getType().getName());
                }
                glob.safeAccept(jsonFieldValueVisitor);
                jsonWriter.endObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void end() {
            try {
                jsonWriter.endArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
