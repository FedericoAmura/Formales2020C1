(ns tlclisp.core-test
  (:require [clojure.test :refer :all]
            [tlclisp.core :refer :all]))

; controlar-aridad
(deftest controlar-aridad-few-arguments
  (testing "Should return too few arguments if that happens"
    (is (= '(*error* too-few-args) (controlar-aridad '(a b c) 4)))))

(deftest controlar-aridad-correct
  (testing "Should return argument count when correct"
    (is (= 4 (controlar-aridad '(a b c d) 4)))))

(deftest controlar-aridad-many-arguments
  (testing "Should return too many arguments if that happens"
    (is (= '(*error* too-many-args) (controlar-aridad '(a b c d e) 4)))))