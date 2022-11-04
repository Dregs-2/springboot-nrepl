package debug;

import clojure.java.api.Clojure;
import clojure.lang.Keyword;
import clojure.lang.Symbol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

@ConditionalOnExpression("${NREPL_ENABLE:false}")
@Configuration
public class SpringClojure implements ApplicationRunner {

    @Value("${NREPL_PORT:7888}")
    private int nreplServerPort;
    @Value("${NREPL_NS:user}")
    private String ns;

    @Autowired
    private void setApplicationContext(ApplicationContext $applicationContext){
        applicationContext = $applicationContext;
    }

    private static ApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext(){
        return applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("nrepl server starting...");
        Clojure.var("clojure.core", "require").invoke(Symbol.intern("nrepl.server"));
        Clojure.var("nrepl.server", "start-server").invoke(Keyword.intern("port"), this.nreplServerPort);
        Clojure.var("clojure.core", "load-string").invoke(String.format("(ns %s) (import debug.SpringClojure) (def spring-context (debug.SpringClojure/getApplicationContext)) (defn bean> [^String name] (.getBean spring-context name))", this.ns));
        System.out.println(String.format("nrepl server listen on port %s", this.nreplServerPort));
    }

}
