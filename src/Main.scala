object Pages {
    sealed trait Page
    case object Persons extends Page
    case class Person(parent : Persons.type, name : String) extends Page
    case class PersonEdit(parent : Person) extends Page
    case class PersonCars(parent : Person) extends Page
    case class PersonCar(parent : PersonCars, id : Long) extends Page
}

object Main {
    import Pages._
    val subRouter = new Router[Page]
    import subRouter._

    val router =
        constantRoot("people", Persons).apply(
            variable(string, Person).apply(
                constant("edit", PersonEdit),
                constant("cars", PersonCars).apply(
                    variable(long, PersonCar)
                )
            )
        )

    def main(args : Array[String]) : Unit = {
        val person = Person(Persons, "John Rambo")
        val pages = List[Page](Persons,
            person,
            PersonEdit(person),
            PersonCars(person),
            PersonCar(PersonCars(person), 42)
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
