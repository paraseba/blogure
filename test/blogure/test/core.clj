(ns blogure.test.core
  (:use [blogure.core] :reload)
  (:use [clojure.test]))

(deftest calls-producers
  (testing "no dependencies"
    (is (= 3 (generate [{:target :a, :dependencies [], :producer inc}
                        {:target :b, :dependencies [], :producer (comp inc inc)}]
                        0))))
  (testing "with dependencies"
    (let [res (generate [{:target :a, :dependencies [], :producer inc}
                         {:target :b, :dependencies [:a], :producer (comp inc inc)}
                         {:target :c, :dependencies [:b], :producer (comp inc inc inc)}]
                        0)]
      (is (= 6 res)))))

(deftest generator-order
  (let [generators [{:target :a, :dependencies [:b], :producer #(conj % :a)}
                         {:target :b, :dependencies [:d], :producer #(conj % :b)}
                         {:target :c, :dependencies [:d], :producer #(conj % :c)}
                         {:target :d, :dependencies [], :producer #(conj % :d)}]
        res (generate generators [])]
    (is (or (= [:d :b :c :a] res)
            (= [:d :c :b :a] res)))))

(deftest console-report
  (let [generators [{:target :a, :dependencies [], :producer inc}
                         {:target :b, :dependencies [:a], :producer inc}
                         {:target :c, :dependencies [:b], :producer inc}
                         {:target :d, :dependencies [:b], :producer inc}]]
    (is (= "Generating :a\nGenerating :b\nGenerating :c\nGenerating :d\n"
           (with-out-str (generate generators 0))))))

(deftest run-only-once
  (let [counter (atom 0)
        f (fn [_] (swap! counter inc))
        generators [{:target :a, :dependencies [:b], :producer f}
                         {:target :b, :dependencies [:d], :producer f}
                         {:target :c, :dependencies [:d], :producer f}
                         {:target :d, :dependencies [], :producer f}]]
    (generate generators [])
    (is (= 4 @counter))))

(deftest generator-from-files
  (is (= [{:target :a, :dependencies [], :provider inc}
          {:target :z, :dependencies [:a], :provider dec}]
         (file->generators "test/resources/basic-generators.clj")))
  (is (= #{{:target :a, :dependencies [], :provider inc}
           {:target :z, :dependencies [:a], :provider dec}
           {:target :b, :dependencies [], :provider inc}}
         (set (dir->generators "test/resources")))))

