package sub

import scala.util.Try
import java.net.{URLDecoder, URLEncoder}
import scala.reflect.ClassTag

class SubRouter[TopPage : ClassTag] {

    case class PathItemMap[A] (
        fromPath : String => Option[A],
        toPath : A => Option[String],
        name : String
    )

    val long = PathItemMap[Long](
        fromPath = { path : String => for {
            l <- Try{path.toLong}.toOption
            if l.toString == path
        } yield l},
        toPath = {l => Some(l.toString)},
        name = "long"
    )

    val string = PathItemMap[String](
        fromPath = {path => Some(URLDecoder.decode(path, "UTF-8"))},
        toPath = {s => Some(URLEncoder.encode(s, "UTF-8"))},
        name = "string"
    )

    case class SubPathMap[A <: TopPage : ClassTag, B <: TopPage : ClassTag] (
        f : PathMap[A] => PathMap[B]
    ) {
        def apply[C <: TopPage : ClassTag] (child : SubPathMap[B, C]) : SubPathMap[A, TopPage] = {
            val g : PathMap[A] => PathMap[TopPage] = {a =>
                val b = f(a)
                val c = child.f(b)
                b.up.orElse(c.up)
            }
            SubPathMap[A, TopPage](g)
        }

        def apply[C <: TopPage : ClassTag, D <: TopPage : ClassTag] (child1 : SubPathMap[B, C], child2 : SubPathMap[B, D]) : SubPathMap[A, TopPage] = {
            val g : PathMap[A] => PathMap[TopPage] = {a =>
                val b = f(a)
                val c = child1.f(b)
                val d = child2.f(b)
                b.up.orElse(c.up.orElse(d.up))
            }
            SubPathMap[A, TopPage](g)
        }

    }

    case class PathMap[A <: TopPage] (
        fromPath : List[String] => Option[A],
        toPath : A => Option[List[String]],
        prettyPaths : List[String]
    ) {
        def apply(subMap: SubPathMap[A, TopPage])(implicit classTagA : ClassTag[A]) : PathMap[TopPage] = {
            this.up.orElse(subMap.f(this))
        }

        def up(implicit classTagA : ClassTag[A]) : PathMap[TopPage] = {
            // Make it possible to call this method twice
            if(classTagA == scala.reflect.classTag[TopPage]) {
                return this.asInstanceOf[PathMap[TopPage]]
            }
            PathMap[TopPage](
                fromPath = fromPath,
                toPath = broaden(toPath, classTagA),
                prettyPaths = prettyPaths
            )
        }

        def orElse(that : PathMap[A]) : PathMap[A] = copy(
            fromPath = orElseO(fromPath, that.fromPath),
            toPath = orElseO(toPath, that.toPath),
            prettyPaths = prettyPaths ++ that.prettyPaths
        )
    }

    private def orElseO[A, B, A1 <: A, B1 >: B](f: A => Option[B], g: A1 => Option[B1]): A1 => Option[B1] = {a1 =>
        f(a1).orElse(g(a1))
    }

    private def broaden[A <: C, B, C](f : A => Option[B], classTagA : ClassTag[A]) : C => Option[B] = {a : C =>
        if(a.getClass == classTagA.runtimeClass) f(a.asInstanceOf[A])
        else None
    }

    // Root
    def constantRoot[A <: TopPage](name : String, a : A) = PathMap[A](
        fromPath = { path =>
            Some(a).filter(_ => path == List(name))
        },
        toPath = {_ => Some(List(name))},
        prettyPaths = List(s"'$name'->$a")
    )

    // Non root
    def constant[Page <: TopPage with Product : ClassTag, Parent <: TopPage : ClassTag](name : String, f : Parent => Page) = SubPathMap[Parent, Page]({ parentMap: PathMap[Parent] =>
        PathMap[Page](
            fromPath = { path: List[String] =>
                for {
                    init <- Try {
                        path.init
                    }.toOption
                    last <- Try {
                        path.last
                    }.toOption
                    p <- parentMap.fromPath(init)
                    if last == name
                } yield f(p)
            },
            toPath = { a =>
                val List(p: Parent) = a.productIterator.toList
                parentMap.toPath(p).map(_ ++ List(name))
            },
            prettyPaths = parentMap.prettyPaths.map(_ + s" / '$name'->${scala.reflect.classTag[Page].runtimeClass.getSimpleName}")
        )
    })

    // Root
    def variableRoot[I, Page <: TopPage with Product : ClassTag](itemMap : PathItemMap[I], f : I => Page) = PathMap[Page](
        fromPath = { path : List[String] =>
            for {
                last <- path.headOption.filter(_ => path.length == 1)
                i <- itemMap.fromPath(last)
            } yield f(i)
        },
        toPath = {page =>
            val List(i : I) = page.productIterator.toList
            itemMap.toPath(i).map{x => List(x)}
        },
        prettyPaths = List(s"${itemMap.name}->${scala.reflect.classTag[Page].runtimeClass.getSimpleName}")
    )

    // Non root
    def variable[I, Page <: TopPage with Product : ClassTag, Parent <: TopPage : ClassTag](itemMap : PathItemMap[I],f : (Parent, I) => Page) = SubPathMap[Parent, Page]({ parentMap: PathMap[Parent] =>
        PathMap[Page](
            fromPath = { path =>
                for {
                    init <- Try {
                        path.init
                    }.toOption
                    last <- Try {
                        path.last
                    }.toOption
                    p <- parentMap.fromPath(init)
                    i <- itemMap.fromPath(last)
                } yield f(p, i)
            },
            toPath = { page =>
                val List(parent: Parent, i: I) = page.productIterator.toList
                for {
                    parentPath <- parentMap.toPath(parent)
                    itemPath <- itemMap.toPath(i)
                } yield parentPath ++ List(itemPath)
            },
            prettyPaths = parentMap.prettyPaths.map(_ + s" / ${itemMap.name}->${scala.reflect.classTag[Page].runtimeClass.getSimpleName}")
        )
    })

    def test[A <: TopPage](pathMap : PathMap[A], as : A*): Unit = {
        pathMap.prettyPaths.foreach(println)
        println
        for(a <- as) {
            val path = pathMap.toPath(a).getOrElse{
                throw new RuntimeException(s"pathMap.toPath($a) = None")
            }
            val Some(a2) = pathMap.fromPath(path)
            if(a == a2) {
                val p = '/' + path.mkString("/")
                println(f"$p%-30s <--> $a2")
            } else {
                println(s"ERROR: $a != fromPath(toPath($a)) = $a2")
            }
        }
    }
}


object SubMain {
    val subRouter = new SubRouter[TopPage]
    import subRouter._

    // Page data
    sealed trait TopPage
    case object Persons extends TopPage
    case class Person(parent : Persons.type, name : String) extends TopPage
    case class PersonEdit(parent : Person) extends TopPage
    case class PersonCars(parent : Person) extends TopPage
    case class PersonCar(parent : PersonCars, id : Long) extends TopPage

    val r =
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
        test(r,
            Persons,
            person,
            PersonEdit(person),
            PersonCars(person),
            PersonCar(PersonCars(person), 42)
        )
    }
}

