(ns blogure.test.helpers
  (:require [clj-time.core :as t])
  (:use blogure.core clojure.test clojure.contrib.java-utils)
  (:use [blogure.helpers] :reload))

(def now (t/now))

(defn simple-post [days-back title content]
  (make-post title (t/minus now (t/days days-back)) (constantly content)))

(deftest generate-posts-map
  (let [sys-generators (blog-generators 3)
        post1 (simple-post 40 "post one" "This is post one")
        post2 (simple-post 30 "post two" "This is post two")
        post3 (simple-post 20 "post three" "This is post three")
        post4 (simple-post 10 "post four" "This is post four")
        post-generators (concat post1 post2 post3 post4)
        generators (concat sys-generators post-generators)
        res (generate generators {})]

    (is (= [["post-four" "post-three" "post-two"] ["post-one"]] (:pagination res)))

    (is (= [{:next nil :prev "post-three" :post "post-four"}
            {:next "post-four" :prev "post-two" :post "post-three"}
            {:next "post-three" :prev "post-one" :post "post-two"}
            {:next "post-two" :prev nil :post "post-one"}]
            (:sorted-posts res)))

    (let [post-1-meta {:url "post-one" :title "post one"
                       :published-at (t/minus now (t/days 40))
                       :publish? true}
          post-2-meta {:url "post-two" :title "post two"
                       :published-at (t/minus now (t/days 30))
                       :publish? true}
          post-3-meta {:url "post-three" :title "post three"
                       :published-at (t/minus now (t/days 20))
                       :publish? true}
          post-4-meta {:url "post-four" :title "post four"
                       :published-at (t/minus now (t/days 10))
                       :publish? true}]
      (is (= {"post-one" post-1-meta
              "post-two" post-2-meta
              "post-three" post-3-meta
              "post-four" post-4-meta}
             (:posts-metadata res)))

      (is (= (assoc post-1-meta :content "This is post one")
             (get-in res [:posts "post-one"])))
      (is (= (assoc post-2-meta :content "This is post two")
             (get-in res [:posts "post-two"])))
      (is (= (assoc post-3-meta :content "This is post three")
             (get-in res [:posts "post-three"])))
      (is (= (assoc post-4-meta :content "This is post four")
             (get-in res [:posts "post-four"]))))))

(deftest pass-correct-next-and-prev
  (let [sys-generators (blog-generators 3)
        check1 #(do
                  (is (= nil %2))
                  (is (= "post-2" (:url %3))))
        check2 #(do
                  (is (= "post-1" (:url %2)))
                  (is (= "post-3" (:url %3))))
        check3 #(do
                  (is (= "post-2" (:url %2)))
                  (is (=  nil (:url %3))))
        post1 (make-post "post 1" (t/minus now (t/days 10)) check1)
        post2 (make-post "post 2" (t/minus now (t/days 5)) check2)
        post3 (make-post "post 3" (t/minus now (t/days 3)) check3)
        post-generators (concat post1 post2 post3)
        generators (concat sys-generators post-generators)]
   (generate generators {})))

(deftest generate-post-files
  (let [sys-generators (blog-generators 3)
        post1 (simple-post 40 "post one" "This is post one")
        post2 (simple-post 30 "post two" "This is post two")
        post-generators (concat post1 post2)
        generators (concat sys-generators post-generators)
        tmp (file (get-system-property "java.io.tmpdir") "blogure_test")
        res (generate generators {:base-path tmp})]
    (is (= "This is post one" (slurp (file tmp "post_one"))))
    (is (= "This is post two" (slurp (file tmp "post_two"))))))

