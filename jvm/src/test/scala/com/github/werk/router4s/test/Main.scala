package com.github.werk.router4s.test

object Pages {
    sealed trait Page
    case object Home extends Page
    case class Bands(parent : Home.type) extends Page
    case class BandsLimit(limit : Int, parent : Bands) extends Page
    case class Band(name : String, parent : BandsLimit) extends Page
    case class BandEdit(parent : Band) extends Page
    case class BandAlbums(parent : Band) extends Page
    case class BandAlbum(id : Long, parent : BandAlbums) extends Page
    case class About(parent : Home.type) extends Page
}

object Main {
    import Pages._
    import com.github.werk.router4s.Router
    import com.github.werk.router4s.Router.long
    import com.github.werk.router4s.Router.int
    import com.github.werk.router4s.Extra.string

    val path = new Router[Page]

    val router = path(Home,
        path("bands", Bands,
            path.query("limit", int, BandsLimit,
                path(string, Band,
                    path("edit", BandEdit),
                    path("albums", BandAlbums,
                        path(long, BandAlbum)
                    )
                )
            )
        ),
        path("about", About)
    )

    println(router.data("/bands/Pink%20Floyd/albums/42?limit=12"))
    println(router.path(BandAlbum(42, BandAlbums(Band("Pink Floyd", BandsLimit(12, Bands(Home)))))))

    def main(args : Array[String]) : Unit = {
        val pages = List[Page](
            Home,
            Bands(Home),
            BandsLimit(12, Bands(Home)),
            Band("Pink Floyd", BandsLimit(12, Bands(Home))),
            BandEdit(Band("Pink Floyd", BandsLimit(12, Bands(Home)))),
            BandAlbums(Band("Pink Floyd", BandsLimit(12, Bands(Home)))),
            BandAlbum(42, BandAlbums(Band("Pink Floyd", BandsLimit(12, Bands(Home))))),
            About(Home)
        )

        router.prettyPaths.foreach(println)
        println()

        for(a <- pages) {
            val pathQuery = router.toPath(a).getOrElse{
                throw new RuntimeException(s"pathMap.toPath($a) = None")
            }
            val Some(a2) = router.fromPath(pathQuery)
            if(a == a2) {
                val p = Router.PathQuery.show(pathQuery)
                println(f"$p%-50s <--> $a2")
            } else {
                println(s"ERROR: $a != fromPath(toPath($a)) = $a2")
                val p = Router.PathQuery.show(pathQuery)
                println(f"    $p%-50s <--> $a2")
            }
        }
    }
}
