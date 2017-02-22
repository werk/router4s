import scala.reflect.ClassTag
import scala.util.Try

object Iso {

    case class Iso[A] (
        fromPath : PartialFunction[String, A],
        toPath : PartialFunction[A, String]
    ) {
        def up[Super >: A](c : Class[_ <: A] /*TODO*/) : Iso[Super] = {
            Iso[Super](
                fromPath = fromPath,
                toPath = broaden(toPath, c)
            )
        }

        def orElse(that : Iso[A]) : Iso[A] = copy(
            fromPath = fromPath.orElse(that.fromPath),
            toPath = toPath.orElse(that.toPath)
        )
    }

    private def broaden[A <: C, B, C](f : PartialFunction[A, B], c : Class[A]) : PartialFunction[C, B] = {
        case a if c == a.getClass && f.isDefinedAt(a.asInstanceOf[A]) => f(a.asInstanceOf[A])
    }

    def singleton[A, S >: A](a : A, c : Class[_ <: A]) : Iso[S] = singleton(a, None).up(c)
    def singleton[A, S >: A](a : A, c : Class[_ <: A], name : String) : Iso[A] = singleton(a, Some(name)).up(c)

    private def singleton[A](a : A, name : Option[String]) : Iso[A] = {
        val path = name.getOrElse(defaultPath(a))
        Iso[A](
            fromPath = new PartialFunction[String, A] {
                def isDefinedAt(p : String) : Boolean = p == path
                def apply(p : String) : A = a
            },
            toPath = {case _ => path }
        )
    }

    def string[A <: Product, S >: A](f : {def apply(path : String) : A}, c : Class[A], prefix : String) : Iso[S] = Iso[A](
        fromPath = new PartialFunction[String, A]{
            def isDefinedAt(path : String) : Boolean = path.startsWith(prefix)
            def apply(path : String) : A = f(path.drop(prefix.length))
        },
        toPath = {case a =>
            val List(s) = a.productIterator.toList
            prefix + s
        }
    ).up(c)

    def long[A <: Product, S >: A](f : {def apply(path : Long) : A}, c : Class[A]) : Iso[S] = Iso[A](
        fromPath = new PartialFunction[String, A]{
            def isDefinedAt(path : String) : Boolean = Try {
                path.toLong
            }.toOption.exists(_.toString == path)
            def apply(path : String) : A = f(path.toLong)
        },
        toPath = {case a =>
            val List(l) = a.productIterator.toList
            l.toString
        }
    ).up(c)

    private def defaultPath[A](a : A) = a.getClass.getName.reverse.dropWhile(_ == '$').takeWhile(_ != '$').reverse

    def test[A](iso : Iso[A], a : A): Unit = {
        println()
        val path = iso.toPath(a)
        println(s"toPath($a) = $path")
        val a2 = iso.fromPath(path)
        println(s"fromPath($path) = $a2")
        println(s"Correct: ${a == a2}")
    }

    def router[P](isos : Iso[P]*) : Iso[P] = isos.reduceLeft (_ orElse _)
}

object IsoMain {
    import Iso._

    sealed trait IsoPage
    case object Persons extends IsoPage
    case class Person(name : String) extends IsoPage
    case class PersonCars() extends IsoPage
    case class PersonCar(id : Long) extends IsoPage

    // /people/JohnDoe/cars/42

    def main(args : Array[String]) : Unit = {

        val all = router[IsoPage](
            singleton(Persons, Persons.getClass),
            string(Person, classOf[Person], "person-"),
            long(PersonCar, classOf[PersonCar])
        )

        test(all, Persons)
        test(all, Person("JohnDoe"))
        test(all, PersonCar(1337))

    }

}
