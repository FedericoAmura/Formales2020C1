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

; igual?
(deftest igual?-equal-values
  (testing "true = (igual? 5 5)"
    (is (= true (igual? 5 5)))))

(deftest igual?-diff-values
  (testing "false = (igual? 4 5)"
    (is (= false (igual? 4 5)))))

(deftest igual?-nil-literal
  (testing "true = (igual? nil 'NIL)"
    (is (= true (igual? nil 'NIL)))))

(deftest igual?-nil-string
  (testing "true = (igual? nil \"NIL\")"
    (is (= true (igual? nil "NIL")))))

(deftest igual?-nil-lista
  (testing "true = (igual? nil ())"
    (is (= true (igual? nil ())))))

(deftest igual?-lista-string
  (testing "true = (igual? () 'NIL)"
    (is (= true (igual? () 'NIL)))))