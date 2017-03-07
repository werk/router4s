# Router4s

Router4s is a URL path routing library for Scala and Scala.js. It let you map your URL paths to datatypes, and back again. This is usefull for routing URLs in single-page webapps and it provides a way to ensure valid link generation.

This router was initially intended as a suplement to [React4s](https://github.com/Ahnfelt/react4s) (a simple React wrapper for Scala), but the project is self-contained and independent.

# Example

Say we are building a simple music database with the following kind of URLs

```
/
/bands
/bands/Pink+Floyd
/bands/Pink+Floyd/edit
/bands/Pink+Floyd/albums
/bands/Pink+Floyd/albums/42
/about 
```

We then define the following sum type, where each case object/class correstponds, in order, to the URLs above

```scala
sealed trait Page
case object Home extends Page
case class Bands(parent : Home.type) extends Page
case class Band(name : String, parent : Bands) extends Page
case class BandEdit(parent : Band) extends Page
case class BandAlbums(parent : Band) extends Page
case class BandAlbum(id : Long, parent : BandAlbums) extends Page
case class About(parent : Home.type) extends Page
```

All the case classes must take a value called `parent` as the last parameter. This is the page that represents the parent path. `Band` and `BandAlbum` takes another value as the first argument, as they represent variable path items.

We are not able to build the router that will provide the mapping functions between our URLs and `Page`.
```scala
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
```

The above lines are a bit tedious as they only let us choose the constant path items. But we now have a router, and it was build without reflection or macros, for anyone who care. Now let's give it a try.

```scala
# router.fromPath("/bands/Pink+Floyd/albums/42")
> Some(BandAlbum(42,BandAlbums(Band(Pink+Floyd,Bands(Home)))))

# router.toPath(BandAlbum(42, BandAlbums(Band("Pink Floyd", Bands(Home)))))
> Some(List(bands, Pink Floyd, albums, 42))
```
