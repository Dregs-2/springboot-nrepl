(require '[clojure.reflect :as clojure-reflect])
(require '[clojure.pprint :as clojure-pprint])
(def ^:built-in ^:ls spring-context (SpringClojure/getApplicationContext))
(defn ^:built-in ^:ls spring-bean> [$] (.getBean spring-context $))

(defmacro ^:built-in ^:ls try>
  "print out the detailed exception stack\n\nuser=> (/ 1 0)\nExecution error (ArithmeticException) at user/eval2398 (REPL:1).\nDivide by zero\n\nuser=> (try> (/ 1 0))\njava.lang.ArithmeticException: Divide by zero\n at clojure.lang.Numbers.divide (Numbers.java:190)\n    clojure.lang.Numbers.divide (Numbers.java:3911)\n    user$eval2400.invokeStatic (NO_SOURCE_FILE:1)\n    user$eval2400.invoke (NO_SOURCE_FILE:1)\n    clojure.lang.Compiler.eval (Compiler.java:7194)\n    clojure.lang.Compiler.eval (Compiler.java:7149)\n    clojure.core$eval.invokeStatic (core.clj:3215)\n    clojure.core$eval.invoke (core.clj:3211)\n"
  [& form]
  `(try ~@form (catch Exception e# (clojure.stacktrace/print-stack-trace e#))))

(defmacro ^:built-in ^:ls invoke> [$ method & args]
  (if (some->> $ eval clojure-reflect/reflect :members (filter #(= method (:name %))) first :flags (#(contains? % :static)))
    `(try> (. ~$ ~method ~@args))
    `(try> (. (spring-bean> ~$) ~method ~@args))))

(defn ^:built-in ^:ls class-method> [class#]
  (clojure-pprint/print-table [:name :parameter-types :return-type] (sort-by :name (filter #(-> % :flags (contains? :public)) (filter :return-type (:members (clojure.reflect/reflect class#))))))
  )
(defmacro ^:built-in ^:ls safe-def> [import-classes-str & body]
  (let [import-classes-sym (mapv symbol import-classes-str)
        import-classes (mapv resolve import-classes-sym)]
    (when-not (contains? (set import-classes) nil)
      `(let [*import-classes-sym* (vector ~@import-classes-sym)] ~@body)
      )
    )
  )

(defn built-in []
  (->> (all-ns) (mapcat ns-map) (map second) distinct (filter #(:ls (meta %))))
  )
(defn ls []
  (->> (all-ns) (mapcat ns-map) (map second) distinct (filter #(:ls (meta %))))
  )
