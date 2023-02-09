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

import java.io.IOException;
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
    @Value("${PLUGIN_DIRECTORY:null}")
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
        clojureFnInvoke("clojure.core", "load-string",
                String.format("(ns %s) " +
                                "(import %s) " +
                                "(def spring-context (SpringClojure/getApplicationContext)) " +
                                "(defn spring-bean> [$] (.getBean spring-context $))" +
                                "(defmacro invoke> [$ method & args] `(. (spring-bean> ~$) ~method ~@args))"
                        , this.ns, SpringClojure.class.getName()));
        loadPlugins();
        System.out.println(String.format("nrepl server listen on port %s", this.nreplServerPort));
    }

    private IFn clojureFn(String ns, String name) {
        return Clojure.var(ns, name);
    }

    private Object clojureFnInvoke(String ns, String name, Object... args) {

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
                .forEach(path -> {
                    try {
                        String code = Files
                                .lines(path)
                                .collect(Collectors.joining("\n"));
                        clojureFnInvoke("clojure.core", "load-string", String.format("(ns %s)\n%s", this.ns, code));
                        System.out.println(String.format("loaded successfully [%s]", path));
                    } catch (Exception e) {
                        System.err.println(String.format("loaded failure [%s]", path));
                        e.printStackTrace();
                    }
                });
        System.out.println("plugins loaded finish");
    }
    
}
