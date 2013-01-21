(ns blame.stty
  (:import [java.io IOException ByteArrayOutputStream InputStream]))

(defn- update-output! [baos ins]
  (loop [c (.read ins)]
    (when (not (neg? c))
      (do (.write baos c)
          (recur (.read ins))))))

(defn- exec [& cmd]
  (let [bout (ByteArrayOutputStream.)
        p (.exec (Runtime/getRuntime) (into-array String cmd))]
    (do (update-output! bout (.getInputStream p))
        (update-output! bout (.getErrorStream p))
        (String. (.toByteArray bout)))))

(defn stty
  "Runs /bin/stty with the given arguments and reading from /dev/tty."
  [args]
  ;; clojure.java.shell/sh isn't used because this needs to run on the
  ;; main thread. Running it on a background threads causes stty to not
  ;; have the correct connection to the terminal.
  (exec "sh" "-c" (str "stty " args " < /dev/tty")))

(comment 
  (defn stty [& args]
    (:out (apply shell/sh (concat '("stty") args ["< /dev/tty" :env nil :dir nil])))))
