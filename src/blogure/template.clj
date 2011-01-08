(ns blogure.template
  (:use [clojure.java.io :only (file)]
        [clojure.contrib.strint :only (<<)])
  (:gen-class))

(defn- create-dir [base-path path]
  (.mkdirs (file base-path path)))

(defn- config-template [base-path]
  (<< "
; Blogure configuration
; ---------------------
;
; Edit this file according to your needs


{
  :source-dir \"~(.getAbsolutePath base-path)\"
  :dest-dir \"~(.getAbsolutePath (file base-path \"site\"))\"
}"))

(defn- script-template [base-path]
"
#!/bin/bash

java -cp \"src:classes:lib/*\" blogure.generate blogure_config.clj")

(defn- create-config [base-path]
  (spit (file base-path "blogure_config.clj")
        (config-template base-path)))

(defn- create-script [base-path]
  (spit (file base-path "scripts/generate")
        (script-template base-path)))

(defn -main [& base-path]
  (let [base-path (file (or base-path "."))]
    (doseq [path ["public/html" "public/css" "posts" "scripts"]]
      (create-dir base-path path))
    (create-script base-path)
    (create-config base-path)))
