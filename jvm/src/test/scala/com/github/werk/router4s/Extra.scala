package com.github.werk.router4s

import java.net.{URLDecoder, URLEncoder}

import com.github.werk.router4s.Router.Node

object Extra {

    // TODO how do I put this in the shared Router object?
    val string : Node[String] = Node[String](
        fromPath = { path => Some(URLDecoder.decode(path, "UTF-8")) },
        toPath = { s => Some(encodeURIComponent(s)) },
        name = "string"
    )

    /**
      * Encodes the passed String as UTF-8 using an algorithm that's compatible
      * with JavaScript's <code>encodeURIComponent</code> function.
      */
    private def encodeURIComponent(s : String) : String = {
        URLEncoder.encode(s, "UTF-8")
            .replaceAll("\\+", "%20")
            .replaceAll("\\%21", "!")
            .replaceAll("\\%27", "'")
            .replaceAll("\\%28", "(")
            .replaceAll("\\%29", ")")
            .replaceAll("\\%7E", "~")
    }
}
