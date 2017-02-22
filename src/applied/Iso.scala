package applied

import scala.reflect.ClassTag
import scala.util.Try
import java.net.{URLEncoder, URLDecoder}

object Iso {

    case class PathItemMap[A] (
        fromPath : PartialFunction[String, A],
        toPath : PartialFunction[A, String]
    )

    val long = PathItemMap[Long](
        fromPath = partial[String, Long]{ path : String => for {
            l <- Try{path.toLong}.toOption
            if l.toString == path
        } yield l},
        toPath = {case l => l.toString}
    )

    val string = PathItemMap[String](
        fromPath = {case path => URLDecoder.decode(path, "UTF-8")},
        toPath = {case s => URLEncoder.encode(s, "UTF-8")}
    )

    case class PathMap[A] (
        fromPath : PartialFunction[List[String], A],
        toPath : PartialFunction[A, List[String]]
    ) {
        def ups[Super >: A](implicit tag : ClassTag[A]) : PathMap[Super] = {
            PathMap[Super](
                fromPath = fromPath,
                toPath = broaden(toPath, tag.runtimeClass.asInstanceOf[Class[A]])
            )
        }

        def up[Super >: A](c : Class[_ <: A] /*TODO*/) : PathMap[Super] = {
            PathMap[Super](
                fromPath = fromPath,
                toPath = broaden(toPath, c)
            )
        }

        def orElse(that : PathMap[A]) : PathMap[A] = copy(
            fromPath = fromPath.orElse(that.fromPath),
            toPath = toPath.orElse(that.toPath)
        )

        def child[B](f : PathMap[A] => PathMap[B]) : PathMap[B] = f(this)

        def apply[B](f : PathMap[A] => PathMap[B]) : PathMap[B] = f(this)
    }

    private def broaden[A <: C, B, C](f : PartialFunction[A, B], c : Class[A]) : PartialFunction[C, B] = {
        case a if c == a.getClass && f.isDefinedAt(a.asInstanceOf[A]) => f(a.asInstanceOf[A])
    }

    // Root
    def constantRoot[A](name : String, a : A) = PathMap[A](
        fromPath = new PartialFunction[List[String], A] {
            def isDefinedAt(path : List[String]) : Boolean = path == List(name)
            def apply(path : List[String]) : A = a
        },
        toPath = {case _ => List(name) }
    )

    // Non root
    def constant[A <: Product, B](name : String, f : B => A) (parentIso : PathMap[B]) = PathMap[A](
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
    def variableRoot[I, Page <: Product](itemMap : PathItemMap[I], f : I => Page) = PathMap[Page](
        fromPath = partial[List[String], Page]{ path : List[String] => for {
            last <- path.headOption.filter(_ => path.length == 1)
            i <- itemMap.fromPath.lift(last)
        } yield f(i)}
        ,
        toPath = partial[Page, List[String]] {page =>
            val List(i : I) = page.productIterator.toList
            itemMap.toPath.lift(i).map{x =>
                List(x)
            }
        }
    )

    // Non root
    def variable[I, Page <: Product, Parent](itemMap : PathItemMap[I],f : (Parent, I) => Page) (parentMap : PathMap[Parent]) = PathMap[Page](
        fromPath = partial[List[String], Page]{ path : List[String] => for {
            init <- Try{path.init}.toOption
            last <- Try{path.last}.toOption
            p <- parentMap.fromPath.lift(init)
            i <- itemMap.fromPath.lift(last)
        } yield f(p, i)}
        ,
        toPath = partial[Page, List[String]] {page =>
            val List(parent : Parent, i : I) = page.productIterator.toList
            itemMap.toPath.lift(i).map{x =>
                parentMap.toPath(parent) ++ List(x)
            }
        }
    )

    private def partial[A, B](f : A => Option[B]) = new PartialFunction[A, B] {
        override def isDefinedAt(a : A): Boolean = f(a).isDefined
        override def apply(a : A): B = f(a).get
    }

    def test[A](iso : PathMap[A], as : A*): Unit = {
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

    def router[P](maps : PathMap[P]*) : PathMap[P] = maps.reduceLeft (_ orElse _)
}

object IsoMain {
    import Iso._

    // Page data
    sealed trait TopPage
    case object Persons extends TopPage
    case class Person(parent : Persons.type, name : String) extends TopPage
    case class PersonEdit(parent : Person) extends TopPage
    case class PersonCars(parent : Person) extends TopPage
    case class PersonCar(parent : PersonCars, id : Long) extends TopPage

    // Router: URL, page mapping
    val p1 = constantRoot("users", Persons)
    val p2 = variable(string, Person)(p1)
    val p3 = constant("edit", PersonEdit)(p2)
    val p4 = constant("cars", PersonCars)(p2)
    val p5 = variable(long, PersonCar)(p4)

    /*
    constantRoot("users", Persons) ( p1 =>
        variable(Person)(p1) ( p2 =>
            constant("edit", PersonEdit)(p2),
            constant("cars", PersonCars)(p2) ( p4 =>
                variable(PersonCar)(p4)
            )
        )
    )

    r("users", Persons) (
        r(string, Person)(
            r("edit", PersonEdit)(),
            r("cars", PersonCars)(
                r(long, PersonCar)()
            )
        )
    )

     */

    val z = constantRoot("users", Persons)(
        variable(string, Person)
    )

    val x = constantRoot("users", Persons)
        .child(variable(string, Person))
        .child(constant("cars", PersonCars))
        .child(variable(long, PersonCar))

    val r : PathMap[TopPage] = router(
        p1.up(Persons.getClass),
        p2.up(classOf[Person]),
        p3.up(classOf[PersonEdit]),
        p4.up(classOf[PersonCars]),
        p5.up(classOf[PersonCar])
    )

    def main(args : Array[String]) : Unit = {
        val person = Person(Persons, "John Rambo")
        test(r,
            Persons,
            person,
            PersonEdit(person),
            PersonCars(person),
            PersonCar(PersonCars(person), 42)
        )
    }
}

