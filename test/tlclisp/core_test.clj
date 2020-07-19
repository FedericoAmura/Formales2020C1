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

; actualizar-amb
(deftest actualizar-amb-new-value
  (testing "Should add value if new"
    (is (= '(+ add - sub x 1) (actualizar-amb '(+ add - sub) 'x 1)))))

(deftest actualizar-amb-update-value
  (testing "Should update value if present"
    (is (= '(+ add - sub x 3 y 2) (actualizar-amb '(+ add - sub x 1 y 2) 'x 3)))))

; revisar-f
(deftest revisar-f-literal
  (testing "Should return nil"
    (is (= nil (revisar-f 'doble)))))

(deftest revisar-f-error
  (testing "Should return error"
    (is (= '(*error* too-few-args) (revisar-f '(*error* too-few-args))))))
