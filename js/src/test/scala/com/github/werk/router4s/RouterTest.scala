package com.github.werk.router4s

import utest._
import utest.framework.{Test, Tree}

object RouterTest extends TestSuite {

    sealed trait Page
    case object Home extends Page
    case class Persons(parent : Home.type) extends Page
    case class Person(name : String, parent : Persons) extends Page
    case class PersonEdit(parent : Person) extends Page
    case class PersonCars(parent : Person) extends Page
    case class PersonCar(id : Long, parent : PersonCars) extends Page
    case class About(parent : Home.type) extends Page

    import Extra.string
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

    val pages = List[Page](
        Home,
        Persons(Home),
        Person("John Rambo", Persons(Home)),
        PersonEdit(Person("John Rambo", Persons(Home))),
        PersonCars(Person("John Rambo", Persons(Home))),
        PersonCar(42, PersonCars(Person("John Rambo", Persons(Home)))),
        About(Home)
    )

    router.prettyPaths.foreach(println)
    println()


    val tests : Tree[Test] = this{
        "Forall pages: fromPath(toPath(page)) == page" - {
            for(page <- pages) {
                val path = router.toPath(page).getOrElse{
                    throw new RuntimeException(s"pathMap.toPath($page) = None")
                }
                val Some(a2) = router.fromPath(path)
                if(page == a2) {
                    val p = '/' + path.mkString("/")
                    println(f"$p%-30s <--> $a2")
                } else {
                    println(s"ERROR: $page != fromPath(toPath($page)) = $a2")
                }
                assert(page == a2)
            }
        }
    }
}
