package org.globsframework.json;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.globsframework.metamodel.Field;
import org.globsframework.metamodel.GlobType;
import org.globsframework.model.Glob;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class GSonUtils {

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
        List<Glob> glob = new ArrayList<>();
        try {
            JsonReader in = new JsonReader(reader);
            in.beginArray();
            while (in.peek() != JsonToken.END_ARRAY) {
                in.beginObject();
                glob.add(GlobGSonDeserializer.readFields(in, globType));
                in.endObject();
            }
        } catch (IOException e) {
            throw new RuntimeException("Fail to convert to Glob", e);
        }
        return glob.toArray(new Glob[0]);
    }

    public static String encode(Glob glob, boolean withKind) {
        try {
            StringWriter out = new StringWriter();
            JsonWriter jsonWriter = new JsonWriter(out);
            JsonFieldValueVisitor jsonFieldValueVisitor = new JsonFieldValueVisitor(jsonWriter);
            jsonWriter.beginObject();
            if (withKind) {
                jsonWriter.name(GlobsGson.KIND_NAME).value(glob.getType().getName());
            }
            glob.safeAccept(jsonFieldValueVisitor);
            jsonWriter.endObject();
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
}
