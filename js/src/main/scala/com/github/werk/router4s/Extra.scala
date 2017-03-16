package com.github.werk.router4s

import java.net.URLDecoder
import scala.scalajs.js.URIUtils.encodeURIComponent

import com.github.werk.router4s.Router.Node

object Extra {

    // TODO how do I put this in the shared Router object?
    val string : Node[String] = Node[String](
        fromPath = { path => Some(URLDecoder.decode(path, "UTF-8")) },
        toPath = { s => Some(encodeURIComponent(s)) },
        name = "string"
    )
}
