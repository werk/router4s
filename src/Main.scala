object Pages {
    sealed trait Page
    case object Home extends Page
    case class Persons(parent : Home.type) extends Page
    case class Person(name : String, parent : Persons) extends Page
    case class PersonEdit(parent : Person) extends Page
    case class PersonCars(parent : Person) extends Page
    case class PersonCar(id : Long, parent : PersonCars) extends Page
}

object Main {
    import Pages._
    import Router.{string, long}

    val path = new Router[Page]

    val router = path(Home).apply(
        path("people", Persons).apply(
            path(string, Person).apply(
                path("edit", PersonEdit),
                path("cars", PersonCars).apply(
                    path(long, PersonCar)
                )
            )
        )
    )

    def main(args : Array[String]) : Unit = {
        val person = Person("John Rambo", Persons(Home))
        val pages = List[Page](
            Home,
            Persons(Home),
            person,
            PersonEdit(person),
            PersonCars(person),
            PersonCar(42, PersonCars(Person("John Rambo", Persons(Home))))
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
