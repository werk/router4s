package com.github.werk.router4s.test

import scala.reflect.ClassTag

object DslExperiment {

    case class ShowRead[A](
        show: A => Option[String],
        read: String => Option[A]
    )

    sealed trait VariableType[A]
    sealed trait VariablePathPart[A]

    case class ConstantDirectory(segment : String)
    case class RequiredType[A](showRead : ShowRead[A], name : String) extends VariableType[A] with VariablePathPart[A]
    case class OptionalType[A](requiredType : RequiredType[A]) extends VariableType[Option[A]]
    case class QueryParameter[A](name: String, variableType : VariableType[A]) extends VariablePathPart[A]


    // Smart-constructors
    def option[A](requiredVariableType : RequiredType[A]) = OptionalType[A](requiredVariableType)
    def q[A](name: String, variableType: VariableType[A]) = QueryParameter[A](name, variableType)


    // Basic (required) types
    val int : RequiredType[Int] = null
    val long : RequiredType[Long] = null
    val string : RequiredType[String] = null

    case class Router[A](){
        def child[B](part : RouterPart[A, B]) : Router[B] = ???
        def |[B](part : RouterPart[A, B]) : Router[B] = ???
    }
    case class RouterPart[A, B]() {
    }

    // PathN where N is the number of variables in the path
    trait Path0 {
        def /(constantDirectory : String) : Path0
        def /[V1](v1 : RequiredType[V1]) : Path1[V1]
        def &[V1](queryParameter : (String, VariableType[V1])) : Path1[V1]

        def router[C <: Product : ClassTag](f : C) : Router[C]
        def part[P, C <: Product : ClassTag](f : P => C) : RouterPart[P, C]

        def |[C <: Product : ClassTag](f : C) : Router[C]
        def |[P, C <: Product : ClassTag](f : P => C) : RouterPart[P, C]
    }

    val root : Path0 = ???

    trait Path1[V1] {
        def /(constantDirectory : String) : Path1[V1]
        def /[V2](v2 : RequiredType[V2]) : Path2[V1, V2]
        def &[V2](queryParameter : (String, VariableType[V2])) : Path2[V1, V2]

        def router[C <: Product : ClassTag](f : V1 => C) : Router[C]
        def part[P, C <: Product : ClassTag](f : (V1, P) => C) : RouterPart[P, C]

        def |[C <: Product : ClassTag](f : V1 => C) : Router[C]
        def |[P, C <: Product : ClassTag](f : (V1, P) => C) : RouterPart[P, C]
    }

    trait Path2[V1, V2] {
        def /(constantDirectory : String) : Path2[V1, V2]
        def /[V3](v3 : RequiredType[V3]) : Path3[V1, V2, V3]
        def &[V3](queryParameter : (String, VariableType[V3])) : Path3[V1, V2, V3]

        def router[C <: Product : ClassTag](f : (V1, V2) => C) : Router[C]
        def part[P, C <: Product : ClassTag](f : (V1, V2, P) => C) : RouterPart[P, C]

        def |[C <: Product : ClassTag](f : (V1, V2) => C) : Router[C]
        def |[P, C <: Product : ClassTag](f : (V1, V2, P) => C) : RouterPart[P, C]
    }

    trait Path3[V1, V2, V3] {
        def /(constantDirectory : String) : Path3[V1, V2, V3]
        def /[V4](v4 : RequiredType[V4]) : Path4[V1, V2, V3, V4]
        def &[V4](queryParameter : (String, VariableType[V4])) : Path4[V1, V2, V3, V4]

        def router[C <: Product : ClassTag](f : (V1, V2, V3) => C) : Router[C]
        def part[P, C <: Product : ClassTag](f : (V1, V2, V3, P) => C) : RouterPart[P, C]

        def |[C <: Product : ClassTag](f : (V1, V2, V3) => C) : Router[C]
        def |[P, C <: Product : ClassTag](f : (V1, V2, V3, P) => C) : RouterPart[P, C]
    }

    trait Path4[V1, V2, V3, V4] {
        def /(constantDirectory : String) : Path4[V1, V2, V3, V4]
        def /[V5](v4 : RequiredType[V4]) : Path5[V1, V2, V3, V4, V5]
        def &[V5](queryParameter : (String, VariableType[V5])) : Path5[V1, V2, V3, V4, V5]

        def router[C <: Product : ClassTag](f : (V1, V2, V3, V4) => C) : Router[C]
        def part[P, C <: Product : ClassTag](f : (V1, V2, V3, V4, P) => C) : RouterPart[P, C]

        def |[C <: Product : ClassTag](f : (V1, V2, V3, V4) => C) : Router[C]
        def |[P, C <: Product : ClassTag](f : (V1, V2, V3, V4, P) => C) : RouterPart[P, C]
    }

    trait Path5[V1, V2, V3, V4, V5] {
        def router[C <: Product : ClassTag](f : (V1, V2, V3, V4, V4) => C) : Router[C]
        def part[P, C <: Product : ClassTag](f : (V1, V2, V3, V4, V4, P) => C) : RouterPart[P, C]

        def |[C <: Product : ClassTag](f : (V1, V2, V3, V4, V4) => C) : Router[C]
        def |[P, C <: Product : ClassTag](f : (V1, V2, V3, V4, V4, P) => C) : RouterPart[P, C]
    }

    def path(constantDirectory : String) : Path0 = null

    def path[V1](v1 : RequiredType[V1]) : Path1[V1] = null
    def path[V1](constantDirectory : String, v1 : VariablePathPart[V1]) : Path1[V1] = null

    def path[V1, V2](v1 : RequiredType[V1], v2 : VariablePathPart[V2]) : Path2[V1, V2] = null
    def path[V1, V2](constantDirectory : String, v1 : VariablePathPart[V1], v2 : VariablePathPart[V2]) : Path2[V1, V2] = null

    def path[V1, V2, V3](v1 : RequiredType[V1], v2 : VariablePathPart[V2], v3 : VariablePathPart[V3]) : Path3[V1, V2, V3] = null
    def path[V1, V2, V3](constantDirectory : String, v1 : VariablePathPart[V1], v2 : VariablePathPart[V2], v3 : VariablePathPart[V3]) : Path3[V1, V2, V3] = null

    def path[V1, V2, V3, V4](v1 : RequiredType[V1], v2 : VariablePathPart[V2], v3 : VariablePathPart[V3], v4 : VariablePathPart[V4]) : Path4[V1, V2, V3, V4] = null
    def path[V1, V2, V3, V4](constantDirectory : String, v1 : VariablePathPart[V1], v2 : VariablePathPart[V2], v3 : VariablePathPart[V3], v4 : VariablePathPart[V4]) : Path4[V1, V2, V3, V4] = null

    def path[V1, V2, V3, V4, V5](v1 : RequiredType[V1], v2 : VariablePathPart[V2], v3 : VariablePathPart[V3], v4 : VariablePathPart[V4], v5 : VariablePathPart[V5]) : Path5[V1, V2, V3, V4, V5] = null
    def path[V1, V2, V3, V4, V5](constantDirectory : String, v1 : VariablePathPart[V1], v2 : VariablePathPart[V2], v3 : VariablePathPart[V3], v4 : VariablePathPart[V4], v5 : VariablePathPart[V5]) : Path5[V1, V2, V3, V4, V5] = null


    // Usage experiments
    case class Stuff(i : Int, l : Long, name : Option[String], limit : Int)
    case class Token(token : String)
    case class TokenStuff(i : Int, l : Long, name : Option[String], limit : Int, parent : Token)

    val path4a = path("foo", int, long, q("name", option(string)), q("limit", int))
    val path4b = path("foo") / int / long & "name" -> option(string) & "limit" -> int

    implicit class FunkyString(s : String) {
        def =?[V](v : RequiredType[V]) = (s, option(v))
        def =![V](v : RequiredType[V]) = (s, v)
        def /(constantDirectory : String) : Path0 = ???
        def /[V1](v1 : RequiredType[V1]) : Path1[V1] = ???
        def &[V1](queryParameter : (String, VariableType[V1])) : Path1[V1] = ???
    }

    val path4c = "foo" / int / long & "name" =? string & "limit" =! int
    val router = "foo" / int / long & "name" =? string & "limit" =! int | Stuff

    path4a.router(Stuff)

    ("home" & "token" =! string) | Token | (
        "foo" / int / long & "name" =? string & "limit" =! int | TokenStuff
    )

    // Usage experiment 2

    /*
    sealed trait Page
    case object Home extends Page
    case class Bands(parent : Home.type) extends Page
    case class BandsLimit(limit : Int, parent : Bands) extends Page
    case class Band(name : String, parent : BandsLimit) extends Page
    case class BandEdit(parent : Band) extends Page
    case class BandAlbums(parent : Band) extends Page
    case class BandAlbum(id : Long, parent : BandAlbums) extends Page
    case class About(parent : Home.type) extends Page

    val router = path(Home,
        path("bands", Bands,
            path.query("limit" -> int, BandsLimit,
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
     */

    // Preferred
    //sealed trait Page
    //case object Home extends Page
    //case class Bands(limit : Option[Int]) extends Page
    //case class Band(name : String, bands : Bands) extends Page
    //case class BandEdit(parent : Band) extends Page
    //case class BandAlbums(band : Band) extends Page
    //case class BandAlbum(id : Long, band : Band) extends Page
    //case object About extends Page

    sealed trait Page
    case object Home extends Page
    case class Bands(limit : Option[Int], parent : Home.type) extends Page
    case class Band(name : String, bands : Bands) extends Page
    case class BandEdit(parent : Band) extends Page
    case class BandAlbums(band : Band) extends Page
    case class BandAlbum(id : Long, band : Band) extends Page
    case object About extends Page

    root | Home | (
        "bands" & "limit" =? int | Bands
    )
}
