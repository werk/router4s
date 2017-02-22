package nested

import scala.reflect.ClassTag
import scala.util.Try

object Iso {

    case class Iso[A] (
        fromPath : PartialFunction[List[String], A],
        toPath : PartialFunction[A, List[String]]
    ) {
        def ups[Super >: A](implicit tag : ClassTag[A]) : Iso[Super] = {
            Iso[Super](
                fromPath = fromPath,
                toPath = broaden(toPath, tag.runtimeClass.asInstanceOf[Class[A]])
            )
        }

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

    // Root
    def constant[A](name : String, a : A) = Iso[A](
        fromPath = new PartialFunction[List[String], A] {
            def isDefinedAt(path : List[String]) : Boolean = path == List(name)
            def apply(path : List[String]) : A = a
        },
        toPath = {case _ => List(name) }
    )

    // Non root
    def constant[A <: Product, B](name : String, f : B => A, parentIso : Iso[B]) = Iso[A](
        fromPath = partial[List[String], A]{ path : List[String] => for {
            init <- Try{path.init}.toOption
            last <- Try{path.last}.toOption
            p <- parentIso.fromPath.lift(init)
            if last == name
        } yield f(p)}
        ,
        toPath = {case a =>
            val List(p : B) = a.productIterator.toList
            parentIso.toPath(p) ++ List(name)
        }
    )

    // Root
    def variable[A <: Product](f : Long => A) = Iso[A](
        fromPath = partial[List[String], A]{ path : List[String] => for {
            last <- path.headOption.filter(_ => path.length == 1)
            l <- Try{last.toLong}.toOption
            if l.toString == last
        } yield f(l)}
        ,
        toPath = {case a =>
            val List(l) = a.productIterator.toList
            List(l.toString)
        }
    )

    // Non root
    def variable[A <: Product, B](f : (B, Long) => A, parentIso : Iso[B]) = Iso[A](
        fromPath = partial[List[String], A]{ path : List[String] => for {
            init <- Try{path.init}.toOption
            last <- Try{path.last}.toOption
            p <- parentIso.fromPath.lift(init)
            l <- Try{last.toLong}.toOption
            if l.toString == last
        } yield f(p, l)}
        ,
        toPath = {case a =>
            val List(p : B, l) = a.productIterator.toList
            parentIso.toPath(p) ++ List(l.toString)
        }
    )

    private def defaultPath[A](a : A) = a.getClass.getName.reverse.dropWhile(_ == '$').takeWhile(_ != '$').reverse

    private def partial[A, B](f : A => Option[B]) = new PartialFunction[A, B] {
        override def isDefinedAt(a : A): Boolean = f(a).isDefined
        override def apply(a : A): B = f(a).get
    }

    def test[A](iso : Iso[A], as : A*): Unit = {
        for(a <- as) {
            val path = iso.toPath(a)
            val a2 = iso.fromPath(path)
            if(a == a2) {
                val p = '/' + path.mkString("/")
                println(f"$p%-30s <--> $a2")
            } else {
                println(s"ERROR: $a != fromPath(toPath($a)) = $a2")
            }
        }
    }

    def router[P](isos : Iso[P]*) : Iso[P] = isos.reduceLeft (_ orElse _)
}

object IsoMain {
    import Iso._

    // Page data
    sealed trait IsoPage
    case object Persons extends IsoPage
    case class Person(parent : Persons.type, name : Long) extends IsoPage
    case class PersonCars(parent : Person) extends IsoPage
    case class PersonCar(parent : PersonCars, id : Long) extends IsoPage

    case object Companies extends IsoPage
    case class Company(parent : Companies.type, name : Long) extends IsoPage
    case class CompanyCars(parent : Company) extends IsoPage
    case class CompanyCar(parent : CompanyCars, id : Long) extends IsoPage
    case class CompanyEmployees(parent : Company) extends IsoPage
    case class CompanyEmployee(parent : CompanyEmployees, id : Long) extends IsoPage

    // Router: URL, page mapping
    val p1 = constant("people", Persons)
    val p2 = variable(Person, p1)
    val p3 = constant("cars", PersonCars, p2)
    val p4  = variable(PersonCar, p3)

    val c1 = constant("companies", Companies)
    val c2 = variable(Company, c1)
    val c3 = constant("cars", CompanyCars, c2)
    val c4  = variable(CompanyCar, c3)
    val c5 = constant("employees", CompanyEmployees, c2)
    val c6 = variable(CompanyEmployee, c5)

    val r : Iso[IsoPage] = router(
        p1.up(Persons.getClass),
        p2.up(classOf[Person]),
        p3.up(classOf[PersonCars]),
        p4.up(classOf[PersonCar]),
        c1.up(Companies.getClass),
        c2.ups,
        c3.up(classOf[CompanyCars]),
        c4.up(classOf[CompanyCar]),
        c5.up(classOf[CompanyEmployees]),
        c6.up(classOf[CompanyEmployee])
    )

    def main(args : Array[String]) : Unit = {
        test(r,
            Persons,
            Person(Persons, 1337),
            PersonCars(Person(Persons, 1337)),
            PersonCar(PersonCars(Person(Persons, 1337)), 42),
            Companies,
            Company(Companies, 101),
            CompanyCars(Company(Companies, 101)),
            CompanyCar(CompanyCars(Company(Companies, 101)), 17),
            CompanyEmployees(Company(Companies, 101)),
            CompanyEmployee(CompanyEmployees(Company(Companies, 101)), 1337)
        )
    }
}

