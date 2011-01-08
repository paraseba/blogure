(ns blogure.core
  (:import java.util.Date)
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(defn read-pages [dir]
  (let [files (filter #(and (.isFile %)
                            (.endsWith (.getName %) ".clj"))
                      (file-seq (io/file dir)))]
    (map
      (fn [file]
        (println (str "Processing " file))
        (load-file (str file)))
      files)))

(defn read-posts [dir]
  (let [files (filter #(and (.isFile %)
                            (.endsWith (.getName %) ".clj"))
                      (file-seq (io/file dir)))]
    (map
      (fn [file]
        (println (str "Processing " file))
        (load-file (str file)))
      files)))


(defn published-posts [posts]
  (filter :publish? posts))

(defn site-data []
  {:now (Date.)})


(defn process [site-data post]
  (reduce
    (fn [res [k v]]
      (assoc res k (v site-data)))
    {} post))

(defn process-posts [posts site-data]
  (sort-by #(-> % :publish-date .getTime -)
           (pmap (partial process site-data) posts)))

(defn process-pages [pages posts site-data]
  (let [site-data (assoc site-data :posts posts)]
    (pmap (partial process site-data) pages)))

;fixme 
(defn url-to-path [url]
  (string/replace (str "site/" url) "-" "_"))

(defn save-html [base-dir post-or-page]
  (let [{:keys [url content]} post-or-page
        path (io/file base-dir (url-to-path url))]
    (.mkdirs (.getParentFile path))
    (spit (.getAbsolutePath path) content)))

(defn generate [config]
  (let [{:keys [source-dir dest-dir]} config
        posts-dir (io/file source-dir "posts")
        pages-dir (io/file source-dir "public/html")
        data (site-data)
        posts (published-posts (read-posts posts-dir))
        posts (process-posts posts data)
        pages (process-pages (read-pages pages-dir) posts data)]
    (doall (map (partial save-html dest-dir) posts))
    (doall (map (partial save-html dest-dir) pages)))
  (shutdown-agents))

