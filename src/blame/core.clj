(ns blame.core
  (:use [blame.stty :only [stty]])
  (:require [clojure.string :as string]
            [blame.key :as key]
            [noise.core :as noise])
  (:import [java.io IOException])
  (:gen-class))

(def ^:private ttyconfig (atom nil))

(defn set-terminal-to-cbreak! []
  (do (reset! ttyconfig (stty "-g"))
      (stty "raw")
      (stty "-echo")))

(defn to-byte [c] (byte (int c)))

(defn write-to-term [& cmd]
  (.write System/out (into-array Byte/TYPE cmd)))

(defn move-to [x y]
  (apply write-to-term (map to-byte (cons 0x1b (str \[ (inc x) \; (inc y) \H)))))

(defn clear-screen []
  (apply write-to-term (map to-byte (cons 0x1b "[2J")))
  (move-to 0 0))

(defn log-input [cnt inp]
  (printf "%d [" cnt)
  (loop [ic inp]
    (if (first ic)
      (do (printf "%s" (first ic))
          (when (first (rest ic))
            (printf " "))
          (recur (rest ic)))
      (printf "]\r\n")))
  (flush))

(defn prompt []
  (print (str (char 0x276F) " "))
  (flush))

(def client (atom nil))

(defn connect-to-server! [host port]
  (reset! client (noise/client :port port :bind host)))

(def byte-mask 0x7F)
(def neg-byte-mask -128)
(defn <byte [ibyte]
  (if (>= ibyte 128)
    (byte (bit-or neg-byte-mask (bit-and byte-mask ibyte)))
    (byte (bit-and byte-mask ibyte))))

(defn read-full-input []
  (let [c (transient [])]
    (loop []
      (do (conj! c (char (<byte (.read System/in))))
          (if (zero? (.available System/in))
            (persistent! c)
            (recur))))))

(defn read-input! []
  (try (do (set-terminal-to-cbreak!)
           (clear-screen)
           ;(println (stty "-a"))
           (key/set-basic-pattern!)
           (prompt)
           (loop [i 0]
             (let [inp [(key/decode (read-full-input))]]
               (do (log-input i inp)
                   (if (= (nth inp 0) :esc)
                     nil
                     (do (prompt)
                         (recur (inc i))))))))
       (catch IOException e (binding [*out* *err*]
                              (println "IOException")
                              (.printStackTrace e)))
       (catch InterruptedException e (binding [*out* *err*]
                                       (println "InterruptedException")))
       (finally (when @ttyconfig 
                  (try (stty (string/trim @ttyconfig))
                       (catch Exception e (binding [*out* *err*]
                                            (println "Exception restorying tty config"))))))))

(defn print-usage []
  (println "blame host port"))

(defn -main [& args]
  (if (< (count args) 2)
    (print-usage)
    (do (comment (connect-to-server! (first args) (Long/parseLong (first (rest args)))))
        (read-input!))))
