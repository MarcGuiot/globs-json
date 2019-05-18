package org.globsframework.json.helper;

import com.google.gson.*;
import org.globsframework.json.GlobTypeResolver;
import org.globsframework.json.GlobsGson;
import org.globsframework.metamodel.GlobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LoadingGlobTypeResolver implements GlobTypeResolver {
    static private final Logger LOGGER = LoggerFactory.getLogger(LoadingGlobTypeResolver.class);
    private Gson gson;
    private Map<String, JsonObject> typesToLoad;
    private Map<String, GlobType> created = new HashMap<>();
    private GlobTypeAccessor globTypeResolver;

    private LoadingGlobTypeResolver(GlobTypeAccessor globTypeResolver,
                                   Map<String, JsonObject> typesToLoad) {
        this.globTypeResolver = globTypeResolver;
        this.typesToLoad = typesToLoad;
    }

    static public Builder builder(GlobTypeAccessor globTypeResolver) {
        return new Builder(globTypeResolver);
    }

    public static class Builder {
        Map<String, JsonObject> typeToJsonObject = new HashMap<>();
        private GlobTypeAccessor globTypeResolver;

        private Builder(GlobTypeAccessor globTypeResolver) {
            this.globTypeResolver = globTypeResolver;
        }

        public void add(Reader reader) {
            extractJsonType(reader, typeToJsonObject);
        }

        public Collection<GlobType> read() {
            LoadingGlobTypeResolver loadingGlobTypeResolver = new LoadingGlobTypeResolver(globTypeResolver::find, typeToJsonObject);
            Gson gson = GlobsGson.create(loadingGlobTypeResolver);
            return loadingGlobTypeResolver.load(gson);
        }

    }

    public static Collection<GlobType> parse(Reader reader, GlobTypeAccessor globTypeResolver) {
        Builder builder = new Builder(globTypeResolver);
        builder.add(reader);
        return builder.read();
    }

    private static void extractJsonType(Reader streamReader, Map<String, JsonObject> jsonTypes) {
        JsonElement root = new JsonParser().parse(streamReader);
        if (root.isJsonArray()) {
            JsonArray arrayOfJsonGlobType = root.getAsJsonArray();
            for (JsonElement element : arrayOfJsonGlobType) {
                JsonObject jsonGlobType = element.getAsJsonObject();
                String kind = jsonGlobType.get(GlobsGson.GLOB_TYPE_KIND).getAsString();
                jsonTypes.put(kind, jsonGlobType);
            }
        } else if (root.isJsonObject()) {
            JsonObject asJsonObject = root.getAsJsonObject();
            String kind = asJsonObject.get(GlobsGson.GLOB_TYPE_KIND).getAsString();
            jsonTypes.put(kind, asJsonObject);
        }
        else if (root.isJsonNull()){
            //ignore
        }
        else{
            throw new RuntimeException(root.toString() + " should be an array of glob type or a glob type");
        }
    }

    public GlobType get(String s) {
        GlobType globType = created.get(s);
        if (globType != null) {
            return globType;
        }
        GlobType wanted = globTypeResolver.find(s);
        if (wanted == null) {
            JsonObject typeToLoad = typesToLoad.get(s);
            wanted = gson.fromJson(typeToLoad, GlobType.class);
            created.put(s, wanted);
        }
        if (wanted == null) {
            String msg = "type " + s + " not found.";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        return wanted;
    }

    public Collection<GlobType> load(Gson gson) {
        this.gson = gson;
        for (String s : typesToLoad.keySet()) {
            created.put(s, get(s)); // force adding it because created do not contains local type (from GlobTypeResolver)
        }
        return created.values();
    }

}
