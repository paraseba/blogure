(ns blogure.helpers
  (require [clojure.pprint :as pp]
           [clojure.string :as string]))

(defn- make-triplett [[{next :url} {post :url} {prev :url}]]
  {:post post :next next :prev prev})

(defn- sort-posts [data]
  (let [sorted (reverse (sort-by :published-at (vals (:posts-metadata data))))
        partitioned (partition 3 1 (concat [nil] sorted [nil]))
        tripletts (map make-triplett partitioned)]
    (assoc data :sorted-posts tripletts)))

(def sort-posts-generator
  {:target :sorted-posts, :dependencies [:posts-metadata], :producer sort-posts})

(defn- paginate-posts [per-page data]
  (let [paginated (partition per-page per-page [] (map :post (:sorted-posts data)))]
    (assoc data :pagination paginated)))

(defn paginate-posts-generator
  [per-page]
  {:target :paginated-posts, :dependencies [:sorted-posts]
   :producer (partial paginate-posts per-page)})

(defn dump-site-data-generator [after]
  {:target :dump, :dependencies [after], :producer pp/pprint})

(defn blog-generators
  ([per-page] (blog-generators per-page []))
  ([per-page dump-after]
   (let [dump-after (seq (flatten [dump-after]))
         res [sort-posts-generator (paginate-posts-generator per-page)]]
     (if dump-after
       (concat res (map dump-site-data-generator dump-after))
       res))))

(defn- add-post-meta [post-meta site-data]
  (assoc-in site-data [:posts-metadata (:url post-meta)] post-meta))

(defn- find-sorted-post [coll url]
  (some #(if (= (:post %) url) %) coll))

(defn- create-post-producer [url content-function]
  (fn [site-data]
    (let [{:keys [post next prev]} (find-sorted-post (:sorted-posts site-data) url)
          this-post (get-in site-data [:posts-metadata post])
          next (get-in site-data [:posts-metadata next])
          prev (get-in site-data [:posts-metadata prev])
          content (content-function site-data prev next)]
      (assoc-in site-data [:posts url] (assoc this-post :content content)))))

(defn post-url [title]
  (string/replace title #"\W" "-"))

(defn make-post [title date content-function]
  (let [url (post-url title)
        post-meta {:title title, :published-at date,
                   :publish? true, :url url}]
    [{:target :posts-metadata, :dependencies [],
      :producer (partial add-post-meta post-meta)}
     {:target :post, :dependencies [:sorted-posts],
      :producer (create-post-producer url content-function)}]))
