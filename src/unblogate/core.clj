(ns unblogate.core
  (:import java.util.Date)
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

;(defprotocol Post
  ;(publish-date [post])
  ;(title [post])
  ;(publish? [post])
  ;(tags [post])
  ;(url [post])
  ;(content [post]))

(defn read-pages [dir]
  (let [files (filter #(and (.isFile %)
                            (.endsWith (.getName %) ".clj"))
                      (file-seq (io/file dir)))]
    (map
      (fn [file]
        (prn (str "Processing " file))
        (load-file (str file)))
      files)))

(defn read-posts [dir]
  (let [files (filter #(and (.isFile %)
                            (.endsWith (.getName %) ".clj"))
                      (file-seq (io/file dir)))]
    (map
      (fn [file]
        (prn (str "Processing " file))
        (load-file (str file)))
      files)))


(defn published-posts [posts]
  (filter :publish? posts))

(defn site-data []
  {:now (Date.)})


(defn post-processor [site-data]
  (fn [post]
    (reduce
      (fn [res [k v]]
        (assoc res k (v site-data)))
      {} post)))

(defn process-posts [posts site-data]
  (let [proc (post-processor site-data)]
    (sort-by #(-> % :publish-date .getTime -)
             (map proc posts))))

(defn process-pages [pages posts site-data]
  (let [site-data (assoc site-data :posts posts)
        proc (post-processor site-data)]
    (map proc pages)))

(defn url-to-path [url]
  (string/replace (str "site/" url) "-" "_"))

(defn save-html [post-or-page]
  (let [{:keys [url content]} post-or-page
        path (url-to-path url)]
    (.mkdirs (.getParentFile (io/file path)))
    (spit path content)))

