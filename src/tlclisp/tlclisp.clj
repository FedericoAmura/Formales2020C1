(ns tlclisp.tlclisp)

(declare evaluar)
(declare aplicar)
(declare controlar-aridad)
(declare igual?)
(declare cargar-arch)
(declare imprimir)
(declare actualizar-amb)
(declare revisar-f)
(declare revisar-lae)
(declare buscar)
(declare evaluar-cond)
(declare evaluar-secuencia-en-cond)
(defn esError? [lis] (= '*error* (first lis)))

; REPL (read–eval–print loop).
; Aridad 0: Muestra mensaje de bienvenida y se llama recursivamente con el ambiente inicial.
; Aridad 1: Muestra >>> y lee una expresion y la evalua.
; Si la 2da. posicion del resultado es nil, retorna true (caso base de la recursividad).
; Si no, imprime la 1ra. pos. del resultado y se llama recursivamente con la 2da. pos. del resultado.
(defn repl
  ([]
   (println "Interprete de TLC-LISP en Clojure")
   (println "Trabajo Practico de 75.14/95.48 - Lenguajes Formales 2020")
   (println "Inspirado en:")
   (println "TLC-LISP Version 1.51 for the IBM Personal Computer")
   (println "Copyright (c) 1982, 1983, 1984, 1985 The Lisp Company") (flush)
   (repl '(add add append append cond cond cons cons de de env env equal equal eval eval exit exit
               first first ge ge gt gt if if lambda lambda length length list list load load lt lt nil nil not not
               null null or or prin3 prin3 quote quote read read rest rest reverse reverse setq setq sub sub
               t t terpri terpri + add - sub)))
  ([amb]
   (print ">>> ") (flush)
   (try (let [res (evaluar (read) amb nil)]
          (if (nil? (fnext res))
            true
            (do (imprimir (first res)) (repl (fnext res)))))
        (catch Exception e (println) (print "*error* ") (println (get (Throwable->map e) :cause)) (repl amb))))
  )

; Carga el contenido de un archivo.
; Aridad 3: Recibe los ambientes global y local y el nombre de un archivo
; (literal como string o atomo, con o sin extension .lsp, o el simbolo ligado al nombre de un archivo en el ambiente), abre el archivo
; y lee un elemento de la entrada (si falla, imprime nil), lo evalua y llama recursivamente con el (nuevo?) amb., nil, la entrada y un arg. mas: el resultado de la evaluacion.
; Aridad 4: lee un elem. del archivo (si falla, imprime el ultimo resultado), lo evalua y llama recursivamente con el (nuevo?) amb., nil, la entrada y el resultado de la eval.
(defn cargar-arch
  ([amb-global amb-local arch]
   (let [nomb (first (evaluar arch amb-global amb-local))]
     (if (and (seq? nomb) (igual? (first nomb) '*error*))
       (do (imprimir nomb) amb-global)
       (let [nm (clojure.string/lower-case (str nomb)),
             nom (if (and (> (count nm) 4) (clojure.string/ends-with? nm ".lsp")) nm (str nm ".lsp")),
             ret (try (with-open [in (java.io.PushbackReader. (clojure.java.io/reader nom))]
                        (binding [*read-eval* false] (try (let [res (evaluar (read in) amb-global nil)]
                                                            (cargar-arch (fnext res) nil in res))
                                                          (catch Exception e (imprimir nil) amb-global))))
                      (catch java.io.FileNotFoundException e (imprimir (list '*error* 'file-open-error 'file-not-found nom '1 'READ)) amb-global))]
         ret))))
  ([amb-global amb-local in res]
   (try (let [res (evaluar (read in) amb-global nil)] (cargar-arch (fnext res) nil in res))
        (catch Exception e (imprimir (first res)) amb-global)))
  )

; Evalua una expresion usando los ambientes global y local. Siempre retorna una lista con un resultado y un ambiente.
; Si la evaluacion falla, el resultado es una lista con '*error* como primer elemento, por ejemplo: (list '*error* 'too-many-args) y el ambiente es el ambiente global.
; Si la expresion es un escalar numero o cadena, retorna la expresion y el ambiente global.
; Si la expresion es otro tipo de escalar, la busca (en los ambientes local y global) y retorna el valor y el ambiente global.
; Si la expresion es una secuencia nula, retorna nil y el ambiente global.
; Si el primer elemento de la expresion es '*error*, retorna la expresion y el ambiente global.
; Si el primer elemento de la expresion es una forma especial o una macro, valida los demas elementos y retorna el resultado y el (nuevo?) ambiente.
; Si no lo es, se trata de una funcion en posicion de operador (es una aplicacion de calculo lambda), por lo que se llama a la funcion aplicar,
; pasandole 4 argumentos: la evaluacion del primer elemento, una lista con las evaluaciones de los demas, el ambiente global y el ambiente local.
(defn evaluar [expre amb-global amb-local]
  (if (not (seq? expre))
    (if (or (number? expre) (string? expre)) (list expre amb-global) (list (buscar expre (concat amb-local amb-global)) amb-global))
    (cond (igual? expre nil) (list nil amb-global)
          (igual? (first expre) '*error*) (list expre amb-global)
          (igual? (first expre) 'exit) (if (< (count (next expre)) 1) (list nil nil) (list (list '*error* 'too-many-args) amb-global))
          (igual? (first expre) 'setq) (cond (< (count (next expre)) 2) (list (list '*error* 'list 'expected nil) amb-global)
                                             (igual? (fnext expre) nil) (list (list '*error* 'cannot-set nil) amb-global)
                                             (not (symbol? (fnext expre))) (list (list '*error* 'symbol 'expected (fnext expre)) amb-global)
                                             (= (count (next expre)) 2) (let [res (evaluar (first (nnext expre)) amb-global amb-local)]
                                                                          (list (first res) (actualizar-amb amb-global (fnext expre) (first res))))
                                             true (let [res (evaluar (first (nnext expre)) amb-global amb-local)]
                                                    (evaluar (cons 'setq (next (nnext expre))) (actualizar-amb amb-global (fnext expre) (first res)) amb-local)))
          (igual? (first expre) 'de) (cond (< (count (next expre)) 2) (list (list '*error* 'list 'expected nil) amb-global)
                                           (and (not (igual? (first (nnext expre)) nil)) (not (seq? (first (nnext expre))))) (list (list '*error* 'list 'expected (first (nnext expre))) amb-global)
                                           (igual? (fnext expre) nil) (list (list '*error* 'cannot-set nil) amb-global)
                                           (not (symbol? (fnext expre))) (list (list '*error* 'symbol 'expected (fnext expre)) amb-global)
                                           true (list (fnext expre) (actualizar-amb amb-global (fnext expre) (cons 'lambda (nnext expre)))))
          (igual? (first expre) 'quote) (list (if (igual? (fnext expre) nil) nil (fnext expre)) amb-global)
          (igual? (first expre) 'lambda) (cond (< (count (next expre)) 1) (list (list '*error* 'list 'expected nil) amb-global)
                                               (and (not (igual? (fnext expre) nil)) (not (seq? (fnext expre)))) (list (list '*error* 'list 'expected (fnext expre)) amb-global)
                                               true (list expre amb-global))
          (igual? (first expre) 'cond) (evaluar-cond (next expre) amb-global amb-local)
          (igual? (first expre) 'load) (cond (< (count (next expre)) 1) (list (list '*error* 'too-few-args) amb-global)
                                             (= (count (next expre)) 2) (list (list '*error* 'not-implemented) amb-global)
                                             (> (count (next expre)) 2) (list (list '*error* 'too-many-args) amb-global)
                                             true (list nil (cargar-arch amb-global amb-local (fnext expre))))
          (igual? (first expre) 'if) (cond (< (count (next expre)) 1) (list (list '*error* 'list 'expected nil) amb-global)
                                           true (let [res (evaluar (fnext expre) amb-global amb-local)]
                                                  (cond (and (list? (first res)) (= (ffirst res) '*error*)) res
                                                        (igual? (first res) nil) (evaluar (fnext (next (next expre))) (fnext res) amb-local)
                                                        true (evaluar (fnext (next expre)) (fnext res) amb-local))))
          (igual? (first expre) 'or) (cond (< (count (next expre)) 1) (list nil amb-global)
                                           true (let [res (evaluar (fnext expre) amb-global amb-local)]
                                                  (cond (and (list? (first res)) (= (ffirst res) '*error*)) res
                                                        (igual? (first res) nil) (evaluar (cons 'or (next (next expre))) (fnext res) amb-local)
                                                        true (list (first res) (fnext res)))))
          true (aplicar (first (evaluar (first expre) amb-global amb-local)) (map (fn [x] (first (evaluar x amb-global amb-local))) (next expre)) amb-global amb-local)))
  )

; Aplica una funcion a una lista de argumentos evaluados, usando los ambientes global y local. Siempre retorna una lista con un resultado y un ambiente.
; Si la aplicacion falla, el resultado es una lista con '*error* como primer elemento, por ejemplo: (list '*error* 'arg-wrong-type) y el ambiente es el ambiente global.
; Aridad 4: Recibe la func., la lista de args. evaluados y los ambs. global y local. Se llama recursivamente agregando 2 args.: la func. revisada y la lista de args. revisada.
; Aridad 6: Si la funcion revisada no es nil, se la retorna con el amb. global.
; Si la lista de args. evaluados revisada no es nil, se la retorna con el amb. global.
; Si no, en caso de que la func. sea escalar (predefinida o definida por el usuario), se devuelven el resultado de su aplicacion (controlando la aridad) y el ambiente global.
; Si la func. no es escalar, se valida que la cantidad de parametros y argumentos coincidan, y:
; en caso de que se trate de una func. lambda con un solo cuerpo, se la evalua usando el amb. global intacto y el local actualizado con los params. ligados a los args.,
; en caso de haber multiples cuerpos, se llama a aplicar recursivamente, pasando la funcion lambda sin el primer cuerpo, la lista de argumentos evaluados,
; el amb. global actualizado con la eval. del 1er. cuerpo (usando el amb. global intacto y el local actualizado con los params. ligados a los args.) y el amb. local intacto.
(defn aplicar
  ([f lae amb-global amb-local]
   (aplicar (revisar-f f) (revisar-lae lae) f lae amb-global amb-local))
  ([resu1 resu2 f lae amb-global amb-local]
   (cond resu1 (list resu1 amb-global)
         resu2 (list resu2 amb-global)
         true (if (not (seq? f))
                (list (cond
                        (igual? f 'env) (if (> (count lae) 0)
                                          (list '*error* 'too-many-args)
                                          (concat amb-global amb-local))
                        (igual? f 'first) (let [ari (controlar-aridad lae 1)]
                                            (cond (seq? ari) ari
                                                  (igual? (first lae) nil) nil
                                                  (not (seq? (first lae))) (list '*error* 'list 'expected (first lae))
                                                  true (ffirst lae)))
                        (or (igual? f 'add) (igual? f '+)) (if (< (count lae) 2)
                                                             (list '*error* 'too-few-args)
                                                             (try (reduce + lae)
                                                                  (catch Exception e (list '*error* 'number-expected))))
                        (or (igual? f 'sub) (igual? f '-)) (if (< (count lae) 2)
                                                             (list '*error* 'too-few-args)
                                                             (try (reduce - lae)
                                                                  (catch Exception e (list '*error* 'number-expected))))
                        (igual? f 'cons) (cond (< (count lae) 2) (list '*error* 'too-few-args)
                                               (> (count lae) 2) (list '*error* 'too-many-args)
                                               ;(not (list? (fnext lae))) (list '*error* 'not-implemented)
                                               true (apply cons lae))
                        (igual? f 'append) (cond (< (count lae) 2) (list '*error* 'too-few-args)
                                                 (> (count lae) 2) (list '*error* 'too-many-args)
                                                 ;(not (list? (first lae))) (do (println "append" lae) (list '*error* 'list-expected))
                                                 ;(not (list? (fnext lae))) (list '*error* 'not-implemented)
                                                 true (apply concat lae))
                        (igual? f 'list) lae
                        (igual? f 'length) (cond (< (count lae) 1) (list '*error* 'too-few-args)
                                                 (> (count lae) 1) (list '*error* 'too-many-args)
                                                 (igual? (first lae) nil) 0
                                                 ;(not (list? (first lae))) (list '*error* 'arg-wrong-type)
                                                 true (count (first lae)))
                        (igual? f 'reverse) (cond (< (count lae) 1) (list '*error* 'too-few-args)
                                                  (> (count lae) 1) (list '*error* 'too-many-args)
                                                  (igual? (first lae) nil) nil
                                                  ;(not (list? (first lae))) (list '*error* 'list 'expected)
                                                  true (reverse (first lae)))
                        (igual? f 'rest) (cond (< (count lae) 1) (list '*error* 'too-few-args)
                                               (> (count lae) 2) (list '*error* 'too-many-args)
                                               (= (count lae) 2) (list '*error* 'not-implemented)
                                               (igual? (first lae) nil) nil
                                               ;(not (list? (first lae))) (list '*error* 'list 'expected)
                                               true (next (first lae)))
                        (igual? f 'equal) (cond (= (count lae) 1) (list '*error* 'too-few-args)
                                                (> (count lae) 2) (list '*error* 'too-many-args)
                                                true (if (igual? (first lae) (fnext lae)) 't nil))
                        (igual? f 'null) (cond (< (count lae) 1) (list '*error* 'too-few-args)
                                               (> (count lae) 1) (list '*error* 'too-many-args)
                                               true (if (igual? (first lae) nil) 't nil))
                        (igual? f 'lt) (cond (= (count lae) 1) (list '*error* 'too-few-args)
                                             (> (count lae) 2) (list '*error* 'too-many-args)
                                             (or (not (number? (first lae))) (not (number? (fnext lae)))) (list '*error* 'number-expected)
                                             true (if (< (first lae) (fnext lae)) 't nil))
                        (igual? f 'ge) (cond (= (count lae) 1) (list '*error* 'too-few-args)
                                             (> (count lae) 2) (list '*error* 'too-many-args)
                                             (or (not (number? (first lae))) (not (number? (fnext lae)))) (list '*error* 'number-expected)
                                             true (if (>= (first lae) (fnext lae)) 't nil))
                        (igual? f 'gt) (cond (= (count lae) 1) (list '*error* 'too-few-args)
                                             (> (count lae) 2) (list '*error* 'too-many-args)
                                             (or (not (number? (first lae))) (not (number? (fnext lae)))) (list '*error* 'number-expected)
                                             true (if (> (first lae) (fnext lae)) 't nil))
                        (igual? f 'not) (cond (< (count lae) 1) (list '*error* 'too-few-args)
                                              (> (count lae) 1) (list '*error* 'too-many-args)
                                              (igual? (first lae) 't) nil
                                              (igual? (first lae) nil) 't
                                              true (list '*error* 'no-boolean-value))
                        (igual? f 'read) (read)
                        (igual? f 'prin3) (cond (< (count lae) 1) (list '*error* 'too-few-args)
                                                (= (count lae) 2) (list '*error* 'not-implemented)
                                                (> (count lae) 2) (list '*error* 'too-many-args)
                                                true (do (print (first lae)) (flush) (first lae)))
                        (igual? f 'terpri) (cond (= (count lae) 1) (list '*error* 'not-implemented)
                                                 (> (count lae) 1) (list '*error* 'too-many-args)
                                                 true (prn))
                        (igual? f 'eval) (cond (< (count lae) 1) (list '*error* 'too-few-args)
                                               (> (count lae) 1) (list '*error* 'too-many-args)
                                               (not (list? (first lae))) (first lae)
                                               true (first (evaluar (first lae) amb-global amb-local)))
                        true (let [lamb (buscar f (concat amb-local amb-global))]
                               (cond (or (number? lamb) (igual? lamb 't) (igual? lamb nil)) (list '*error* 'non-applicable-type lamb)
                                     (or (number? f) (igual? f 't) (igual? f nil)) (list '*error* 'non-applicable-type f)
                                     (igual? (first lamb) '*error*) lamb
                                     true (aplicar lamb lae amb-global amb-local)))) amb-global)
                (cond (< (count lae) (count (fnext f))) (list (list '*error* 'too-few-args) amb-global)
                      (> (count lae) (count (fnext f))) (list (list '*error* 'too-many-args) amb-global)
                      true (if (nil? (next (nnext f)))
                             (evaluar (first (nnext f)) amb-global (concat (reduce concat (map list (fnext f) lae)) amb-local))
                             (aplicar (cons 'lambda (cons (fnext f) (next (nnext f)))) lae (fnext (evaluar (first (nnext f)) amb-global (concat (reduce concat (map list (fnext f) lae)) amb-local))) amb-local))))))
  )

; Falta terminar de implementar las 2 funciones anteriores (aplicar y evaluar)

; Falta implementar las 9 funciones auxiliares (actualizar-amb, controlar-aridad, imprimir, buscar, etc.)
; Controla la aridad (cantidad de argumentos de una funcion).
; Recibe una lista y un numero. Si la longitud de la lista coincide con el numero, retorna el numero.
; Si es menor, retorna (list '*error* 'too-few-args).
; Si es mayor, retorna (list '*error* 'too-many-args).
(defn controlar-aridad [lis val-esperado]
  (cond (= (count lis) val-esperado) val-esperado
        (< (count lis) val-esperado) (list '*error* 'too-few-args)
        (> (count lis) val-esperado) (list '*error* 'too-many-args)
        )
  )

; Compara la igualdad de dos simbolos.
; Recibe dos simbolos a y b. Retorna true si se deben considerar iguales; si no, false.
; Se utiliza porque TLC-LISP no es case-sensitive y ademas no distingue entre nil y la lista vacia.
(defn falsy? [v] (or (= v ()) (if (or (symbol? v) (string? v)) (or (= (clojure.string/lower-case v) "nil") (= (clojure.string/lower-case v) "null")) false)))
(defn lower [s] (if (or (symbol? s) (string? s)) (clojure.string/lower-case s) s))
(defn igual? [a b]
  (cond (falsy? a) (igual? nil b)
        (falsy? b) (igual? a nil)
        true (= (lower a) (lower b))
        )
  )

; Imprime, con salto de linea, atomos o listas en formato estandar (las cadenas con comillas) y devuelve su valor. Muestra errores sin parentesis.
; Aridad 1: Si recibe un escalar, lo imprime con salto de linea en formato estandar (pero si es \space no lo imprime), purga la salida y devuelve el escalar.
; Si recibe una secuencia cuyo primer elemento es '*error*, se llama recursivamente con dos argumentos iguales: la secuencia recibida.
; Si no, imprime lo recibido con salto de linea en formato estandar, purga la salida y devuelve la cadena.
; Aridad 2: Si el primer parametro es nil, imprime un salto de linea, purga la salida y devuelve el segundo parametro.
; Si no, imprime su primer elemento en formato estandar, imprime un espacio y se llama recursivamente con la cola del primer parametro y el segundo intacto.
(defn imprimir
  ([elem]
   (cond (and (seq? elem) (esError? elem)) (imprimir elem elem)
         (= elem \space) \space
         true (do (prn elem) (flush) elem)
         )
   )
  ([lis orig]
   (cond (igual? lis nil) (do (prn) (flush) orig)
         true (do
                (pr (first lis)) (print \space)
                (imprimir (if (= (count lis) 0) nil (pop lis)) orig))
         )
   )
  )

; Actualiza un ambiente (una lista con claves en las posiciones impares [1, 3, 5...] y valores en las pares [2, 4, 6...]
; Recibe el ambiente, la clave y el valor.
; Si el valor no es escalar y en su primera posicion contiene '*error*, retorna el ambiente intacto.
; Si no, coloca la clave y el valor en el ambiente (puede ser un alta o una actualizacion) y lo retorna.
(defn actualizar-amb [amb-global clave valor]
  (cond (and (seq? valor) (esError? valor)) amb-global
        (< (.indexOf amb-global clave) 0) (concat amb-global (list clave valor)) ; nuevo valor, concatenamos
        true (concat (take (.indexOf amb-global clave) amb-global) (list clave valor) (take-last (- (count amb-global) (.indexOf amb-global clave) 2) amb-global))
        )
  )

; Revisa una lista que representa una funcion.
; Recibe la lista y, si esta comienza con '*error*, la retorna. Si no, retorna nil.
(defn revisar-f [lis]
  (cond (and (seq? lis) (esError? lis)) lis
        true nil
        )
  )

; Revisa una lista de argumentos evaluados.
; Recibe la lista y, si esta contiene alguna sublista que comienza con '*error*, retorna esa sublista. Si no, retorna nil.
(defn revisar-lae [lis]
  (cond (some (every-pred seq? esError?) lis) (first (filter (every-pred seq? esError?) lis))
        true nil))

; Busca una clave en un ambiente (una lista con claves en las posiciones impares [1, 3, 5...] y valores en las pares [2, 4, 6...] y retorna el valor asociado.
; Si no la encuentra, retorna una lista con '*error* en la 1ra. pos., 'unbound-symbol en la 2da. y el elemento en la 3ra.
(defn index-of [item coll]
  (let [index (count (take-while (partial not= true) (map (fn [x] (igual? item x)) coll)))]
    (if (< index (count coll)) index -1)))
(defn buscar [elem lis]
  (let [index (index-of elem (flatten (partition 1 2 lis)))] ; hacemos el partition para buscar solo sobre las claves
    (if (< index 0) (list '*error* 'unbound-symbol elem)
                    (nth lis (+ 1 (* 2 index)))))
  )

; Evalua el cuerpo de una macro COND. Siempre retorna una lista con un resultado y un ambiente.
; Recibe una lista de sublistas (cada una de las cuales tiene una condicion en su 1ra. posicion) y los ambientes global y local
; Si la lista es nil, el resultado es nil y el ambiente retornado es el global.
; Si no, evalua (con evaluar) la cabeza de la 1ra. sublista y, si el resultado no es nil, retorna el res. de invocar a evaluar-secuencia-en-cond con la cola de esa sublista.
; En caso contrario, sigue con las demas sublistas.
(defn evaluar-cond [lis amb-global amb-local]
  (cond (nil? lis) (list nil amb-global)
        (not= (first (evaluar (ffirst lis) amb-global amb-local)) nil) (evaluar-secuencia-en-cond (next (first lis)) amb-global amb-local)
        true (evaluar-cond (rest lis) amb-global amb-local)
        )
  )

; Evalua (con evaluar) secuencialmente las sublistas de una lista y retorna el valor de la ultima evaluacion.
(defn evaluar-secuencia-en-cond [lis amb-global amb-local]
  (if (nil? (first lis)) (list nil amb-global) (let [res (evaluar (first lis) amb-global amb-local)]
                                                 (cond (and (list? (first res)) (= (ffirst res) '*error)) res
                                                       (= (count (next lis)) 0) res
                                                       true (evaluar-secuencia-en-cond (next lis) (fnext res) amb-local)))))

; Al terminar de cargar el archivo, se retorna true.
true