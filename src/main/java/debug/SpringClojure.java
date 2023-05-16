package debug;


import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.ISeq;
import clojure.lang.Keyword;
import clojure.lang.PersistentVector;
import clojure.lang.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@ConditionalOnExpression("${NREPL_ENABLE:false}")
@Configuration
public class SpringClojure implements ApplicationRunner {

    @Value("${NREPL_PORT:7888}")
    private int nreplServerPort;
    @Value("${NREPL_NS:user}")
    private String ns;
    @Value("${NREPL_PLUGIN_DIRECTORY:}")
    private String pluginDirectory;

    @Autowired
    private void setApplicationContext(ApplicationContext $applicationContext) {
        applicationContext = $applicationContext;
    }

    private static ApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        System.out.println("nrepl server starting...");
        clojureFnInvoke("clojure.core", "require", Symbol.intern("nrepl.server"));
        clojureFnInvoke("nrepl.server", "start-server", Keyword.intern("port"), this.nreplServerPort);
        System.out.println("nrepl server listen on port " + this.nreplServerPort);
        String builtIn = StreamUtils.copyToString(ResourceUtils.getURL("classpath:builtIn.clj").openStream(), StandardCharsets.UTF_8);
        clojureFnInvoke("clojure.core", "load-string",
                String.format("(ns %s)\n(import %s)\n%s", this.ns, SpringClojure.class.getName(), builtIn));
        loadPlugins();

    }

    private static IFn clojureFn(String ns, String name) {
        return Clojure.var(ns, name);
    }

    private static Object clojureFnInvoke(String ns, String name, Object... args) {

        IFn iFn = clojureFn(ns, name);

        ISeq argsSeq = Optional
                .of(args)
                .map(Arrays::stream)
                .map(stream -> stream.collect(Collectors.toList()))
                .map(PersistentVector::create)
                .map(PersistentVector::seq)
                .orElse(null);

        return null == argsSeq ? iFn.invoke() : iFn.applyTo(argsSeq);
    }

    private void loadPlugins() throws IOException {
        if (null == this.pluginDirectory || this.pluginDirectory.trim().isEmpty()) {
            return;
        }
        Path pluginDirectoryPath = Paths.get(this.pluginDirectory);
        if (!Files.exists(pluginDirectoryPath)) {
            System.err.println(String.format("[%s] does not exist", pluginDirectoryPath));
            return;
        }
        if (!Files.isDirectory(pluginDirectoryPath)) {
            System.err.println(String.format("[%s] not a directory", pluginDirectoryPath));
            return;
        }
        System.out.println("start loading plugins...");
        Files
                .walk(Paths.get(this.pluginDirectory))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".clj"))
                .sorted()
                .forEach(path -> SpringClojure.load(this.ns, path));
        System.out.println("plugins loaded finish");
    }

    public static void load(String ns, String path) {
        load(ns, Paths.get(path));
    }

    public static void load(String ns, Path path) {
        try {
            String code = Files
                    .lines(path)
                    .collect(Collectors.joining("\n"));
            clojureFnInvoke("clojure.core", "load-string", String.format("(ns %s)\n%s", ns, code));
            System.out.println(String.format("loaded successfully [%s]", path));
        } catch (Exception e) {
            System.err.println(String.format("loaded failure [%s]", path));
            e.printStackTrace();
        }
    }

}
