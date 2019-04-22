package org.globsframework.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeBuilder;
import org.globsframework.metamodel.annotations.AnnotationModel;
import org.globsframework.metamodel.fields.DoubleField;
import org.globsframework.metamodel.fields.IntegerField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.metamodel.impl.DefaultGlobModel;
import org.globsframework.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.model.Glob;
import org.globsframework.model.MutableGlob;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PerfReadWriteTest {

    static {
        System.setProperty("org.globsframework.builder", "org.globsframework.model.generator.GeneratorGlobFactoryService");
        System.setProperty("globsframework.field.no.check", "true");
    }

    @Test
    public void perf() {
        GlobTypeBuilder globTypeBuilder = DefaultGlobTypeBuilder.init("perf");
        StringField str_1 = globTypeBuilder.declareStringField("str_1");
        StringField str_2 = globTypeBuilder.declareStringField("str_2");
        IntegerField anInt = globTypeBuilder.declareIntegerField("anInt");
        DoubleField aDouble = globTypeBuilder.declareDoubleField("aDouble");

        GlobType globType = globTypeBuilder.get();

        List<MutableGlob> collect = IntStream.range(0, 1000)
                .mapToObj(i ->
                        globType.instantiate()
                .set(str_1, "str_1_" + i)
                .set(str_2, "str_2_" + i)
                .set(anInt, i)
                .set(aDouble, i))
                .collect(Collectors.toList());
        DefaultGlobModel globTypes = new DefaultGlobModel(AnnotationModel.MODEL, globType);
        Gson gson = GlobsGson.create(globTypes::getType);
        String s = "";
        s = write(collect, gson, s);
        s = write(collect, gson, s);
        s = write(collect, gson, s);
        s = write(collect, gson, s);
        s = write(collect, gson, s);
        s = write(collect, gson, s);
        s = write(collect, gson, s);
        read(gson, s);
        read(gson, s);
        read(gson, s);
        read(gson, s);
        read(gson, s);
        read(gson, s);
        read(gson, s);
        read(gson, s);
    }

    private String write(List<MutableGlob> collect, Gson gson, String s) {
        long start = System.nanoTime();
        for (int i = 0 ; i < 1000; i++) {
            s = gson.toJson(collect);
        }
        long end = System.nanoTime();
        System.out.println("write " + (end - start) / 1000000. + "ms");  // 1100ms puis 600ms
        return s;
    }

    private void read(Gson gson, String s) {
        long start = System.nanoTime();
        for (int i = 0 ; i < 1000; i++) {
            List<Glob> globList = gson.fromJson(s, new TypeToken<List<Glob>>(){}.getType());
            Assert.assertEquals(globList.size(), 1000);
        }
        long end = System.nanoTime();
        System.out.println("read " + (end - start) / 1000000. + "ms => " + ((1000. * 1000.) / ((end - start) / 1000000.) * 1000.) + " objects/s");  // 600ms (1.7Millions par second)
    }
}
