(ns blogure.core
  (:import java.util.Date)
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.contrib.graph :as gr]
            [clojure.contrib.with-ns :as cns]))

(defn- is-clojure? [file]
  (.endsWith (.getName file) ".clj"))

(defn separate [f coll]
  [(filter f coll)
   (filter (complement f) coll)])

(defn glob [dir]
  (let [files (filter (memfn isFile) (file-seq (io/file dir)))]
    (separate is-clojure? files)))

(defn- read-file [f]
  (cns/with-temp-ns
    (load-file (str f))
    (pages)))

(defn published-posts [posts]
  (filter :publish? posts))

(defn site-data []
  {:now (Date.)})

(defn process [site-data post]
  (reduce
    (fn [res [k v]]
      (assoc res k (v site-data)))
    {}
    post))

(defn- first-pass-processor [site-data]
  (fn [post]
    (merge
      post
      (reduce
        (fn [res [k v]]
          (assoc res k (v site-data)))
        {}
        (dissoc post :content)))))

(defn- second-pass-processor [site-data]
  (fn [post]
    (assoc post :content ((:content post) site-data))))

(defn process-posts-1st-pass [site-data posts]
  (sort-by #(-> % :publish-date .getTime -)
           (pmap
             (first-pass-processor site-data)
             posts)))

(defn process-posts-2nd-pass [site-data posts]
  (pmap (second-pass-processor site-data) posts))

(defn process-posts [site-data posts]
  (let [fst-pass (process-posts-1st-pass site-data posts)]
    (process-posts-2nd-pass (assoc site-data :posts fst-pass)
                            fst-pass)))

(defn process-pages [pages posts site-data]
  (pmap (partial process (assoc site-data :posts posts))
        pages))

;fixme
(defn url-to-path [url]
  (string/replace url "-" "_"))

(defn save-html [base-dir post-or-page]
  (let [{:keys [url content]} post-or-page
        path (io/file base-dir (url-to-path url))]
    (.mkdirs (.getParentFile path))
    (spit (.getAbsolutePath path) content)))

(defn copy-resource [base-dir dest-dir file]
  (let [rel-path (string/replace (.getAbsolutePath file) (.getAbsolutePath base-dir) "")
        rel-path (string/replace rel-path #"^[/\\]" "")
        dest-file (io/file dest-dir rel-path)]
    (.. dest-file (getParentFile) (mkdirs))
    (io/copy file dest-file)))

;(defn generate [config]
  ;(let [{:keys [source-dir dest-dir]} config
        ;posts-dir (io/file source-dir "posts")
        ;pages-dir (io/file source-dir "public")
        ;data (site-data)
        ;post-files (first (glob posts-dir))
        ;post-templates (filter published-posts (map read-file post-files))
        ;posts (process-posts data post-templates)
        ;[template-pages resource-files] (glob pages-dir)
        ;pages (process-pages (map read-file template-pages) posts data)]
    ;(println "Saving posts")
    ;(doall (map (partial save-html dest-dir) posts))
    ;(println "Saving html files")
    ;(doall (map (partial save-html dest-dir) pages))
    ;(println "Copying resource files")
    ;(doall (map (partial copy-resource pages-dir dest-dir) resource-files)))
  ;(shutdown-agents))

(defn make-graph [generators]
  (let [[nodes neigh] (reduce
                        (fn [[nodes neigh] {:keys [target dependencies]}]
                          [(conj nodes target)
                           (merge-with concat neigh {target dependencies})])
                        [#{} {}]
                        generators)]
    (struct gr/directed-graph nodes neigh)))

(defn step-seq [dependencies]
  (for [dep-level dependencies step dep-level]
    step))

(defn new-step [step]
  (fn [data]
    (println (str "Generating " step))
    data))

(defn producers [gen-map steps]
  (mapcat
    #(conj (map :producer (gen-map %)) (new-step %))
    steps))

(defn generate [generators site-data]
  (let [graph (make-graph generators)
        generators (group-by :target generators)
        deps (gr/dependency-list graph)
        all-producers (producers generators (step-seq deps))]
    (reduce
      (fn [data producer]
        (producer data))
      site-data
      all-producers)))

