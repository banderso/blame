(ns blame.key)

(def ^:const esc-code (char 0x1B))

(def patterns (atom nil))

(defn set-pattern! [pattern]
  (reset! patterns pattern ))

(defn set-basic-pattern! [] 
  (set-pattern! {[esc-code] :esc
                 [esc-code \[ \A] :arrow-up
                 [esc-code \[ \B] :arrow-down
                 [esc-code \[ \C] :arrow-left
                 [esc-code \[ \D] :arrow-right
                 [\space] :space
                 [\tab] :tab
                 [\newline] :enter
                 [\return] :enter
                 [(char 0x7F)] :backspace
                 [esc-code \[ \2 \~] :insert
                 [esc-code \[ \3 \~] :delete
                 [esc-code \O \H] :home
                 [esc-code \O \F] :end
                 [esc-code \[ \5 \~] :page-up
                 [esc-code \[ \6 \~] :page-down}))

(defn decode [input]
  (if (nil? input)
    nil
    (if-let [code (get @patterns input)]
      code
      (first input))))
