(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b] ; for b/git-count-revs
            [clojure.string :as str]
            [org.corfield.build :as bb]))

(def lib 'net.clojars.hellonico/jquants-api-jvm)
(def version "0.2.7")
#_ ; alternatively, use MAJOR.MINOR.COMMITS:
(def version (format "1.0.%s" (b/git-count-revs nil)))

(def scm-url "git@github.com:hellonico/jquants-api-jvm.git")

(defn test "Run the tests." [opts]
  (bb/run-tests opts))

(defn sha
  [{:keys [dir path] :or {dir "."}}]
  (-> {:command-args (cond-> ["git" "rev-parse" "HEAD"]
                       path (conj "--" path))
       :dir (.getPath (b/resolve-path dir))
       :out :capture}
      b/process
      :out
      str/trim))

(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))

(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib 
             :version version            
             :scm {:tag (sha nil)
                   :connection (str "scm:git:" scm-url)
                   :developerConnection (str "scm:git:" scm-url)
                   :url scm-url})
      ; (bb/run-tests)
        (b/compile-clj )
      (bb/clean)
      (bb/jar)))

(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn mine " hello " [ opts]
  (let [bopts
        (-> opts
            (assoc :lib lib
                   :version version
                   :main 'hellonico.jquants-api
                   :basis basis
                   :src-dirs ["src"]
                   :class-dir class-dir))]
    (b/compile-clj bopts)
    (bb/jar bopts)
    (bb/install bopts)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))
