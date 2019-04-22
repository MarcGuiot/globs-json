package org.globsframework.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.globsframework.metamodel.GlobType;
import org.globsframework.model.Glob;

import java.io.IOException;

class GlobGsonAdapter extends TypeAdapter<Glob> {
    private final GlobTypeResolver resolver;

    public GlobGsonAdapter(GlobTypeResolver resolver) {
        this.resolver = resolver;
    }

    public void write(JsonWriter out, Glob value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.beginObject();
        GlobType type = value.getType();
        out.name(GlobsGson.KIND_NAME).value(type.getName());
        value.safeAccept(new JsonFieldValueVisitor(out));
        out.endObject();
    }

    public Glob read(JsonReader in) throws IOException {
        return GlobGSonDeserializer.read(in, this.resolver);
    }
}
