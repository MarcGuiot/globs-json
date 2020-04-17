package org.globsframework.json;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.globsframework.metamodel.GlobType;
import org.globsframework.model.Key;

import java.io.IOException;

public class KeyGsonAdapter {

    public static void write(JsonWriter out, Key value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        GlobType type = value.getGlobType();
        out.name(GlobsGson.KIND_NAME).value(type.getName());
        value.safeAcceptOnKeyField(new JsonFieldValueVisitor(out));
        out.endObject();
    }

    public static Key read(JsonReader in, GlobTypeResolver resolver) throws IOException {
        return GlobGSonDeserializer.readKey(in, resolver);
    }
}
