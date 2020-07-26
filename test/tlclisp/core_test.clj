(ns tlclisp.core-test
  (:require [clojure.test :refer :all]
            [tlclisp.core :refer :all]))

; evaluar
(deftest evaluar-return-value-set
  (testing "Should return value returned by setq"
    (is (= '(3 (+ add r 3)) (evaluar '(setq r 3) '(+ add) nil)))))

(deftest evaluar-return-function
  (testing "Should return defined function"
    (is (= '(doble (+ add doble (lambda (x) (+ x x)))) (evaluar '(de doble (x) (+ x x)) '(+ add) nil)))))

(deftest evaluar-return-value-calc
  (testing "Should return value calculated"
    (is (= '(5 (+ add)) (evaluar '(+ 2 3) '(+ add) nil)))))

(deftest evaluar-unbound-symbol
  (testing "Should return error unbound-symbol"
    (is (= '((*error* unbound-symbol +) (add add)) (evaluar '(+ 2 3) '(add add) nil)))))

(deftest evaluar-lambda
  (testing "Should return lambda calculated value"
    (is (= '(6 (+ add doble (lambda (x) (+ x x)))) (evaluar '(doble 3) '(+ add doble (lambda (x) (+ x x))) nil)))))

(deftest evaluar-lambda-variable
  (testing "Should return lambda using env variable"
    (is (= '(8 (+ add r 4 doble (lambda (x) (+ x x)))) (evaluar '(doble r) '(+ add r 4 doble (lambda (x) (+ x x))) nil)))))

(deftest evaluar-lambda-parameter
  (testing "Should return lambda function parameter value"
    (is (= '(6 (+ add)) (evaluar '((lambda (x) (+ x x)) 3) '(+ add) nil)))))

; aplicar
(deftest aplicar-cons
  (testing "Should return function result"
    (is (= '((a b) (cons cons)) (aplicar 'cons '(a (b)) '(cons cons) nil)))))

(deftest aplicar-function
  (testing "Should return function result"
    (is (= '(9 (+ add r 5)) (aplicar 'add '(4 5) '(+ add r 5) nil)))))

(deftest aplicar-lambda
  (testing "Should return lambda result"
    (is (= '(8 (+ add r 4 doble (lambda (x) (+ x x)))) (aplicar '(lambda (x) (+ x x)) '(4) '(+ add r 4 doble (lambda (x) (+ x x))) nil)))))

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
(deftest revisar-f-valid
  (testing "Should return nil"
    (is (= nil (revisar-f 'doble)))))

(deftest revisar-f-error
  (testing "Should return error"
    (is (= '(*error* too-few-args) (revisar-f '(*error* too-few-args))))))

; revisar-lae
(deftest revisar-lae-valid
  (testing "Should return nil"
    (is (= nil (revisar-lae '(1 add first))))))

(deftest revisar-lae-error
  (testing "Should return error"
    (is (= '(*error* too-many-args) (revisar-lae '(1 add (*error* too-many-args) first))))))

; buscar
(deftest buscar-valid
  (testing "Should return sub"
    (is (= 'sub  (buscar '- '(+ add - sub))))))

(deftest buscar-error
  (testing "Should return error"
    (is (= '(*error* unbound-symbol doble) (buscar 'doble '(+ add - sub))))))

; evaluar-cond
(deftest evaluar-cond-nil
  (testing "Should return nil when nil lis"
    (is (= '(nil (equal equal setq setq))  (evaluar-cond nil '(equal equal setq setq) nil)))))

(deftest evaluar-cond-last-nil
  (testing "Should return nil "
    (is (= '(nil (equal equal first first)) (evaluar-cond '(((equal 'a 'b) (setq x 1))) '(equal equal first first) nil)))))

(deftest evaluar-cond-1
  (testing "Should return 2"
    (is (= '(2 (equal equal setq setq y 2))  (evaluar-cond '(((equal 'a 'b) (setq x 1)) ((equal 'a 'a) (setq y 2))) '(equal equal setq setq) nil)))))

(deftest evaluar-cond-2
  (testing "Should return 3"
    (is (= '(3 (equal equal setq setq y 2 z 3)) (evaluar-cond '(((equal 'a 'b) (setq x 1)) ((equal 'a 'a) (setq y 2) (setq z 3))) '(equal equal setq setq) nil)))))

; evaluar-secuencia-en-cond
(deftest evaluar-secuencia-en-cond-1
  (testing "Should return 2"
    (is (= '(2 (setq setq y 2))  (evaluar-secuencia-en-cond '((setq y 2)) '(setq setq) nil)))))

(deftest evaluar-secuencia-en-cond-2
  (testing "Should return 3"
    (is (= '(3 (setq setq y 2 z 3)) (evaluar-secuencia-en-cond '((setq y 2) (setq z 3)) '(setq setq) nil)))))
