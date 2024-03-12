package com.github.werk.router4s

import Extra.string

import scala.scalajs.js.annotation.JSExportTopLevel

object TestApp {
    import Pages._
    import Router.long

    val path = new Router[Page]

    val router = path(Home,
        path("people", Persons,
            path(string, Person,
                path("edit", PersonEdit),
                path("cars", PersonCars,
                    path(long, PersonCar)
                )
            )
        ),
        path("about", About)
    )

    @JSExportTopLevel("TestApp")
    def main() : Unit = {
        val person = Person("John Rambo", Persons(Home))
        val pages = List[Page](
            Home,
            Persons(Home),
            person,
            PersonEdit(person),
            PersonCars(person),
            PersonCar(42, PersonCars(Person("John Rambo", Persons(Home)))),
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

object Pages {
    sealed trait Page
    case object Home extends Page
    case class Persons(parent : Home.type) extends Page
    case class Person(name : String, parent : Persons) extends Page
    case class PersonEdit(parent : Person) extends Page
    case class PersonCars(parent : Person) extends Page
    case class PersonCar(id : Long, parent : PersonCars) extends Page
    case class About(parent : Home.type) extends Page
}

