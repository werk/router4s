package com.github.werk.router4s.test

object Pages {
    sealed trait Page
    case object Home extends Page
    case class Bands(parent : Home.type) extends Page
    case class Band(name : String, parent : Bands) extends Page
    case class BandEdit(parent : Band) extends Page
    case class BandAlbums(parent : Band) extends Page
    case class BandAlbum(id : Long, parent : BandAlbums) extends Page
    case class About(parent : Home.type) extends Page
}

object Main {
    import Pages._
    import com.github.werk.router4s.Router
    import com.github.werk.router4s.Router.{long, string}

    val path = new Router[Page]

    val router = path(Home,
        path("bands", Bands,
            path(string, Band,
                path("edit", BandEdit),
                path("albums", BandAlbums,
                    path(long, BandAlbum)
                )
            )
        ),
        path("about", About)
    )

    println(router.data("/bands/Pink+Floyd/albums/42"))
    println(router.path(BandAlbum(42, BandAlbums(Band("Pink Floyd", Bands(Home))))))

    def main(args : Array[String]) : Unit = {
        val pages = List[Page](
            Home,
            Bands(Home),
            Band("Pink Floyd", Bands(Home)),
            BandEdit(Band("Pink Floyd", Bands(Home))),
            BandAlbums(Band("Pink Floyd", Bands(Home))),
            BandAlbum(42, BandAlbums(Band("Pink Floyd", Bands(Home)))),
            About(Home)
        )

        router.prettyPaths.foreach(println)
        println()

        for(a <- pages) {
            val path = router.toPath(a).getOrElse{
                throw new RuntimeException(s"pathMap.toPath($a) = None")
            }
            val Some(a2) = router.fromPath(path)
            if(a == a2) {
                val p = '/' + path.mkString("/")
                println(f"$p%-30s <--> $a2")
            } else {
                println(s"ERROR: $a != fromPath(toPath($a)) = $a2")
            }
        }
    }
}
