(ns minasss-games.pixi-scene
  "It would be AWESOME to use a language like hiccup to define a scene or a
  container, it could look something like this
  `[:container {:anchor [0 0] :position [100 100]}
    [:sprite 'sprite/image.png' {:position [50 50]}]
    [:text 'Hello world' {:style {:fill '#d73637' :fontSize 16}}]]`

  It would be even more handy with graphics
  [:graphic
     [:fill-mode 0xff00ff
        [rect 0 0 100 100]]]"
  )
